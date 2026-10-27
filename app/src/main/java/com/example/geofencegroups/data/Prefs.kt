package com.example.geofencegroups.data

import android.content.Context

/** Tiny local flags. Nothing sensitive is kept here. */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("orbit_prefs", Context.MODE_PRIVATE)
    var onboarded: Boolean
        get() = p.getBoolean("onboarded", false)
        set(v) { p.edit().putBoolean("onboarded", v).apply() }
}
