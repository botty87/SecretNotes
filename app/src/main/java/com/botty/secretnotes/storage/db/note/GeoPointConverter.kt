package com.botty.secretnotes.storage.db.note

import com.google.firebase.firestore.GeoPoint
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.converter.PropertyConverter

class GeoPointConverter: PropertyConverter<GeoPoint, String> {

    override fun convertToDatabaseValue(geoPoint: GeoPoint?): String? {
        return geoPoint?.run {
            getMoshiAdapter().toJson(this)
        }
    }

    override fun convertToEntityProperty(geoPointJson: String?): GeoPoint? {
        return geoPointJson?.run {
            getMoshiAdapter().fromJson(this)
        }
    }

    private fun getMoshiAdapter(): JsonAdapter<GeoPoint> {
        return Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(GeoPoint::class.java)
    }

}