package util

import com.soywiz.klock.seconds
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.delay
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import kotlin.reflect.KClass

class LoadingProxyScene(nextScreen: NextScreen, private val infoImage: Image?) : Scene() {
    
    private lateinit var loadingText: Text
    private val text: String = "Loading..."
    private val nextScreen: KClass<*> = nextScreen.nextScreenClass
    
    override suspend fun Container.sceneInit() {
        infoImage?.let { info ->
            info.anchor(.5, .5)
            info.position(views.virtualWidth / 2, views.virtualHeight / 2)
            info.scale = .8
            addChild(info)
        }
        
        loadingText = text(text) {
            textSize = 6.0
            position(-this.width, views.virtualHeight - 20)
            filtering = false
        }
    }
    
    override suspend fun Container.sceneMain() {
        loadingText.tween(loadingText::x[views.virtualWidth - loadingText.width - 20].easeOut(), time = 1.seconds)
        infoImage?.let { delay(3.seconds) }
        sceneContainer.changeTo(clazz = nextScreen as KClass<Scene>, time = .5.seconds)
    }
    
    override suspend fun sceneBeforeLeaving() {
        loadingText.tween(loadingText::x[views.virtualWidth].easeIn(), time = .5.seconds)
    }
    
    data class NextScreen(val nextScreenClass: KClass<*>)
}