package com.botty.secretnotes.storage.storage_extensions

import android.app.Activity
import com.botty.secretnotes.MainActivity
import com.botty.secretnotes.R
import com.botty.secretnotes.settings.SettingsActivity
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.storage.new_db.note.Note
import com.botty.secretnotes.storage.new_db.note.NoteNonce
import com.botty.secretnotes.storage.new_db.note.Note_
import com.botty.secretnotes.utilities.await
import com.botty.secretnotes.utilities.getDialog
import com.botty.secretnotes.utilities.logException
import com.botty.secretnotes.utilities.toastError
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.WriteBatch
import io.objectbox.kotlin.query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
suspend fun MainActivity.moveDBToFirebase(): Boolean {

    suspend fun downloadExistingUserDbRef(): List<DocumentReference> {
        suspend fun downloadCategories(): List<DocumentReference> {
            getCategoriesCollection().get().await().documents.let {documents ->
                return mutableListOf<DocumentReference>().apply {
                    documents.forEach {document ->
                        add(document.reference)
                    }
                }
            }
        }

        suspend fun downloadNotes(): List<DocumentReference> {
            getNotesCollection().get().await().documents.let { documents ->
                return mutableListOf<DocumentReference>().apply {
                    documents.forEach {document ->
                        add(document.reference)
                    }
                }
            }
        }

        fun getFullList(categories: List<DocumentReference>, notes: List<DocumentReference>): List<DocumentReference> {
            return mutableListOf<DocumentReference>().apply {
                addAll(categories)
                addAll(notes)
            }
        }

        return getFullList(downloadCategories(), downloadNotes())
    }

    fun uploadDb(firestoreBatch: WriteBatch = getFirestoreWriteBatch()) {
        val categories = getCategoriesBox().all
        val notesCol = getNotesCollection()

        if(categories.isNotEmpty()) {
            val categoriesCol = getCategoriesCollection()
            categories.forEach {category ->
                val categoryDoc = categoriesCol.document()
                category.firestoreId = categoryDoc.id
                firestoreBatch.set(categoryDoc, Category.getFirestoreMap(category))

                category.notes.forEach {note ->
                    val noteDoc = notesCol.document()
                    note.firestoreCatId = category.firestoreId
                    firestoreBatch.set(noteDoc, Note.getFirestoreMap(note))
                }
            }
        }

        getNotesBox().query {
            equal(Note_.categoryId, Category.NO_CATEGORY_ID)
        }.forEach {note ->
            val noteDoc = notesCol.document()
            firestoreBatch.set(noteDoc, Note.getFirestoreMap(note))
        }

        firestoreBatch.commit().addOnCompleteListener {
            deleteAllBoxStore()
        }
    }

    suspend fun startMigration() {
        val existingUserDb = downloadExistingUserDbRef()

        if(existingUserDb.isNotEmpty()) {
            //If true maintain the local db, else simply download the existing one
            suspendCoroutine<Boolean> {continuation ->
                getDialog()
                        .title(R.string.notes_conflict)
                        .message(R.string.notes_conflict_message)
                        .positiveButton(R.string.maintain) {
                            continuation.resume(true)
                        }
                        .negativeButton(R.string.download_remote_db) {
                            continuation.resume(false)
                        }
                        .show()
            }.run {
                if(this) {
                    getFirestoreWriteBatch().apply {
                        existingUserDb.forEach {docRef ->
                            delete(docRef)
                        }
                    }.run {
                        uploadDb(this)
                    }
                }
                else {
                    deleteAllBoxStore()
                }
            }
        }
        else {
            uploadDb()
        }
    }

    return try {
        startMigration()
        true
    } catch (e: Exception) {
        if(e !is CancellationException) {
            toastError(e.localizedMessage)
            logException(e)
        }
        false
    }
}

@ExperimentalCoroutinesApi
suspend fun SettingsActivity.moveDBLocally(): Boolean {
    return moveDBLocally(removeNotes = true, deleteUser = true)
}

@ExperimentalCoroutinesApi
suspend fun MainActivity.moveDBLocally(removeNotes: Boolean): Boolean {
    return moveDBLocally(removeNotes, deleteUser = false)
}

@ExperimentalCoroutinesApi
private suspend fun Activity.moveDBLocally(removeNotes: Boolean, deleteUser: Boolean): Boolean {

    suspend fun downloadCategories(): Pair<List<Category>, List<DocumentReference>> {
        return getCategoriesCollection().get().await().documents
                .let {documents ->
                    Pair<MutableList<Category>, MutableList<DocumentReference>>(mutableListOf(), mutableListOf())
                            .apply {
                                documents.forEach {document ->
                                    first.add(Category.getNoteFromFirestoreDoc(document))
                                    second.add(document.reference)
                                }
                            }
                }
    }

    suspend fun downloadNotes(): Pair<List<Note>, List<DocumentReference>> {
        return getNotesCollection().get().await().documents
                .let {documents ->
                    Pair<MutableList<Note>, MutableList<DocumentReference>>(mutableListOf(), mutableListOf())
                            .apply {
                                documents.forEach {document ->
                                    val note = Note.getNoteFromFirestoreDoc(document)
                                    note.nonceArray?.forEachIndexed { pos, value ->
                                        note.nonce.add(NoteNonce(pos.toByte(), value))
                                    }

                                    first.add(note)
                                    second.add(document.reference)
                                }
                            }
                }
    }

    suspend fun saveDBLocally(categories: Pair<List<Category>, List<DocumentReference>>,
                              notes: Pair<List<Note>, List<DocumentReference>>) :
            //First categories, second notes
            Pair< List<DocumentReference>, List<DocumentReference> >{
        withContext(NonCancellable) {
            deleteAllBoxStore()

            val categoriesBox = getCategoriesBox()
            categories.first.forEach {category ->
                notes.first.filter {note ->
                    note.firestoreCatId == category.firestoreId
                }.run {
                    category.notes.addAll(this)
                    categoriesBox.put(category)
                }
            }

            notes.first.filter {note ->
                note.firestoreCatId == null
            }.run {
                getNotesBox().put(this)
            }
        }
        return categories.second to notes.second
    }

    val categoriesNotesRef =
    try {
        saveDBLocally(downloadCategories(), downloadNotes())
    }
    catch (e: Exception) {
        if(e !is CancellationException) {
            toastError(e.localizedMessage)
            logException(e)
        }
        return false
    }

    var isOk = true
    withContext(NonCancellable) {
        try {
            if (deleteUser) {
                FirebaseAuth.getInstance().currentUser?.delete()?.await()
            } else {
                AuthUI.getInstance().signOut(this@moveDBLocally).await()
            }
        }
        catch (e: Exception) {
            toastError(e.localizedMessage)
            logException(e)
            isOk = false
            deleteAllBoxStore()
            return@withContext
        }

        FirebaseAuth.getInstance().signInAnonymously().await()

        if(removeNotes) {
            val firestoreBatch = getFirestoreWriteBatch()
            categoriesNotesRef.first.forEach {docRef ->
                firestoreBatch.delete(docRef)
            }
            categoriesNotesRef.second.forEach {docRef ->
                firestoreBatch.delete(docRef)
            }

            firestoreBatch.commit()
        }
    }
    return isOk
}

