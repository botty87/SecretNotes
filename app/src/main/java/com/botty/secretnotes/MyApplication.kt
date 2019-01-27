package com.botty.secretnotes

import android.app.Application
import com.botty.secretnotes.storage.new_db.MyObjectBox
import com.github.ajalt.reprint.core.Reprint
import com.google.android.gms.ads.MobileAds
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestore
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.LocalDateTime


class MyApplication: Application() {
    lateinit var boxStore: BoxStore
        private set

    var appStartPause: LocalDateTime? = null

    override fun onCreate() {
        super.onCreate()

        boxStore = MyObjectBox.builder().androidContext(this).build()
        if (BuildConfig.DEBUG) {
            AndroidObjectBrowser(boxStore).start(this)
        }

        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore.firestoreSettings = settings

        Reprint.initialize(this, object: Reprint.Logger{
            override fun logException(throwable: Throwable?, message: String?) {
                throwable?.run { com.botty.secretnotes.utilities.logException(this as Exception) }
            }

            override fun log(message: String?) {
            }

        })

        JodaTimeAndroid.init(this)

        MobileAds.initialize(this, getString(R.string.ad_mod_id))
    }
}