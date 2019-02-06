package com.botty.secretnotes.storage

import com.marcinmoskala.kotlinpreferences.PreferenceHolder

object AppPreferences: PreferenceHolder() {
    var alphabetSort: Boolean by bindToPreferenceField(true)
    var ascendingSort: Boolean by bindToPreferenceField(true)
    var noCategoriesNotes: Boolean by bindToPreferenceField(true)
    var noteAutoSave: Boolean by bindToPreferenceField(false)
    var oneColumn: Boolean by bindToPreferenceField(true)

    var migrationNeeded: Boolean by bindToPreferenceField(true)
    var userHasAccount: Boolean by bindToPreferenceField(false)
    var firstSnackbar: Boolean by bindToPreferenceField(true)
    var userAccountToSet: Boolean by bindToPreferenceField(true)
    var masterPasToSet: Boolean by bindToPreferenceField(true)
    var masterPas: String? by bindToPreferenceFieldNullable()
    var autoLock: Boolean by bindToPreferenceField(false)
    var firstTimeNotePosition: Boolean by bindToPreferenceField(true)
}

/*fun Context.getAppPreferences(): SharedPreferences {
    return getSharedPreferences("note_preferences", Context.MODE_PRIVATE)
}*/