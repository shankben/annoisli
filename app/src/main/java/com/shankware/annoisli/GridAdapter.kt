package com.shankware.annoisli

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

class GridAdapter(private val mContext: Context, gridItems: Array<GridItem>) : BaseAdapter() {
    private val mGridItems: Array<GridItem> = gridItems

    override fun getCount(): Int {
        return mGridItems.size
    }

    override fun getItem(i: Int): Any {
        return mGridItems[i]
    }

    override fun getItemId(i: Int): Long {
        return mGridItems[i].hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val gridItem: GridItem? = mGridItems[position]
        if (gridItem?.view == null) {
            gridItem?.view = LayoutInflater.from(mContext).inflate(
                R.layout.grid_item, parent,
                false
            )
        }
        return gridItem!!.view!!
    }

}