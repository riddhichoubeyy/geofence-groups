package com.example.geofencegroups.data.groups

import com.example.geofencegroups.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepository(private val db: FirebaseFirestore, private val fns: FirebaseFunctions) {

    fun groups(uid: String): Flow<List<Group>> = callbackFlow {
        val reg = db.collection("groups").whereArrayContains("memberUids", uid)
            .addSnapshotListener { s, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(s?.documents?.mapNotNull { it.toGroup() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun fetchGroups(uid: String): List<Group> =
        db.collection("groups").whereArrayContains("memberUids", uid).get().await()
            .documents.mapNotNull { it.toGroup() }

    fun groupFlow(id: String): Flow<Group?> = callbackFlow {
        val reg = db.collection("groups").document(id).addSnapshotListener { s, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            trySend(s?.toGroup())
        }
        awaitClose { reg.remove() }
    }

    fun membersFlow(id: String): Flow<List<Member>> = callbackFlow {
        val reg = db.collection("groups").document(id).collection("members")
            .addSnapshotListener { s, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(s?.documents?.map { it.toMember() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun eventsFlow(id: String): Flow<List<GeoEvent>> = callbackFlow {
        val reg = db.collection("groups").document(id).collection("events")
            .orderBy("serverTime", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { s, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(s?.documents?.map { it.toEvent() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }


    private suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?> {
        val result = fns.getHttpsCallable(name).call(data).await()
        @Suppress("UNCHECKED_CAST")
        return (result.getData() as? Map<String, Any?>).orEmpty()
    }


    suspend fun createGroup(name: String): String {
        val groupId = java.util.UUID.randomUUID().toString()
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: error("Please sign in first")

        val groupData = hashMapOf(
            "name" to name,
            "ownerUid" to user.uid,
            "memberUids" to listOf(user.uid),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "geofence" to null
        )

        db.collection("groups").document(groupId).set(groupData).await()

        db.collection("groups").document(groupId)
            .collection("members").document(user.uid)
            .set(
                hashMapOf(
                    "displayName" to (user.displayName ?: user.email ?: "Owner"),
                    "role" to "owner",
                    "joinedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "insideGeofence" to null,
                    "lastTransitionAt" to null
                )
            ).await()

        return groupId
    }
    suspend fun joinGroup(code: String): String = call("joinGroup", mapOf("code" to code))["groupId"] as String

    suspend fun getInviteCode(groupId: String): String =
        call("getInviteCode", mapOf("groupId" to groupId))["inviteCode"] as? String
            ?: error("Invite code unavailable")

    suspend fun leaveGroup(groupId: String) { call("leaveGroup", mapOf("groupId" to groupId)) }

    suspend fun setGeofence(groupId: String, lat: Double, lng: Double, radiusM: Double) {
        call("setGeofence", mapOf("groupId" to groupId, "lat" to lat, "lng" to lng, "radiusM" to radiusM))
    }

    suspend fun setMemberRole(groupId: String, targetUid: String, role: String) {
        call("setMemberRole", mapOf("groupId" to groupId, "targetUid" to targetUid, "role" to role))
    }

    suspend fun reportEvent(groupId: String, type: String, lat: Double?, lng: Double?, clientTime: Long) {
        call("reportGeofenceEvent", mapOf(
            "groupId" to groupId, "type" to type, "lat" to lat, "lng" to lng, "clientTime" to clientTime,
        ))
    }
}
