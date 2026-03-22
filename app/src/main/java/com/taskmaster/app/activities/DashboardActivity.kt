package com.taskmaster.app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskmaster.app.R
import com.taskmaster.app.adapters.TaskAdapter
import com.taskmaster.app.databinding.ActivityDashboardBinding
import com.taskmaster.app.models.Task
import com.taskmaster.app.models.Alarm
import com.taskmaster.app.utils.AlarmScheduler
import com.taskmaster.app.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    private var clockHandler: Handler? = null
    private var clockRunnable: Runnable? = null
    private var taskListener: ListenerRegistration? = null
    private var alarmListener: ListenerRegistration? = null
    private var noteListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI(user.displayName ?: user.email?.substringBefore("@") ?: "User")
        setupRecyclerView()
        startRealtimeClock()
        loadTodayTasks()
        loadCounts()
        setupBottomNav()
    }

    private fun setupUI(name: String) {
        binding.tvUserName.text = name

        // Greeting based on time
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good Morning ☀️"
            hour < 17 -> "Good Afternoon 🌤"
            else -> "Good Evening 🌙"
        }

        // Animate header
        binding.llHeader.alpha = 0f
        binding.llHeader.animate().alpha(1f).setDuration(600).start()

        // Animate stats
        val scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        binding.llStats.startAnimation(scaleIn)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(taskList,
            onTaskClick = { task ->
                startActivity(Intent(this, AddEditTaskActivity::class.java).apply {
                    putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.id)
                })
            },
            onCheckChanged = { task, checked -> updateTaskCompletion(task, checked) }
        )
        binding.rvTodayTasks.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = taskAdapter
        }
    }

    private fun startRealtimeClock() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

        clockRunnable = object : Runnable {
            override fun run() {
                val now = Date()
                binding.tvTime.text = timeFormat.format(now)
                binding.tvDate.text = dateFormat.format(now)
                clockHandler?.postDelayed(this, 1000)
            }
        }
        clockHandler = Handler(Looper.getMainLooper())
        clockHandler?.post(clockRunnable!!)
    }

    private fun loadTodayTasks() {
        val userId = auth.currentUser?.uid ?: return
        val todayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val today = todayFormat.format(Date())

        taskListener = db.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                taskList.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Task::class.java)?.let { taskList.add(it) }
                }
                taskAdapter.notifyDataSetChanged()
                binding.llEmpty.visibility = if (taskList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvTodayTasks.visibility = if (taskList.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
    }

    private fun loadCounts() {
        val userId = auth.currentUser?.uid ?: return

        // Tasks count
        db.collection("tasks").whereEqualTo("userId", userId)
            .addSnapshotListener { snap, _ ->
                binding.tvTaskCount.text = snap?.size()?.toString() ?: "0"
            }

        // Alarms count
        db.collection("alarms").whereEqualTo("userId", userId)
            .addSnapshotListener { snap, _ ->
                binding.tvAlarmCount.text = snap?.size()?.toString() ?: "0"
            }

        // Notes count
        db.collection("notes").whereEqualTo("userId", userId)
            .addSnapshotListener { snap, _ ->
                binding.tvNoteCount.text = snap?.size()?.toString() ?: "0"
            }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true  // already here
                R.id.nav_todos -> {
                    startActivity(Intent(this, TodoActivity::class.java))
                    overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out)
                    true
                }
                R.id.nav_alarms -> {
                    startActivity(Intent(this, AlarmActivity::class.java))
                    overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out)
                    true
                }
                R.id.nav_notes -> {
                    startActivity(Intent(this, NotesActivity::class.java))
                    overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out)
                    true
                }
                R.id.nav_profile -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }

        // Tap on stat cards
        binding.cardTasks.setOnClickListener {
            startActivity(Intent(this, TodoActivity::class.java))
        }
        binding.cardAlarmsStat.setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }
        binding.cardNotesStat.setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java))
        }
    }

    private fun updateTaskCompletion(task: Task, completed: Boolean) {
        db.collection("tasks").document(task.id)
            .update("isCompleted", completed)
            .addOnFailureListener { showToast("Failed to update task") }
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? This will also clear all scheduled alarms from this device.")
            .setPositiveButton("Logout") { _, _ ->
                clearAllAlarmsAndSignOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllAlarmsAndSignOut() {
        val userId = auth.currentUser?.uid ?: return
        
        // Fetch all alarms to cancel them in AlarmManager
        db.collection("alarms").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val alarmId = doc.id
                    listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
                        .forEach { day ->
                            AlarmScheduler.cancelAlarm(this, "${alarmId}_$day".hashCode())
                        }
                }
                // Also tasks
                db.collection("tasks").whereEqualTo("userId", userId).get().addOnSuccessListener { tSnap ->
                    tSnap.documents.forEach { tDoc ->
                        AlarmScheduler.cancelAlarm(this, "task_${tDoc.id}".hashCode())
                    }
                    performSignOut()
                }.addOnFailureListener { performSignOut() }
            }
            .addOnFailureListener { performSignOut() }
    }

    private fun performSignOut() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler?.removeCallbacks(clockRunnable!!)
        taskListener?.remove()
        alarmListener?.remove()
        noteListener?.remove()
    }
}
