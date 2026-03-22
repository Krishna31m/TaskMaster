package com.taskmaster.app.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.taskmaster.app.R
import com.taskmaster.app.databinding.ItemTaskBinding
import com.taskmaster.app.models.Task

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onCheckChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var lastAnimatedPosition = -1

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val ctx = holder.itemView.context

        with(holder.binding) {
            tvTitle.text = task.title

            if (task.description.isNotEmpty()) {
                tvDescription.text = task.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            tvDateTime.text = buildString {
                append(task.date)
                if (task.time.isNotEmpty()) append(" • ${task.time}")
            }

            // Priority badge + strip
            val (priorityLabel, priorityColor) = when (task.priority) {
                "high" -> "High" to ContextCompat.getColor(ctx, R.color.priority_high)
                "low" -> "Low" to ContextCompat.getColor(ctx, R.color.priority_low)
                else -> "Med" to ContextCompat.getColor(ctx, R.color.priority_medium)
            }
            tvPriority.text = priorityLabel
            tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(priorityColor)
            viewPriorityStrip.setBackgroundColor(priorityColor)

            // Completed state
            cbCompleted.setOnCheckedChangeListener(null)
            cbCompleted.isChecked = task.isCompleted
            if (task.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.5f
                root.alpha = 0.75f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1f
                root.alpha = 1f
            }

            cbCompleted.setOnCheckedChangeListener { _, checked ->
                onCheckChanged(task, checked)
            }

            root.setOnClickListener { onTaskClick(task) }
        }

        // Item entrance animation
        if (position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_up)
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }

    override fun getItemCount() = tasks.size
}
