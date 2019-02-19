package com.botty.secretnotes.storage.storage_extensions

import com.botty.secretnotes.storage.db.category.Category
import com.botty.secretnotes.storage.db.note.Note
import com.botty.secretnotes.storage.db.note.NoteNonce
import com.botty.secretnotes.storage.db.note.NoteNonce_
import com.botty.secretnotes.storage.db.noteReminder.NoteReminder
import com.botty.secretnotes.storage.storage_extensions.ObjectBoxStorage.boxStore
import com.evernote.android.job.JobManager
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query

object ObjectBoxStorage {
    //Init in MyApplication!
    lateinit var boxStore: BoxStore
}

internal fun getCategoriesBox(): Box<Category> {
    return boxStore.boxFor()
}

internal fun getNotesBox(): Box<Note> {
    return boxStore.boxFor()
}

internal fun getNotesNonceBox(): Box<NoteNonce> {
    return boxStore.boxFor()
}

internal fun getNoteReminderBox(): Box<NoteReminder> {
    return boxStore.boxFor()
}

internal fun cleanNotesNonce() {
    getNotesNonceBox().query {
        equal(NoteNonce_.noteId, 0)
    }.remove()
}

internal fun deleteAllBoxStore(cleanJobs: Boolean) {
    getCategoriesBox().removeAll()
    getNotesBox().removeAll()
    getNotesNonceBox().removeAll()
    if(cleanJobs) {
        JobManager.instance().cancelAll()
        getNoteReminderBox().removeAll()
    }
}