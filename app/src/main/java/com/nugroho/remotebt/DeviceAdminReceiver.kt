package com.nugroho.remotebt

import android.app.admin.DeviceAdminReceiver as SystemDeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : SystemDeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Izin kunci layar RemoteBT diaktifkan", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Izin kunci layar RemoteBT dimatikan", Toast.LENGTH_SHORT).show()
    }
}
