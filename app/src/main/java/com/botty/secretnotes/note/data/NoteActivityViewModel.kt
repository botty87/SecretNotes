package com.botty.secretnotes.note.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.botty.secretnotes.storage.db.note.Note

class NoteActivityViewModel: ViewModel() {
    val note = MutableLiveData<Note>()
    val password = MutableLiveData<String?>()
}