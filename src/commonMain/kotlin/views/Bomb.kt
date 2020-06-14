package views

import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readSound
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.std.resourcesVfs
import kotlinx.coroutines.GlobalScope

class Bomb: Container() {
    enum class State{
        READY,
        EXPLOTING
    }

    private lateinit var bombSound: NativeSound
    private lateinit var explodingView: Image
    var state: Bomb.State = Bomb.State.READY
    private val rotationExploding = 300

    suspend fun loadBomb(){
        state = Bomb.State.READY
        bombSound = resourcesVfs["sounds/fx/bomb.wav"].readSound()
        bombSound.volume -= .1
        scale = 0.0
        val explodingView = image(resourcesVfs["graphics/game_scene/bomb/bomb_exploding.png"].readBitmap()){
            scale = .9
            anchor(.5, .5)
            smoothing = false
        }
        visible = false
    }

    fun explode() {
        visible = true
        state = State.EXPLOTING
        bombSound.play()
        GlobalScope.launch {
            this.tween(this::scale[.8], this::rotationDegrees[800], time = 1.seconds)
            this.tween(this::scale[3], this::rotationDegrees[rotationDegrees+400], time = 0.2.seconds)
            this.tween(this::rotationDegrees[rotationDegrees+400], time = 0.2.seconds)
            this.tween(this::scale[0], this::rotationDegrees[rotationDegrees+400], time = 0.2.seconds)
            visible = false
            scale = .8
            rotationDegrees = 0.0
            state = State.READY
        }
    }
}