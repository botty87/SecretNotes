package com.botty.secretnotes.settings

import android.content.Context
import androidx.core.content.edit
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.botty.secretnotes.utilities.getAppPreferences

internal data class SettingsContainer (
    @Bindable
    var alphabetSort: Boolean,
    @Bindable
    var ascendingSort: Boolean,
    @Bindable
    var showNoCategoriesNotes: Boolean,
    @Bindable
    var noteAutoSave: Boolean,
    @Bindable
    var oneColumn: Boolean): BaseObservable() {

    fun storeSettings(context: Context) {
        context.getAppPreferences().edit {
            putBoolean(ALPHABET_SORT_KEY, alphabetSort)
            putBoolean(ASCENDING_SORT_KEY, ascendingSort)
            putBoolean(NO_CAT_NOTES_KEY, showNoCategoriesNotes)
            putBoolean(NOTE_AUTO_SAVE_KEY, noteAutoSave)
            putBoolean(ONE_COLUMN_KEY, oneColumn)
        }

    }

    companion object {
        private const val ALPHABET_SORT_KEY = "alph_sort"
        private const val ALPHABET_SORT_DEFAULT = true
        private const val ASCENDING_SORT_KEY = "asc_sort"
        private const val ASCENDING_SORT_DEFAULT = true
        private const val NO_CAT_NOTES_KEY = "no_cat_notes"
        private const val NO_CAT_NOTES_DEFAULT = true
        private const val NOTE_AUTO_SAVE_KEY = "note_auto_save"
        private const val NOTE_AUTO_SAVE_DEFAULT = false
        private const val ONE_COLUMN_KEY = "one_column"
        private const val ONE_COLUMN_DEFAULT = true

        fun getSettingsContainer(context: Context): SettingsContainer {
            val preferences = context.getAppPreferences()
            val alphSort = preferences.getBoolean(ALPHABET_SORT_KEY, ALPHABET_SORT_DEFAULT)
            val ascSort = preferences.getBoolean(ASCENDING_SORT_KEY, ASCENDING_SORT_DEFAULT)
            val noCatNots = preferences.getBoolean(NO_CAT_NOTES_KEY, NO_CAT_NOTES_DEFAULT)
            val noteAutoSave = preferences.getBoolean(NOTE_AUTO_SAVE_KEY, NOTE_AUTO_SAVE_DEFAULT)
            val oneColumn = preferences.getBoolean(ONE_COLUMN_KEY, ONE_COLUMN_DEFAULT)
            return SettingsContainer(alphSort, ascSort, noCatNots, noteAutoSave, oneColumn)
        }
    }
}