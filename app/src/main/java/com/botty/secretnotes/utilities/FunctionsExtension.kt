package com.botty.secretnotes.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.location.Location
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.TimePicker
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.botty.secretnotes.BuildConfig
import com.botty.secretnotes.MyApplication
import com.botty.secretnotes.R
import com.botty.secretnotes.note.NoteFragmentCallbacks
import com.botty.secretnotes.storage.AppPreferences
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crashlytics.android.Crashlytics
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.prolificinteractive.materialcalendarview.CalendarDay
import es.dmoral.toasty.Toasty
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import java.util.*
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

fun Context.getDialog(): MaterialDialog {
    return MaterialDialog(this)
            .cancelable(false)
}

fun NoteFragmentCallbacks.getDialog(): MaterialDialog? {
    return this.context?.run {
        MaterialDialog(this)
                .cancelable(false)
    }
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

fun NoteFragmentCallbacks.toastError(stringRes: Int) {
    this.context?.run {
        Toasty.error(this, stringRes, Toast.LENGTH_LONG).show()
    }
}

fun NoteFragmentCallbacks.toastInfo(stringRes: Int) {
    this.context?.run {
        Toasty.info(this, stringRes, Toast.LENGTH_LONG).show()
    }
}

fun NoteFragmentCallbacks.toastSuccess(stringRes: Int) {
    this.context?.run {
        Toasty.success(this, stringRes, Toast.LENGTH_LONG).show()
    }
}

fun NoteFragmentCallbacks.toastSuccess(message: String) {
    this.context?.run {
        Toasty.success(this, message, Toast.LENGTH_LONG).show()
    }
}

fun Activity.showCafeBar(contentRes: Int, coordLayout: CoordinatorLayout? = null, duration: Int? = null,
                         action: Pair<Int, CafeBarCallback>? = null ){

    if(AppPreferences.firstSnackbar) {
        Toasty.info(this, R.string.first_snackbark_advice, Toast.LENGTH_LONG).show()
        AppPreferences.firstSnackbar = false
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

fun Location.getLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun GeoPoint.getLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun LatLng.getGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}

fun MarkerOptions.addToMap(googleMap: GoogleMap): Marker {
    return googleMap.addMarker(this)
}

fun Date.getCalendarDay(): CalendarDay {
    LocalDate.fromDateFields(this).run {
        return CalendarDay.from(year, monthOfYear, dayOfMonth)
    }
}

fun TimePicker.setFromDate(date: Date) {
    val dateTime = LocalDateTime.fromDateFields(date)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        hour = dateTime.hourOfDay
        minute = dateTime.minuteOfHour
    }
    else {
        currentHour = dateTime.hourOfDay
        currentMinute = dateTime.millisOfDay
    }
}

fun TimePicker.getHourAndMinute(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Pair (hour, minute)
    }
    else {
        Pair (currentHour, currentMinute)
    }
}

fun Date.getLocalDateTime(): LocalDateTime {
    return LocalDateTime.fromDateFields(this)
}

suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}