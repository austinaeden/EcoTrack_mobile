package com.example.ecotrack.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Uploads all local data for the given user to Firestore.
     * This is intended to be called during the midnight reset.
     */
    suspend fun syncDataToCloud(userId: String) {
        if (userId.isEmpty()) return

        try {
            // 1. Sync Daily Steps History
            val dailySteps = db.dailyStepDao().getDailyStepsForUser(userId).first()
            dailySteps.forEach { step ->
                firestore.collection("users")
                    .document(userId)
                    .collection("daily_history")
                    .document(step.date)
                    .set(step, SetOptions.merge())
                    .await()
            }

            // 2. Sync Activity Logs
            val logs = db.activityLogDao().getAllLogs(userId).first()
            logs.forEach { log ->
                firestore.collection("users")
                    .document(userId)
                    .collection("activity_logs")
                    .document(log.id.toString())
                    .set(log, SetOptions.merge())
                    .await()
            }

            Log.d("FirebaseSync", "Cloud sync successful for user: $userId")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing to cloud: ${e.localizedMessage}")
        }
    }
}
