package views

import com.soywiz.klock.seconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.circle
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launch
import kotlinx.coroutines.GlobalScope

class Bomb: Container() {
    enum class State{
        READY,
        EXPLOTING
    }

    private var bombView: View = circle(5.0, color = Colors.BLUE){
        anchor(.5, .5)
    }
    var state: Bomb.State = Bomb.State.READY

    init {
        visible = false
    }

    fun explode() {
        visible = true
        state = State.EXPLOTING
        GlobalScope.launch {
            delay(0.5.seconds)
            this.tween(this::scale[3], time = 0.3.seconds)
            delay(0.5.seconds)
            this.tween(this::scale[0], time = 0.3.seconds)
            visible = false
            scale = 1.0
            state = State.READY
        }
    }
}