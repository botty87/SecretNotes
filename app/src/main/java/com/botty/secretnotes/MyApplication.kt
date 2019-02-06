package com.botty.secretnotes

import android.app.Application
import android.util.Log
import com.botty.secretnotes.storage.new_db.MyObjectBox
import com.getkeepsafe.relinker.ReLinker
import com.github.ajalt.reprint.core.Reprint
import com.google.android.gms.ads.MobileAds
import com.marcinmoskala.kotlinpreferences.PreferenceHolder
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.LocalDateTime


class MyApplication: Application() {
    lateinit var boxStore: BoxStore
        private set

    var appStartPause: LocalDateTime? = null

    override fun onCreate() {
        super.onCreate()

        boxStore = MyObjectBox.builder()
                .androidContext(this)
                .androidReLinker(ReLinker.log {message ->
                    Log.d("Relinker", message)
                })
                .build()
        if (BuildConfig.DEBUG) {
            AndroidObjectBrowser(boxStore).start(this)
        }

        Reprint.initialize(this, object: Reprint.Logger{
            override fun logException(throwable: Throwable?, message: String?) {
                throwable?.run { com.botty.secretnotes.utilities.logException(this as Exception) }
            }

            override fun log(message: String?) {
            }

        })

        JodaTimeAndroid.init(this)

        MobileAds.initialize(this, getString(R.string.ad_mod_id))

        PreferenceHolder.setContext(applicationContext)
    }
}