package com.botty.secretnotes.note

import com.botty.secretnotes.storage.new_db.note.Note

interface NoteCallbacks {

    fun getNote(): Note
    fun getIsButtonSaveEnabled() :Boolean
    fun changeFabSaveVisibility(isVisible: Boolean)

}