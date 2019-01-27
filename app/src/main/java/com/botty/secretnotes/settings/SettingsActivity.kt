package com.botty.secretnotes.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.View.GONE
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.botty.secretnotes.R
import com.botty.secretnotes.databinding.ActivitySettingsBinding
import com.botty.secretnotes.storage.storage_extensions.moveDBLocally
import com.botty.secretnotes.utilities.*
import com.botty.secretnotes.utilities.activites.OnPauseTrackActivity
import com.botty.secretnotes.utilities.security.Security
import com.botty.secretnotes.utilities.security.askMasterPassword
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.*
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.layout_settings_user_account.*
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class SettingsActivity : OnPauseTrackActivity(), CoroutineScope by MainScope() {

    private lateinit var settingsBinding: ActivitySettingsBinding

    private var userDeleted = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        settingsBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        settingsBinding.settings = SettingsContainer.getSettingsContainer(this)

        getAppPreferences().getBoolean(Security.AUTO_LOCK_KEY, true).run {
            settingsBinding.autoLock = this
        }

        setBackground(imageViewBackground, R.drawable.settings_background)

        buttonSave.setOnClickListener {
            settingsBinding.settings?.storeSettings(this)
            settingsBinding.autoLock?.let {autoLock ->
                getAppPreferences().edit {
                    putBoolean(Security.AUTO_LOCK_KEY, autoLock)
                }
            }
            toastSuccess(R.string.settings_saved)
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(Constants.USER_DELETED_KEY, userDeleted)
            })
            finish()
        }

        //If we have an user account set the view stub
        setUserAccountStub()

        setButtonMasterPassword()
    }

    private fun setButtonMasterPassword() {

        fun removePassword() {
            askMasterPassword({
                getAppPreferences().edit {
                    remove(Security.MASTER_PAS_KEY)
                }
                toastSuccess(R.string.master_password_removed)
                setButtonMasterPassword()
            })
        }

        fun setPassword() {
            var password: String? = null
            getDialog()
                    .title(R.string.master_password_title)
                    .message(R.string.set_master_password_message)
                    .negativeButton(R.string.cancel)
                    .positiveButton(R.string.set) {
                        password?.run {
                            val passwordHash = Security.getPasswordHash(this)
                            getAppPreferences().edit{
                                putString(Security.MASTER_PAS_KEY, passwordHash)
                            }
                            toastSuccess(R.string.master_password_set)
                            setButtonMasterPassword()
                        }
                    }
                    .show {
                        setActionButtonEnabled(WhichButton.POSITIVE, false)

                        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        input(inputType = inputType, waitForPositiveButton = false) { dialog, text ->
                            val isValid = text.length >= 6
                            dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                            password = if(isValid) text.toString() else null
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getInputField()?.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                        }
                    }
        }

        if(getAppPreferences().contains(Security.MASTER_PAS_KEY)) {
            buttonMasterPassword.text = getString(R.string.remove)
            buttonMasterPassword.setOnClickListener {
                removePassword()
            }
        }
        else {
            buttonMasterPassword.text = getString(R.string.set_new_master_password)
            buttonMasterPassword.setOnClickListener {
                setPassword()
            }
        }
    }

    private fun setUserAccountStub() {
        FirebaseAuth.getInstance().currentUser?.let {user ->
            if(userHasAccount() && !user.isAnonymous) {
                viewStubUserAccount.inflate()

                //Button delete user
                buttonDeleteAccount.setOnClickListener {
                    if(user.providers?.contains(EmailAuthProvider.PROVIDER_ID) == true) {
                        var password: String? = null
                        getDialog()
                                .title(R.string.delete_user_title)
                                .message(R.string.delete_user_password_message)
                                .negativeButton(R.string.no)
                                .positiveButton(R.string.yes)
                                .show {
                                    val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                                    input(inputType = inputType) { _, text ->
                                        password = text.toString()
                                    }
                                }
                                .onDismiss {
                                    password?.runCatching { deleteUser(this, user) }
                                }
                    }
                    else {
                        getDialog()
                                .title(R.string.delete_user_title)
                                .message(R.string.delete_user_no_password_message)
                                .negativeButton(R.string.no)
                                .positiveButton(R.string.yes) {
                                    deleteUser(null, user)
                                }
                                .show()
                    }
                }

                //Button change password
                if(user.providers?.contains(EmailAuthProvider.PROVIDER_ID) == true) {
                    buttonChangePassword.setOnClickListener {
                        var password: String? = null
                        getDialog()
                                .title(R.string.change_user_password_title)
                                .message(R.string.change_user_password_message)
                                .negativeButton(R.string.cancel)
                                .positiveButton(R.string.save)
                                .show {
                                    setActionButtonEnabled(WhichButton.POSITIVE, false)

                                    val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                                    input(inputType = inputType, waitForPositiveButton = false) {dialog, text ->
                                        val isValid = text.length >= 6
                                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                                        password = if (isValid) text.toString() else null
                                    }
                                }
                                .onDismiss {
                                    password?.run { changePassword(this, user) }
                                }
                    }
                }
                else {
                    buttonChangePassword.visibility = GONE
                }
            }
        }
    }

    private fun deleteUser(password: String?, user: FirebaseUser) {
        fun setUserDeleted() {
            getAppPreferences().edit {
                putBoolean(Constants.USER_HAS_ACCOUNT_KEY, false)
            }
            userDeleted = true
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(Constants.USER_DELETED_KEY, userDeleted)
            })
        }

        var deleteJob: Job? = null
        SnackProgressBarManager(settingsCoordLayout).apply {
            setOnDisplayListener(object: SnackProgressBarManager.OnDisplayListener{
                override fun onDismissed(snackProgressBar: SnackProgressBar, onDisplayId: Int) {}

                override fun onShown(snackProgressBar: SnackProgressBar, onDisplayId: Int) {
                    deleteJob = launch {
                        try {
                            if(password != null) {
                                EmailAuthProvider.getCredential(user.email!!, password).run {
                                    user.reauthenticate(this).await()
                                }
                            }
                            else if(user.providers?.contains(GoogleAuthProvider.PROVIDER_ID) == true) {
                                val googleAccount = GoogleSignIn.getLastSignedInAccount(this@SettingsActivity)
                                if (googleAccount != null) {
                                    GoogleAuthProvider.getCredential(googleAccount.idToken, null).run {
                                        user.reauthenticate(this).await()
                                    }
                                } else {
                                    toastError(getString(R.string.no_google_auth_credential))
                                    dismiss()
                                    return@launch
                                }
                            }

                            moveDBLocally()
                            withContext(NonCancellable) {
                                dismiss()
                                toastSuccess(R.string.user_deleted)
                                layoutUserAccount.visibility = GONE
                                setUserDeleted()
                            }
                        }
                        catch (e: Exception) {
                            dismiss()
                            logException(e)
                            if(e is FirebaseAuthRecentLoginRequiredException && user.providers?.contains(FacebookAuthProvider.PROVIDER_ID) == true) {
                                AuthUI.getInstance().signOut(this@SettingsActivity).await()
                                FirebaseAuth.getInstance().signInAnonymously().await()
                                toastError(R.string.facebook_error_delete)
                            }
                            else {
                                toastError(e.localizedMessage)
                            }
                            if(FirebaseAuth.getInstance().currentUser?.isAnonymous != false) {
                                setUserDeleted()
                                layoutUserAccount.visibility = GONE
                            }
                        }
                    }
                }

            })
        }.run {
            SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.deleting_user))
                    .setIsIndeterminate(true)
                    .setAction(getString(R.string.cancel), object: SnackProgressBar.OnActionClickListener{
                        override fun onActionClick() {
                            deleteJob?.cancel()
                            dismiss()
                        }
                    })
                    .run {
                        show(this, SnackProgressBarManager.LENGTH_INDEFINITE, 100)
                    }

        }
    }

    private fun changePassword(newPassword: String, user: FirebaseUser, oldPassword: String? = null) {
        fun requireUserLogin() {
            getDialog()
                    .title(R.string.reauthenticate)
                    .message(R.string.reauthenticate_message)
                    .negativeButton(R.string.cancel)
                    .positiveButton(R.string.ok)
                    .show {
                        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        input(inputType = inputType) { _, text ->
                            changePassword(newPassword, user, text.toString())
                        }
                    }
        }

        var passwordJob: Job? = null
        SnackProgressBarManager(settingsCoordLayout).apply {
            setOnDisplayListener(object: SnackProgressBarManager.OnDisplayListener {
                override fun onDismissed(snackProgressBar: SnackProgressBar, onDisplayId: Int) {}

                override fun onShown(snackProgressBar: SnackProgressBar, onDisplayId: Int) {
                    passwordJob = launch {
                        try {
                            oldPassword?.run {
                                EmailAuthProvider.getCredential(user.email!!, this).run {
                                    user.reauthenticate(this).await()
                                }
                            }

                            user.updatePassword(newPassword).await()
                            toastSuccess(R.string.password_updated)
                            dismiss()
                        }
                        catch (e: Exception) {
                            dismiss()
                            if(e is FirebaseAuthRecentLoginRequiredException) {
                                requireUserLogin()
                            }
                            else {
                                logException(e)
                                toastError(e.localizedMessage)
                            }
                        }
                    }
                }
            })
        }.run {
            SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.updating_password))
                    .setIsIndeterminate(true)
                    .setAction(getString(R.string.cancel), object: SnackProgressBar.OnActionClickListener {
                        override fun onActionClick() {
                            passwordJob?.cancel()
                            dismiss()
                        }
                    })
                    .run {
                        show(this, SnackProgressBarManager.LENGTH_INDEFINITE, 100)
                    }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsBinding.settings = null
    }
}
