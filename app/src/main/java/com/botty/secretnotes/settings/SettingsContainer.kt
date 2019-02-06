package com.botty.secretnotes.settings

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.botty.secretnotes.storage.AppPreferences

internal class SettingsContainer: BaseObservable() {

    @Bindable
    var alphabetSort = AppPreferences.alphabetSort
    @Bindable
    var ascendingSort = AppPreferences.ascendingSort
    @Bindable
    var showNoCategoriesNotes = AppPreferences.noCategoriesNotes
    @Bindable
    var noteAutoSave = AppPreferences.noteAutoSave
    @Bindable
    var oneColumn = AppPreferences.oneColumn

    fun storeSettings() {
        AppPreferences.alphabetSort = alphabetSort
        AppPreferences.ascendingSort = ascendingSort
        AppPreferences.noCategoriesNotes = showNoCategoriesNotes
        AppPreferences.noteAutoSave = noteAutoSave
        AppPreferences.oneColumn = oneColumn
    }
}