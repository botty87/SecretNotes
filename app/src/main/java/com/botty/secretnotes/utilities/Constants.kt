package com.botty.secretnotes.utilities

import com.firebase.ui.auth.AuthUI

object Constants {

    const val LOGIN_ACTIVITY_REQ_CODE = 100
    const val SIGN_UP_ACTIVITY_REQ_CODE = 101
    const val NOTE_ACTIVITY_REQ_CODE = 102
    const val SETTINGS_ACTIVITY_REQ_CODE = 103
    const val CHANGE_USER_ACCOUNT_ACTIVITY_REQ_CODE = 104
    const val SEARCH_PLACE_ACTIVITY_REQ_CODE = 105

    const val NO_LAT_LON_VALUE: Double = 5000.0

    const val NO_DATE_VALUE = 0L

    const val BACKGROUNDS_FOLDER = "backgrounds"

    const val USER_HAS_ACCOUNT_KEY = "user_has_account"

    const val USER_ACCOUNT_TO_SET_KEY = "user_account_to_set"
    const val USER_DELETED_KEY = "user_deleted"

    val LOGIN_PROVIDERS by lazy {
        arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().setRequireName(false).build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build())

    }
}