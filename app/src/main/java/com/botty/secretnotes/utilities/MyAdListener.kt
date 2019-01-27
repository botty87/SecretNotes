package com.botty.secretnotes.utilities

import android.content.Context
import android.os.Bundle
import com.google.android.gms.ads.AdListener
import com.google.firebase.analytics.FirebaseAnalytics

class MyAdListener(context: Context): AdListener() {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun onAdClicked() {
        super.onAdClicked()
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_clicked")
        })
    }

    override fun onAdLoaded() {
        super.onAdLoaded()
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_loaded")
        })
    }

    override fun onAdImpression() {
        super.onAdImpression()
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_impression")
        })
    }

    override fun onAdClosed() {
        super.onAdClosed()
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_closed")
        })
    }

    override fun onAdLeftApplication() {
        super.onAdLeftApplication()
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_left_app")
        })
    }

    override fun onAdOpened() {
        super.onAdOpened()
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_opened")
        })
    }

    override fun onAdFailedToLoad(id: Int) {
        super.onAdFailedToLoad(id)
        firebaseAnalytics.logEvent("on_ad_event", Bundle().apply {
            putString("event", "ad_fail")
            putInt("fail_id", id)
        })
    }
}