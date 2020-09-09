package com.herok.doodle

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.*
import com.herok.doodle.app.R

class AnimatedMenu(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    companion object {
        private const val TAG = "AnimatedMenu"

        private const val TYPE_GROUP = 0
        private const val TYPE_ITEM = 1
        private const val ANIMATE_DURATION = 400L
        private const val TEXT_ANIMATE_DELAY = 40L
        private const val ADD_REMOVE_ANIMATION_DURATION = 200L
    }

    private var groups: MutableList<String> = ArrayList()
    private var items: MutableList<MutableList<String>> = ArrayList()

    var groupTextSize: Int = 0
        set(value) {
            field = value
            resetGroupTextSize()
        }

    var itemTextSize: Int = 0
        set(value) {
            field = value
            resetItemTextSize()
        }

    var groupTextColor: Int = 0
        set(value){
            field = value
            resetGroupTextColor()
        }

    var itemTextColor: Int = 0
        set(value) {
            field = value
            resetItemTextColor()
        }

    private var useDecoration = false

    private var _useHeader = false
    val useHeader get() = _useHeader

    private var _useFooter = false
    val useFooter get() = _useFooter

    private var _animating = false
    val animating get() = _animating

    private var decorationView: View? = null
    private var menuView: LinearLayout? = null

    private var bgColor: Int = Color.BLACK
        set(value) {
            field = value
            setBackgroundColor(Color.argb(0, field.red, field.green, field.blue))
        }

    constructor(context: Context): this(context, null)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AnimatedMenu,
            0, 0
        ).apply {
            try{
                groupTextSize = getDimensionPixelSize(
                    R.styleable.AnimatedMenu_groupTextSize,
                    context.resources.getDimensionPixelSize(R.dimen.default_group_text_size)
                )
                itemTextSize = getDimensionPixelSize(
                    R.styleable.AnimatedMenu_itemTextSize,
                    context.resources.getDimensionPixelSize(R.dimen.default_item_text_size)
                )
                groupTextColor = getColor(
                    R.styleable.AnimatedMenu_groupTextColor,
                    context.getColor(R.color.default_group_text_color)
                )
                itemTextColor = getColor(
                    R.styleable.AnimatedMenu_itemTextColor,
                    context.getColor(R.color.default_item_text_color)
                )
                bgColor = getColor(
                    R.styleable.AnimatedMenu_bgColor,
                    Color.BLACK
                )
                useDecoration = getBoolean(R.styleable.AnimatedMenu_useDecoration, false)
                _useHeader = getBoolean(R.styleable.AnimatedMenu_useHeader, false)
                _useFooter = getBoolean(R.styleable.AnimatedMenu_useFooter, false)
            } finally {
                recycle()
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if(useDecoration && childCount < 1) throw DecorationViewNotFoundException("decoration view(or layout) not found in xml!")
        if(!useDecoration && childCount > 1) throw TooManyChildViewsException("Too many child Views! Are you using decoration view, define useDecoration attribute to true.")

        if(useHeader && useFooter && ((getChildAt(1) as LinearLayout).childCount != 2)){
            throw Exception("Error getting header view and footer view. required two views, header and footer.")
        }else if(((useHeader && !useFooter) || (!useHeader && useFooter)) && ((getChildAt(1) as LinearLayout).childCount != 1)){
            throw Exception("Error getting header view and footer view. required one view, header or footer")
        }else if(!useHeader && !useFooter && ((getChildAt(1) as LinearLayout).childCount != 0)){
            throw TooManyChildViewsException("Too many child views! Are you using header or footer, define useHeader or useFooter in xml.")
        }

        initViews()
        hideWithoutAnimation()

        setOnClickListener{ hide() }
    }

    fun setGroupsAndItems(groups: Array<String>, items: Array<Array<String>>){
        this.groups.clear()
        this.items.clear()

        this.groups.addAll(groups)
        items.forEachIndexed { index, each ->
            this.items.add(ArrayList())
            this.items[index].addAll(each)
        }

        initViews()
    }

    private fun initViews(){
        if(animating) return

        if(groups.size != items.size) throw ItemGroupSizeMismatchException("group size: ${groups.size}, item size: ${items.size}")

        if(useDecoration && decorationView == null){
            decorationView = getChildAt(0)
        }

        if(menuView == null){
            menuView = getMenuLayout()
            menuView?.orientation = LinearLayout.VERTICAL
            menuView?.gravity = Gravity.CENTER
        } else {
            menuView?.removeViews(if(useHeader) 1 else 0, menuView!!.childCount - (if(useHeader) 1 else 0) - (if(useFooter) 1 else 0))
        }

        groups.forEachIndexed { groupIndex, eachGroup ->
            val groupText = createNewTextView(TYPE_GROUP, eachGroup)
            val groupLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            if(groupIndex != 0)
                groupLayoutParams.topMargin =
                    context.resources.getDimension(R.dimen.diff_group_margin).toInt()

            groupText.layoutParams = groupLayoutParams

            menuView?.addView(groupText, menuView!!.childCount - (if(useFooter) 1 else 0))

            items[groupIndex].forEach { eachItem ->
                val itemText = createNewTextView(TYPE_ITEM, eachItem)
                val itemLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                itemText.layoutParams = itemLayoutParams

                menuView?.addView(itemText, menuView!!.childCount - (if(useFooter) 1 else 0))
            }
        }

        menuView?.setPadding(0, context.resources.getDimensionPixelSize(R.dimen.animate_items_translation_y),
            0, context.resources.getDimensionPixelSize(R.dimen.animate_items_translation_y))
    }

    fun addGroup(position: Int, text: String, animate: Boolean = false){
        if(animating) return

        if(animate)
            waitToAnimateFinish(ADD_REMOVE_ANIMATION_DURATION)

        items.add(position, ArrayList())

        val newText = createNewTextView(TYPE_GROUP, text)
        groups.add(position, text)

        val viewPosition = findGroupPosition(position)
        (newText.layoutParams as MarginLayoutParams).topMargin = context.resources.getDimensionPixelSize(
            R.dimen.diff_group_margin
        )
        newText.alpha = if(animate) 0f else 1f
        menuView?.addView(newText, viewPosition)

        if(animate) {
            newText.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    newText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    animateViewIn(newText, viewPosition)
                }
            })
        }

    }

    fun removeGroup(position: Int, animate: Boolean = false){
        if(animating) return

        if(groups.size == 1) {
            Log.e(TAG, "AnimatedMenu must have at least 1 group. You are trying to remove last 1 group.")
            return
        }
        if(position >= groups.size) {
            Log.e(TAG, "Error! group index is bigger than group count - target: $position, size: ${groups.size}")
            return
        }

        if(animate)
            waitToAnimateFinish(ADD_REMOVE_ANIMATION_DURATION)

        val viewPosition = findGroupPosition(position)
        val targetText = menuView!!.getChildAt(viewPosition)

        val removeTexts: MutableList<View> = ArrayList()
        removeTexts.add(targetText)
        items[position].forEachIndexed { index, _ ->
            removeTexts.add(menuView!!.getChildAt(findItemPosition(position, index)))
        }

        if(animate) {
            animateViewOut(removeTexts, viewPosition) {
                menuView?.forEach { it.translationY = 0f }
                removeTexts.forEach { menuView?.removeView(it) }
            }
        }else{
            removeTexts.forEach { menuView?.removeView(it) }
        }

        groups.removeAt(position)
        items.removeAt(position)
    }

    fun addItem(group: Int, position: Int, text: String, onClickListener: OnClickListener?, animate: Boolean = false){
        if(animating) return

        if(animate)
            waitToAnimateFinish(ADD_REMOVE_ANIMATION_DURATION)

        items[group].add(position, text)

        val newText = createNewTextView(TYPE_ITEM, text)
        newText.setOnClickListener(onClickListener)
        newText.alpha = if(animate) 0f else 1f

        val viewPosition = findItemPosition(group, position)
        menuView?.addView(newText, viewPosition)

        if(animate) {
            newText.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    newText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    animateViewIn(newText, viewPosition)
                }
            })
        }
    }

    fun removeItem(group: Int, position: Int, animate: Boolean = false){
        if(animating) return

        if(group >= groups.size) {
            Log.e(TAG, "Error! group index is bigger than group count - target: $group, size: ${groups.size}")
            return
        }
        if(position >= items[group].size) {
            Log.e(TAG, "Error! group index is bigger than group count - target: $position, size: ${items[group].size}")
            return
        }

        if(animate)
            waitToAnimateFinish(ADD_REMOVE_ANIMATION_DURATION)

        val viewPosition = findItemPosition(group, position)
        val targetText = menuView!!.getChildAt(viewPosition)

        if(animate) {
            animateViewOut(MutableList(1) { targetText }, viewPosition) {
                menuView?.forEach { it.translationY = 0f }
                menuView?.removeView(targetText)
            }
        }else{
            menuView?.removeView(targetText)
        }

        items[group].removeAt(position)
    }

    fun addItemClickListener(group: Int, position: Int, listener: ((View) -> (Unit))?){
        menuView!!.getChildAt(findItemPosition(group, position)).setOnClickListener(listener)
    }

    private fun animateViewIn(targetText: TextView, startPosition: Int){
        targetText.animate().setDuration(100L).setStartDelay(100L).alpha(1f).start()
        for(itemIndex in 0 until startPosition){
            menuView!!.getChildAt(itemIndex).translationY = (targetText.height.toFloat() + targetText.marginTop)/2
            createInAnimator(itemIndex).start()
        }
        for(itemIndex in (startPosition + 1) until (menuView?.childCount ?: startPosition + 1)){
            menuView!!.getChildAt(itemIndex).translationY = -(targetText.height.toFloat() + targetText.marginTop)/2
            createInAnimator(itemIndex).start()
        }
    }

    private fun createInAnimator(itemIndex: Int): ViewPropertyAnimator{
        return menuView!!.getChildAt(itemIndex)
            .animate()
            .setStartDelay(0L)
            .setDuration(ADD_REMOVE_ANIMATION_DURATION)
            .translationY(0f)
            .withEndAction(null)
    }

    private fun animateViewOut(targetTexts: List<View>, startPosition: Int, endAction: (() -> (Unit))?){
        var removedHeight = 0
        targetTexts.forEach { targetText ->
            removedHeight += targetText.height + targetText.marginTop
            targetText.animate().alpha(0f).setStartDelay(0L).setDuration(100L).withEndAction(null).start()
        }
        val animateTargets: MutableList<View> = ArrayList()
        for(itemIndex in 0 until startPosition){
            animateTargets.add(menuView!!.getChildAt(itemIndex) as TextView)
        }
        for(itemIndex in (startPosition + targetTexts.size) until (menuView?.childCount ?: startPosition + targetTexts.size)){
            animateTargets.add(menuView!!.getChildAt(itemIndex) as TextView)
        }
        animateTargets.forEachIndexed { index, textView ->
            val animator: ViewPropertyAnimator = if(index < startPosition) {
                createOutAnimator(textView, (removedHeight / 2).toFloat())
            }else{
                createOutAnimator(textView, (-removedHeight / 2).toFloat())
            }
            if(index == animateTargets.size - 1){
                animator.setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator?) {}
                    override fun onAnimationEnd(p0: Animator?) {
                        endAction?.invoke()
                    }
                    override fun onAnimationRepeat(p0: Animator?) {}
                    override fun onAnimationCancel(p0: Animator?) {}
                })
            }
            animator.start()
        }
    }

    private fun createOutAnimator(view: View, into: Float): ViewPropertyAnimator{
        return view.animate()
            .setStartDelay(0L)
            .setDuration(ADD_REMOVE_ANIMATION_DURATION)
            .translationY(into)
            .setListener(null)
            .withEndAction(null)
    }

    private fun findGroupPosition(group: Int): Int{
        var groupPosition = 0
        for(groupIndex in 0 until group){
            groupPosition += (1 + items[groupIndex].size)
        }
        return groupPosition + if(useHeader) 1 else 0
    }

    private fun findItemPosition(group: Int, position: Int): Int{
        var itemPosition = 1 + position
        for(groupIndex in 0 until group){
            itemPosition += (1 + items[groupIndex].size)
        }
        return itemPosition + if(useHeader) 1 else 0
    }

    private fun createNewTextView(type: Int, text: String): TextView{
        val newText = TextView(context)

        newText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        newText.setPadding(20, 20, 20, 20)
        newText.text = text
        newText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (if(type == TYPE_ITEM) itemTextSize else groupTextSize).toFloat())
        newText.setTextColor(if(type == TYPE_ITEM) itemTextColor else groupTextColor)

        return newText
    }

    private fun getMenuLayout(): LinearLayout{
        return if(useDecoration && childCount == 2){
            getChildAt(1) as LinearLayout
        }else if(!useDecoration && childCount == 1){
            getChildAt(0) as LinearLayout
        }else{
            val menuLayout = LinearLayout(context)
            val menuLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            menuLayout.layoutParams = menuLayoutParams
            menuLayout.gravity = Gravity.CENTER
            menuLayout.orientation = LinearLayout.VERTICAL
            addView(menuLayout)
            menuLayout
        }
    }

    private fun waitToAnimateFinish(duration: Long){
        _animating = true
        Handler(Looper.getMainLooper()).postDelayed({
            _animating = false
        }, duration)
    }

    private fun resetGroupTextSize() {
        groups.forEachIndexed { index, _ ->
            ((menuView!!.getChildAt(findGroupPosition(index))) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_PX, groupTextSize.toFloat())
        }
    }

    private fun resetItemTextSize() {
        groups.forEachIndexed{ groupIndex, _ ->
            items[groupIndex].forEachIndexed { itemIndex, _ ->
                ((menuView!!.getChildAt(findItemPosition(groupIndex, itemIndex))) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize.toFloat())
            }
        }
    }

    private fun resetGroupTextColor(){
        groups.forEachIndexed { index, _ ->
            ((menuView!!.getChildAt(findGroupPosition(index))) as TextView).setTextColor(groupTextColor)
        }
    }

    private fun resetItemTextColor(){
        groups.forEachIndexed{ groupIndex, _ ->
            items[groupIndex].forEachIndexed { itemIndex, _ ->
                ((menuView!!.getChildAt(findItemPosition(groupIndex, itemIndex))) as TextView).setTextColor(itemTextColor)
            }
        }
    }

    fun addHeader(headerView: View){
        if(useHeader){
            Log.e(TAG, "header already exists! to replace it, call removeHeader() first.")
            return
        }
        menuView?.addView(headerView, 0)
        _useHeader = true
    }

    fun removeHeader(){
        if(!useHeader){
            Log.e(TAG, "header not exists!")
            return
        }
        menuView?.removeViewAt(0)
        _useHeader = false
    }

    fun addFooter(footerView: View){
        if(useFooter){
            Log.e(TAG, "footer already exists! to replace it, call removeHeader() first.")
            return
        }
        menuView?.addView(footerView)
        _useFooter = true
    }

    fun removeFooter(){
        if(!useFooter){
            Log.e(TAG, "footer not exists!")
            return
        }
        menuView?.removeViewAt(menuView!!.childCount - 1)
        _useFooter = false
    }

    fun show(){
        if(animating) return

        val duration = (ANIMATE_DURATION - (TEXT_ANIMATE_DELAY * menuView!!.childCount).coerceAtMost(350L)).coerceAtMost(70)

        waitToAnimateFinish((duration * menuView!!.childCount).coerceAtLeast(ANIMATE_DURATION))

        visibility = VISIBLE
        val animator = ValueAnimator.ofInt(0, 200).setDuration(ANIMATE_DURATION)
        animator.addUpdateListener { value ->
            setBackgroundColor(Color.argb(value.animatedValue.toString().toInt(), bgColor.red, bgColor.green, bgColor.blue))
        }
        animator.start()
        decorationView?.alpha = 0f
        decorationView?.animate()?.alpha(1f)?.setDuration(ANIMATE_DURATION)?.start()
        menuView?.forEachIndexed { index, view ->
            view.translationY = context.resources.getDimension(R.dimen.animate_items_translation_y)
            view.alpha = 0f

            view.animate().translationY(0f)
                .setDuration(duration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .setInterpolator(DecelerateInterpolator())
                .start()

            view.animate().alpha(1f)
                .setDuration(duration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .start()
        }
    }

    fun hide(){
        if(animating) return

        val duration = (ANIMATE_DURATION - (TEXT_ANIMATE_DELAY * menuView!!.childCount).coerceAtMost(350L)).coerceAtMost(70)

        waitToAnimateFinish((duration * menuView!!.childCount).coerceAtLeast(ANIMATE_DURATION))

        val animator = ValueAnimator.ofInt(200, 0).setDuration(ANIMATE_DURATION)
        animator.addUpdateListener { value ->
            setBackgroundColor(Color.argb(value.animatedValue.toString().toInt(), bgColor.red, bgColor.green, bgColor.blue))
            if(value.animatedValue.toString().toInt() == 0) visibility = GONE
        }
        animator.start()
        decorationView?.alpha = 1f
        decorationView?.animate()?.alpha(0f)?.setDuration(ANIMATE_DURATION)?.start()

        menuView?.forEachIndexed { index, view ->
            view.translationY = 0f
            view.alpha = 1f

            view.animate().translationY(-context.resources.getDimension(R.dimen.animate_items_translation_y))
                .setDuration(duration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .setInterpolator(DecelerateInterpolator())
                .start()

            view.animate().alpha(0f)
                .setDuration(duration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .start()
        }
    }

    fun showWithoutAnimation(){
        if(animating) return

        visibility = VISIBLE
        setBackgroundColor(Color.argb(200, bgColor.red, bgColor.green, bgColor.blue))
        decorationView?.alpha = 1f
        menuView?.forEach { childEach ->
            childEach.alpha = 1f
        }
    }

    fun hideWithoutAnimation(){
        if(animating) return

        visibility = GONE
        setBackgroundColor(Color.argb(0, bgColor.red, bgColor.green, bgColor.blue))
        decorationView?.alpha = 0f
        menuView?.forEach { childEach ->
            childEach.alpha = 0f
        }
    }

    class ItemGroupSizeMismatchException(message: String): Exception(message)

    class DecorationViewNotFoundException(message: String): Exception(message)

    class TooManyChildViewsException(message: String): Exception(message)

}