package com.botty.secretnotes.storage.db.category

import android.os.Parcel
import android.os.Parcelable
import com.botty.secretnotes.storage.db.note.Note
import com.google.firebase.firestore.DocumentSnapshot
import io.objectbox.annotation.*
import io.objectbox.relation.ToMany

@Entity
//Parcelable only for saveInstanceState
class Category() : Parcelable {

    @Id
    var id: Long = 0
    @Transient
    var firestoreId: String? = null

    @Unique
    lateinit var name: String

    @Transient
    var isSelected = false
    //var color: Int? = null

    @Backlink
    lateinit var notes: ToMany<Note>

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        firestoreId = parcel.readString()
    }

    constructor(name: String) : this() {
        this.name = name
    }

    //useful for objectbox
    constructor(id: Long, name: String) : this() {
        this.id = id
        this.name = name
    }

    //Private constructor for making a copy
    private constructor(category: Category) : this() {
        this.id = category.id
        this.firestoreId = category.firestoreId
        this.name = category.name
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(firestoreId)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun matchCategory(category: Category?): Boolean {
        return if(firestoreId.isNullOrBlank() && category?.firestoreId.isNullOrBlank()) {
            id == category?.id
        }
        else {
            firestoreId == category?.firestoreId
        }
    }

    fun hasNoCategoryID(): Boolean {
        return id == NO_CATEGORY_ID && firestoreId.isNullOrBlank()
    }

    override fun equals(other: Any?): Boolean {
        return if(other != null && other is Category) {
            other.id == this.id &&
            other.firestoreId == this.firestoreId &&
            other.name == this.name
        }
        else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (firestoreId?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + isSelected.hashCode()
        result = 31 * result + notes.hashCode()
        return result
    }

    fun getCopy(): Category {
        return Category(this)
    }

    companion object CREATOR : Parcelable.Creator<Category> {
        override fun createFromParcel(parcel: Parcel): Category {
            return Category(parcel)
        }

        override fun newArray(size: Int): Array<Category?> {
            return arrayOfNulls(size)
        }

        const val NO_CATEGORY_ID: Long = 0
        const val SELECTED_CATEGORY_KEY = "sel_category"
        const val NAME_KEY = "name"

        fun getFirestoreMap(category: Category): HashMap<String, Any> {
            return HashMap<String, Any>().apply {
                this[NAME_KEY] = category.name
            }
        }

        fun getNoteFromFirestoreDoc(document: DocumentSnapshot): Category {
            return Category().apply {
                firestoreId = document.id
                document.data?.let {
                    firestoreId = document.id
                    name = it[NAME_KEY] as String
                }
            }
        }
    }
}