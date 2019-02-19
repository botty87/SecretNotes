package com.botty.secretnotes.utilities.security

import android.app.Activity
import android.os.Build
import android.text.InputType
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.botty.secretnotes.R
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.utilities.getDialog
import com.botty.secretnotes.utilities.toastError
import com.github.ajalt.reprint.core.AuthenticationFailureReason
import com.github.ajalt.reprint.core.AuthenticationListener
import com.github.ajalt.reprint.core.Reprint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.dip

@ExperimentalCoroutinesApi
private fun hasFingerprint(): Boolean {
    return Reprint.hasFingerprintRegistered() && Reprint.isHardwarePresent()
}

@ExperimentalCoroutinesApi
private fun fingerprintAuth(onSuccess: () -> Unit, onFailure: () -> Unit) {
    Reprint.authenticate(object: AuthenticationListener {
        override fun onSuccess(moduleTag: Int) {
            onSuccess.invoke()
        }

        override fun onFailure(failureReason: AuthenticationFailureReason?, fatal: Boolean, errorMessage: CharSequence?, moduleTag: Int, errorCode: Int) {
            if(fatal) {
                onFailure.invoke()
            }
        }
    })
}

@ExperimentalCoroutinesApi
fun Activity.askMasterPassword(onSuccess: (() -> Unit)? = null, onDenied: (() -> Unit)? = null) {

    fun askPasswordInput() {
        var retries = 3
        getDialog()
                .title(R.string.master_password_title)
                .message(R.string.insert_master_pas)
                .positiveButton(R.string.unlock)
                .negativeButton(R.string.cancel) {
                    onDenied?.invoke()
                }
                .noAutoDismiss()
                .show {
                    val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    input(inputType = inputType) {_, password ->
                        when {
                            Security.passwordMatch(AppPreferences.masterPas, password.toString()) -> {
                                dismiss()
                                onSuccess?.invoke()
                            }
                            retries > 0 -> {
                                retries--
                                toastError(getString(R.string.wrong_master_password))
                            }
                            else -> {
                                dismiss()
                                toastError(R.string.access_denied)
                                onDenied?.invoke()
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getInputField().importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                    }
                }
    }

    fun askFingerprintInput() {
        val imageViewFinger = ImageView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(dip(100), dip(100))
            setImageResource(R.drawable.fingerprint_100dp)
        }

        getDialog()
                .title(R.string.master_password_title)
                .customView(view = imageViewFinger)
                .negativeButton(R.string.cancel) {
                    onDenied?.invoke()
                }
                .positiveButton(R.string.insert_password) {
                    askPasswordInput()
                }
                .show {
                    fingerprintAuth({
                        dismiss()
                        onSuccess?.invoke()
                    }, {
                        toastError(R.string.fingerprint_not_recognized)
                        dismiss()
                        askPasswordInput()
                    })
                }
    }

    if(hasFingerprint()) {
        askFingerprintInput()
    }
    else {
        askPasswordInput()
    }

}