package com.capstone.smartbite.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.smartbite.R

class ProgressPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DAILY = 0
        private const val TYPE_WEEKLY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_DAILY else TYPE_WEEKLY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DAILY) {
            val view = inflater.inflate(R.layout.item_daily_progress, parent, false)
            DailyViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_weekly_progress, parent, false)
            WeeklyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Data statis dummy di XML sesuai permintaan
    }

    override fun getItemCount(): Int = 2

    class DailyViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class WeeklyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}