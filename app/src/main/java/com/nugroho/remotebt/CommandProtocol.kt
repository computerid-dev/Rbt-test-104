package com.nugroho.remotebt

import java.util.UUID

/**
 * Protokol komunikasi sederhana lewat Bluetooth RFCOMM.
 *
 * Setiap pesan adalah satu baris teks yang diakhiri newline ("\n").
 * Sebelum boleh mengirim perintah, klien wajib mengirim AUTH:<pin>
 * dan menerima balasan OK dari host. Tanpa itu, host menolak
 * semua perintah lain dan langsung menutup koneksi.
 */
object CommandProtocol {

    // UUID khusus aplikasi ini, dipakai host & klien supaya tidak bentrok
    // dengan layanan Bluetooth lain di perangkat.
    val SERVICE_UUID: UUID = UUID.fromString("6b8a7e2e-2f3a-4b8a-9d0b-1a2b3c4d5e6f")
    const val SERVICE_NAME = "RemoteBT-Service"

    const val AUTH_PREFIX = "AUTH:"
    const val AUTH_OK = "OK"
    const val AUTH_DENY = "DENY"

    // Perintah yang dikenali host. Sengaja dibatasi (whitelist),
    // host menolak string apa pun di luar daftar ini.
    const val VOL_UP = "VOL_UP"
    const val VOL_DOWN = "VOL_DOWN"
    const val MUTE_TOGGLE = "MUTE_TOGGLE"
    const val PLAY_PAUSE = "PLAY_PAUSE"
    const val NEXT_TRACK = "NEXT_TRACK"
    const val PREV_TRACK = "PREV_TRACK"
    const val FLASH_ON = "FLASH_ON"
    const val FLASH_OFF = "FLASH_OFF"
    const val LOCK_SCREEN = "LOCK_SCREEN"

    val KNOWN_COMMANDS = setOf(
        VOL_UP, VOL_DOWN, MUTE_TOGGLE, PLAY_PAUSE,
        NEXT_TRACK, PREV_TRACK, FLASH_ON, FLASH_OFF, LOCK_SCREEN
    )

    fun buildAuthMessage(pin: String) = "$AUTH_PREFIX$pin"

    fun isKnownCommand(raw: String) = KNOWN_COMMANDS.contains(raw.trim())
}
