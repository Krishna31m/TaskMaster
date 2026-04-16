package com.taskmaster.mkrishna.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.taskmaster.mkrishna.R
import com.taskmaster.mkrishna.databinding.ItemAlarmBinding
import com.taskmaster.mkrishna.models.Alarm

class AlarmAdapter(
    private val alarms: List<Alarm>,
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    private var lastAnimatedPosition = -1

    inner class AlarmViewHolder(val binding: ItemAlarmBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]

        with(holder.binding) {
            tvTime.text = alarm.time
            tvLabel.text = alarm.label.ifEmpty { "Alarm" }
            tvDate.text = alarm.date

            // Suppress listener before setting state
            switchAlarm.setOnCheckedChangeListener(null)
            switchAlarm.isChecked = alarm.isEnabled

            // Dim card if disabled
            root.alpha = if (alarm.isEnabled) 1f else 0.55f

            switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            btnDelete.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.scale_in))
                onDelete(alarm)
            }
        }

        if (position > lastAnimatedPosition) {
            val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_up)
            holder.itemView.startAnimation(anim)
            lastAnimatedPosition = position
        }
    }

    override fun getItemCount() = alarms.size
}
