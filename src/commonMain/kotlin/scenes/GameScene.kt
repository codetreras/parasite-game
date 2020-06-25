package scenes

import com.soywiz.kds.Pool
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSoundChannel
import com.soywiz.korau.sound.readMusic
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
import views.*
import kotlin.random.Random

class GameScene : Scene() {
    
    private var paused = false
    private val fieldMargin = 15
    
    private var shakeScreen: Boolean = false
    private var shakeScreenTimer = 0.seconds
    private var shakeScreenDuration = .6.seconds
    
    private lateinit var bg: Image
    private lateinit var lights: Image
    private lateinit var teleportSymbol: Image
    
    private lateinit var scorePanel: Score
    private lateinit var player: Player
    private lateinit var gameOverPanel: GameOverPanel
    
    private lateinit var bgMusic: NativeSoundChannel
    
    private var teleportActivationPeriod = 5.seconds
    private var teleportTimer = 0.seconds
    
    private var newHordeTimer = 0.seconds
    private var numberOfHordes: Int = 0
    private val enemiesPoolSize: Int = 30
    private val restPeriodBetweenHordes = 3.seconds
    private var enemiesPerHorde: Int = 1
    
    private var enemiesZIndex: Int = 0
    private lateinit var enemies: Pool<Enemy>
    private val activeEnemies: MutableList<Enemy> = mutableListOf()
    
    override suspend fun Container.sceneInit() {
        
        bg = image(resourcesVfs["graphics/game_scene/bg.png"].readBitmap()) {
            smoothing = false
            tint = Colors.DARKGRAY
        }
        
        teleportSymbol = image(resourcesVfs["graphics/game_scene/teleport_symbol.png"].readBitmap()) {
            smoothing = false
            anchor(.5, .5)
            scale = .8
            position(views.virtualWidth / 2, 146)
            tint = Colors.DARKMAGENTA
        }
        
        scorePanel = Score()
        scorePanel.loadScore()
        addChild(scorePanel)
        
        player = Player()
        player.loadPlayer()
        player.position(1 + views.virtualWidth / 2, 146)
        addChild(player)
        addChild(player.bomb)
        
        enemies = Pool({ it.resetEnemy() }, enemiesPoolSize, fun(_: Int): Enemy {
            val enemy = Enemy(Point(Random.nextInt(views.virtualWidth), Random.nextInt(views.virtualHeight)).normalized)
            CoroutineScope(coroutineContext).launchImmediately {
                enemy.loadEnemy()
            }
            return enemy
        })
        
        lights = image(resourcesVfs["graphics/game_scene/lights_glow.png"].readBitmap()) {
            tint = Colors.LIGHTPINK
            alpha = 0.5
            blendMode = BlendMode.HARDLIGHT
        }
        
        enemiesZIndex = sceneView.numChildren
        
        gameOverPanel = GameOverPanel(sceneContainer)
        gameOverPanel.position(views.virtualWidth, 0.0)
        gameOverPanel.loadPanel()
        addChild(gameOverPanel)
        
        addUpdater { update(it) }
    }
    
    override suspend fun sceneAfterInit() {
        super.sceneAfterInit()
        bgMusic = resourcesVfs["sounds/ingame_music_1.mp3"].readMusic().playForever()
        bgMusic.volume = 0.0
        sceneContainer.tween(bgMusic::volume[0.8], time = 1.5.seconds)
        player.live()
    }
    
    private fun update(dt: TimeSpan) {
        
        checkInput(dt)
        checkShakeScreen(dt)
        
        if (paused) return
        
        checkTeleportAvailability(dt)
        checkHordeCreation(dt)
        checkActiveEnemies(dt)
    }
    
    private fun checkInput(dt: TimeSpan) {
        
        if (views.input.keys.justReleased(Key.P)) paused = !paused
        
        if (paused) return
        
        if (player.state == Player.State.MOVING || player.state == Player.State.HURT) {
            
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
            
            if (views.input.keys[Key.E] && player.isTeleportActive) teleportPlayer()
        }
        
    }
    
    private fun checkShakeScreen(dt: TimeSpan) {
        if (shakeScreen) {
            if (shakeScreenTimer <= shakeScreenDuration) {
                sceneView.position(Random.nextDouble(-3.0, 3.0), Random.nextDouble(-3.0, 3.0))
                shakeScreenTimer += dt
            } else {
                shakeScreen = false
                shakeScreenTimer = 0.seconds
            }
        }
    }
    
    private fun checkTeleportAvailability(dt: TimeSpan) {
        teleportTimer += dt
        if (teleportTimer.seconds >= teleportActivationPeriod.seconds) {
            teleportSymbol.tint = Colors.DARKMAGENTA
            player.isTeleportActive = true
        }
    }
    
    private fun teleportPlayer() {
        if (activeEnemies.isNotEmpty()) {
            teleportTimer = 0.seconds
            CoroutineScope(coroutineContext).launchImmediately {
                teleportSymbol.tween(teleportSymbol::scale[3], time = 0.2.seconds)
                teleportSymbol.tween(teleportSymbol::scale[.8], time = 0.2.seconds)
            }
            teleportSymbol.tint = Colors.DARKGRAY
            val enemy = activeEnemies.first()
            player.teleport(enemy.x, enemy.y) {
                infectEnemy(enemy)
                player.isTeleportActive = false
            }
        }
    }
    
    private fun checkHordeCreation(dt: TimeSpan) {
        if (activeEnemies.isEmpty()) {
            newHordeTimer += dt
        }
        
        if (newHordeTimer.seconds >= restPeriodBetweenHordes.seconds) {
            numberOfHordes++
            CoroutineScope(coroutineContext).launch {
                repeat(enemiesPerHorde) {
                    delay(.5.seconds)
                    createEnemy()
                }
            }
            newHordeTimer = 0.seconds
            enemiesPerHorde += 1
        }
    }
    
    private fun createEnemy() {
        val newEnemy = enemies.alloc().apply {
            position(Random.nextInt(fieldMargin, views.virtualWidth - fieldMargin),
                    Random.nextInt(fieldMargin, views.virtualHeight - fieldMargin))
        }
        
        if (Random.nextDouble(.0, 1.0) <= .3) {
            newEnemy.type = Enemy.EnemyType.CHASER
        } else {
            newEnemy.type = Enemy.EnemyType.STANDARD
        }
        sceneView.addChildAt(newEnemy, enemiesZIndex - 1)
        activeEnemies.add(newEnemy)
        newEnemy.live()
    }
    
    private fun infectEnemy(enemy: Enemy) {
        enemy.infect {
            activeEnemies.remove(enemy)
            sceneView.removeChild(enemy)
            enemies.free(enemy)
            scorePanel.addAditionalPoints(3)
        }
    }
    
    private fun killEnemy(enemy: Enemy) {
        scorePanel.addAditionalPoints(2)
        enemy.die {
            activeEnemies.remove(enemy)
            sceneView.removeChild(enemy)
            enemies.free(enemy)
        }
    }
    
    private fun checkActiveEnemies(dt: TimeSpan) {
        val iterator = activeEnemies.iterator()
        while (iterator.hasNext()) {
            try {
                val activeEnemy = iterator.next()
                
                if (activeEnemy.state == Enemy.State.MOVING && player.state == Player.State.MOVING) {
                    
                    if (activeEnemy.collidesWith(player, CollisionKind.SHAPE)
                            && player.state == Player.State.MOVING) {
                        shakeScreen = true
                        player.hurt()
                        scorePanel.updateLivesCounter(player.lives)
                        
                        if (player.lives == 0) {
                            paused = true
                            player.die {
                                CoroutineScope(coroutineContext).launchImmediately {
                                    sceneView.tween(bgMusic::volume[0.0], time = .5.seconds)
                                    bgMusic.stop()
                                    gameOverPanel.tween(gameOverPanel::x[gameOverPanel.x - gameOverPanel.width],
                                            time = .3.seconds, easing = Easing.EASE_OUT)
                                }
                            }
                        }
                    } else if (activeEnemy.collidesWith(player.bomb, CollisionKind.SHAPE)
                            && player.bomb.state == Bomb.State.EXPLODING) {
                        killEnemy(activeEnemy)
                    }
                    
                    val distanceEnemyPlayer = Point(player.x - activeEnemy.x, player.y - activeEnemy.y)
                    
                    if (distanceEnemyPlayer.magnitude <= activeEnemy.chaseRadius) {
                        activeEnemy.x += distanceEnemyPlayer.normalized.x * activeEnemy.moveSpeed * dt.seconds
                        activeEnemy.y += distanceEnemyPlayer.normalized.y * activeEnemy.moveSpeed * dt.seconds
                    } else {
                        activeEnemy.x += activeEnemy.direction.x * activeEnemy.moveSpeed * dt.seconds
                        activeEnemy.y += activeEnemy.direction.y * activeEnemy.moveSpeed * dt.seconds
                    }
                    
                    if (activeEnemy.x > views.virtualWidth - fieldMargin || activeEnemy.x <= fieldMargin) activeEnemy.direction.x *= -1
                    else if (activeEnemy.y > views.virtualHeight - fieldMargin || activeEnemy.y <= fieldMargin) activeEnemy.direction.y *= -1
                }
            } catch (e: ConcurrentModificationException) {
                break
            }
        }
    }
    
    override suspend fun sceneBeforeLeaving() {
        sceneContainer.tween(bgMusic::volume[0.0], time = .4.seconds)
        bgMusic.stop()
        super.sceneBeforeLeaving()
    }
}
