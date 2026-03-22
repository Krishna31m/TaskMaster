package com.taskmaster.app.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.taskmaster.app.R
import com.taskmaster.app.databinding.ActivityAddEditNoteBinding
import com.taskmaster.app.models.Note
import com.taskmaster.app.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class AddEditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditNoteBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var noteId: String? = null
    private var selectedColor = "#FFE4E1"
    private var isColorPickerVisible = false

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        val NOTE_COLORS = listOf(
            "#FFE4E1", "#E1F5FE", "#F3E5F5",
            "#E8F5E9", "#FFF8E1", "#FCE4EC"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val isEditMode = noteId != null

        binding.tvScreenTitle.text = if (isEditMode) getString(R.string.edit_note) else getString(R.string.add_note)
        binding.btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE

        binding.tvDate.text = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date())

        if (isEditMode) loadNote(noteId!!)

        setupColorPicker()
        setupClickListeners()

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.cardNote.startAnimation(slideUp)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveNote() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnColor.setOnClickListener { toggleColorPicker() }
    }

    private fun setupColorPicker() {
        val colorViews = listOf(
            binding.color1, binding.color2, binding.color3,
            binding.color4, binding.color5, binding.color6
        )
        colorViews.forEachIndexed { index, view ->
            view.setBackgroundColor(Color.parseColor(NOTE_COLORS[index]))
            view.setOnClickListener {
                selectedColor = NOTE_COLORS[index]
                applyNoteColor(selectedColor)
                toggleColorPicker()
            }
        }
    }

    private fun toggleColorPicker() {
        isColorPickerVisible = !isColorPickerVisible
        if (isColorPickerVisible) {
            binding.cardColorPicker.visibility = View.VISIBLE
            binding.cardColorPicker.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
        } else {
            binding.cardColorPicker.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down))
            binding.cardColorPicker.visibility = View.GONE
        }
    }

    private fun applyNoteColor(hexColor: String) {
        try {
            binding.cardNote.setCardBackgroundColor(Color.parseColor(hexColor))
        } catch (e: Exception) { /* ignore invalid color */ }
    }

    private fun loadNote(id: String) {
        db.collection("notes").document(id).get()
            .addOnSuccessListener { doc ->
                val note = doc.toObject(Note::class.java) ?: return@addOnSuccessListener
                binding.etTitle.setText(note.title)
                binding.etContent.setText(note.content)
                selectedColor = note.color
                applyNoteColor(note.color)
            }
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            showToast("Note is empty"); return
        }

        val userId = auth.currentUser?.uid ?: return
        val note = Note(
            id = noteId ?: "",
            userId = userId,
            title = title.ifEmpty { "Untitled" },
            content = content,
            color = selectedColor,
            updatedAt = System.currentTimeMillis()
        )

        if (noteId == null) {
            db.collection("notes").add(note)
                .addOnSuccessListener { showToast("Note saved!"); finish() }
                .addOnFailureListener { showToast("Failed to save note") }
        } else {
            db.collection("notes").document(noteId!!).set(note)
                .addOnSuccessListener { showToast("Note updated!"); finish() }
                .addOnFailureListener { showToast("Failed to update note") }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                noteId?.let { id ->
                    db.collection("notes").document(id).delete()
                        .addOnSuccessListener { showToast(getString(R.string.note_deleted)); finish() }
                        .addOnFailureListener { showToast("Failed to delete note") }
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}
