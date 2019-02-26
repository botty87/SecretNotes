package com.botty.secretnotes.storage.adapters

import androidx.recyclerview.widget.DiffUtil
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.db.note.Note

object NoteDiffCallback: DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return if(AppPreferences.userHasAccount) {
            oldItem.firestoreId == newItem.firestoreId
        }
        else {
            oldItem.id == newItem.id
        }
    }

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem == newItem
    }
}