package com.botty.secretnotes.storage.db.note

import android.os.Parcel
import android.os.Parcelable
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
class NoteNonce() : Parcelable {

    @Id
    var id: Long = 0

    var position: Byte = 0
    var value: Byte = 0
    lateinit var note: ToOne<Note>

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        position = parcel.readByte()
        value = parcel.readByte()
    }

    //Objectbox const
    @Suppress("unused")
    constructor(id: Long, position: Byte, value: Byte, noteId: Long) : this() {
        this.id = id
        this.position = position
        this.value = value
        this.note.targetId = noteId
    }

    constructor(position: Byte, value: Byte) : this() {
        this.position = position
        this.value = value
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeByte(position)
        parcel.writeByte(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NoteNonce> {
        override fun createFromParcel(parcel: Parcel): NoteNonce {
            return NoteNonce(parcel)
        }

        override fun newArray(size: Int): Array<NoteNonce?> {
            return arrayOfNulls(size)
        }
    }
}