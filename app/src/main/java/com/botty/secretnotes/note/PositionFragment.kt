package com.botty.secretnotes.note

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.botty.secretnotes.R
import com.botty.secretnotes.storage.AppPreferences
import com.botty.secretnotes.utilities.*
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.fragment_position.*
import permissions.dispatcher.*
import java.util.*


@RuntimePermissions
class PositionFragment : NoteFragmentCallbacks() {

    private lateinit var googleMap: GoogleMap
    private lateinit var userLocation: Location
    private val locationListener by lazy {
        object : LocationListener {
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}

            override fun onLocationChanged(location: Location) {
                userLocation = location
                updateUserLocationOnMap()
            }
        }
    }
    private lateinit var userMarker: Marker
    private var positionMarker: Marker? = null
    private var firstLocationRequest = true

    private var isCameraSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_position, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MapsInitializer.initialize(activity)
        mapNotePosition.onCreate(savedInstanceState)
        mapNotePosition.getMapAsync {
            googleMap = it
            googleMap.uiSettings.isMapToolbarEnabled = false
            if(userVisibleHint) {
                updateUserLocationOnMap()
            }
            setNotePosition()

            googleMap.setOnMapLongClickListener {newPosition ->
                noteCallbacks?.getNote()?.position = newPosition.getGeoPoint()
                setNotePosition()
                toastSuccess(R.string.end_note_position_drag)
            }

            setMapButtons()
        }
    }

    private fun setMapButtons() {
        fun setSearchLocation() {
            buttonSearchPlace.setOnClickListener {
                context?.run {
                    if(!Places.isInitialized()) {
                        Places.initialize(applicationContext, "AIzaSyCxHJcbSH_6pnXFEO9v7T2TZ2iUx2oH_oc")
                    }

                    val fields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG)

                    Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this)
                            .run {
                                startActivityForResult(this, Constants.SEARCH_PLACE_ACTIVITY_REQ_CODE)
                            }
                }
            }
        }

        setSearchLocation()

        buttonMinusZoom.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        buttonPlusZoom.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        buttonUserPosition.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(userLocation.getLatLng()))
            userMarker.showInfoWindow()
        }

        buttonNotePosition.setOnClickListener {
            positionMarker?.run { googleMap.animateCamera(CameraUpdateFactory.newLatLng(position)) }
            positionMarker?.showInfoWindow()
        }

        buttonRemoveNotePosition.setOnClickListener {
            val oldPosition = noteCallbacks?.getNote()?.position

            noteCallbacks?.getNote()?.position = null
            positionMarker?.remove()
            positionMarker = null
            buttonRemoveNotePosition.visibility = View.GONE
            buttonNotePosition.visibility = View.GONE

            activity?.run {
                showCafeBar(R.string.note_position_removed, noteCoordLayout, CafeBar.Duration.LONG,
                        R.string.undo to CafeBarCallback {
                            noteCallbacks?.getNote()?.position = oldPosition
                            setNotePosition()
                            it.dismiss()
                        })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Constants.SEARCH_PLACE_ACTIVITY_REQ_CODE && resultCode == RESULT_OK) {
            data?.run {
                val place = Autocomplete.getPlaceFromIntent(this)
                noteCallbacks?.getNote()?.position = place.latLng?.getGeoPoint()
                place.name?.run {
                    val message = getString(R.string.note_position_now_is) + ": " + this
                    toastSuccess(message)
                }
                setNotePosition()
            }
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if(isVisibleToUser) {
            if(AppPreferences.firstTimeNotePosition && ::googleMap.isInitialized ) {
                toastInfo(R.string.map_long_press_marker)
                AppPreferences.firstTimeNotePosition = false
            }
            startListenGpsPositionWithPermissionCheck()
        }
        else {
            stopListenGpsPosition()
        }
    }

    @SuppressLint("MissingPermission")
    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startListenGpsPosition() {
        (context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.run {
            Criteria().apply {
                powerRequirement = Criteria.POWER_LOW
                accuracy = Criteria.ACCURACY_FINE
            }.run {
                getBestProvider(this, true)?.run {
                    userLocation = getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
                updateUserLocationOnMap()
                requestLocationUpdates(1000, 20f, this, locationListener, null)
            }
        }
    }

    private fun stopListenGpsPosition() {
        (context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.run {
            removeUpdates(locationListener)
        }
    }

    private fun updateUserLocationOnMap() {
        if(!::userLocation.isInitialized or !::googleMap.isInitialized) {
            return
        }

        if(buttonUserPosition.visibility != View.VISIBLE) {
            buttonUserPosition.visibility = View.VISIBLE
        }

        userLocation.getLatLng().let {userLatLng ->
            if(::userMarker.isInitialized) {
                userMarker.position = userLatLng
            }
            else {
                if(noteCallbacks?.getNote()?.position == null) {
                    if(isCameraSet) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(userLatLng))
                    }
                    else {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                        isCameraSet = true
                    }
                }
                userMarker = MarkerOptions()
                        .position(userLatLng)
                        .title(getString(R.string.your_position))
                        .addToMap(googleMap)
            }
        }
    }

    private fun setNotePosition() {
        noteCallbacks?.getNote()?.position?.run {
            if(positionMarker != null) {
                getLatLng().run {
                    positionMarker!!.position = this
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(this))
                }
            }
            else {
                getLatLng().let { latLng ->
                    positionMarker = MarkerOptions()
                            .position(latLng)
                            .title(getString(R.string.your_saved_position))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.note_position_marker))
                            .draggable(true)
                            .addToMap(googleMap)

                    if(isCameraSet) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    }
                    else {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                        isCameraSet = true
                    }
                }

                googleMap.setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener {
                    override fun onMarkerDragStart(p0: Marker?) {
                        toastInfo(R.string.start_note_position_drag)
                    }

                    override fun onMarkerDragEnd(marker: Marker?) {
                        toastSuccess(R.string.end_note_position_drag)
                        marker?.position?.run {
                            noteCallbacks?.getNote()?.position = getGeoPoint()
                            googleMap.animateCamera(CameraUpdateFactory.newLatLng(this))
                        }
                    }

                    override fun onMarkerDrag(p0: Marker?) {}

                })
            }

            buttonNotePosition.visibility = View.VISIBLE

            buttonRemoveNotePosition.visibility = View.VISIBLE
        }
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    fun askFineLocationPermission(request: PermissionRequest) {
        if(firstLocationRequest) {
            askFineLocationPermissionDialog(request)
        }
    }

    private fun askFineLocationPermissionDialog(request: PermissionRequest) {
        firstLocationRequest = false
        getDialog()?.apply {
            title(R.string.user_position_title)
            message(R.string.user_position_message)
            positiveButton(R.string.allow) { request.proceed() }
            negativeButton(R.string.deny) { request.cancel()}
        }?.show()
    }

    @Suppress("unused")
    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onFineLocationPermissionDenied() {
        if(firstLocationRequest) {
            startListenGpsPositionWithPermissionCheck()
        }
        else {
            toastError(R.string.no_location_permission)
        }
    }

    @Suppress("unused")
    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onNeverAskAgainLocationPermission() {
        toastError(R.string.no_location_permission_no_ask)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onStart() {
        super.onStart()
        mapNotePosition?.onStart()
        if(userVisibleHint) {
            startListenGpsPositionWithPermissionCheck()
        }
    }

    override fun onResume() {
        super.onResume()
        mapNotePosition?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapNotePosition?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapNotePosition?.onStop()
        stopListenGpsPosition()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapNotePosition?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapNotePosition?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapNotePosition?.onLowMemory()
    }

    companion object {
        fun newInstance(): PositionFragment {
            return PositionFragment()
        }
    }
}
