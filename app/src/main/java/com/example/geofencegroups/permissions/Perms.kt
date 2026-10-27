package com.example.geofencegroups.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

object Perms {
    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun fine(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
    fun background(ctx: Context) =
        Build.VERSION.SDK_INT < 29 || granted(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    fun notifications(ctx: Context) =
        Build.VERSION.SDK_INT < 33 || granted(ctx, Manifest.permission.POST_NOTIFICATIONS)
    fun allLocation(ctx: Context) = fine(ctx) && background(ctx)
    fun batteryUnrestricted(ctx: Context) =
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
}
