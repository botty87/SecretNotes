package com.botty.secretnotes.storage.db.noteReminder

import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.db.note.Note
import com.botty.secretnotes.storage.storage_extensions.getNoteReminderBox
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique
import io.objectbox.kotlin.query
import java.util.*

@Entity
data class NoteReminder (
        @Id
        var id: Long = 0,

        @Unique
        var noteObjBoxId: Long? = null,

        @Unique
        var noteFirestoreId: String? = null,

        var reminder: Date? = null,

        @Unique
        var jobId: Int? = null
)

fun getNoteReminderFromDB(note: Note): NoteReminder? {
    val noteReminder = if(AppPreferences.userHasAccount) {
        getNoteReminderBox().query {
            equal(NoteReminder_.noteFirestoreId, note.firestoreId!!)
        }.findUnique()
    }
    else {
        getNoteReminderBox().query {
            equal(NoteReminder_.noteObjBoxId, note.id)
        }.findUnique()
    }

    return noteReminder?.run {
        if(checkIfReminderIsBeforeNow(this)) {
            null
        }
        else {
            this
        }
    }
}

//Really rare, almost impossible!
private fun checkIfReminderIsBeforeNow(noteReminder: NoteReminder): Boolean {
    return if(noteReminder.reminder?.before(Date()) == true) {
        getNoteReminderBox().remove(noteReminder)
        true
    } else {
        false
    }
}

//Really rare, almost impossible!
fun removeNoteReminderFromNote(note: Note) {
    if(AppPreferences.userHasAccount) {
        getNoteReminderBox().query {
            equal(NoteReminder_.noteFirestoreId, note.firestoreId!!)
        }.remove()
    }
    else {
        getNoteReminderBox().query {
            equal(NoteReminder_.noteObjBoxId, note.id)
        }.remove()
    }
}