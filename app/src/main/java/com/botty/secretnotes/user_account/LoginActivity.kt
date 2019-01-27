package com.botty.secretnotes.user_account

import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.botty.secretnotes.R
import com.botty.secretnotes.utilities.Constants
import com.botty.secretnotes.utilities.logException
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        fun setTransition() {
            with(window) {
                requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
                exitTransition = Fade()
            }
        }

        setTransition()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        buttonNoAccount.setOnClickListener {
            if(FirebaseAuth.getInstance().currentUser?.isAnonymous == true) {
                userLogged(false)
                return@setOnClickListener
            }
            val snackProgressBarManager = SnackProgressBarManager(mainLayout)
            snackProgressBarManager.setOnDisplayListener(object : SnackProgressBarManager.OnDisplayListener {
                override fun onDismissed(snackProgressBar: SnackProgressBar, onDisplayId: Int) {}

                override fun onShown(snackProgressBar: SnackProgressBar, onDisplayId: Int) {
                    FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(this@LoginActivity) {task ->
                        if(task.isSuccessful) {
                            userLogged(false)
                        }
                        else {
                            snackProgressBarManager.dismiss()
                            task.exception?.run {
                                Toasty.error(this@LoginActivity, localizedMessage, Toast.LENGTH_LONG).show()
                            } ?: Toasty.error(this@LoginActivity, getString(R.string.generic_error), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })

            SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.init_no_user))
                    .setIsIndeterminate(true)
                    .run {
                        snackProgressBarManager.show(this, SnackProgressBarManager.LENGTH_INDEFINITE, 100)
                    }
        }

        buttonLogin.setOnClickListener {
            val loginIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(Constants.LOGIN_PROVIDERS)
                    .setLogo(R.drawable.user_login)
                    .setTheme(R.style.AppTheme)
                    .build()

            startActivityForResult(loginIntent, Constants.LOGIN_ACTIVITY_REQ_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if(resultCode == RESULT_OK) {
            if(requestCode == Constants.SIGN_UP_ACTIVITY_REQ_CODE || requestCode == Constants.LOGIN_ACTIVITY_REQ_CODE) {
                userLogged(true)
                return
            }
        }
        else if(requestCode == Constants.LOGIN_ACTIVITY_REQ_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            response?.error?.run {
                Toasty.error(this@LoginActivity, localizedMessage, Toast.LENGTH_LONG).show()
                logException(this)
            }
        }
    }

    private fun userLogged(userHasAccount: Boolean) {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.USER_HAS_ACCOUNT_KEY, userHasAccount)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
