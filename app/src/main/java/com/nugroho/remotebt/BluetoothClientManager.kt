package com.nugroho.remotebt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import kotlin.concurrent.thread

/**
 * Dipakai di perangkat yang berperan sebagai REMOTE (client).
 * Membuka satu koneksi RFCOMM ke host, melakukan handshake PIN,
 * lalu mengirim perintah satu baris per panggilan sendCommand().
 */
class BluetoothClientManager(
    private val onConnected: () -> Unit,
    private val onAuthFailed: () -> Unit,
    private val onDisconnected: (String) -> Unit
) {
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null

    fun connect(device: BluetoothDevice, pin: String) {
        thread(name = "RemoteBT-Client") {
            try {
                val newSocket = device.createRfcommSocketToServiceRecord(CommandProtocol.SERVICE_UUID)
                newSocket.connect()
                socket = newSocket
                output = newSocket.outputStream

                val input = BufferedReader(InputStreamReader(newSocket.inputStream))
                writeLine(CommandProtocol.buildAuthMessage(pin))

                val reply = input.readLine()
                if (reply?.trim() == CommandProtocol.AUTH_OK) {
                    onConnected()
                    listenForDisconnect(newSocket)
                } else {
                    onAuthFailed()
                    closeQuietly()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Gagal konek: ${e.message}", e)
                onDisconnected(e.message ?: "Gagal terhubung")
                closeQuietly()
            } catch (e: SecurityException) {
                Log.e(TAG, "Izin Bluetooth kurang", e)
                onDisconnected("Izin Bluetooth belum diberikan")
            }
        }
    }

    private fun listenForDisconnect(activeSocket: BluetoothSocket) {
        try {
            val input = activeSocket.inputStream
            val buffer = ByteArray(1)
            // read() akan melempar/return -1 begitu koneksi diputus host
            while (input.read(buffer) >= 0) {
                // host tidak mengirim data lain setelah handshake, jadi loop ini
                // murni dipakai untuk mendeteksi kapan koneksi terputus
            }
        } catch (e: IOException) {
            // normal terjadi saat koneksi ditutup
        } finally {
            onDisconnected("Koneksi berakhir")
            closeQuietly()
        }
    }

    fun sendCommand(command: String) {
        if (!CommandProtocol.isKnownCommand(command)) return
        writeLine(command)
    }

    private fun writeLine(text: String) {
        try {
            output?.write((text + "\n").toByteArray())
            output?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Gagal mengirim perintah", e)
            onDisconnected("Gagal mengirim data")
        }
    }

    fun disconnect() {
        closeQuietly()
    }

    private fun closeQuietly() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        output = null
    }

    companion object {
        private const val TAG = "BluetoothClientManager"
    }
}
