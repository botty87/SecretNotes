package com.botty.secretnotes.note

interface NoteCallbacks {
    fun isButtonSaveEnabled(): Boolean
    fun changeFabSaveVisibility(isVisible: Boolean)
}