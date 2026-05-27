package com.voicefx.core.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {

    private val client: FusedLocationProviderClient
        get() = LocationServices.getFusedLocationProviderClient(context)

    private var cachedLocation: Location? = null
    private val prefs
        get() = context.getSharedPreferences("location_cache", Context.MODE_PRIVATE)

    fun getCachedLocation(): Location? {
        if (cachedLocation != null) return cachedLocation
        val lat = prefs.getFloat("lat", 0f)
        val lon = prefs.getFloat("lon", 0f)
        if (lat != 0f || lon != 0f) {
            return Location("").also {
                it.latitude = lat.toDouble()
                it.longitude = lon.toDouble()
            }
        }
        return null
    }

    fun requestSingleLocation(
        onResult: (Location?) -> Unit,
        onUnavailable: () -> Unit = {}
    ) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L
        ).setWaitForAccurateLocation(false)
            .setMaxUpdateDelayMillis(5000L)
            .build()

        val cancellationToken = CancellationTokenSource()

        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                location?.let { cacheLocation(it) }
                onResult(location)
            }
            .addOnFailureListener {
                client.lastLocation.addOnSuccessListener { lastLocation ->
                    lastLocation?.let { cacheLocation(it) }
                    onResult(lastLocation)
                }.addOnFailureListener {
                    onUnavailable()
                }
            }
    }

    private fun cacheLocation(location: Location) {
        cachedLocation = location
        prefs.edit()
            .putFloat("lat", location.latitude.toFloat())
            .putFloat("lon", location.longitude.toFloat())
            .apply()
    }
}
