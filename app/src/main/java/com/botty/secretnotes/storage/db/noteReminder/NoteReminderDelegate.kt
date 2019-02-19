package com.botty.secretnotes.storage.db.noteReminder

import com.botty.secretnotes.storage.db.note.Note
import java.util.*
import kotlin.reflect.KProperty

class NoteReminderDelegate {
    private var firstGet = true
    private var reminder: Date? = null

    operator fun getValue(note: Note, property: KProperty<*>): Date? {
        if(firstGet) {
            firstGet = false
            if(note.id == 0L && note.firestoreId.isNullOrBlank()) {
                this.reminder = null
            }
            else {
                reminder = getNoteReminderFromDB(note)?.reminder
            }
        }
        //Check if the reminder is gone
        if(reminder?.before(Date()) == true) {
            removeNoteReminderFromNote(note)
            reminder = null
        }

        return reminder
    }

    operator fun setValue(note: Note, property: KProperty<*>, date: Date?) {
        firstGet = false
        this.reminder = date
    }
}