package com.example.traccarcam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.BatteryManager
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    companion object {
        const val EXTRA_BASE_URL = "baseUrl"
        const val EXTRA_DEVICE_ID = "deviceId"
        private const val CHANNEL_ID = "traccar_tracking"
        private const val NOTIF_ID = 1001
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var baseUrl: String = ""
    private var deviceId: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        baseUrl = intent?.getStringExtra(EXTRA_BASE_URL) ?: ""
        deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID) ?: ""
        if (baseUrl.isBlank() || deviceId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification("Sending GPS to Traccar"))
        startLocationUpdates()
        return START_STICKY
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Traccar Tracking", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TraccarCam")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun startLocationUpdates() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10_000L
        ).setMinUpdateIntervalMillis(5_000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val l = result.lastLocation ?: return
                val batt = getBatteryPct()

                val httpUrl = baseUrl.toHttpUrlOrNull()?.newBuilder()?.apply {
                    addQueryParameter("id", deviceId)
                    addQueryParameter("lat", l.latitude.toString())
                    addQueryParameter("lon", l.longitude.toString())
                    addQueryParameter("timestamp", (System.currentTimeMillis() / 1000).toString())
                    addQueryParameter("speed", l.speed.toString())
                    addQueryParameter("bearing", l.bearing.toString())
                    addQueryParameter("altitude", l.altitude.toString())
                    addQueryParameter("accuracy", l.accuracy.toString())
                    addQueryParameter("batt", batt.toString())
                }?.build() ?: return

                val req = Request.Builder().url(httpUrl).get().build()
                client.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: java.io.IOException) { /* ignore */ }
                    override fun onResponse(call: Call, response: Response) { response.close() }
                })
            }
        }

        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
    }

    private fun getBatteryPct(): Int {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, iFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else -1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (this::fusedClient.isInitialized && this::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }
}
