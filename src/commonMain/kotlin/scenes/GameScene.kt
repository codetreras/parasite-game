package scenes

import com.soywiz.kds.Pool
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.NativeSoundChannel
import com.soywiz.korau.sound.readMusic
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Point
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import views.Bomb
import views.Enemy
import views.Player
import views.Score
import kotlin.math.min
import kotlin.random.Random

class GameScene: Scene() {

    private val fieldMargin = 15
    private var paused = false
    private lateinit var player: Player

    private lateinit var lights: Image
    private lateinit var score: Score
    private lateinit var bg: Image
    private lateinit var gameOverPanel: GameOverView

    private lateinit var pointSound: NativeSound
    private lateinit var bgMusic: NativeSoundChannel

    private var teleportPeriod = 8.seconds
    private var teleportTimer = 0.seconds

    private var dificultTimer = 0.seconds
    private var increaseDificultPeriod = 20.seconds

    private var newHordRestPeriod = 4.seconds
    private var newHordTimer = 0.seconds
    private val numberOfEnemies:Int = 30
    private var hordNumberEnemies:Int = 3
    private var enemiesIndex:Int = 0

    private lateinit var enemies: Pool<Enemy>
    private val activeEnemies: MutableList<Enemy> = mutableListOf()

    override suspend fun Container.sceneInit() {
        bgMusic = resourcesVfs["sounds/ingame_music_1.mp3"].readMusic().playForever()
        bgMusic.volume = 0.0
        pointSound = resourcesVfs["sounds/fx/point.mp3"].readSound()
        pointSound.volume -= 1
        bg = image(resourcesVfs["graphics/game_scene/bg.png"].readBitmap()){
            smoothing = false
            tint = Colors.DARKGRAY
        }

        score = Score()
        score.loadScore()
        addChild(score)

        player = Player()
        player.loadPlayer()
        player.position(1 + views.virtualWidth / 2, 146)
        addChild(player.bomb)
        addChild(player)

        enemies = Pool( { it.resetEnemy() }, numberOfEnemies, fun(it: Int) : Enemy{
            var enemy = Enemy(Point(Random.nextInt(views.virtualWidth), Random.nextInt(views.virtualHeight)).normalized)
            CoroutineScope(coroutineContext).launch {
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

        gameOverPanel = GameOverView(sceneContainer)
        gameOverPanel.position(views.virtualWidth, 0.0)
        gameOverPanel.loadPanel()
        addChild(gameOverPanel)

        addUpdater{ update(it) }
    }

    override suspend fun sceneAfterInit() {
        super.sceneAfterInit()
        sceneContainer.tween(bgMusic::volume[0.8], time = 2.seconds)
        player.live()
        repeat(hordNumberEnemies){
            delay(.5.seconds)
            createEnemy()
        }
    }

    private fun update(dt: TimeSpan): Unit{

        checkInput(dt)

        if(paused) return

        if(activeEnemies.isEmpty()){
            newHordTimer += dt
        }

        if( newHordTimer.seconds >= newHordRestPeriod.seconds) {
            CoroutineScope(coroutineContext).launch {
                repeat(hordNumberEnemies){
                    delay(.5.seconds)
                    createEnemy()
                }
            }
            newHordTimer = 0.seconds
        }

        teleportTimer += dt
        if( teleportTimer.seconds >= teleportPeriod.seconds) {
            player.isTeleportActive = true
            teleportTimer = 0.seconds
        }

        dificultTimer += dt
        if( dificultTimer.seconds >= increaseDificultPeriod.seconds) {
            hordNumberEnemies += 1
            dificultTimer = 0.seconds
            increaseDificultPeriod -= 1.seconds
        }

        checkActiveEnemies(dt)
    }

    private fun checkInput(dt: TimeSpan) {

        if (views.input.keys.justReleased(Key.P)) paused = !paused

        if (paused) return

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

            if (views.input.keys[Key.X] && player.isTeleportActive) teleportPlayer()


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
        pointSound.play()
        enemy.die(){
            activeEnemies.remove(enemy)
            sceneView.removeChild(enemy)
            enemies.free(enemy)
            score.addAditionalPoints(2)
        }
    }

    private fun createEnemy() {
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
                        player.die {
                            CoroutineScope(coroutineContext).launchImmediately {
                                sceneView.tween(bgMusic::volume[0.0], time = .5.seconds)
                                bgMusic.stop()
                                gameOverPanel.tween(gameOverPanel::x[gameOverPanel.x-gameOverPanel.width], time = .3.seconds, easing = Easing.EASE_OUT)
                            }
                        }
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

    override suspend fun sceneBeforeLeaving() {
        sceneContainer.tween(bgMusic::volume[0.0], time = .4.seconds)
        bgMusic.stop()
        super.sceneBeforeLeaving()
    }
}
