package com.botty.secretnotes.storage.db.category

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.botty.secretnotes.utilities.logException
import com.google.firebase.firestore.ListenerRegistration
import io.objectbox.query.Query
import io.objectbox.reactive.DataSubscription

class CategoryLiveData: MutableLiveData<List<Category>> {
    //Objectbox
    private val objectBoxQuery: Query<Category>?
    private var categorySubscription: DataSubscription? = null

    //Firestore
    private val firestoreQuery: com.google.firebase.firestore.Query?
    private var categoryListener: ListenerRegistration? = null

    constructor(objectBoxQuery: Query<Category>) {
        this.objectBoxQuery = objectBoxQuery
        this.firestoreQuery = null
    }

    constructor(firestoreQuery: com.google.firebase.firestore.Query) {
        this.objectBoxQuery = null
        this.firestoreQuery = firestoreQuery
    }

    fun clearAll(owner: LifecycleOwner? = null) {
        categorySubscription?.cancel()
        categorySubscription = null

        categoryListener?.remove()
        categoryListener = null

        owner?.run {
            removeObservers(this)
        }
    }

    override fun onActive() {
        fun postCategories(categories: List<Category>) {
            val equals = value?.toTypedArray()?.contentDeepEquals(categories.toTypedArray()) ?: false
            if(!(equals)) {
                postValue(categories)
            }
        }

        super.onActive()
        categorySubscription = objectBoxQuery?.subscribe()?.observer(::postCategories)

        categoryListener = firestoreQuery?.addSnapshotListener { querySnapshot, exception ->
            exception?.run {
                logException(this)
            }

            querySnapshot?.let {snapshot ->
                val categories = mutableListOf<Category>()
                snapshot.documents.forEach {document ->
                    categories.add(Category.getNoteFromFirestoreDoc(document))
                }
                postCategories(categories)
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        clearAll()
    }
}