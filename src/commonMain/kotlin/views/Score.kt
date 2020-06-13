package views

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.round

class Score: Container() {

    private var counter = 0.0
    private lateinit var score: Text
    private lateinit var bg: Image

    suspend fun loadScore(){
        bg = image(resourcesVfs["graphics/game_scene/score_bg.png"].readBitmap()){
            smoothing = false
        }
        score = text("0"){
            color = RGBA(0, 72, 124)
            filtering = false
            textSize = 9.0
            position(16, 34)
        }
    }

    fun addTime(dt: TimeSpan){
        counter += dt.seconds
        score.text = round(counter).toString()
    }

    fun addAditionalPoints(points: Int){
        GlobalScope.launch {
            score.tint = Colors.MAGENTA
            score.tween(score::scale[1.5], time = .1.seconds, easing = Easing.EASE_IN_OUT)
            counter += points
            score.text = round(counter).toString()
            score.tween(score::scale[1.0], time = .05.seconds, easing = Easing.EASE_IN_OUT)
            score.tint = Colors.WHITE
        }
    }
}