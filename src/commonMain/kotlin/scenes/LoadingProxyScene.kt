package scenes

import com.soywiz.klock.seconds
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import kotlin.reflect.KClass

class LoadingProxyScene(nextScreen: LoadingProxyScene.NextScreen): Scene() {

    private lateinit var loadingText: Text
    private val text: String = "Loading..."
    private val nextScreen: KClass<*> = nextScreen.nextScreenClass

    override suspend fun Container.sceneInit() {
        loadingText = text(text) {
            position(-this.width, views.virtualHeight / 2 - this.height / 2)
            filtering = false
        }
    }

    override suspend fun Container.sceneMain() {
        loadingText.tween(loadingText::x[views.virtualWidth / 2 - loadingText.width / 2].easeOutBounce(), time = 1.seconds)
        sceneContainer.changeToAsync(clazz = nextScreen as KClass<Scene>, time = .5.seconds)
    }

    override suspend fun sceneBeforeLeaving() {
        loadingText.tween(loadingText::x[views.virtualWidth].easeInBounce(), time = .5.seconds)
    }

    data class NextScreen(val nextScreenClass: KClass<*>)
}