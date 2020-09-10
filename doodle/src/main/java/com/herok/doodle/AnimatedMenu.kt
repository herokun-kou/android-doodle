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

/**
 * An AnimatedMenu class extends FrameLayout.
 *
 * Creates animated pop-up menu view, which has menu groups and items.
 * Define this view at xml to use. See github README.md for more information.
 *
 * @author herok
 *
 * @constructor Creates AnimatedMenu object.
 * @property[groups] Holds groups data. Set by [setGroupsAndItems] and setter function receive array type but this object is mutable list.
 * @property[items] Holds items data. 2-dimensional. Same as groups, this object is mutable list.
 * @property[groupTextSize] Group text styling-textSize. Modifying this value applies changes immediately.
 * @property[itemTextSize] Item text styling-textSize. Modifying this value applies changes immediately.
 * @property[groupTextColor] Group text styling-textColor. Modifying this value applies changes immediately.
 * @property[itemTextColor] Item text styling-textColor. Modifying this value applies changes immediately.
 * @property[bgColor] AnimatedMenu styling-backgroundColor. alpha value is driven by this class, you can set only r, g, b values.
 * @property[useDecoration] true if this animated menu uses decoration view. read-only.
 * @property[useHeader] true if this animated menu uses header view. read-only.
 * @property[useFooter] true if this animated menu uses footer view. read-only.
 * @property[animating] true if this animated menu is animating something. This can be show/hide animation, or group(item) add/remove animation.
 * @property[decorationView] holds decoration view object. can be null.
 * @property[menuLayout] holds menu view(contains header, footer, groups, items) object. can be null.
 */
class AnimatedMenu(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    /**
     * @property[TAG] Class tag.
     * @property[TYPE_GROUP] Const used in creating new TextView, distinguish what type of target.
     * @property[TYPE_ITEM] Const used in creating new TextView, distinguish what type of target.
     * @property[ANIMATE_DURATION] Const used to set show/hide animation duration.
     * @property[TEXT_ANIMATE_DELAY] Const used to set text-in animation delay.
     * @property[TEXT_ANIMATE_MAX_DURATION] Const used to set max duration of each text in/out animation on show/hide
     * @property[TEXT_ANIMATE_MIN_DURATION] Const used to set min duration of each text in/out animation on show/hide
     * @property[ADD_REMOVE_ANIMATION_DURATION] Const used to set group/item add/remove animation duration.
     */
    companion object {
        private const val TAG = "AnimatedMenu"

        private const val TYPE_GROUP = 0
        private const val TYPE_ITEM = 1
        private const val ANIMATE_DURATION = 400L
        private const val TEXT_ANIMATE_DELAY = 30L
        private const val TEXT_ANIMATE_MAX_DURATION = 140L
        private const val TEXT_ANIMATE_MIN_DURATION = 70L
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

    var bgColor: Int = Color.BLACK
        set(value) {
            field = value
            setBackgroundColor(Color.argb(0, field.red, field.green, field.blue))
        }

    private var _useDecoration = false
    val useDecoration get() = _useDecoration

    private var _useHeader = false
    val useHeader get() = _useHeader

    private var _useFooter = false
    val useFooter get() = _useFooter

    private var _animating = false
    val animating get() = _animating

    private var decorationView: View? = null
    private var menuLayout: LinearLayout? = null

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
                _useDecoration = getBoolean(R.styleable.AnimatedMenu_useDecoration, false)
                _useHeader = getBoolean(R.styleable.AnimatedMenu_useHeader, false)
                _useFooter = getBoolean(R.styleable.AnimatedMenu_useFooter, false)
            } finally {
                recycle()
            }
        }
    }


    /**
     * First initialization. Throws some exceptions when Child View count is mismatching with given xml attributes.
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        if(useDecoration && childCount < 1) throw Exception("decoration view(or layout) not found in xml!")
        if(!useDecoration && childCount > 1) throw Exception("Too many child Views! Are you using decoration view, define useDecoration attribute to true.")

        if(useHeader && useFooter && ((getChildAt(1) as LinearLayout).childCount != 2)){
            throw Exception("Error getting header view and footer view. required two views, header and footer.")
        }else if(((useHeader && !useFooter) || (!useHeader && useFooter)) && ((getChildAt(1) as LinearLayout).childCount != 1)){
            throw Exception("Error getting header view and footer view. required one view, header or footer")
        }else if(!useHeader && !useFooter && ((getChildAt(1) as LinearLayout).childCount != 0)){
            throw Exception("Too many child views! Are you using header or footer, define useHeader or useFooter in xml.")
        }

        initViews()
        hideWithoutAnimation()

        setOnClickListener{ hide() }
    }


    /**
     * Sets [groups] and [items] with given array. Receive type is array, but [groups] and [items] are MutableList.
     * Must call this function to use this view.
     */
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


    /**
     * Internal call only. Initialize menuView, decorationView, etc and adds TextView to menuView matches with [groups] and [items].
     */
    private fun initViews(){
        if(animating) return

        if(groups.size != items.size) throw Exception("Item and group size mismatch : group size: ${groups.size}, item size: ${items.size}")

        if(useDecoration && decorationView == null){
            decorationView = getChildAt(0)
        }

        if(menuLayout == null){
            menuLayout = getMenuLayout()
            menuLayout?.orientation = LinearLayout.VERTICAL
            menuLayout?.gravity = Gravity.CENTER
        } else {
            menuLayout?.removeViews(if(useHeader) 1 else 0, menuLayout!!.childCount - (if(useHeader) 1 else 0) - (if(useFooter) 1 else 0))
        }

        groups.forEachIndexed { groupIndex, eachGroup ->
            val groupText = createNewTextView(TYPE_GROUP, eachGroup)
            val groupLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            if(groupIndex != 0)
                groupLayoutParams.topMargin =
                    context.resources.getDimension(R.dimen.diff_group_margin).toInt()

            groupText.layoutParams = groupLayoutParams

            menuLayout?.addView(groupText, menuLayout!!.childCount - (if(useFooter) 1 else 0))

            items[groupIndex].forEach { eachItem ->
                val itemText = createNewTextView(TYPE_ITEM, eachItem)
                val itemLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                itemText.layoutParams = itemLayoutParams

                menuLayout?.addView(itemText, menuLayout!!.childCount - (if(useFooter) 1 else 0))
            }
        }

        menuLayout?.setPadding(0, context.resources.getDimensionPixelSize(R.dimen.animate_items_translation_y),
            0, context.resources.getDimensionPixelSize(R.dimen.animate_items_translation_y))
    }


    /**
     * Adds group dynamically. Pass [animate] to false or skip setting last argument to add group without animation.
     * @param[position] Position where new group inserted to.
     * @param[text] Title of group which textView displays.
     * @param[animate] true if you want to add group with animation. default is false.
     */
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
        menuLayout?.addView(newText, viewPosition)

        if(animate) {
            newText.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    newText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    animateViewIn(newText)
                }
            })
        }

    }


    /**
     * Removes group dynamically. Pass [animate] to false or skip setting last argument to remove group without animation.
     * @param[position] Position where target group remove to.
     * @param[animate] true if you want to remove group with animation. default is false.
     */
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

        val targetText = menuLayout!!.getChildAt(findGroupPosition(position))

        val removeTexts: MutableList<View> = ArrayList()
        removeTexts.add(targetText)
        items[position].forEachIndexed { index, _ ->
            removeTexts.add(menuLayout!!.getChildAt(findItemPosition(position, index)))
        }

        if(animate) {
            animateViewOut(removeTexts) {
                menuLayout?.forEach { it.translationY = 0f }
                removeTexts.forEach { menuLayout?.removeView(it) }
            }
        }else{
            removeTexts.forEach { menuLayout?.removeView(it) }
        }

        groups.removeAt(position)
        items.removeAt(position)
    }


    /**
     * Adds item dynamically. Pass [animate] to false or skip setting last argument to add item without animation.
     * @param[groupPosition] Position of group where new item added to.
     * @param[positionInGroup] Position in group where new item inserted to.
     * @param[text] Title of item which textView displays.
     * @param[animate] true if you want to add item with animation. default is false.
     * @param[onClickListener] Default onClickListener set to item text. You can pass this value null to set listener later.
     */
    fun addItem(groupPosition: Int, positionInGroup: Int, text: String, animate: Boolean = false, onClickListener: ((View) -> (Unit))?){
        if(animating) return

        if(animate)
            waitToAnimateFinish(ADD_REMOVE_ANIMATION_DURATION)

        items[groupPosition].add(positionInGroup, text)

        val newText = createNewTextView(TYPE_ITEM, text)
        newText.setOnClickListener(onClickListener)
        newText.alpha = if(animate) 0f else 1f

        val viewPosition = findItemPosition(groupPosition, positionInGroup)
        menuLayout?.addView(newText, viewPosition)

        if(animate) {
            newText.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    newText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    animateViewIn(newText)
                }
            })
        }
    }

    /**
     * Removes item dynamically. Pass [animate] to false or skip setting last argument to remove item without animation.
     * @param[groupPosition] Position of group where target item removed to.
     * @param[positionInGroup] Position in group where target item remove to.
     * @param[animate] true if you want to remove item with animation. default is false.
     */
    fun removeItem(groupPosition: Int, positionInGroup: Int, animate: Boolean = false){
        if(animating) return

        if(groupPosition >= groups.size) {
            Log.e(TAG, "Error! group index is bigger than group count - target: $groupPosition, size: ${groups.size}")
            return
        }
        if(positionInGroup >= items[groupPosition].size) {
            Log.e(TAG, "Error! group index is bigger than group count - target: $positionInGroup, size: ${items[groupPosition].size}")
            return
        }

        if(animate)
            waitToAnimateFinish(ADD_REMOVE_ANIMATION_DURATION)

        val viewPosition = findItemPosition(groupPosition, positionInGroup)
        val targetText = menuLayout!!.getChildAt(viewPosition)

        if(animate) {
            animateViewOut(MutableList(1) { targetText }) {
                menuLayout?.forEach { it.translationY = 0f }
                menuLayout?.removeView(targetText)
            }
        }else{
            menuLayout?.removeView(targetText)
        }

        items[groupPosition].removeAt(positionInGroup)
    }


    /**
     * Sets OnClickListener with lambda expression to item TextView.
     * @param[groupPosition] Group of target item.
     * @param[positionInGroup] Position in group of target item.
     * @param[listener] Target OnClickListener.
     */
    fun setItemClickListener(groupPosition: Int, positionInGroup: Int, listener: ((View) -> (Unit))?){
        menuLayout!!.getChildAt(findItemPosition(groupPosition, positionInGroup)).setOnClickListener(listener)
    }


    /**
     * Sets OnClickListener object to item TextView.
     * @param[groupPosition] Group of target item.
     * @param[positionInGroup] Position in group of target item.
     * @param[listener] Target OnClickListener.
     */
    fun setItemClickListener(groupPosition: Int, positionInGroup: Int, listener: OnClickListener?){
        menuLayout!!.getChildAt(findItemPosition(groupPosition, positionInGroup)).setOnClickListener(listener)
    }


    /**
     * Animates in animation when new view inserted to [menuLayout].
     * @param[target] Added View.
     */
    private fun animateViewIn(target: View){
        val startPosition = menuLayout!!.indexOfChild(target)
        target.animate().setDuration(100L).setStartDelay(100L).alpha(1f).start()
        for(itemIndex in 0 until startPosition){
            menuLayout!!.getChildAt(itemIndex).translationY = (target.height.toFloat() + target.marginTop)/2
            createInAnimator(menuLayout!!.getChildAt(itemIndex)).start()
        }
        for(itemIndex in (startPosition + 1) until (menuLayout?.childCount ?: startPosition + 1)){
            menuLayout!!.getChildAt(itemIndex).translationY = -(target.height.toFloat() + target.marginTop)/2
            createInAnimator(menuLayout!!.getChildAt(itemIndex)).start()
        }
    }

    /**
     * Creates in animator.
     * @param[view] Target view to create animator.
     */
    private fun createInAnimator(view: View): ViewPropertyAnimator{
        return view.animate()
            .setStartDelay(0L)
            .setDuration(ADD_REMOVE_ANIMATION_DURATION)
            .translationY(0f)
            .withEndAction(null)
    }

    /**
     * Animates out animation when new view removed to [menuLayout].
     * @param[targets] Views which will remove when animate finishes.
     * @param[endAction] Animate end action.
     */
    private fun animateViewOut(targets: List<View>, endAction: (() -> (Unit))?){
        var removedHeight = 0
        val startPosition = menuLayout!!.indexOfChild(targets[0])
        targets.forEach { targetText ->
            removedHeight += targetText.height + targetText.marginTop
            targetText.animate().alpha(0f).setStartDelay(0L).setDuration(100L).withEndAction(null).start()
        }
        val animateTargets: MutableList<View> = ArrayList()
        for(itemIndex in 0 until startPosition){
            animateTargets.add(menuLayout!!.getChildAt(itemIndex) as TextView)
        }
        for(itemIndex in (startPosition + targets.size) until (menuLayout?.childCount ?: startPosition + targets.size)){
            animateTargets.add(menuLayout!!.getChildAt(itemIndex) as TextView)
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


    /**
     * Creates out animator.
     * @param[view] Target view to create animator.
     */
    private fun createOutAnimator(view: View, into: Float): ViewPropertyAnimator{
        return view.animate()
            .setStartDelay(0L)
            .setDuration(ADD_REMOVE_ANIMATION_DURATION)
            .translationY(into)
            .setListener(null)
            .withEndAction(null)
    }


    /**
     * Finds group view index of menuView with given group position.
     * @param[group] Group position of target view.
     */
    private fun findGroupPosition(group: Int): Int{
        var groupPosition = 0
        for(groupIndex in 0 until group){
            groupPosition += (1 + items[groupIndex].size)
        }
        return groupPosition + if(useHeader) 1 else 0
    }


    /**
     * Finds item view index of menuView with given group position and item position in group.
     * @param[groupPosition] Group position of target view.
     * @param[positionInGroup] Position in group of target view.
     */
    private fun findItemPosition(groupPosition: Int, positionInGroup: Int): Int{
        var itemPosition = 1 + positionInGroup
        for(groupIndex in 0 until groupPosition){
            itemPosition += (1 + items[groupIndex].size)
        }
        return itemPosition + if(useHeader) 1 else 0
    }


    /**
     * Creates new TextView.
     * @param[type] [TYPE_GROUP] if creates group text, [TYPE_ITEM] if creates item text.
     * @param[text] Title text of newly created TextView.
     */
    private fun createNewTextView(type: Int, text: String): TextView{
        val newText = TextView(context)

        newText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        newText.setPadding(20, 20, 20, 20)
        newText.text = text
        newText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (if(type == TYPE_ITEM) itemTextSize else groupTextSize).toFloat())
        newText.setTextColor(if(type == TYPE_ITEM) itemTextColor else groupTextColor)

        return newText
    }


    /**
     * Gets menu layout. Used in initViews().
     */
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

    /**
     * Creates Handler and call postDelayed with given duration.
     * @param[duration] Delay ms.
     */
    private fun waitToAnimateFinish(duration: Long){
        _animating = true
        Handler(Looper.getMainLooper()).postDelayed({
            _animating = false
        }, duration)
    }


    /**
     * Resets group TextViews size.
     */
    private fun resetGroupTextSize() {
        groups.forEachIndexed { index, _ ->
            ((menuLayout!!.getChildAt(findGroupPosition(index))) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_PX, groupTextSize.toFloat())
        }
    }


    /**
     * Resets item TextViews size.
     */
    private fun resetItemTextSize() {
        groups.forEachIndexed{ groupIndex, _ ->
            items[groupIndex].forEachIndexed { itemIndex, _ ->
                ((menuLayout!!.getChildAt(findItemPosition(groupIndex, itemIndex))) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize.toFloat())
            }
        }
    }

    /**
     * Resets group TextViews color.
     */
    private fun resetGroupTextColor(){
        groups.forEachIndexed { index, _ ->
            ((menuLayout!!.getChildAt(findGroupPosition(index))) as TextView).setTextColor(groupTextColor)
        }
    }

    /**
     * Resets item TextViews color.
     */
    private fun resetItemTextColor(){
        groups.forEachIndexed{ groupIndex, _ ->
            items[groupIndex].forEachIndexed { itemIndex, _ ->
                ((menuLayout!!.getChildAt(findItemPosition(groupIndex, itemIndex))) as TextView).setTextColor(itemTextColor)
            }
        }
    }


    /**
     * Adds decoration view without any animations.
     * @param[decoration] Target decoration view.
     */
    fun addDecoration(decoration: View){
        if(useDecoration){
            Log.e(TAG, "decoration already exists! to replace it, call removeDecoration() first.")
            return
        }
        _useDecoration = true
        addView(decoration, 0)
    }


    /**
     * Removes decoration view without any animations.
     */
    fun removeDecoration(){
        if(!useDecoration){
            Log.e(TAG, "decoration not exists!!")
            return
        }
        _useDecoration = false
        removeViewAt(0)
    }


    /**
     * Adds header view without any animations.
     * @param[headerView] Target header view.
     */
    fun addHeader(headerView: View){
        if(useHeader){
            Log.e(TAG, "header already exists! to replace it, call removeHeader() first.")
            return
        }
        menuLayout?.addView(headerView, 0)
        _useHeader = true
    }


    /**
     * Removes header view without any animations.
     */
    fun removeHeader(){
        if(!useHeader){
            Log.e(TAG, "header not exists!")
            return
        }
        menuLayout?.removeViewAt(0)
        _useHeader = false
    }


    /**
     * Adds footer view without any animations.
     * @param[footerView] Target footer view.
     */
    fun addFooter(footerView: View){
        if(useFooter){
            Log.e(TAG, "footer already exists! to replace it, call removeFooter() first.")
            return
        }
        menuLayout?.addView(footerView)
        _useFooter = true
    }


    /**
     * Removes footer view without any animations.
     */
    fun removeFooter(){
        if(!useFooter){
            Log.e(TAG, "footer not exists!")
            return
        }
        menuLayout?.removeViewAt(menuLayout!!.childCount - 1)
        _useFooter = false
    }


    /**
     * Shows AnimatedMenu with animation.
     */
    fun show(){
        if(animating) return

        val textAnimDuration = (ANIMATE_DURATION - (TEXT_ANIMATE_DELAY * menuLayout!!.childCount).coerceAtMost(ANIMATE_DURATION - TEXT_ANIMATE_MIN_DURATION))
            .coerceAtMost(TEXT_ANIMATE_MAX_DURATION)

        waitToAnimateFinish((textAnimDuration + (menuLayout!!.childCount * TEXT_ANIMATE_DELAY)).coerceAtLeast(ANIMATE_DURATION))

        visibility = VISIBLE
        val animator = ValueAnimator.ofInt(0, 200).setDuration(ANIMATE_DURATION)
        animator.addUpdateListener { value ->
            setBackgroundColor(Color.argb(value.animatedValue.toString().toInt(), bgColor.red, bgColor.green, bgColor.blue))
        }
        animator.start()
        decorationView?.alpha = 0f
        decorationView?.animate()?.alpha(1f)?.setDuration(ANIMATE_DURATION)?.start()
        menuLayout?.forEachIndexed { index, view ->
            view.translationY = context.resources.getDimension(R.dimen.animate_items_translation_y)
            view.alpha = 0f

            view.animate().translationY(0f)
                .setDuration(textAnimDuration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .setInterpolator(DecelerateInterpolator())
                .start()

            view.animate().alpha(1f)
                .setDuration(textAnimDuration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .start()
        }
    }


    /**
     * Hides AnimatedMenu with animation.
     */
    fun hide(){
        if(animating) return

        val textAnimDuration = (ANIMATE_DURATION - (TEXT_ANIMATE_DELAY * menuLayout!!.childCount).coerceAtMost(ANIMATE_DURATION - TEXT_ANIMATE_MIN_DURATION))
            .coerceAtMost(TEXT_ANIMATE_MAX_DURATION)

        waitToAnimateFinish((textAnimDuration + (menuLayout!!.childCount * TEXT_ANIMATE_DELAY)).coerceAtLeast(ANIMATE_DURATION))

        val animator = ValueAnimator.ofInt(200, 0).setDuration(ANIMATE_DURATION)
        animator.addUpdateListener { value ->
            setBackgroundColor(Color.argb(value.animatedValue.toString().toInt(), bgColor.red, bgColor.green, bgColor.blue))
            if(value.animatedValue.toString().toInt() == 0) visibility = GONE
        }
        animator.start()
        decorationView?.alpha = 1f
        decorationView?.animate()?.alpha(0f)?.setDuration(ANIMATE_DURATION)?.start()

        menuLayout?.forEachIndexed { index, view ->
            view.translationY = 0f
            view.alpha = 1f

            view.animate().translationY(-context.resources.getDimension(R.dimen.animate_items_translation_y))
                .setDuration(textAnimDuration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .setInterpolator(DecelerateInterpolator())
                .start()

            view.animate().alpha(0f)
                .setDuration(textAnimDuration)
                .setStartDelay(TEXT_ANIMATE_DELAY * index)
                .start()
        }
    }


    /**
     * Shows AnimatedMenu without animation.
     */
    fun showWithoutAnimation(){
        if(animating) return

        visibility = VISIBLE
        setBackgroundColor(Color.argb(200, bgColor.red, bgColor.green, bgColor.blue))
        decorationView?.alpha = 1f
        menuLayout?.forEach { childEach ->
            childEach.alpha = 1f
        }
    }


    /**
     * Hides AnimatedMenu without animation.
     */
    fun hideWithoutAnimation(){
        if(animating) return

        visibility = GONE
        setBackgroundColor(Color.argb(0, bgColor.red, bgColor.green, bgColor.blue))
        decorationView?.alpha = 0f
        menuLayout?.forEach { childEach ->
            childEach.alpha = 0f
        }
    }

}