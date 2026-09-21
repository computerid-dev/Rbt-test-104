package com.nugroho.remotebt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nugroho.remotebt.actions.DeviceActionExecutor
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Service yang berjalan di perangkat yang INGIN DIKONTROL (host).
 * Membuka satu server socket RFCOMM, menerima satu koneksi pada satu
 * waktu, memverifikasi PIN sesi sebelum menerima perintah apa pun.
 */
class BluetoothHostService : Service() {

    private val binder = LocalBinder()
    private var serverSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null
    private val running = AtomicBoolean(false)
    private lateinit var executor: DeviceActionExecutor
    private lateinit var sessionPin: String

    var statusListener: ((String) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHostService = this@BluetoothHostService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        executor = DeviceActionExecutor(applicationContext)
        sessionPin = PinGenerator.generate()
        createNotificationChannel()
    }

    fun getSessionPin(): String = sessionPin

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        startListening()
        return START_STICKY
    }

    private fun startListening() {
        if (running.get()) return
        running.set(true)

        thread(name = "RemoteBT-Accept") {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
                    reportStatus("Bluetooth tidak tersedia di perangkat ini")
                    return@thread
                }

                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    CommandProtocol.SERVICE_NAME,
                    CommandProtocol.SERVICE_UUID
                )
                reportStatus("Menunggu koneksi\u2026")

                while (running.get()) {
                    val socket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        if (running.get()) Log.e(TAG, "accept() gagal", e)
                        null
                    } ?: break

                    handleClient(socket)
                }
            } catch (e: SecurityException) {
                reportStatus("Izin Bluetooth belum diberikan")
                Log.e(TAG, "Izin kurang", e)
            } catch (e: IOException) {
                Log.e(TAG, "Gagal membuka server socket", e)
            }
        }
    }

    private fun handleClient(socket: BluetoothSocket) {
        currentClientSocket = socket
        val deviceName = try {
            socket.remoteDevice?.name ?: "perangkat tidak dikenal"
        } catch (e: SecurityException) {
            "perangkat tidak dikenal"
        }

        try {
            val input = BufferedReader(InputStreamReader(socket.inputStream))
            val output = socket.outputStream

            val firstLine = input.readLine() ?: run {
                socket.close()
                return
            }

            if (!authenticate(firstLine, output)) {
                reportStatus("Percobaan koneksi ditolak (PIN salah) dari $deviceName")
                socket.close()
                return
            }

            reportStatus("Terhubung dengan $deviceName")

            var line: String?
            while (running.get()) {
                line = input.readLine() ?: break
                val command = line.trim()
                if (CommandProtocol.isKnownCommand(command)) {
                    executor.execute(command)
                } else {
                    Log.w(TAG, "Perintah asing ditolak: $command")
                }
            }
        } catch (e: IOException) {
            Log.i(TAG, "Koneksi dengan $deviceName berakhir: ${e.message}")
        } finally {
            reportStatus("Terputus dari $deviceName. Menunggu koneksi\u2026")
            try { socket.close() } catch (_: IOException) {}
            currentClientSocket = null
        }
    }

    private fun authenticate(firstLine: String, output: OutputStream): Boolean {
        val trimmed = firstLine.trim()
        if (!trimmed.startsWith(CommandProtocol.AUTH_PREFIX)) {
            writeLine(output, CommandProtocol.AUTH_DENY)
            return false
        }
        val pinAttempt = trimmed.removePrefix(CommandProtocol.AUTH_PREFIX)
        return if (pinAttempt == sessionPin) {
            writeLine(output, CommandProtocol.AUTH_OK)
            true
        } else {
            writeLine(output, CommandProtocol.AUTH_DENY)
            false
        }
    }

    private fun writeLine(output: OutputStream, text: String) {
        try {
            output.write((text + "\n").toByteArray())
            output.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Gagal menulis balasan", e)
        }
    }

    private fun reportStatus(message: String) {
        Log.i(TAG, message)
        statusListener?.invoke(message)
    }

    override fun onDestroy() {
        running.set(false)
        try { currentClientSocket?.close() } catch (_: IOException) {}
        try { serverSocket?.close() } catch (_: IOException) {}
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_running))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "BluetoothHostService"
        private const val CHANNEL_ID = "remotebt_service"
        private const val NOTIF_ID = 1001
    }
}
