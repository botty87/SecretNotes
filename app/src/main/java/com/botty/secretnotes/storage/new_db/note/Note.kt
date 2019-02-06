package com.botty.secretnotes.storage.new_db.note

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.botty.secretnotes.storage.new_db.category.Category
import com.botty.secretnotes.utilities.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import io.objectbox.annotation.*
import io.objectbox.exception.DbDetachedException
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@Entity
class Note() : Parcelable, BaseObservable() {

    @Id
    var id: Long = 0
    @Transient
    var firestoreId: String? = null

    @Bindable
    var title: String = ""
    @Bindable
    var content: String = ""

    var passwordHash: String? = null
        set(value) {
            field = if(value?.isBlank() != false) {
                null
            } else {
                value
            }
        }

    @Backlink
    lateinit var nonce: ToMany<NoteNonce>

    @Transient
    var nonceArray: ByteArray? = null

    var lastModified: Date? = null
        set(value) {
            field = value
            lastModifiedFormatted = dateFormatter.format(value)
        }

    @Transient
    @Bindable
    var lastModifiedFormatted: String? = null

    lateinit var category: ToOne<Category>

    @Transient
    var firestoreCatId: String? = null

    @Convert(converter = GeoPointConverter::class, dbType = String::class)
    var position: GeoPoint? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        firestoreId = parcel.readString()
        title = parcel.readString()!!
        content = parcel.readString()!!
        passwordHash = parcel.readString()
        category.targetId = parcel.readLong()
        firestoreCatId = parcel.readString()

        val latitude = parcel.readDouble()
        val longitude = parcel.readDouble()
        if(latitude != Constants.NO_LAT_LON_VALUE && longitude != Constants.NO_LAT_LON_VALUE) {
            position = GeoPoint(latitude, longitude)
        }

        val nonceArraySize = parcel.readInt()
        if(nonceArraySize > 0) {
            nonceArray = ByteArray(nonceArraySize)
            parcel.readByteArray(nonceArray)
        }
    }

    constructor(id: Long, title: String, content: String,
                passwordHash: String?, lastModified: Date?, categoryId: Long) : this() {
        this.id = id
        this.title = title
        this.content = content
        this.passwordHash = passwordHash
        this.lastModified = lastModified
        this.category.targetId = categoryId
    }

    private constructor(title: String, content: String, passwordHash: String?, lastModified: Long?): this() {
        this.lastModified = lastModified?.run { Date(this) }
        this.title = title
        this.content = content
        this.passwordHash = passwordHash
    }

    fun hasPassword(): Boolean {
        return passwordHash?.isNotBlank() ?: false
    }

    fun isValid(): Boolean {
        return title.isNotBlank() && content.isNotBlank()
    }

    fun isNewNote(): Boolean {
        return id == 0L && firestoreId?.isBlank() ?: true
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(firestoreId)
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeString(passwordHash)
        parcel.writeLong(category.targetId)
        parcel.writeString(firestoreCatId)
        parcel.writeDouble(position?.latitude ?: Constants.NO_LAT_LON_VALUE)
        parcel.writeDouble(position?.longitude ?: Constants.NO_LAT_LON_VALUE)

        try {
            if (nonce.isNotEmpty()) {
                nonceArray = ByteArray(nonce.size).apply {
                    nonce.forEach {
                        this[it.position.toInt()] = it.value
                    }
                }
            }
        }
        catch (e: DbDetachedException) {
            //Fine, continue!
        }

        nonceArray?.run {
            parcel.writeInt(size)
            parcel.writeByteArray(this)
        } ?: parcel.writeInt(0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return if(other != null && other is Note) {
            other.id == this.id &&
            other.firestoreId == this.firestoreId &&
            other.category.targetId == this.category.targetId &&
            other.firestoreCatId == this.firestoreCatId &&
            other.passwordHash == this.passwordHash &&
            other.title == this.title &&
            other.content == this.content &&
            other.position == this.position &&
            other.lastModified?.equals(this.lastModified) ?: false
        }
        else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (firestoreId?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + (passwordHash?.hashCode() ?: 0)
        result = 31 * result + (lastModified?.hashCode() ?: 0)
        result = 31 * result + (lastModifiedFormatted?.hashCode() ?: 0)
        result = 31 * result + category.hashCode()
        result = 31 * result + (firestoreCatId?.hashCode() ?: 0)
        result = 31 * result + (nonce.hashCode())
        result = 31 * result + (position?.hashCode() ?: 0)
        return result
    }

    @SuppressLint("SimpleDateFormat")
    companion object CREATOR : Parcelable.Creator<Note> {
        override fun createFromParcel(parcel: Parcel): Note {
            return Note(parcel)
        }

        override fun newArray(size: Int): Array<Note?> {
            return arrayOfNulls(size)
        }

        val dateFormatter by lazy { SimpleDateFormat("dd/MM/yyyy HH:mm:ss") }

        const val NOTE_TAG = "note_key"
        const val NOTE_PAS = "note_pas"
        const val NOTE_TO_DELETE = "note_del"
        const val NOTE_DISCARDED = "note_disc"

        const val TITLE_KEY = "title"
        private const val CONTENT_KEY = "content"
        private const val PASSWORD_HASH_KEY = "passwordHash"
        const val LAST_MODIFIED_KEY = "lastModified"
        private const val NONCE_KEY = "nonce"
        const val FIRESTORE_CAT_ID_KEY = "firestoreCatId"
        private const val POSITION_KEY = "position"

        fun getFirestoreMap(note: Note): HashMap<String, Any?> {
            return HashMap<String, Any?>().apply {
                this[TITLE_KEY] = note.title
                this[CONTENT_KEY] = note.content
                this[PASSWORD_HASH_KEY] = note.passwordHash
                this[LAST_MODIFIED_KEY] = note.lastModified
                this[FIRESTORE_CAT_ID_KEY] = note.firestoreCatId
                this[POSITION_KEY] = note.position

                val nonceList =
                        when {
                            note.nonceArray?.isNotEmpty() == true -> note.nonceArray!!.map { it.toInt() }
                            note.nonce.isNotEmpty() -> note.nonce.sortedBy {it.position}.map { it.value.toInt() }
                            else -> null
                        }
                this[NONCE_KEY] = nonceList
            }
        }

        fun getNoteFromFirestoreDoc(document: DocumentSnapshot): Note {
            return Note().apply {
                firestoreId = document.id
                document.data?.let {
                    title = it[TITLE_KEY] as String
                    content = it[CONTENT_KEY] as String
                    passwordHash = it[PASSWORD_HASH_KEY] as String?
                    lastModified = (it[LAST_MODIFIED_KEY] as Timestamp?)?.toDate()
                    firestoreCatId = it[FIRESTORE_CAT_ID_KEY] as String?
                    position = it[POSITION_KEY] as GeoPoint?
                    (it[NONCE_KEY] as List<Int>?)?.run {
                        if(this.isNotEmpty()) {
                            nonceArray = ByteArray(this.size)
                            forEachIndexed { pos, value ->
                                nonceArray!![pos] = value.toByte()
                            }
                        }
                    }
                }
            }
        }
    }
}