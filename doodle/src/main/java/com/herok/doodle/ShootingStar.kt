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


/**
 * @author herok
 *
 * A ShootingStar class extends FrameLayout.
 *
 * Creates shooting stars in android activity or fragment, etc.
 * Define this view at xml to use. See github README.md for more information.
 *
 * @constructor Creates ShootingStar object.
 * @property[minStarDelay] Minimum delay of each star restarts shooting after finishes previous shooting.
 * @property[maxStarDelay] Maximum delay of each star restarts shooting after finishes previous shooting.
 * @property[shootingType] Star Shooting Type. Must be one of [SHOOTING_TYPE_DYNAMIC] and [SHOOTING_TYPE_STATIC]
 * @property[shootingSpeed] Star shooting speed. Must in [0, 10]
 * @property[starRotation] Star rotation angle. Must in (-90, 90)
 * @property[starColor] Color of Star.
 * @property[starCount] Counts of stars to add this view. read-only.
 * @property[starDirection] Direction of Star shoots. read-only.
 * @property[shooting] true if one or more stars are shooting or waiting for shoot, false otherwise. read-only.
 * @property[layoutParamsArray] Array of FrameLayout.LayoutParams.
 * @property[delayHandler] Handler object manages star shooting delay.
 */
class ShootingStar(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    companion object {
        const val SHOOTING_TYPE_DYNAMIC = 0
        const val SHOOTING_TYPE_STATIC = 1
    }

    var minStarDelay = 2500L
    var maxStarDelay = 10000L

    var shootingType = 0

    var shootingSpeed = 5
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
                val starHolder = it as Star
                starHolder.rotation = field
            }
        }

    var starColor = Color.WHITE
        set(value){
            field = value
            forEach {
                val starHolder = it as Star
                val star = starHolder.getChildAt(0)
                star.setBackgroundColor(field)
            }
        }

    private var _starCount = 0
    val starCount get() = _starCount

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
                _starCount = getInt(R.styleable.ShootingStar_starCount, 0)

                starRotation = getFloat(R.styleable.ShootingStar_starRotation, -30f)

                minStarDelay = getInt(R.styleable.ShootingStar_minStarDelay, 2500).toLong()
                maxStarDelay = getInt(R.styleable.ShootingStar_minStarDelay, 10000).toLong()

                starColor = getColor(R.styleable.ShootingStar_starColor, Color.WHITE)

                shootingSpeed = getInt(R.styleable.ShootingStar_shootingSpeed, 5)

                shootingType = getInt(R.styleable.ShootingStar_shootingType, 0)
            } finally {
                recycle()
            }
        }
        arrayOf(50, 150, 250, 350, 450, 550).forEach {
            addSize(it)
        }
    }

    /**
     * First initialization.
     */
    override fun onFinishInflate() {
        super.onFinishInflate()

        setUp()
    }

    /**
     * Overrides removeView. If there are no children in this view, sets shooting flag to false.
     */
    override fun removeView(view: View?) {
        super.removeView(view)
        if(childCount == 0){
            delayHandler.removeCallbacksAndMessages(null)
            _shooting = false
        }
    }

    /**
     * Initialization function.
     */
    private fun setUp(){
        stop()

        removeAllViews()

        for(index in 0 until starCount){
            createStar()
        }
    }

    /**
     * Recursive function. Post action with random delay. When delay finishes, star starts shoot.
     * @param[star] target star to shoot.
     */
    private fun shoot(star: Star){
        delayHandler.postDelayed({
            if(layoutParamsArray.isEmpty()) {
                stop()
                return@postDelayed
            }

            star.layoutParams = layoutParamsArray[Random.nextInt(layoutParamsArray.size)]
            //Layout params settings are not applied immediately, so add global layout listener and remove that in listener action.
            star.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    star.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    //Sets location of star randomly.
                    star.translationX = 20 + Random.nextFloat() * (width - 40)
                    star.translationY = 20 + Random.nextFloat() * (height - 40)

                    val lightView = star.getChildAt(0)
                    val duration = ((600 + Random.nextFloat() * 500) * ((11f - shootingSpeed)/10f)).roundToInt().toLong()

                    //Start animating.
                    lightView.animate()
                        .translationX(-starDirection * lightView.width.toFloat())
                        .setDuration(duration)
                        .withEndAction {
                            if(star.markedToRemove) {
                                removeView(star)
                            }else {
                                shoot(star)
                            }
                        }
                        .start()
                    if(shootingType == SHOOTING_TYPE_DYNAMIC){
                        val translationX = starDirection * (200 + Random.nextFloat() * 500)
                        star.animate()
                            .translationX(star.translationX - translationX)
                            .translationY(star.translationY - (tan(Math.toRadians(starRotation.toDouble())).toFloat() * translationX))
                            .setDuration(duration)
                            .start()
                    }
                }
            })
        }, minStarDelay + ((maxStarDelay - minStarDelay) * Random.nextFloat()).toLong())
    }

    /**
     * Creates new star and add to view.
     */
    private fun createStar(){
        val starHolder = Star(context)
        val lightView = View(context)
        lightView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        lightView.setBackgroundColor(starColor)
        starHolder.addView(lightView)
        starHolder.layoutParams = layoutParamsArray[Random.nextInt(layoutParamsArray.size)]
        addView(starHolder)

        lightView.viewTreeObserver.addOnGlobalLayoutListener{
            lightView.translationX = starDirection * lightView.width.toFloat()
        }
        starHolder.viewTreeObserver.addOnGlobalLayoutListener {
            starHolder.rotation = starRotation
        }
    }

    /**
     * Enables star shooting.
     */
    fun start(){
        if(shooting) return

        _shooting = true

        forEach { starHolder ->
            shoot(starHolder as Star)
        }
    }

    /**
     * Force stops star shooting.
     */
    fun stop(){
        if(!shooting) return

        _shooting = false

        delayHandler.removeCallbacksAndMessages(null)
        forEach { starHolder ->
            starHolder.animate().cancel()
            val star = (starHolder as Star).getChildAt(0)
            star.animate().cancel()
            star.translationX = starDirection * star.width.toFloat()
        }
    }

    /**
     * Requests to stop star shooting. This waits the star to complete shooting.
     */
    fun requestStop(){
        if(!shooting) return

        delayHandler.removeCallbacksAndMessages(null)
        Handler(Looper.getMainLooper()).postDelayed({
            _shooting = false
        }, (1100 * ((11f - shootingSpeed)/10f)).toLong())
    }

    /**
     * Add size param of star.
     * @param[size] new size of star.
     */
    fun addSize(size: Int){
        layoutParamsArray.add(LayoutParams(size, context.resources.getDimensionPixelSize(R.dimen.star_height)))
    }

    /**
     * Clears all size params of star.
     */
    fun clearSizes(){
        layoutParamsArray.clear()
    }

    /**
     * Adds star with given count. Do NOT add too much stars, adjust [minStarDelay] and [maxStarDelay] instead.
     * @param[count] Count of star to add
     */
    fun addStars(count: Int){
        for(index in 0 until count){
            createStar()
            shoot(getChildAt(childCount - 1) as Star)
        }
    }

    /**
     * Mark the first n stars to remove after completes shooting.
     * @param[count] Count of star to remove
     */
    fun requestRemoveStars(count: Int){
        for(index in 0 until count)
            (getChildAt(index) as Star).markedToRemove = true
    }

    /**
     * Remove stars Immediately.
     * * @param[count] Count of star to remove
     */
    fun forceRemoveStars(count: Int){
        removeViews(0, count)
    }

    /**
     * Custom LinearLayout class with markedToRemove flag.
     */
    class Star(context: Context):LinearLayout(context, null){
        var markedToRemove = false
    }

}