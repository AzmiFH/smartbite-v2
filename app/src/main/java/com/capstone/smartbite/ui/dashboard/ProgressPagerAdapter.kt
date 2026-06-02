package com.capstone.smartbite.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.smartbite.R

class ProgressPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PROGRESS = 0
        private const val TYPE_EMPTY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_PROGRESS else TYPE_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_PROGRESS) {
            val view = inflater.inflate(R.layout.item_daily_progress, parent, false)
            ProgressViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_empty_progress, parent, false)
            EmptyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Data statis dummy di XML
    }

    override fun getItemCount(): Int = 2

    class ProgressViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}