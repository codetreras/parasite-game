import com.soywiz.klock.seconds
import com.soywiz.korge.*
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.*
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.SizeInt
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.interpolation.Easing
import scenes.GameScene
import scenes.LoadingProxyScene
import scenes.MainScene
import kotlin.reflect.KClass

suspend fun main() = Korge(Korge.Config(module = MainModule))

object MainModule: Module(){

	override val mainScene: KClass<out Scene>
		get() = GameScene::class
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
		mapInstance(LoadingProxyScene.NextScreen(MainScene::class))
		mapPrototype { MainScene() }
		mapPrototype { GameScene() }
		mapPrototype { LoadingProxyScene(get()) }
	}
}