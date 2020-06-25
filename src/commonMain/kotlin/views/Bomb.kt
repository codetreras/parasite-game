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
import com.soywiz.korma.geom.vector.circle
import kotlinx.coroutines.GlobalScope

class Bomb : Container() {
    
    enum class State {
        READY,
        EXPLODING
    }
    
    private lateinit var bombSound: NativeSound
    var state: State = State.READY
    
    suspend fun loadBomb() {
        state = State.READY
        bombSound = resourcesVfs["sounds/fx/bomb_fx.wav"].readSound().apply {
            volume -= .1
        }
        
        image(resourcesVfs["graphics/game_scene/bomb/bomb_exploding.png"].readBitmap()) {
            scale = .9
            anchor(.5, .5)
            smoothing = false
        }
        
        hitShape {
            circle(width / 2, height / 2, width / 2)
        }
        
        scale = 0.0
        
        visible = false
    }
    
    fun explode() {
        visible = true
        state = State.EXPLODING
        GlobalScope.launch {
            bombSound.play()
            this.tween(this::scale[.8], this::rotationDegrees[800], time = 1.seconds)
            this.tween(this::scale[3], this::rotationDegrees[rotationDegrees + 400], time = 0.2.seconds)
            this.tween(this::rotationDegrees[rotationDegrees + 400], time = 0.2.seconds)
            this.tween(this::scale[0], this::rotationDegrees[rotationDegrees + 400], time = 0.2.seconds)
            visible = false
            scale = .8
            rotationDegrees = 0.0
            state = State.READY
        }
    }
}