package com.botty.secretnotes

import android.app.Application
import android.util.Log
import com.botty.secretnotes.storage.db.MyObjectBox
import com.botty.secretnotes.storage.storage_extensions.ObjectBoxStorage.boxStore
import com.botty.secretnotes.utilities.MyJobCreator
import com.evernote.android.job.JobManager
import com.getkeepsafe.relinker.ReLinker
import com.github.ajalt.reprint.core.Reprint
import com.google.android.gms.ads.MobileAds
import com.marcinmoskala.kotlinpreferences.PreferenceHolder
import io.karn.notify.Notify
import io.objectbox.android.AndroidObjectBrowser
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.LocalDateTime


class MyApplication: Application() {

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

        JobManager.create(this).addJobCreator(MyJobCreator())

        Notify.defaultConfig {
            alerting(Notify.CHANNEL_DEFAULT_KEY) {
                this.channelImportance = Notify.IMPORTANCE_MAX
            }
        }
    }
}