package com.botty.secretnotes.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.afollestad.materialdialogs.MaterialDialog
import com.botty.secretnotes.BuildConfig
import com.botty.secretnotes.MyApplication
import com.botty.secretnotes.R
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crashlytics.android.Crashlytics
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import es.dmoral.toasty.Toasty
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random


fun logException(e: Exception) {
    if(!BuildConfig.DEBUG) {
        Crashlytics.logException(e)
    }
}

fun setCrashlyticsUserId(uid: String) {
    if(!BuildConfig.DEBUG) {
        Crashlytics.setUserIdentifier(uid)
    }
}

fun Context.getColorStateListCompat(colorRes: Int): ColorStateList? {
    return ContextCompat.getColorStateList(this, colorRes)
}

fun Activity.setBackground(imageView: ImageView, imageRes: Int? = null) {
    try {
        val imageID = Random.nextInt(0, 10)
        val image = "$imageID.webp"
        val imageRef = imageRes ?:
        FirebaseStorage.getInstance().reference.child(Constants.BACKGROUNDS_FOLDER).child(image)
        GlideApp.with(this)
                .load(imageRef)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .fallback(R.drawable.default_background)
                .error(R.drawable.default_background)
                .into(imageView)
    } catch (e: Exception) {
        logException(e)
    }
}

fun Activity.loadAd(adView: AdView) {
    val adRequest = AdRequest.Builder()
            .apply {
                if(BuildConfig.DEBUG) {
                    addTestDevice("09C88A22A06FE310E2DA9CC27BA4D3AF")
                }
            }.build()

    adView.adListener = MyAdListener(this)
    adView.loadAd(adRequest)
}

fun Context.getAppPreferences(): SharedPreferences {
    return getSharedPreferences("note_preferences", Context.MODE_PRIVATE)
}

fun Context.userHasAccount(): Boolean {
    return getAppPreferences().getBoolean(Constants.USER_HAS_ACCOUNT_KEY, false)
}

fun Context.getDialog(): MaterialDialog {
    return MaterialDialog(this)
            .cancelable(false)
}

fun Context.toastSuccess(stringRes: Int) {
    Toasty.success(this, stringRes).show()
}

fun Context.toastError(stringRes: Int) {
    Toasty.error(this, stringRes, Toast.LENGTH_LONG).show()
}

fun Context.toastError(message: String) {
    Toasty.error(this, message, Toast.LENGTH_LONG).show()
}

fun Activity.showCafeBar(contentRes: Int, coordLayout: CoordinatorLayout? = null, duration: Int? = null,
                         action: Pair<Int, CafeBarCallback>? = null ){

    if(getAppPreferences().getBoolean(Constants.FIRST_SNACKBAR_KEY, true)) {
        Toasty.info(this, R.string.first_snackbark_advice, Toast.LENGTH_LONG).show()

        getAppPreferences().edit {
            putBoolean(Constants.FIRST_SNACKBAR_KEY, false)
        }
    }

    val builder = CafeBar.builder(this)
            .swipeToDismiss(true)
            .autoDismiss(true)
            .content(contentRes)

    coordLayout?.run {
        builder.to(this)
    }

    duration?.run {
        builder.duration(this)
    }

    builder.build().let {cafeBar ->
        action?.let {action ->
            cafeBar.setAction(action.first, action.second)
            cafeBar.setAction(action.first, action.second)
        }
        cafeBar.show()
    }
}

fun Activity.getMyApplication(): MyApplication {
    return (application as MyApplication)
}

fun Activity.openDialer(phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse(phone)
    }
    startActivity(intent)
}

suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}