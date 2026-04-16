package com.taskmaster.mkrishna.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.taskmaster.mkrishna.R
import com.taskmaster.mkrishna.databinding.ItemNoteBinding
import com.taskmaster.mkrishna.models.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var lastAnimatedPosition = -1
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class NoteViewHolder(val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        with(holder.binding) {
            tvTitle.text = note.title
            tvContent.text = note.content
            tvDate.text = note.updatedAt.let {
                if (it > 0) dateFormat.format(Date(it)) else ""
            }

            // Apply note background color
            try {
                cardNote.setCardBackgroundColor(Color.parseColor(note.color))
            } catch (e: Exception) {
                cardNote.setCardBackgroundColor(Color.parseColor("#FFE4E1"))
            }

            root.setOnClickListener { onNoteClick(note) }
            btnDelete.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.scale_in))
                onNoteDelete(note)
            }
        }

        if (position > lastAnimatedPosition) {
            val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
            holder.itemView.startAnimation(anim)
            lastAnimatedPosition = position
        }
    }

    override fun getItemCount() = notes.size
}
