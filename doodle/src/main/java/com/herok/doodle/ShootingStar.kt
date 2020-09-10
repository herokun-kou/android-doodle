package com.herok.doodle

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.forEach
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.random.Random

class ShootingStar(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    companion object {
        const val SHOOTING_TYPE_DYNAMIC = 0
        const val SHOOTING_TYPE_STATIC = 1
    }

    var minStarDelay = 2500L
    var maxStarDelay = 10000L

    var shootingType = 0

    var speed = 5
        set(value){
            if(value < 0 || value > 10) return
            field = value
        }

    var starRotation = -30f
        set(value){
            if(value >= 90 || value <= -90) return
            field = value

            if(value < 0) _starDirection = 1
            if(value > 0) _starDirection = -1

            forEach {
                val starHolder = it as LinearLayout
                starHolder.rotation = field
            }
        }

    var starColor = Color.WHITE
        set(value){
            field = value
            forEach {
                val starHolder = it as LinearLayout
                val star = starHolder.getChildAt(0)
                star.setBackgroundColor(field)
            }
        }

    private var maxStarCount = 0

    private var _starDirection = 1
    val starDirection get() = _starDirection

    private var _shooting = false
    val shooting get() = _shooting

    private var layoutParamsArray = ArrayList<LayoutParams>()

    private val delayHandler = Handler(Looper.getMainLooper())

    constructor(context: Context): this(context, null)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ShootingStar,
            0, 0
        ).apply {
            try {
                maxStarCount = getInt(R.styleable.ShootingStar_maxStarCount, 0)

                starRotation = getFloat(R.styleable.ShootingStar_starRotation, -30f)

                minStarDelay = getInt(R.styleable.ShootingStar_minStarDelay, 2500).toLong()
                maxStarDelay = getInt(R.styleable.ShootingStar_minStarDelay, 10000).toLong()

                starColor = getColor(R.styleable.ShootingStar_starColor, Color.WHITE)

                speed = getInt(R.styleable.ShootingStar_shootingSpeed, 5)

                shootingType = getInt(R.styleable.ShootingStar_shootingType, 0)
            } finally {
                recycle()
            }
        }
        arrayOf(50, 150, 250, 350, 450, 550).forEach {
            addSize(it)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        setUp()
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        if(childCount == 0){
            _shooting = false
        }
    }

    private fun setUp(){
        stop()

        removeAllViews()

        for(index in 0 until maxStarCount){
            createStar()
        }
    }

    private fun shoot(starHolder: LinearLayout){
        delayHandler.postDelayed({
            if(layoutParamsArray.isEmpty()) {
                stop()
                return@postDelayed
            }

            starHolder.layoutParams = layoutParamsArray[Random.nextInt(layoutParamsArray.size)]
            starHolder.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    starHolder.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    starHolder.translationX = 20 + Random.nextFloat() * (width - 40)
                    starHolder.translationY = 20 + Random.nextFloat() * (height - 40)
                    val star = starHolder.getChildAt(0)
                    val duration = ((600 + Random.nextFloat() * 500) * ((11f - speed)/10f)).roundToInt().toLong()
                    star.animate()
                        .translationX(-starDirection * star.width.toFloat())
                        .setDuration(duration)
                        .withEndAction {
                            if(!starHolder.isEnabled) {
                                removeView(starHolder)
                            }else {
                                shoot(starHolder)
                            }
                        }
                        .start()
                    if(shootingType == SHOOTING_TYPE_DYNAMIC){
                        val translationX = starDirection * (200 + Random.nextFloat() * 500)
                        starHolder.animate()
                            .translationX(starHolder.translationX - translationX)
                            .translationY(starHolder.translationY - (tan(Math.toRadians(starRotation.toDouble())).toFloat() * translationX))
                            .setDuration(duration)
                            .start()
                    }
                }
            })
        }, minStarDelay + ((maxStarDelay - minStarDelay) * Random.nextFloat()).toLong())
    }

    private fun createStar(){
        val starHolder = LinearLayout(context)
        val star = View(context)
        star.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        star.setBackgroundColor(starColor)
        starHolder.addView(star)
        starHolder.layoutParams = layoutParamsArray[Random.nextInt(layoutParamsArray.size)]
        addView(starHolder)

        star.viewTreeObserver.addOnGlobalLayoutListener{
            star.translationX = starDirection * star.width.toFloat()
        }
        starHolder.viewTreeObserver.addOnGlobalLayoutListener {
            starHolder.rotation = starRotation
        }
    }

    fun start(){
        if(shooting) return

        _shooting = true

        forEach { starHolder ->
            shoot(starHolder as LinearLayout)
        }
    }

    fun stop(){
        if(!shooting) return

        _shooting = false

        delayHandler.removeCallbacksAndMessages(null)
        forEach { starHolder ->
            starHolder.animate().cancel()
            val star = (starHolder as LinearLayout).getChildAt(0)
            star.animate().cancel()
            star.translationX = starDirection * star.width.toFloat()
        }
    }

    fun requestStop(){
        if(!shooting) return

        delayHandler.removeCallbacksAndMessages(null)
        Handler(Looper.getMainLooper()).postDelayed({
            _shooting = false
        }, (1100 * ((11f - speed)/10f)).toLong())
    }

    fun addSize(size: Int){
        layoutParamsArray.add(LayoutParams(size, context.resources.getDimensionPixelSize(R.dimen.star_height)))
    }

    fun clearSizes(){
        layoutParamsArray.clear()
    }

    fun addStars(count: Int){
        for(index in 0 until count){
            createStar()
            shoot(getChildAt(childCount - 1) as LinearLayout)
        }
    }

    fun requestRemoveStars(count: Int){
        for(index in 0 until count)
            getChildAt(index).isEnabled = false
    }

    fun forceRemoveStars(count: Int){
        removeViews(0, count)
    }

}