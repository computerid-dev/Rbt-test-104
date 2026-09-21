package com.nugroho.remotebt

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nugroho.remotebt.databinding.ActivityHostBinding

class HostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostBinding
    private var service: BluetoothHostService? = null
    private var bound = false
    private var serviceRunning = false

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            launchService()
        } else {
            binding.tvStatus.text = "Status: izin ditolak, layanan tidak bisa jalan"
        }
    }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* hasil ditangani lewat isAdminActive saat dibutuhkan */ }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, binder: IBinder?) {
            val localBinder = binder as BluetoothHostService.LocalBinder
            service = localBinder.getService()
            bound = true
            binding.tvPin.text = service?.getSessionPin() ?: "------"
            service?.statusListener = { message ->
                runOnUiThread { binding.tvStatus.text = "Status: $message" }
            }
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            bound = false
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnToggleService.setOnClickListener {
            if (serviceRunning) stopService() else requestPermissionsThenStart()
        }

        binding.btnDeviceAdmin.setOnClickListener {
            requestDeviceAdmin()
        }
    }

    private fun requestPermissionsThenStart() {
        val missing = requiredPermissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            launchService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun launchService() {
        val intent = Intent(this, BluetoothHostService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        serviceRunning = true
        binding.btnToggleService.text = "Matikan Layanan"
        binding.tvStatus.text = "Status: layanan aktif"
    }

    private fun stopService() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        stopService(Intent(this, BluetoothHostService::class.java))
        serviceRunning = false
        binding.btnToggleService.text = "Aktifkan Layanan"
        binding.tvStatus.text = "Status: layanan dimatikan"
        binding.tvPin.text = "------"
    }

    private fun requestDeviceAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            binding.btnDeviceAdmin.text = "Izin Kunci Layar Aktif"
            return
        }
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Dibutuhkan agar RemoteBT bisa mengunci layar saat diminta dari perangkat lain."
            )
        }
        deviceAdminLauncher.launch(intent)
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }
}
