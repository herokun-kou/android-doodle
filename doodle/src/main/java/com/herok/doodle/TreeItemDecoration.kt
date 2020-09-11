package com.herok.doodle

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author herok
 *
 * A TreeItemDecoration class extends RecyclerView.ItemDecoration.
 * If RecyclerView.Adapter is tree whose all leaf nodes has same depth, This ItemDecoration may useful.
 *
 * @constructor Creates TreeItemDecoration object.
 * @property[maxDepth] max depth of leaf node in the data tree.
 * @property[decorationsByDepth] decoration view of each internal nodes.
 * @property[helper] TreeItemDecoration.Helper object. Must not null. Recommends to implement this class in adapter.
 */
class TreeItemDecoration(
    private val maxDepth: Int,
    private val decorationsByDepth: Array<Decoration>,
    private val helper: Helper
): RecyclerView.ItemDecoration() {

    /**
     * Override function onDraw().
     */
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        if(parent.adapter == null) return

        val firstView = parent.getChildAt(0) ?: return
        val secondView = parent.getChildAt(1)

        if(secondView == null){
            for(depth in 0 until maxDepth) {
                drawRemainingDecorations(depth, parent, c, 0)
            }
        }

        val firstViewAdapterPosition = parent.getChildAdapterPosition(firstView)
        val secondViewAdapterPosition = parent.getChildAdapterPosition(secondView)

        if(firstViewAdapterPosition !in 0 until parent.adapter!!.itemCount ||
           secondViewAdapterPosition !in 0 until parent.adapter!!.itemCount)
            return

        for(depth in 0 until maxDepth){
            var hasDifferentParentNode = false
            for(i in 0 until depth){
                if(helper.getInternalNodeName(i, firstViewAdapterPosition) != helper.getInternalNodeName(i, secondViewAdapterPosition)){
                    hasDifferentParentNode = true
                    break
                }
            }

            val firstViewNodeNameInDepth = helper.getInternalNodeName(depth, firstViewAdapterPosition)
            val secondViewNodeNameInDepth = helper.getInternalNodeName(depth, secondViewAdapterPosition)

            if((firstViewNodeNameInDepth != secondViewNodeNameInDepth) || hasDifferentParentNode){
                drawSpecificDecoration(depth, firstViewAdapterPosition, firstView, c, firstView.top)

                drawSpecificDecoration(depth, secondViewAdapterPosition, secondView, c, secondView.top)

                drawRemainingDecorations(depth, parent, c, 2)
            }else{
                drawSpecificDecoration(depth, firstViewAdapterPosition, firstView, c, 0)

                drawRemainingDecorations(depth, parent, c, 1)
            }
        }

    }

    /**
     * Draws specific decoration into canvas.
     * @param[depth] Target depth to draw.
     * @param[leafNodePosition] adapterPosition of target node.
     * @param[leafNodeView] view of target node.
     * @param[canvas] Canvas object to draw at.
     * @param[dY] Y position to draw.
     */
    private fun drawSpecificDecoration(
        depth: Int,
        leafNodePosition: Int,
        leafNodeView: View,
        canvas: Canvas,
        dY: Int
    ){
        val decoration = decorationsByDepth[depth]
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            helper.getDecorationViewWidth(depth, leafNodePosition),
            View.MeasureSpec.EXACTLY
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(leafNodeView.height, View.MeasureSpec.EXACTLY)

        val decorationView = decoration.root

        decorationView.measure(widthSpec, heightSpec)
        decorationView.layout(0, 0, decorationView.measuredWidth, decorationView.measuredHeight)

        var dX = 0
        for(depthIndex in 0 until depth){
            dX += helper.getDecorationViewWidth(depthIndex, leafNodePosition)
        }

        helper.setupDecoration(depth, leafNodePosition, decorationsByDepth[depth])

        canvas.save()
        canvas.translate(dX.toFloat(), dY.toFloat())
        decorationView.draw(canvas)
        canvas.restore()
    }

    /**
     * Draws all remaining decorations in screen.
     * @param[depth] Target depth to draw.
     * @param[parent] RecyclerView which this TreeItemDecoration object attached to.
     * @param[canvas] Canvas object to draw at.
     * @param[startPosition] position of item to start drawing decorations.
     */
    private fun drawRemainingDecorations(
        depth: Int,
        parent: RecyclerView,
        canvas: Canvas,
        startPosition: Int
    ){
        var drawFinishedNodeName = if(startPosition == 0) null
        else helper.getInternalNodeName(depth, parent.getChildAdapterPosition(parent.getChildAt(startPosition - 1)))

        for(index in startPosition until parent.childCount){
            val leafNode = parent.getChildAt(index)
            val leafNodePosition = parent.getChildAdapterPosition(leafNode)

            if(leafNodePosition !in 0 until parent.adapter!!.itemCount) continue

            for(i in 0 until depth){
                if(leafNodePosition != 0 &&
                    (helper.getInternalNodeName(i, leafNodePosition) != helper.getInternalNodeName(i, leafNodePosition - 1))){
                    drawFinishedNodeName = null
                    break
                }
            }

            val internalNodeName = helper.getInternalNodeName(depth, leafNodePosition)
            if(drawFinishedNodeName != internalNodeName){
                drawFinishedNodeName = internalNodeName
                drawSpecificDecoration(depth, leafNodePosition, leafNode, canvas, leafNode.top)
            }
        }
    }

    /**
     * Decoration class. Holds view objects. Must extend this class, create object and pass to this TreeItemDecoration object.
     * Extend this class and initialize child views of root view in constructor.
     * After that, override Helper class's abstract function [Helper.setupDecoration] and setup views using 'as' keyword.
     */
    open class Decoration(val root: View)

    /**
     * Helper interface. Must implement somewhere and pass to this TreeItemDecoration object.
     */
    interface Helper {

        /**
         * Gets internal node's name with given depth and leafNodePosition.
         * @param[depth] depth of target internal node.
         * @param[leafNodePosition] position of leaf node to specify the internal node.
         */
        fun getInternalNodeName(depth: Int, leafNodePosition: Int): String

        /**
         * Gets internal node's decoration view width to measure decoration view.
         * @param[depth] depth of target decoration view.
         * @param[leafNodePosition] position of leaf node to specify the internal node's decoration view.
         */
        fun getDecorationViewWidth(depth: Int, leafNodePosition: Int): Int

        /**
         * Set decoration view something. May be setting text of TextView, image of ImageView, etc.
         * @param[depth] depth of target to set views differently.
         * @param[leafNodePosition] position of leaf node to set views differently.
         * @param[decoView] internal node's decoration view. Recommends not to call findViewById in this function.
         */
        fun setupDecoration(depth: Int, leafNodePosition: Int, decoView: Decoration)

    }

}