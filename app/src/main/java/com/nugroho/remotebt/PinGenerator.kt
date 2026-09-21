package com.nugroho.remotebt

import kotlin.random.Random

object PinGenerator {

    /** Menghasilkan PIN numerik 6 digit, dipakai ulang setiap kali service dinyalakan. */
    fun generate(): String {
        val value = Random.nextInt(0, 1_000_000)
        return value.toString().padStart(6, '0')
    }
}
