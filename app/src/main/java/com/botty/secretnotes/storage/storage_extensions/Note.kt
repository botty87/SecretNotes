package com.botty.secretnotes.storage.storage_extensions

import android.app.Activity
import com.botty.secretnotes.MainActivity
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.storage.new_db.note.*
import com.google.firebase.firestore.Query
import io.objectbox.kotlin.query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

fun Activity.saveNote(note: Note, updateTime: Boolean = true) {
    if(updateTime) {
        note.lastModified = Date()
    }

    if(AppPreferences.userHasAccount) {
        val notesCol = getNotesCollection()
        val noteDocument = note.firestoreId?.run {
            notesCol.document(this)
        } ?: notesCol.document()

        noteDocument.set(Note.getFirestoreMap(note))
    }
    else {
        note.nonceArray?.run {
            getNotesBox().attach(note)
            note.nonce.clear()
            forEachIndexed { pos, value ->
                note.nonce.add(NoteNonce(pos.toByte(), value))
            }
        }
        getNotesBox().put(note)
        cleanNotesNonce()
    }
}

@ExperimentalCoroutinesApi
fun MainActivity.getNotes(category: Category?, noteViewModel: NoteViewModel, searchText: String? = null) {
    noteViewModel.noteLiveData?.clearAll(this)

    //val settingsContainer = SettingsContainer.getSettingsContainer(this)

    if(AppPreferences.userHasAccount) {
        var query = if(AppPreferences.alphabetSort) {
            if(AppPreferences.ascendingSort) {
                getNotesCollection().orderBy(Note.TITLE_KEY, Query.Direction.ASCENDING)
            }
            else {
                getNotesCollection().orderBy(Note.TITLE_KEY, Query.Direction.DESCENDING)
            }
        }
        else {
            if(AppPreferences.ascendingSort) {
                getNotesCollection().orderBy(Note.LAST_MODIFIED_KEY, Query.Direction.ASCENDING)
            }
            else {
                getNotesCollection().orderBy(Note.LAST_MODIFIED_KEY, Query.Direction.DESCENDING)
            }
        }

        if(category?.firestoreId != null || AppPreferences.noCategoriesNotes) {
            query = query.whereEqualTo(Note.FIRESTORE_CAT_ID_KEY, category?.firestoreId)
        }

        noteViewModel.noteLiveData = NoteLiveData(query, category, searchText).apply {
            observe(this@getNotes, androidx.lifecycle.Observer(noteViewModel::onNotesChanged))
        }
    }
    else {
        val categoryId = category?.id ?: Category.NO_CATEGORY_ID
        val query = getNotesBox().query {
            if(categoryId != Category.NO_CATEGORY_ID || AppPreferences.noCategoriesNotes) {
                equal(Note_.categoryId, categoryId)
            }

            searchText?.run {
                startsWith(Note_.title, this)
                or()
                contains(Note_.content, this)
            }

            if(AppPreferences.alphabetSort) {
                if(AppPreferences.ascendingSort) {
                    order(Note_.title)
                }
                else {
                    orderDesc(Note_.title)
                }
            }
            else {
            }
            if(AppPreferences.ascendingSort) {
                order(Note_.lastModified)
            }
            else {
                orderDesc(Note_.lastModified)
            }
        }

        noteViewModel.noteLiveData = NoteLiveData(query, category, searchText).apply {
            observe(this@getNotes, androidx.lifecycle.Observer(noteViewModel::onNotesChanged))
        }
    }
}

@ExperimentalCoroutinesApi
fun MainActivity.deleteNote(note: Note) {
    if(AppPreferences.userHasAccount) {
        note.firestoreId?.run {
            getNotesCollection().document(this).delete()
        }
    }
    else {
        getNotesBox().remove(note)
        cleanNotesNonce()
    }
}