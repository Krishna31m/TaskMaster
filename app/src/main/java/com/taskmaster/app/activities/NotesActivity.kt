package com.taskmaster.app.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.taskmaster.app.R
import com.taskmaster.app.adapters.NoteAdapter
import com.taskmaster.app.databinding.ActivityNotesBinding
import com.taskmaster.app.models.Note
import com.taskmaster.app.utils.showToast

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var noteAdapter: NoteAdapter
    private val allNotes = mutableListOf<Note>()
    private val filteredNotes = mutableListOf<Note>()
    private var noteListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupSearch()
        loadNotes()

        binding.fabAdd.setOnClickListener {
            binding.fabAdd.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce))
            startActivity(Intent(this, AddEditNoteActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(filteredNotes,
            onNoteClick = { note ->
                startActivity(Intent(this, AddEditNoteActivity::class.java).apply {
                    putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.id)
                })
            },
            onNoteDelete = { note -> deleteNote(note) }
        )
        binding.rvNotes.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = noteAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterNotes(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterNotes(query: String) {
        filteredNotes.clear()
        if (query.isBlank()) {
            filteredNotes.addAll(allNotes)
        } else {
            val lq = query.lowercase()
            filteredNotes.addAll(allNotes.filter {
                it.title.lowercase().contains(lq) || it.content.lowercase().contains(lq)
            })
        }
        noteAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun loadNotes() {
        val userId = auth.currentUser?.uid ?: return
        noteListener = db.collection("notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { showToast("Failed to load notes"); return@addSnapshotListener }
                allNotes.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Note::class.java)?.let { allNotes.add(it) }
                }
                allNotes.sortByDescending { it.updatedAt }
                filteredNotes.clear()
                filteredNotes.addAll(allNotes)
                noteAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
    }

    private fun deleteNote(note: Note) {
        db.collection("notes").document(note.id).delete()
            .addOnSuccessListener { showToast(getString(R.string.note_deleted)) }
            .addOnFailureListener { showToast("Failed to delete note") }
    }

    private fun updateEmptyState() {
        val isEmpty = filteredNotes.isEmpty()
        binding.llEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvNotes.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        noteListener?.remove()
    }
}
