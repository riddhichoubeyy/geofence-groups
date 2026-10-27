package com.example.geofencegroups.data.device

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class DeviceTokenRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    private fun col(uid: String) = db.collection("users").document(uid).collection("devices")

    suspend fun registerCurrent() {
        register(FirebaseMessaging.getInstance().token.await())
    }

    suspend fun register(token: String) {
        val uid = auth.currentUser?.uid ?: return
        col(uid).document(token).set(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun unregisterCurrent() {
        val uid = auth.currentUser?.uid ?: return
        val token = FirebaseMessaging.getInstance().token.await()
        col(uid).document(token).delete().await()
        FirebaseMessaging.getInstance().deleteToken().await()
    }
}
