package com.example.geofencegroups

import android.content.Context
import com.example.geofencegroups.data.Prefs
import com.example.geofencegroups.data.auth.AuthRepository
import com.example.geofencegroups.data.device.DeviceTokenRepository
import com.example.geofencegroups.data.groups.GroupRepository
import com.example.geofencegroups.geofence.GeofenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance() // default region us-central1
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val prefs = Prefs(context)
    val devices = DeviceTokenRepository(firebaseAuth, firestore)
    val geofences = GeofenceManager(context)
    val auth = AuthRepository(firebaseAuth, firestore, devices, geofences)
    val groups = GroupRepository(firestore, functions)
}
