package com.botty.secretnotes.utilities.activites

import androidx.appcompat.app.AppCompatActivity
import com.botty.secretnotes.utilities.getAppPreferences
import com.botty.secretnotes.utilities.getMyApplication
import com.botty.secretnotes.utilities.security.Security
import com.botty.secretnotes.utilities.security.askMasterPassword
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.joda.time.LocalDateTime
import org.joda.time.Minutes

//Track the pause and resume, for ask the master password, if needed
abstract class OnPauseTrackActivity: AppCompatActivity() {

    private var passwordAsked = false

    override fun onPause() {
        super.onPause()
        if(getAppPreferences().getBoolean(Security.AUTO_LOCK_KEY, false)) {
            if(passwordAsked) {
                finishAffinity()
            }
            else {
                getMyApplication().appStartPause = LocalDateTime.now()
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun onResume() {
        fun checkIfLockDueInactivity() {
            if(getAppPreferences().getBoolean(Security.AUTO_LOCK_KEY, false)) {
                getMyApplication().appStartPause?.run {
                    val minutes = Minutes.minutesBetween(this, LocalDateTime.now()).minutes
                    if(minutes >= 1) {
                        passwordAsked = true
                        askMasterPassword(onDenied = {
                            finishAffinity()
                        })
                    }
                }
            }
        }

        checkIfLockDueInactivity()
        super.onResume()
        getMyApplication().appStartPause = null
    }

}