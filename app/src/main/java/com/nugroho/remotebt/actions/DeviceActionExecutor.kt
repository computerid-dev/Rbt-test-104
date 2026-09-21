package com.nugroho.remotebt.actions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import com.nugroho.remotebt.CommandProtocol
import com.nugroho.remotebt.DeviceAdminReceiver

/**
 * Menjalankan efek nyata dari sebuah perintah yang sudah divalidasi
 * lewat CommandProtocol.isKnownCommand(). Hanya dipanggil dari
 * BluetoothHostService, setelah koneksi lolos autentikasi PIN.
 */
class DeviceActionExecutor(private val context: Context) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val devicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val adminComponent by lazy {
        ComponentName(context, DeviceAdminReceiver::class.java)
    }

    private var flashOn = false

    fun execute(command: String) {
        Log.i(TAG, "Menjalankan perintah: $command")
        when (command) {
            CommandProtocol.VOL_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            CommandProtocol.VOL_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            CommandProtocol.MUTE_TOGGLE -> adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)
            CommandProtocol.PLAY_PAUSE -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            CommandProtocol.NEXT_TRACK -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            CommandProtocol.PREV_TRACK -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            CommandProtocol.FLASH_ON -> setTorch(true)
            CommandProtocol.FLASH_OFF -> setTorch(false)
            CommandProtocol.LOCK_SCREEN -> lockScreen()
            else -> Log.w(TAG, "Perintah tidak dikenal, diabaikan: $command")
        }
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun sendMediaKey(keyCode: Int) {
        val eventTime = System.currentTimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun setTorch(enabled: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cameraManager.setTorchMode(cameraId, enabled)
            flashOn = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengubah senter", e)
        }
    }

    private fun lockScreen() {
        try {
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    devicePolicyManager.lockNow()
                }
            } else {
                Log.w(TAG, "Izin device admin belum aktif, kunci layar dilewati")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Tidak punya izin untuk mengunci layar", e)
        }
    }

    companion object {
        private const val TAG = "DeviceActionExecutor"
    }
}
