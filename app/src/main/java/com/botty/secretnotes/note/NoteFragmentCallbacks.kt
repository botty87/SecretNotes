package com.botty.secretnotes.note

import android.content.Context
import androidx.fragment.app.Fragment

abstract class NoteFragmentCallbacks: Fragment() {

    internal var noteCallbacks: NoteCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        noteCallbacks = context as NoteCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        noteCallbacks = null
    }

}