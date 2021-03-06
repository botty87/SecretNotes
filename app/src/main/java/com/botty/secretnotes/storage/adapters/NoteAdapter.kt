package com.botty.secretnotes.storage.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.botty.secretnotes.R
import com.botty.secretnotes.databinding.NoteCardBinding
import com.botty.secretnotes.storage.db.note.Note
import kotlinx.android.synthetic.main.note_card.view.*

class NoteAdapter(val context: Context) : RecyclerView.Adapter<NoteAdapter.NoteHolder>() {

    private val notes = AsyncListDiffer(this, NoteDiffCallback)

    var onItemClick: ((Note) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val binding: NoteCardBinding =
                DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.note_card, parent, false)

        return NoteHolder(binding)
    }

    override fun getItemCount(): Int {
        return notes.currentList.size
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val note = notes.currentList[position]
        holder.binding.note = note
        if(note.passwordHash.isNullOrBlank()) {
            holder.itemView.imageViewLocked.visibility = View.GONE
            holder.itemView.textViewContent.visibility = View.VISIBLE
        }
        else {
            holder.itemView.textViewContent.visibility = View.GONE
            holder.itemView.imageViewLocked.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(note)
        }
    }

    fun submitNotes(newNotes: List<Note>) {
        this.notes.submitList(newNotes)
    }

    inner class NoteHolder(val binding: NoteCardBinding) : RecyclerView.ViewHolder(binding.root)
}