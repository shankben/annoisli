/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shankware.annoisli

import android.content.Context
import android.database.DataSetObservable
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*

class HeaderGridView : GridView {
    private class FixedViewInfo {
        var view: View? = null
        var viewContainer: ViewGroup? = null
        var data: Any? = null
        var isSelectable = false
    }

    private val mHeaderViewInfos = ArrayList<FixedViewInfo>()

    private fun initHeaderGridView() {
        super.setClipChildren(false)
    }

    constructor(context: Context?) : super(context) {
        initHeaderGridView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initHeaderGridView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initHeaderGridView()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val adapter = adapter
        if (adapter != null && adapter is HeaderViewGridAdapter) {
            adapter.setNumColumns(numColumns)
        }
    }

    override fun setClipChildren(clipChildren: Boolean) {
        // Ignore, since the header rows depend on not being clipped
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     *
     *
     * NOTE: Call this before calling setAdapter. This is so HeaderGridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v            The view to add.
     * @param data         Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    @JvmOverloads
    fun addHeaderView(v: View?, data: Any? = null, isSelectable: Boolean = true) {
        val adapter = adapter
        check(!(adapter != null && adapter !is HeaderViewGridAdapter)) { "Cannot add header view to grid -- setAdapter has already been called." }
        val info = FixedViewInfo()
        val fl: FrameLayout = FullWidthFixedViewLayout(
            context
        )
        fl.addView(v)
        info.view = v
        info.viewContainer = fl
        info.data = data
        info.isSelectable = isSelectable
        mHeaderViewInfos.add(info)
        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            (adapter as HeaderViewGridAdapter).notifyDataSetChanged()
        }
    }

    val headerViewCount: Int
        get() = mHeaderViewInfos.size

    /**
     * Removes a previously-added header view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a header
     * view
     */
    fun removeHeaderView(v: View): Boolean {
        if (mHeaderViewInfos.size > 0) {
            var result = false
            val adapter = adapter
            if (adapter != null && (adapter as HeaderViewGridAdapter).removeHeader(v)) {
                result = true
            }
            removeFixedViewInfo(v, mHeaderViewInfos)
            return result
        }
        return false
    }

    private fun removeFixedViewInfo(v: View, where: ArrayList<FixedViewInfo>) {
        val len = where.size
        for (i in 0 until len) {
            val info = where[i]
            if (info.view === v) {
                where.removeAt(i)
                break
            }
        }
    }

    override fun setAdapter(adapter: ListAdapter) {
        if (mHeaderViewInfos.size > 0) {
            val headerAdapter = HeaderViewGridAdapter(mHeaderViewInfos, adapter)
            val numColumns = numColumns
            if (numColumns > 1) {
                headerAdapter.setNumColumns(numColumns)
            }
            super.setAdapter(headerAdapter)
        } else {
            super.setAdapter(adapter)
        }
    }

    private inner class FullWidthFixedViewLayout(context: Context?) :
        FrameLayout(context!!) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            var widthMeasureSpec = widthMeasureSpec
            val targetWidth = this@HeaderGridView.measuredWidth -
                    this@HeaderGridView.paddingLeft - this@HeaderGridView.paddingRight
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                targetWidth,
                MeasureSpec.getMode(widthMeasureSpec)
            )
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    /**
     * ListAdapter used when a HeaderGridView has header views. This ListAdapter
     * wraps another one and also keeps track of the header views and their
     * associated data objects.
     *
     * This is intended as a base class; you will probably not need to
     * use this class directly in your own code.
     */
    private class HeaderViewGridAdapter(
        headerViewInfos: ArrayList<FixedViewInfo>?,
        private val mAdapter: ListAdapter
    ) :
        WrapperListAdapter, Filterable {
        // This is used to notify the container of updates relating to number of columns
        // or headers changing, which changes the number of placeholders needed
        private val mDataSetObservable = DataSetObservable()
        private var mNumColumns = 1

        // This ArrayList is assumed to NOT be null.
        var mHeaderViewInfos: ArrayList<FixedViewInfo>
        var mAreAllFixedViewsSelectable: Boolean
        private val mIsFilterable: Boolean = mAdapter is Filterable
        val headersCount: Int
            get() = mHeaderViewInfos.size

        override fun isEmpty(): Boolean = mAdapter.isEmpty && headersCount == 0

        fun setNumColumns(numColumns: Int) {
            require(numColumns >= 1) { "Number of columns must be 1 or more" }
            if (mNumColumns != numColumns) {
                mNumColumns = numColumns
                notifyDataSetChanged()
            }
        }

        private fun areAllListInfosSelectable(infos: ArrayList<FixedViewInfo>?): Boolean {
            if (infos != null) {
                for (info in infos) {
                    if (!info.isSelectable) {
                        return false
                    }
                }
            }
            return true
        }

        fun removeHeader(v: View): Boolean {
            for (i in mHeaderViewInfos.indices) {
                val info = mHeaderViewInfos[i]
                if (info.view === v) {
                    mHeaderViewInfos.removeAt(i)
                    mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos)
                    mDataSetObservable.notifyChanged()
                    return true
                }
            }
            return false
        }

        override fun getCount(): Int = headersCount * mNumColumns + mAdapter.count

        override fun areAllItemsEnabled(): Boolean =
            mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled()


        override fun isEnabled(position: Int): Boolean {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            val numHeadersAndPlaceholders = headersCount * mNumColumns
            if (position < numHeadersAndPlaceholders) {
                return (position % mNumColumns == 0
                        && mHeaderViewInfos[position / mNumColumns].isSelectable)
            }
            // Adapter
            val adjPosition = position - numHeadersAndPlaceholders
            var adapterCount = 0
            adapterCount = mAdapter.count
            if (adjPosition < adapterCount) {
                return mAdapter.isEnabled(adjPosition)
            }
            throw ArrayIndexOutOfBoundsException(position)
        }

        override fun getItem(position: Int): Any? {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            val numHeadersAndPlaceholders = headersCount * mNumColumns
            if (position < numHeadersAndPlaceholders) {
                return if (position % mNumColumns == 0) {
                    mHeaderViewInfos[position / mNumColumns].data!!
                } else null
            }
            // Adapter
            val adjPosition = position - numHeadersAndPlaceholders
            var adapterCount = 0
            adapterCount = mAdapter.count
            if (adjPosition < adapterCount) {
                return mAdapter.getItem(adjPosition)
            }
            throw ArrayIndexOutOfBoundsException(position)
        }

        override fun getItemId(position: Int): Long {
            val numHeadersAndPlaceholders = headersCount * mNumColumns
            if (position >= numHeadersAndPlaceholders) {
                val adjPosition = position - numHeadersAndPlaceholders
                val adapterCount = mAdapter.count
                if (adjPosition < adapterCount) {
                    return mAdapter.getItemId(adjPosition)
                }
            }
            return -1
        }

        override fun hasStableIds(): Boolean {
            return mAdapter?.hasStableIds() ?: false
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            var convertView: View? = convertView
            val numHeadersAndPlaceholders = headersCount * mNumColumns
            if (position < numHeadersAndPlaceholders) {
                val headerViewContainer: View? =
                    mHeaderViewInfos[position / mNumColumns].viewContainer
                return if (position % mNumColumns == 0) {
                    headerViewContainer!!
                } else {
                    if (convertView == null) {
                        convertView = View(parent?.context)
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.visibility = INVISIBLE
                    convertView.minimumHeight = headerViewContainer!!.height
                    convertView
                }
            }
            // Adapter
            val adjPosition = position - numHeadersAndPlaceholders
            var adapterCount = 0
            adapterCount = mAdapter.count
            if (adjPosition < adapterCount) {
                return mAdapter.getView(adjPosition, convertView, parent)
            }
            throw ArrayIndexOutOfBoundsException(position)
        }

        override fun getItemViewType(position: Int): Int {
            val numHeadersAndPlaceholders = headersCount * mNumColumns
            if (position < numHeadersAndPlaceholders && position % mNumColumns != 0) {
                // Placeholders get the last view type number
                return mAdapter.viewTypeCount ?: 1
            }
            if (position >= numHeadersAndPlaceholders) {
                val adjPosition = position - numHeadersAndPlaceholders
                val adapterCount = mAdapter.count
                if (adjPosition < adapterCount) {
                    return mAdapter.getItemViewType(adjPosition)
                }
            }
            return ITEM_VIEW_TYPE_HEADER_OR_FOOTER
        }

        override fun getViewTypeCount(): Int = mAdapter.viewTypeCount + 1

        override fun registerDataSetObserver(observer: DataSetObserver) {
            mDataSetObservable.registerObserver(observer)
            mAdapter.registerDataSetObserver(observer)
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            mDataSetObservable.unregisterObserver(observer)
            mAdapter.unregisterDataSetObserver(observer)
        }

        override fun getFilter(): Filter? {
            return if (mIsFilterable) { (mAdapter as Filterable).filter } else null
        }

        override fun getWrappedAdapter(): ListAdapter = mAdapter

        fun notifyDataSetChanged() {
            mDataSetObservable.notifyChanged()
        }

        init {
            requireNotNull(headerViewInfos) { "headerViewInfos cannot be null" }
            mHeaderViewInfos = headerViewInfos
            mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos)
        }
    }

    companion object {
        private const val TAG = "HeaderGridView"
    }
}