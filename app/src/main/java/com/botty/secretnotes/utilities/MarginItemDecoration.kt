package com.botty.secretnotes.utilities

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.dip

private class MarginItemDecoration(private val dipMargin: Int, private val isGridLayout: Boolean) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {

        with(outRect) {
            if (parent.getChildAdapterPosition(view) == 0 || isGridLayout) {
                top = dipMargin
            }
            left =  dipMargin
            right = dipMargin
            bottom = dipMargin
        }
    }
}

fun RecyclerView.addItemsMargins(dipMargin: Int, isGridLayout: Boolean) {
    addItemDecoration(MarginItemDecoration(dip(dipMargin), isGridLayout))
}