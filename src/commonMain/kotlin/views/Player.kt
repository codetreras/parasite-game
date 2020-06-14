package views

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readSound
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Player: Container() {

    enum class State{
        READY,
        APPEARING,
        MOVING,
        DYING
    }

    private lateinit var dieSound: NativeSound
    private lateinit var teleportSound: NativeSound
    private lateinit var portalSound: NativeSound
    var isTeleportActive: Boolean = true
        set(value) {
            field = value
            tint = if (value) Colors.WHITE else Colors.DARKRED
        }
    private lateinit var idleView: Image
    private lateinit var appearingView: Sprite

    var moveSpeed = 100.0
    var bomb: Bomb = Bomb()
    lateinit var state: Player.State

    suspend fun loadPlayer() {
        state = State.READY
        portalSound = resourcesVfs["sounds/fx/portal.wav"].readSound()
        teleportSound = resourcesVfs["sounds/fx/teleport.wav"].readSound()
        dieSound = resourcesVfs["sounds/fx/die.wav"].readSound()
        teleportSound.volume += 1
        portalSound.volume += .5
        val appearingViewMap = resourcesVfs["graphics/game_scene/player/player_appearing.png"].readBitmap()
        appearingView = Sprite(initialAnimation = SpriteAnimation(
                spriteMap = appearingViewMap,
                spriteWidth = 15,
                spriteHeight = 15,
                columns = 16,
                rows = 1
        ), smoothing = false, anchorX = .5)
        appearingView.spriteDisplayTime = 40.milliseconds

        idleView = Image(resourcesVfs["graphics/game_scene/player/player_idle.png"].readBitmap(),
                smoothing = false, anchorX = .5)
        bomb.loadBomb()

        addChild(appearingView)
    }

    fun live() {
        state = State.APPEARING
        removeChildren()
        addChild(appearingView)
        GlobalScope.launch {
            this@Player.tween(this@Player::scale[2.0], time = .3.seconds, easing = Easing.EASE_IN_OUT)
            this@Player.tween(this@Player::scale[1.0], time = .4.seconds, easing = Easing.EASE_IN_OUT)
        }
        appearingView.playAnimation()
        portalSound.play()
        appearingView.onAnimationCompleted.once{
            state = State.MOVING
            removeChildren()
            addChild(idleView)
        }
    }

    fun teleport(x: Double, y: Double, onTeleport: () -> Unit) {
        state = State.APPEARING
        blendMode = BlendMode.SCREEN
        teleportSound.play()
        GlobalScope.launch {
            this@Player.tween(this@Player::x[x], this@Player::y[y], time = .2.seconds, easing = Easing.EASE_IN)
            onTeleport()
            this@Player.tween(this@Player::scale[2.0], time = .3.seconds, easing = Easing.EASE_IN_OUT)
            this@Player.tween(this@Player::scale[1.0], time = .4.seconds, easing = Easing.EASE_IN_OUT)
            blendMode = BlendMode.NORMAL
            state = State.MOVING
            removeChildren()
            addChild(idleView)
        }
    }

    fun die(onDie: () -> Unit) {
        state = State.DYING
        removeChildren()
        addChild(appearingView)
        tint = Colors.BLUE
        GlobalScope.launch {
            this@Player.tween(this@Player::scale[1.0], time = .2.seconds, easing = Easing.EASE_IN_OUT)
            this@Player.tween(this@Player::scale[0.1], time = .4.seconds, easing = Easing.EASE_IN_OUT)
        }
        dieSound.play()
        appearingView.playAnimation(reversed = true)
        appearingView.onAnimationCompleted.once{
            state = State.READY
            tint = Colors.WHITE
            onDie()
            removeChildren()
        }
    }

    fun dropBomb(x: Double, y: Double) {
        bomb.position(x, y)
        bomb.explode()
    }
}