package com.botty.secretnotes.storage.new_db.note

import androidx.lifecycle.ViewModel

abstract class NoteViewModel: ViewModel() {
    var noteLiveData: NoteLiveData? = null

    abstract fun onNotesChanged(notes: List<Note>)
}