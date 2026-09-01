package com.example.ecotrack.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotrack.R
import com.example.ecotrack.data.DailyStep

class DailyStepAdapter : ListAdapter<DailyStep, DailyStepAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSteps: TextView = view.findViewById(R.id.tvSteps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvDate.text = item.date
        holder.tvSteps.text = "${item.steps} Steps"
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DailyStep>() {
        override fun areItemsTheSame(oldItem: DailyStep, newItem: DailyStep): Boolean = oldItem.date == newItem.date
        override fun areContentsTheSame(oldItem: DailyStep, newItem: DailyStep): Boolean = oldItem == newItem
    }
}
