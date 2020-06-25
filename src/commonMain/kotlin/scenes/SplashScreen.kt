package scenes

import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSoundChannel
import com.soywiz.korau.sound.readMusic
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.image
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.delay
import com.soywiz.korio.file.std.resourcesVfs
import util.LoadingProxyScene

class SplashScreen : Scene() {
    
    private lateinit var bg: Image
    private lateinit var bgMusic: NativeSoundChannel
    
    override suspend fun Container.sceneInit() {
        bgMusic = resourcesVfs["sounds/intro_loop.wav"].readMusic().play()
        bg = image(resourcesVfs["graphics/splash_scene/intro_bg.png"].readBitmap()) {
            smoothing = false
            alpha = 0.0
        }
    }
    
    override suspend fun sceneAfterInit() {
        bg.tween(bg::alpha[1.0], time = 1.seconds)
        delay(2.seconds)
        bg.tween(bg::alpha[0.0], time = .5.seconds)
        sceneContainer.changeTo<LoadingProxyScene>(LoadingProxyScene::class::class,
                LoadingProxyScene.NextScreen(MainScene::class),
                time = .5.seconds)
    }
    
    override suspend fun sceneBeforeLeaving() {
        sceneContainer.tween(bgMusic::volume[0.0], time = .4.seconds)
        bgMusic.stop()
    }
    
}