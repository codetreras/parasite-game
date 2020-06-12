package views

import com.soywiz.klock.seconds
import com.soywiz.korge.time.delay
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.circle
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launch
import com.soywiz.korma.geom.Point
import kotlinx.coroutines.GlobalScope

class Enemy(val direction: Point): Container() {

    enum class State{
        READY,
        APPEARING,
        MOVING,
        DYING
    }

    private var enemyView: View = circle(5.0, Colors.RED){
        anchor(.5, .5)
    }
    var state: State = State.READY
    var radius: Float = 50f

    init {
        speed = 50.0
    }

    fun resetEnemy(): Unit{
        state = State.READY
    }

    fun live() {
        state = State.APPEARING
        GlobalScope.launch {
            state = State.MOVING
        }
    }

    fun move() {
        state = State.MOVING
    }

    fun die(onDie: () -> Unit) {
        state = State.DYING
        GlobalScope.launch {
            delay(1.seconds)
            state = State.READY
            onDie()
        }
    }
}