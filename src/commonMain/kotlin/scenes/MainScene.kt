package scenes

import com.soywiz.klock.seconds
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.interpolation.Easing

class MainScene: Scene() {

    private lateinit var startButton:Image
    private lateinit var title:Image
    private lateinit var bg:Image

    override suspend fun Container.sceneInit() {
        bg = image(resourcesVfs["graphics/main_scene/title_bg.png"].readBitmap()){
            smoothing = false
            tint = Colors.DARKMAGENTA
        }

        title = image(resourcesVfs["graphics/main_scene/title.png"].readBitmap()){
            smoothing = false
            anchor(.5, .5)
            position(bg.width / 2, bg.height / 2)
        }

        startButton = image(resourcesVfs["graphics/main_scene/start_button.png"].readBitmap()){
            smoothing = false
            anchor(.5, 1.0)
            tint = Colors.DARKMAGENTA
            position(views.virtualWidth / 2, views.virtualHeight)
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

    override suspend fun Container.sceneMain() {
        while (true){
            title.tween(title::y[bg.height / 2 - 5], time = 2.seconds, easing = Easing.EASE_IN_OUT)
            title.tween(title::y[bg.height / 2 + 5], time = 2.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}