package com.nugroho.remotebt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nugroho.remotebt.databinding.ActivityClientBinding

class ClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientBinding
    private var pairedDevices: List<BluetoothDevice> = emptyList()
    private var clientManager: BluetoothClientManager? = null

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            loadPairedDevices()
        } else {
            binding.tvClientStatus.text = "Status: izin Bluetooth ditolak"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setControlsEnabled(false)

        val missing = requiredPermissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            loadPairedDevices()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }

        binding.btnConnect.setOnClickListener { connectToSelectedDevice() }

        binding.btnVolUp.setOnClickListener { send(CommandProtocol.VOL_UP) }
        binding.btnVolDown.setOnClickListener { send(CommandProtocol.VOL_DOWN) }
        binding.btnMute.setOnClickListener { send(CommandProtocol.MUTE_TOGGLE) }
        binding.btnPlayPause.setOnClickListener { send(CommandProtocol.PLAY_PAUSE) }
        binding.btnNext.setOnClickListener { send(CommandProtocol.NEXT_TRACK) }
        binding.btnPrev.setOnClickListener { send(CommandProtocol.PREV_TRACK) }
        binding.btnFlashOn.setOnClickListener { send(CommandProtocol.FLASH_ON) }
        binding.btnFlashOff.setOnClickListener { send(CommandProtocol.FLASH_OFF) }
        binding.btnLock.setOnClickListener { send(CommandProtocol.LOCK_SCREEN) }
    }

    private fun loadPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                binding.tvClientStatus.text = "Status: aktifkan Bluetooth terlebih dahulu"
                return
            }
            pairedDevices = adapter.bondedDevices.toList()
            val names = pairedDevices.map { it.name ?: it.address }
            binding.spinnerDevices.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, names
            )
            if (pairedDevices.isEmpty()) {
                binding.tvClientStatus.text = "Status: belum ada perangkat yang di-pairing. Pairing dulu lewat Pengaturan Bluetooth."
            }
        } catch (e: SecurityException) {
            binding.tvClientStatus.text = "Status: izin Bluetooth belum diberikan"
        }
    }

    private fun connectToSelectedDevice() {
        val position = binding.spinnerDevices.selectedItemPosition
        if (position < 0 || position >= pairedDevices.size) {
            Toast.makeText(this, "Pilih perangkat dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val pin = binding.etPin.text.toString().trim()
        if (pin.length != 6) {
            Toast.makeText(this, "PIN harus 6 digit", Toast.LENGTH_SHORT).show()
            return
        }

        val device = pairedDevices[position]
        binding.tvClientStatus.text = "Status: menghubungkan\u2026"

        clientManager = BluetoothClientManager(
            onConnected = {
                runOnUiThread {
                    binding.tvClientStatus.text = "Status: terhubung"
                    setControlsEnabled(true)
                }
            },
            onAuthFailed = {
                runOnUiThread {
                    binding.tvClientStatus.text = "Status: PIN salah, ditolak host"
                    setControlsEnabled(false)
                }
            },
            onDisconnected = { reason ->
                runOnUiThread {
                    binding.tvClientStatus.text = "Status: terputus ($reason)"
                    setControlsEnabled(false)
                }
            }
        )
        clientManager?.connect(device, pin)
    }

    private fun send(command: String) {
        clientManager?.sendCommand(command)
    }

    private fun setControlsEnabled(enabled: Boolean) {
        binding.gridControls.alpha = if (enabled) 1.0f else 0.4f
        binding.gridControls.isEnabled = enabled
        for (i in 0 until binding.gridControls.childCount) {
            binding.gridControls.getChildAt(i).isEnabled = enabled
        }
    }

    override fun onDestroy() {
        clientManager?.disconnect()
        super.onDestroy()
    }
}
