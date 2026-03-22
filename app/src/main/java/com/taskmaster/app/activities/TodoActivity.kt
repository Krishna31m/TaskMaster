package com.taskmaster.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskmaster.app.R
import com.taskmaster.app.adapters.TaskAdapter
import com.taskmaster.app.databinding.ActivityTodoBinding
import com.taskmaster.app.models.Task
import com.taskmaster.app.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class TodoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodoBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var taskAdapter: TaskAdapter
    private val allTasks = mutableListOf<Task>()
    private val filteredTasks = mutableListOf<Task>()
    private var taskListener: ListenerRegistration? = null

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupFilterChips()
        loadTasks()

        binding.fabAdd.setOnClickListener {
            binding.fabAdd.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce))
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(filteredTasks,
            onTaskClick = { task ->
                startActivity(Intent(this, AddEditTaskActivity::class.java).apply {
                    putExtra(EXTRA_TASK_ID, task.id)
                })
            },
            onCheckChanged = { task, checked ->
                db.collection("tasks").document(task.id)
                    .update("isCompleted", checked)
                    .addOnFailureListener { showToast("Update failed") }
            }
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@TodoActivity)
            adapter = taskAdapter
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            applyFilter(checkedIds.firstOrNull() ?: R.id.chip_all)
        }
    }

    private fun applyFilter(chipId: Int) {
        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        filteredTasks.clear()
        when (chipId) {
            R.id.chip_all -> filteredTasks.addAll(allTasks)
            R.id.chip_today -> filteredTasks.addAll(allTasks.filter { it.date == today })
            R.id.chip_upcoming -> {
                val todayCal = Calendar.getInstance()
                filteredTasks.addAll(allTasks.filter {
                    try {
                        val taskDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(it.date)
                        taskDate != null && taskDate.after(todayCal.time)
                    } catch (e: Exception) { false }
                })
            }
            R.id.chip_completed -> filteredTasks.addAll(allTasks.filter { it.isCompleted })
        }
        taskAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun loadTasks() {
        val userId = auth.currentUser?.uid ?: return
        taskListener = db.collection("tasks")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { showToast("Failed to load tasks"); return@addSnapshotListener }
                allTasks.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Task::class.java)?.let { allTasks.add(it) }
                }
                // Sort: incomplete first, then by date
                allTasks.sortWith(compareBy({ it.isCompleted }, { it.date }))
                val checkedId = binding.chipGroupFilter.checkedChipId
                applyFilter(if (checkedId == View.NO_ID) R.id.chip_all else checkedId)
            }
    }

    private fun updateEmptyState() {
        val isEmpty = filteredTasks.isEmpty()
        binding.llEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTasks.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Reload to reflect any changes
    }

    override fun onDestroy() {
        super.onDestroy()
        taskListener?.remove()
    }
}
