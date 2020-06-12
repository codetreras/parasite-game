package scenes

import com.soywiz.kds.Pool
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.input.onOver
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.Point
import views.Bomb
import views.Enemy
import views.Player
import kotlin.random.Random

class GameScene: Scene() {

    private var paused = false

    private val numberOfEnemies:Int = 10
    private val activeEnemies: MutableList<Enemy> = mutableListOf()
    private var newEnemyTimer = 0.seconds
    private var newEnemyPeriod = 3.seconds
    private var targetEnemy: Enemy? = null

    private lateinit var player: Player
    private lateinit var enemies: Pool<Enemy>

    override suspend fun Container.sceneInit() {
        player = Player()
        addChild(player.bomb)
        enemies = Pool( { it.resetEnemy() },numberOfEnemies, {
            Enemy(Point(Random.nextInt(views.virtualWidth), Random.nextInt(views.virtualHeight)).normalized).apply {
                onOver {
                    targetEnemy?.alpha = 1.0
                    alpha = 0.8
                    targetEnemy = this
                }
            }})

        addChild(player)

        addUpdater{ if(!paused) { update(it) } }
    }

    private fun update(dt: TimeSpan): Unit{

        checkInput(dt)
        createEnemy(dt)
        checkActiveEnemies(dt)
    }

    private fun checkInput(dt: TimeSpan) {
        if (views.input.keys[Key.A]) player.x -= player.speed * dt.seconds
        if (views.input.keys[Key.D]) player.x += player.speed * dt.seconds
        if (views.input.keys[Key.W]) player.y -= player.speed * dt.seconds
        if (views.input.keys[Key.S]) player.y += player.speed * dt.seconds

        if (views.input.keys[Key.SPACE] && player.bomb.state == Bomb.State.READY) {
            teleportPlayer(targetEnemy)
        }
    }

    private fun teleportPlayer(enemy: Enemy?) {
        targetEnemy = null
        player.dropBomb(player.x, player.y)
        enemy?.let {
            player.teleport(enemy.x, enemy.y)
            killEnemy(enemy)
        }
    }

    private fun killEnemy(enemy: Enemy) {
        enemy.die(){
            enemies.free(enemy)
            activeEnemies.remove(enemy)
            sceneContainer.removeChild(enemy)
        }
    }

    private fun createEnemy(dt: TimeSpan) {
        newEnemyTimer += dt

        if( newEnemyTimer.seconds >= Random.nextDouble(newEnemyPeriod.seconds / 2, newEnemyPeriod.seconds)) {
            val newEnemy = enemies.alloc()
            newEnemy.position(Random.nextInt(views.virtualWidth), Random.nextInt(views.virtualHeight))
            sceneContainer.addChild(newEnemy)
            activeEnemies.add(newEnemy)
            newEnemy.live()
            println("Is alive")
            newEnemyTimer -= newEnemyPeriod
        }
    }

    private fun checkActiveEnemies(dt: TimeSpan){

        activeEnemies.forEach { enemy ->
            if(enemy.state == Enemy.State.MOVING){

                if(enemy.collidesWith(player)){
                    paused = true
                }else if(enemy.collidesWith(player.bomb) && player.bomb.state == Bomb.State.EXPLOTING){
                    killEnemy(enemy)
                }

                val vector = Point(player.x - enemy.x, player.y - enemy.y)
                if(vector.magnitude <= enemy.radius){
                    enemy.x += vector.normalized.x * enemy.speed * dt.seconds
                    enemy.y += vector.normalized.y * enemy.speed * dt.seconds
                } else {
                    enemy.x += enemy.direction.x * enemy.speed * dt.seconds
                    enemy.y += enemy.direction.y * enemy.speed * dt.seconds
                }

                if(enemy.x > views.virtualWidth || enemy.x <= 0 ) enemy.direction.x *= -1
                else if(enemy.y > views.virtualHeight || enemy.y <= 0) enemy.direction.y *= -1
            }
        }
    }
}
