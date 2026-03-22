package com.taskmaster.app.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskmaster.app.R
import com.taskmaster.app.adapters.AlarmAdapter
import com.taskmaster.app.databinding.ActivityAlarmBinding
import com.taskmaster.app.databinding.DialogAddAlarmBinding
import com.taskmaster.app.models.Alarm
import com.taskmaster.app.utils.AlarmScheduler
import com.taskmaster.app.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableListOf<Alarm>()
    private var alarmListener: ListenerRegistration? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

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
                alarmList.sortBy { it.timestamp }
                alarmAdapter.notifyDataSetChanged()
                binding.llEmpty.visibility = if (alarmList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvAlarms.visibility = if (alarmList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun showAddAlarmDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_MaterialComponents_BottomSheetDialog)
        val dialogBinding = DialogAddAlarmBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        var selectedDate = ""
        var selectedTime = ""

        dialogBinding.etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = dateFormat.format(cal.time)
                dialogBinding.etDate.setText(selectedDate)
                dialogBinding.tilDate.error = null
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogBinding.etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, min ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
                selectedTime = timeFormat.format(cal.time)
                dialogBinding.etTime.setText(selectedTime)
                dialogBinding.tilTime.error = null
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        dialogBinding.btnSaveAlarm.setOnClickListener {
            val label = dialogBinding.etLabel.text.toString().trim().ifEmpty { "Alarm" }

            if (selectedDate.isEmpty()) {
                dialogBinding.tilDate.error = "Select a date"; return@setOnClickListener
            }
            if (selectedTime.isEmpty()) {
                dialogBinding.tilTime.error = "Select a time"; return@setOnClickListener
            }

            val timestamp = parseTimestamp(selectedDate, selectedTime)
            if (timestamp <= System.currentTimeMillis()) {
                showToast("Please select a future time"); return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val alarm = Alarm(
                userId = userId,
                label = label,
                date = selectedDate,
                time = selectedTime,
                timestamp = timestamp,
                isEnabled = true,
                isRepeat = dialogBinding.switchRepeat.isChecked
            )

            db.collection("alarms").add(alarm)
                .addOnSuccessListener { ref ->
                    AlarmScheduler.scheduleAlarm(this, ref.id, label, timestamp)
                    showToast("${getString(R.string.alarm_set)} $selectedTime")
                    dialog.dismiss()
                }
                .addOnFailureListener { showToast("Failed to set alarm") }
        }

        dialog.show()
    }

    private fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        db.collection("alarms").document(alarm.id)
            .update("isEnabled", enabled)
            .addOnSuccessListener {
                if (enabled) {
                    AlarmScheduler.scheduleAlarm(this, alarm.id, alarm.label, alarm.timestamp)
                } else {
                    AlarmScheduler.cancelAlarm(this, alarm.id.hashCode())
                }
            }
    }

    private fun deleteAlarm(alarm: Alarm) {
        db.collection("alarms").document(alarm.id).delete()
            .addOnSuccessListener {
                AlarmScheduler.cancelAlarm(this, alarm.id.hashCode())
                showToast(getString(R.string.alarm_deleted))
            }
    }

    private fun parseTimestamp(date: String, time: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
            sdf.parse("$date $time")?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmListener?.remove()
    }
}
