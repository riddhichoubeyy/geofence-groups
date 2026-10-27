package com.example.geofencegroups.geofence

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.*
import com.example.geofencegroups.App
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.concurrent.TimeUnit

private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

object Work {
    fun enqueueReport(ctx: Context, groupId: String, type: String, loc: Location?) {
        val data = workDataOf(
            "groupId" to groupId, "type" to type, "hasLoc" to (loc != null),
            "lat" to (loc?.latitude ?: 0.0), "lng" to (loc?.longitude ?: 0.0),
            "t" to System.currentTimeMillis(),
        )
        val req = OneTimeWorkRequestBuilder<ReportEventWorker>()
            .setInputData(data).setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // Per-group chain keeps ENTER/EXIT in order even when offline.
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork("report-$groupId", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    fun enqueueSync(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "sync-now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncGeofencesWorker>().setConstraints(network).build(),
        )
    }

    fun schedulePeriodicSync(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "sync-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncGeofencesWorker>(6, TimeUnit.HOURS).setConstraints(network).build(),
        )
    }
}

class ReportEventWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as App).container
        if (c.firebaseAuth.currentUser == null) return Result.failure()
        val hasLoc = inputData.getBoolean("hasLoc", false)
        return try {
            c.groups.reportEvent(
                inputData.getString("groupId")!!, inputData.getString("type")!!,
                if (hasLoc) inputData.getDouble("lat", 0.0) else null,
                if (hasLoc) inputData.getDouble("lng", 0.0) else null,
                inputData.getLong("t", System.currentTimeMillis()),
            )
            Result.success()
        } catch (e: FirebaseFunctionsException) {
            Log.w("ReportEventWorker", "${e.code}: ${e.message}")
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED,
                FirebaseFunctionsException.Code.PERMISSION_DENIED,
                FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                FirebaseFunctionsException.Code.NOT_FOUND,
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> Result.failure()
                else -> if (runAttemptCount > 8) Result.failure() else Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount > 8) Result.failure() else Result.retry()
        }
    }
}

class SyncGeofencesWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as App).container
        val uid = c.firebaseAuth.currentUser?.uid ?: return Result.success()
        return try {
            runCatching { c.devices.registerCurrent() }
            c.geofences.sync(c.groups.fetchGroups(uid))
            Result.success()
        } catch (e: Exception) {
            Log.w("SyncGeofencesWorker", e)
            Result.retry()
        }
    }
}
