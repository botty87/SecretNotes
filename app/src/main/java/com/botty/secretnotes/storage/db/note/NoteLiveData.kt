package com.botty.secretnotes.storage.db.note

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.botty.secretnotes.storage.db.category.Category
import com.botty.secretnotes.utilities.logException
import com.google.firebase.firestore.ListenerRegistration
import io.objectbox.query.Query
import io.objectbox.reactive.DataSubscription
import org.jetbrains.anko.doAsync

class NoteLiveData: MutableLiveData<List<Note>> {

    private val category: Category?
    private val searchText: String?

    //Objectbox
    private val objectBoxQuery: Query<Note>?
    private var noteSubscription: DataSubscription? = null

    //Firestore
    private val firestoreQuery: com.google.firebase.firestore.Query?
    private var noteListener: ListenerRegistration? = null

    //Objectbox
    constructor(objectBoxQuery: Query<Note>, category: Category?, searchText: String? = null): super() {
        this.objectBoxQuery = objectBoxQuery
        this.firestoreQuery = null
        this.category = category
        this.searchText = searchText
    }

    //Firestore
    constructor(firestoreQuery: com.google.firebase.firestore.Query, category: Category?, searchText: String? = null): super() {
        this.objectBoxQuery = null
        this.firestoreQuery = firestoreQuery
        this.category = category
        this.searchText = searchText
    }

    fun clearAll(owner: LifecycleOwner? = null) {
        noteSubscription?.cancel()
        noteSubscription = null

        noteListener?.remove()
        noteListener = null

        owner?.run {
            removeObservers(this)
        }
    }

    override fun onActive() {
        fun postNotes(notes: List<Note>) {
            val equals = value?.toTypedArray()?.contentDeepEquals(notes.toTypedArray()) ?: false
            if(!(equals)) {
                postValue(notes)
            }
        }

        super.onActive()
        noteSubscription = objectBoxQuery?.run {
            subscribe()
            //.on(AndroidScheduler.mainThread())
            .transform {notes ->
                searchText?.run {
                    filterPasNotes(notes, this)
                } ?: notes
            }
            .observer(::postNotes)
        }

        noteListener = firestoreQuery?.addSnapshotListener { querySnapshot, exception ->
            doAsync {
                exception?.run {
                    logException(this)
                }

                querySnapshot?.let {snapshot ->
                    val notes = mutableListOf<Note>()
                    snapshot.documents.forEach {document ->
                        notes.add(Note.getNoteFromFirestoreDoc(document))
                    }

                    searchText?.let {text ->
                        notes.filter {note ->
                            note.title.startsWith(text, true) ||
                            note.content.contains(text, true)
                        }.run {
                            postNotes(filterPasNotes(this, text))
                        }
                    } ?: postNotes(notes)
                }
            }
        }
    }

    private fun filterPasNotes(notes: List<Note>, searchText: String): List<Note> {
        return notes.filterNot {note ->
            val pas = note.hasPassword()
            val content = note.content.contains(searchText, true)
            val title = !note.title.startsWith(searchText, true)
            pas && content && title
        }
    }

    override fun onInactive() {
        super.onInactive()
        clearAll()
    }

}