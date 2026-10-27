package com.example.geofencegroups.data.auth

import com.example.geofencegroups.data.device.DeviceTokenRepository
import com.example.geofencegroups.geofence.GeofenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val devices: DeviceTokenRepository,
    private val geofences: GeofenceManager,
) {
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    suspend fun signUp(name: String, email: String, password: String) {
        require(name.isNotBlank()) { "Enter a display name" }
        val user = auth.createUserWithEmailAndPassword(email, password).await().user!!
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        db.collection("users").document(user.uid).set(
            mapOf("displayName" to name, "email" to email, "createdAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun resetPassword(email: String) {
        require(email.isNotBlank()) { "Enter your email first" }
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun signOut() {
        runCatching { devices.unregisterCurrent() }
        runCatching { geofences.clear() }
        auth.signOut()
    }
}
