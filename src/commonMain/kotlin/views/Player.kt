package views

import com.soywiz.klock.seconds
import com.soywiz.korge.time.delay
import com.soywiz.korge.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Player: Container() {

    enum class State{
        READY,
        IDLE,
        APPEARING,
        MOVING,
        DYING
    }

    private var playerView: View = circle(5.0){
        anchor(.5, .5)
    }
    var state: Player.State = Player.State.READY
    var bomb: Bomb = Bomb()

    init {
        speed = 100.0
    }

    fun idle() {
        state = State.IDLE
    }

    fun teleport(x: Double, y: Double) {
        position(x, y)
        GlobalScope.launch {
            state = State.APPEARING
            delay(1.seconds)
            state = State.MOVING
        }
    }

    fun dropBomb(x: Double, y: Double) {
        bomb.position(x, y)
        bomb.explode()
    }
}