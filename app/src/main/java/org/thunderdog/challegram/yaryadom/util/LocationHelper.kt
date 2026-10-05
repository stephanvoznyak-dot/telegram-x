package org.thunderdog.challegram.yaryadom.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Простой помощник геолокации для модуля «Я рядом».
 * Использует стандартный LocationManager (без Google Play Services).
 */
class LocationHelper(private val context: Context) {

    data class Result(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    )

    interface Callback {
        fun onLocation(result: Result)
        fun onError(message: String)
        fun onPermissionRequired()
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun requestLocation(callback: Callback) {
        if (!hasPermission()) {
            callback.onPermissionRequired()
            return
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> {
                callback.onError("Геолокация отключена. Включите GPS или сеть.")
                return
            }
        }

        // Сначала пробуем last known
        val last = locationManager.getLastKnownLocation(provider)
        if (last != null && System.currentTimeMillis() - last.time < 60_000) {
            callback.onLocation(Result(last.latitude, last.longitude, last.accuracy))
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                callback.onLocation(Result(location.latitude, location.longitude, location.accuracy))
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                locationManager.removeUpdates(this)
                callback.onError("Провайдер геолокации отключён")
            }
        }

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            callback.onError("Не удалось получить геолокацию: ${e.message}")
        }
    }
}
