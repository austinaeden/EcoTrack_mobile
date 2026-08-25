package com.example.ecotrack.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotrack.R
import com.example.ecotrack.data.ActivityLog

class ActivityAdapter : ListAdapter<ActivityLog, ActivityAdapter.LogViewHolder>(DiffCallback) {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSteps: TextView = itemView.findViewById(R.id.tvSteps)
        val tvTemp: TextView = itemView.findViewById(R.id.tvTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvTitle.text = item.title
        holder.tvSteps.text = "Steps: ${item.stepCount}"
        holder.tvTemp.text = "Temp: ${item.temperature}°C"
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean =
            oldItem == newItem
    }
}