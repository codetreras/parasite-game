package scenes

import com.soywiz.klock.seconds
import com.soywiz.korau.sound.*
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.interpolation.Easing
import util.LoadingProxyScene

class MainScene : Scene() {
    
    private lateinit var instructions: Image
    private lateinit var startButton: Image
    private lateinit var title: Image
    private lateinit var bg: Image
    
    private lateinit var bgMusic: NativeSoundChannel
    
    override suspend fun Container.sceneInit() {
        
        bg = image(resourcesVfs["graphics/main_scene/title_bg.png"].readBitmap()) {
            smoothing = false
            tint = Colors.DARKMAGENTA
        }
        
        title = image(resourcesVfs["graphics/main_scene/title.png"].readBitmap()) {
            smoothing = false
            anchor(.5, .5)
            position(bg.width / 2, bg.height / 2)
        }
        
        startButton = image(resourcesVfs["graphics/main_scene/start_button.png"].readBitmap()) {
            smoothing = false
            anchor(.5, 1.0)
            tint = Colors.DARKMAGENTA
            position(views.virtualWidth / 2, views.virtualHeight)
            mouse {
                over {
                    tint = Colors.MAGENTA
                }
                out {
                    tint = Colors.DARKMAGENTA
                }
                onClick {
                    if (mouseEnabled) {
                        mouseEnabled = false
                        scale -= 0.05
                        sceneContainer.changeTo<LoadingProxyScene>(
                                LoadingProxyScene.NextScreen(GameScene::class),
                                instructions,
                                time = .5.seconds
                        )
                    }
                }
            }
        }
        
        instructions = Image(resourcesVfs["graphics/main_scene/instructions.png"].readBitmap()).apply {
            smoothing = false
            scale = .8
        }
    }
    
    override suspend fun Container.sceneMain() {
        bgMusic = resourcesVfs["sounds/menu_music.mp3"].readMusic().play()
        while (true) {
            title.tween(title::y[bg.height / 2 - 5], time = 2.seconds, easing = Easing.EASE_IN_OUT)
            title.tween(title::y[bg.height / 2 + 5], time = 2.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
    
    override suspend fun sceneBeforeLeaving() {
        sceneContainer.tween(bgMusic::volume[0.0], time = .4.seconds)
        bgMusic.stop()
        super.sceneBeforeLeaving()
    }
}