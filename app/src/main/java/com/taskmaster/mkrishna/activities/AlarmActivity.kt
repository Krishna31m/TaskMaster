package com.taskmaster.mkrishna.activities

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskmaster.mkrishna.R
import com.taskmaster.mkrishna.adapters.AlarmAdapter
import com.taskmaster.mkrishna.databinding.ActivityAlarmBinding
import com.taskmaster.mkrishna.databinding.DialogAddAlarmBinding
import com.taskmaster.mkrishna.models.Alarm
import com.taskmaster.mkrishna.utils.AlarmScheduler
import com.taskmaster.mkrishna.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableListOf<Alarm>()
    private var alarmListener: ListenerRegistration? = null

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val dayChipMap = linkedMapOf(
        R.id.chip_sun to Calendar.SUNDAY,
        R.id.chip_mon to Calendar.MONDAY,
        R.id.chip_tue to Calendar.TUESDAY,
        R.id.chip_wed to Calendar.WEDNESDAY,
        R.id.chip_thu to Calendar.THURSDAY,
        R.id.chip_fri to Calendar.FRIDAY,
        R.id.chip_sat to Calendar.SATURDAY
    )

    private val dayNames = mapOf(
        Calendar.SUNDAY    to "Sun",
        Calendar.MONDAY    to "Mon",
        Calendar.TUESDAY   to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY  to "Thu",
        Calendar.FRIDAY    to "Fri",
        Calendar.SATURDAY  to "Sat"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadAlarms()

        binding.fabAdd.setOnClickListener {
            binding.fabAdd.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce))
            showAddAlarmDialog()
        }
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(alarmList,
            onToggle = { alarm, enabled -> toggleAlarm(alarm, enabled) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )
        binding.rvAlarms.apply {
            layoutManager = LinearLayoutManager(this@AlarmActivity)
            adapter = alarmAdapter
        }
    }

    private fun loadAlarms() {
        val userId = auth.currentUser?.uid ?: return
        alarmListener = db.collection("alarms")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                alarmList.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Alarm::class.java)?.let { alarmList.add(it) }
                }
                alarmList.sortBy { it.time }
                alarmAdapter.notifyDataSetChanged()
                binding.llEmpty.visibility = if (alarmList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvAlarms.visibility  = if (alarmList.isEmpty()) View.GONE  else View.VISIBLE
            }
    }

    private fun showAddAlarmDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_MaterialComponents_BottomSheetDialog)
        val dialogBinding = DialogAddAlarmBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        var selectedTime = ""
        var selectedHour = -1
        var selectedMinute = -1
        val selectedDays = mutableSetOf<Int>()
        var everydaySelected = false

        // Time picker
        dialogBinding.etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, min ->
                selectedHour = hour
                selectedMinute = min
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
                selectedTime = timeFormat.format(cal.time)
                dialogBinding.etTime.setText(selectedTime)
                dialogBinding.tilTime.error = null
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        // Individual day chips
        dayChipMap.forEach { (chipId, calDay) ->
            val chip = dialogBinding.root.findViewById<Chip>(chipId)
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedDays.add(calDay)
                    if (selectedDays.size == 7) {
                        everydaySelected = true
                        dialogBinding.chipEveryday.isChecked = true
                        dayChipMap.keys.forEach { id ->
                            dialogBinding.root.findViewById<Chip>(id).isChecked = false
                        }
                        selectedDays.clear()
                    }
                } else {
                    selectedDays.remove(calDay)
                }
                updateSummary(dialogBinding, selectedDays, everydaySelected)
            }
        }

        // Everyday chip
        dialogBinding.chipEveryday.setOnCheckedChangeListener { _, isChecked ->
            everydaySelected = isChecked
            if (isChecked) {
                dayChipMap.keys.forEach { id ->
                    dialogBinding.root.findViewById<Chip>(id).isChecked = false
                }
                selectedDays.clear()
            }
            updateSummary(dialogBinding, selectedDays, everydaySelected)
        }

        // Save
        dialogBinding.btnSaveAlarm.setOnClickListener {
            val label = dialogBinding.etLabel.text.toString().trim().ifEmpty { "Alarm" }

            if (selectedTime.isEmpty()) {
                dialogBinding.tilTime.error = "Please select a time"
                return@setOnClickListener
            }
            if (!everydaySelected && selectedDays.isEmpty()) {
                showToast("Please select at least one day")
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val daysToSchedule: List<Int> = if (everydaySelected) {
                listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                    Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
            } else {
                selectedDays.sortedBy { it }
            }

            val daysLabel = if (everydaySelected) "Everyday"
            else daysToSchedule.mapNotNull { dayNames[it] }.joinToString(", ")

            val alarm = Alarm(
                userId    = userId,
                label     = label,
                time      = selectedTime,
                date      = daysLabel,
                timestamp = 0L,
                isEnabled = true,
                isRepeat  = everydaySelected || daysToSchedule.size > 1
            )

            db.collection("alarms").add(alarm)
                .addOnSuccessListener { ref ->
                    daysToSchedule.forEach { calDay ->
                        val triggerMs = nextTriggerForDay(calDay, selectedHour, selectedMinute)
                        AlarmScheduler.scheduleAlarm(
                            this,
                            "${ref.id}_$calDay",
                            label,
                            triggerMs
                        )
                    }
                    showToast("Alarm set for $daysLabel at $selectedTime")
                    dialog.dismiss()
                }
                .addOnFailureListener { showToast("Failed to set alarm") }
        }

        dialog.show()
    }

    private fun nextTriggerForDay(calDay: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun updateSummary(
        dialogBinding: DialogAddAlarmBinding,
        selectedDays: Set<Int>,
        everydaySelected: Boolean
    ) {
        dialogBinding.tvDaysSummary.text = when {
            everydaySelected       -> "📅 Repeats every day"
            selectedDays.isEmpty() -> "No days selected"
            selectedDays.size == 1 -> "Once on ${dayNames[selectedDays.first()]}"
            else -> "Every ${selectedDays.sortedBy { it }.mapNotNull { dayNames[it] }.joinToString(", ")}"
        }
    }

    private fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        db.collection("alarms").document(alarm.id).update("isEnabled", enabled)
            .addOnSuccessListener {
                if (!enabled) {
                    listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
                        .forEach { day ->
                            AlarmScheduler.cancelAlarm(this, "${alarm.id}_$day".hashCode())
                        }
                }
            }
    }

    private fun deleteAlarm(alarm: Alarm) {
        db.collection("alarms").document(alarm.id).delete()
            .addOnSuccessListener {
                listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                    Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
                    .forEach { day ->
                        AlarmScheduler.cancelAlarm(this, "${alarm.id}_$day".hashCode())
                    }
                showToast(getString(R.string.alarm_deleted))
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmListener?.remove()
    }
}