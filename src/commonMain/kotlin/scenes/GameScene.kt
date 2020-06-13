package scenes

import com.soywiz.kds.Pool
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.delay
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Point
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import views.Bomb
import views.Enemy
import views.Player
import views.Score
import kotlin.random.Random

class GameScene: Scene() {

    private val fieldMargin = 15
    private var paused = false

    private lateinit var player: Player
    private lateinit var lights: Image
    private lateinit var score: Score
    private lateinit var bg: Image

    private var teleportPeriod = 8.seconds
    private var teleportTimer = 0.seconds

    private var newEnemyPeriod = 3.seconds
    private var newEnemyTimer = 0.seconds
    private val numberOfEnemies:Int = 20
    private val numberOfInitialEnemies:Int = 5
    private var enemiesIndex:Int = 0

    private lateinit var enemies: Pool<Enemy>
    private val activeEnemies: MutableList<Enemy> = mutableListOf()

    override suspend fun Container.sceneInit() {

        bg = image(resourcesVfs["graphics/game_scene/bg.png"].readBitmap()){
            smoothing = false
            tint = Colors.DARKGRAY
        }

        score = Score()
        score.loadScore()
        addChild(score)

        player = Player()
        player.loadPlayer()
        player.position(views.virtualWidth / 2, views.virtualHeight / 2)
        addChild(player.bomb)
        addChild(player)

        enemies = Pool( { it.resetEnemy() }, numberOfEnemies, fun(it: Int) : Enemy{
            var enemy = Enemy(Point(Random.nextInt(views.virtualWidth), Random.nextInt(views.virtualHeight)).normalized)
            GlobalScope.launch {
                enemy.loadEnemy()
            }
            return enemy
        })

        lights = image(resourcesVfs["graphics/game_scene/lights_glow.png"].readBitmap()){
            tint = Colors.LIGHTPINK
            alpha = 0.5
            blendMode = BlendMode.HARDLIGHT
        }
        enemiesIndex = sceneView.numChildren

        addUpdater{ if(!paused) { update(it) } }
    }

    override suspend fun sceneAfterInit() {
        super.sceneAfterInit()
        player.live()
        repeat(numberOfInitialEnemies){
            delay(.5.seconds)
            createEnemy(0.seconds)
        }
    }

    private fun update(dt: TimeSpan): Unit{

        checkInput(dt)

        newEnemyTimer += dt
        if( newEnemyTimer.seconds >= Random.nextDouble(newEnemyPeriod.seconds / 2, newEnemyPeriod.seconds)) {
            createEnemy(dt)
            newEnemyTimer -= newEnemyPeriod
        }

        teleportTimer += dt
        if( teleportTimer.seconds >= teleportPeriod.seconds) {
            player.isTeleportActive = true
            teleportTimer -= teleportPeriod
        }

        checkActiveEnemies(dt)
        score.addTime(dt)
    }

    private fun checkInput(dt: TimeSpan) {

        if (player.state == Player.State.MOVING) {

            if (views.input.keys[Key.LEFT]) {
                if (player.x > fieldMargin) player.x -= player.moveSpeed * dt.seconds
            }

            if (views.input.keys[Key.RIGHT]) {
                if (player.x < views.virtualWidth - fieldMargin) player.x += player.moveSpeed * dt.seconds
            }

            if (views.input.keys[Key.UP]) {
                if (player.y > fieldMargin) player.y -= player.moveSpeed * dt.seconds
            }

            if (views.input.keys[Key.DOWN]) {
                if (player.y < views.virtualHeight - fieldMargin) player.y += player.moveSpeed * dt.seconds
            }

            if (views.input.keys[Key.SPACE] && player.bomb.state == Bomb.State.READY) player.dropBomb(player.x, player.y)
            if (views.input.keys[Key.X] && player.bomb.state == Bomb.State.READY && player.isTeleportActive) teleportPlayer()

        }

    }

    private fun teleportPlayer() {
        if (activeEnemies.isNotEmpty()){
            val enemy = activeEnemies.first()
            player.teleport(enemy.x, enemy.y){
                infectEnemy(enemy)
                player.isTeleportActive = false
            }
        }
    }

    private fun infectEnemy(enemy: Enemy) {
        enemy.infect(){
            activeEnemies.remove(enemy)
            sceneView.removeChild(enemy)
            enemies.free(enemy)
            score.addAditionalPoints(3)
        }
    }

    private fun killEnemy(enemy: Enemy) {
        enemy.die(){
            activeEnemies.remove(enemy)
            sceneView.removeChild(enemy)
            enemies.free(enemy)
            score.addAditionalPoints(2)
        }
    }

    private fun createEnemy(dt: TimeSpan) {
        val newEnemy = enemies.alloc()
        newEnemy.position(
                Random.nextInt(fieldMargin, views.virtualWidth - fieldMargin),
                Random.nextInt(fieldMargin, views.virtualHeight- fieldMargin))
        sceneView.addChildAt(newEnemy, enemiesIndex-1)
        activeEnemies.add(newEnemy)
        newEnemy.live()
    }

    private fun checkActiveEnemies(dt: TimeSpan){
        val iterator = activeEnemies.iterator()
        while(iterator.hasNext()){

            try {
                val enemy = iterator.next()

                if(enemy.state == Enemy.State.MOVING && player.state == Player.State.MOVING){
                    if(enemy.collidesWith(player, CollisionKind.GLOBAL_RECT)){
                        paused = true
                        sceneContainer.changeToAsync<LoadingProxyScene>(
                                LoadingProxyScene.NextScreen(MainScene::class),
                                time = .5.seconds
                        )
                    }else if(enemy.collidesWith(player.bomb, CollisionKind.GLOBAL_RECT) && player.bomb.state == Bomb.State.EXPLOTING){
                        killEnemy(enemy)
                    }

                    val vector = Point(player.x - enemy.x, player.y - enemy.y)
                    if(player.state == Player.State.MOVING)
                    if(vector.magnitude <= enemy.radius){
                        enemy.x += vector.normalized.x * enemy.moveSpeed * dt.seconds
                        enemy.y += vector.normalized.y * enemy.moveSpeed * dt.seconds
                    } else {
                        enemy.x += enemy.direction.x * enemy.moveSpeed * dt.seconds
                        enemy.y += enemy.direction.y * enemy.moveSpeed * dt.seconds
                    }

                    if(enemy.x > views.virtualWidth - fieldMargin || enemy.x <= fieldMargin ) enemy.direction.x *= -1
                    else if(enemy.y > views.virtualHeight - fieldMargin || enemy.y <= fieldMargin) enemy.direction.y *= -1
                }
            }catch (e: ConcurrentModificationException) { break }
        }
    }
}
