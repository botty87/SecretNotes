package com.botty.secretnotes.storage.storage_extensions

import android.app.Activity
import com.botty.secretnotes.MyApplication
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.storage.new_db.note.Note
import com.botty.secretnotes.storage.new_db.note.NoteNonce
import com.botty.secretnotes.storage.new_db.note.NoteNonce_
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query

private fun Activity.getBoxStore(): BoxStore {
    return (application as MyApplication).boxStore
}

internal fun Activity.getCategoriesBox(): Box<Category> {
    return getBoxStore().boxFor()
}

internal fun Activity.getNotesBox(): Box<Note> {
    return getBoxStore().boxFor()
}

internal fun Activity.getNotesNonceBox(): Box<NoteNonce> {
    return getBoxStore().boxFor()
}

internal fun Activity.cleanNotesNonce() {
    getNotesNonceBox().query {
        equal(NoteNonce_.noteId, 0)
    }.remove()
}

internal fun Activity.deleteAllBoxStore() {
    getCategoriesBox().removeAll()
    getNotesBox().removeAll()
    getNotesNonceBox().removeAll()
}