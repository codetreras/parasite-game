package scenes

import com.soywiz.klock.seconds
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onOver
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.SceneContainer
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.math.round

class GameOverView(val sceneContainer: SceneContainer): Container() {

    private lateinit var bg: Image
    private lateinit var gameOverText: Image
    private lateinit var menuButton: Image
    private lateinit var restartButton: Image

    suspend fun loadPanel() {
        bg = image(resourcesVfs["graphics/game_over_scene/game_over_bg.png"].readBitmap()){
            smoothing = false
            tint = Colors.MAGENTA
        }

        gameOverText = image(resourcesVfs["graphics/game_over_scene/game_over_text.png"].readBitmap()){
            anchor(.5, .5)
            scale = .5
            position(this@GameOverView.width / 2 - 15, 40)
            smoothing = false
        }

        menuButton = image(resourcesVfs["graphics/game_over_scene/menu_button.png"].readBitmap()){
            tint = Colors.DARKMAGENTA
            position(30, this@GameOverView.height - 50)
            smoothing = false
            mouse {
                over{
                    tint = Colors.MAGENTA
                }
                out{
                    tint = Colors.DARKMAGENTA
                }
                onClick {
                    mouseEnabled = false
                    scale -= 0.05
                    sceneContainer.changeTo<LoadingProxyScene>(
                            LoadingProxyScene.NextScreen(MainScene::class),
                            time = .5.seconds
                    )
                }
            }
        }

        restartButton = image(resourcesVfs["graphics/game_over_scene/restart_button.png"].readBitmap()){
            tint = Colors.DARKMAGENTA
            position(30, this@GameOverView.height - 100)
            smoothing = false
            mouse {
                over{
                    tint = Colors.MAGENTA
                }
                out{
                    tint = Colors.DARKMAGENTA
                }
                onClick {
                    mouseEnabled = false
                    scale -= 0.05
                    sceneContainer.changeTo<LoadingProxyScene>(
                            LoadingProxyScene.NextScreen(GameScene::class),
                            time = .5.seconds
                    )
                }
            }
        }
    }
}