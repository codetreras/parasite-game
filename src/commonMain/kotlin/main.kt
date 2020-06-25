import com.soywiz.korge.*
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.SizeInt
import scenes.*
import util.LoadingProxyScene
import kotlin.reflect.KClass

suspend fun main() = Korge(Korge.Config(module = MainModule))

object MainModule : Module() {
    
    override val mainScene: KClass<out Scene>
        get() = SplashScreen::class
    override val title: String
        get() = "Parasite"
    override val windowSize: SizeInt
        get() = SizeInt(1280, 720)
    override val size: SizeInt
        get() = SizeInt(320, 180)
    override val scaleMode: ScaleMode
        get() = ScaleMode.COVER
    override val bgcolor: RGBA
        get() = Colors.BLACK
    
    override suspend fun AsyncInjector.configure() {
        mapPrototype { MainScene() }
        mapPrototype { GameScene() }
        mapPrototype { SplashScreen() }
        mapPrototype { LoadingProxyScene(get(), getOrNull()) }
    }
}