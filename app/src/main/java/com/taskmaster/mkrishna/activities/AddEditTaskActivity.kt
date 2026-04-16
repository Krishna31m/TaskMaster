package com.taskmaster.mkrishna.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.taskmaster.mkrishna.R
import com.taskmaster.mkrishna.databinding.ActivityAddEditTaskBinding
import com.taskmaster.mkrishna.models.Task
import com.taskmaster.mkrishna.utils.AlarmScheduler
import com.taskmaster.mkrishna.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var taskId: String? = null
    private var existingTask: Task? = null
    private var selectedDate = ""
    private var selectedTime = ""
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val isEditMode = taskId != null

        binding.tvTitle.text = if (isEditMode) getString(R.string.edit_task) else getString(R.string.add_task)
        binding.btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE

        setupClickListeners()
        setupDateTimePickers()

        if (isEditMode) loadTask(taskId!!)

        // Animate card
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.cardForm.startAnimation(slideUp)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveTask() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun setupDateTimePickers() {
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.tilDate.setStartIconOnClickListener { showDatePicker() }
        binding.etTime.setOnClickListener { showTimePicker() }
        binding.tilTime.setStartIconOnClickListener { showTimePicker() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            selectedDate = dateFormat.format(cal.time)
            binding.etDate.setText(selectedDate)
            binding.tilDate.error = null
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            selectedTime = timeFormat.format(cal.time)
            binding.etTime.setText(selectedTime)
            binding.tilTime.error = null
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun loadTask(id: String) {
        db.collection("tasks").document(id).get()
            .addOnSuccessListener { doc ->
                existingTask = doc.toObject(Task::class.java) ?: return@addOnSuccessListener
                existingTask?.let { task ->
                    binding.etTitle.setText(task.title)
                    binding.etDescription.setText(task.description)
                    binding.etDate.setText(task.date)
                    binding.etTime.setText(task.time)
                    selectedDate = task.date
                    selectedTime = task.time
                    binding.switchAlarm.isChecked = task.hasAlarm
                    when (task.priority) {
                        "high" -> binding.rbHigh.isChecked = true
                        "low" -> binding.rbLow.isChecked = true
                        else -> binding.rbMedium.isChecked = true
                    }
                }
            }
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        binding.tilTitle.error = null
        binding.tilDate.error = null
        binding.tilTime.error = null

        if (title.isEmpty()) { binding.tilTitle.error = getString(R.string.error_empty_title); return }
        if (selectedDate.isEmpty() && binding.etDate.text.isNullOrEmpty()) {
            binding.tilDate.error = getString(R.string.error_select_date); return
        }
        if (selectedTime.isEmpty() && binding.etTime.text.isNullOrEmpty()) {
            binding.tilTime.error = getString(R.string.error_select_time); return
        }

        val dateStr = binding.etDate.text.toString().ifEmpty { selectedDate }
        val timeStr = binding.etTime.text.toString().ifEmpty { selectedTime }

        val priority = when {
            binding.rbHigh.isChecked -> "high"
            binding.rbLow.isChecked -> "low"
            else -> "medium"
        }

        val hasAlarm = binding.switchAlarm.isChecked
        var alarmTimestamp = 0L
        if (hasAlarm && dateStr.isNotEmpty() && timeStr.isNotEmpty()) {
            alarmTimestamp = parseTimestamp(dateStr, timeStr)
        }

        val userId = auth.currentUser?.uid ?: return
        val task = Task(
            id = taskId ?: "",
            userId = userId,
            title = title,
            description = description,
            date = dateStr,
            time = timeStr,
            priority = priority,
            isCompleted = existingTask?.isCompleted ?: false,
            hasAlarm = hasAlarm,
            alarmTimestamp = alarmTimestamp,
            updatedAt = System.currentTimeMillis()
        )

        if (taskId == null) {
            db.collection("tasks").add(task)
                .addOnSuccessListener { ref ->
                    if (hasAlarm && alarmTimestamp > System.currentTimeMillis()) {
                        AlarmScheduler.scheduleTaskReminder(this, ref.id, title, alarmTimestamp)
                    }
                    showToast("Task added!")
                    finish()
                }
                .addOnFailureListener { showToast("Failed to add task") }
        } else {
            db.collection("tasks").document(taskId!!).set(task)
                .addOnSuccessListener {
                    if (hasAlarm && alarmTimestamp > System.currentTimeMillis()) {
                        AlarmScheduler.scheduleTaskReminder(this, taskId!!, title, alarmTimestamp)
                    }
                    showToast("Task updated!")
                    finish()
                }
                .addOnFailureListener { showToast("Failed to update task") }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_task))
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> deleteTask() }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun deleteTask() {
        taskId?.let { id ->
            db.collection("tasks").document(id).delete()
                .addOnSuccessListener {
                    AlarmScheduler.cancelAlarm(this, id.hashCode())
                    showToast(getString(R.string.task_deleted))
                    finish()
                }
                .addOnFailureListener { showToast("Failed to delete task") }
        }
    }

    private fun parseTimestamp(date: String, time: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
            sdf.parse("$date $time")?.time ?: 0L
        } catch (e: Exception) { 0L }
    }
}
