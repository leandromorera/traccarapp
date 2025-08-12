\
package com.example.traccarcam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.traccarcam.databinding.ActivityMainBinding
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtmp.utils.ConnectCheckerRtmp

class MainActivity : AppCompatActivity(), ConnectCheckerRtmp {

    private lateinit var binding: ActivityMainBinding
    private lateinit var rtmpCamera2: RtmpCamera2

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.all { it }
        if (!granted) {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rtmpCamera2 = RtmpCamera2(binding.openGlView, this)

        requestPermissionsIfNeeded()

        binding.btnStart.setOnClickListener {
            val url = binding.rtmpUrl.text.toString().trim()
            val traccar = binding.traccarUrl.text.toString().trim()
            val deviceId = binding.deviceId.text.toString().trim()
            if (url.isEmpty() || traccar.isEmpty() || deviceId.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startStreaming(url)
            startLocationService(traccar, deviceId)
        }

        binding.btnStop.setOnClickListener {
            stopStreaming()
            stopLocationService()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun startStreaming(rtmpUrl: String) {
        if (!rtmpCamera2.isStreaming) {
            if (rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
                rtmpCamera2.startPreview()
                rtmpCamera2.startStream(rtmpUrl)
            } else {
                Toast.makeText(this, "Error preparing stream", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopStreaming() {
        if (rtmpCamera2.isStreaming) {
            rtmpCamera2.stopStream()
        }
        rtmpCamera2.stopPreview()
    }

    private fun startLocationService(baseUrl: String, deviceId: String) {
        val i = Intent(this, LocationService::class.java)
        i.putExtra(LocationService.EXTRA_BASE_URL, baseUrl)
        i.putExtra(LocationService.EXTRA_DEVICE_ID, deviceId)
        startForegroundService(i)
    }

    private fun stopLocationService() {
        val i = Intent(this, LocationService::class.java)
        stopService(i)
    }

    // ConnectCheckerRtmp callbacks
    override fun onAuthErrorRtmp() = toast("RTMP auth error")
    override fun onAuthSuccessRtmp() = toast("RTMP auth ok")
    override fun onConnectionFailedRtmp(reason: String) = toast("RTMP failed: $reason")
    override fun onConnectionStartedRtmp(rtmpUrl: String) {}
    override fun onConnectionSuccessRtmp() = toast("RTMP connected")
    override fun onDisconnectRtmp() = toast("RTMP disconnected")
    override fun onNewBitrateRtmp(bitrate: Long) {}

    private fun toast(msg: String) = runOnUiThread {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationService()
        stopStreaming()
    }
}
