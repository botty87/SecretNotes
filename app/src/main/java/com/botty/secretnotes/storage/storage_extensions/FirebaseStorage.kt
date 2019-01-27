package com.botty.secretnotes.storage.storage_extensions

import com.botty.secretnotes.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch

private const val USERS_KEY = "users"
private const val DEVELOP_KEY = "develop"
private const val CATEGORIES_KEY = "categories"
private const val NOTES_KEY = "notes"

private fun getUserDocument(): DocumentReference {
    return if(BuildConfig.DEBUG) {
        FirebaseFirestore.getInstance().collection(DEVELOP_KEY).document(getFirebaseUser().uid)
    }
    else {
        FirebaseFirestore.getInstance().collection(USERS_KEY).document(getFirebaseUser().uid)
    }
}

fun getCategoriesCollection(): CollectionReference {
    return getUserDocument().collection(CATEGORIES_KEY)
}

fun getNotesCollection(): CollectionReference {
    return getUserDocument().collection(NOTES_KEY)
}

fun getFirestoreWriteBatch(): WriteBatch {
    return FirebaseFirestore.getInstance().batch()
}

private fun getFirebaseUser(): FirebaseUser {
    FirebaseAuth.getInstance().currentUser?.run {
        if(isAnonymous) {
            throw NotValidFirebaseUserException()
        }
        else {
            return this
        }
    } ?: throw NotValidFirebaseUserException()
}

private class NotValidFirebaseUserException : Exception()