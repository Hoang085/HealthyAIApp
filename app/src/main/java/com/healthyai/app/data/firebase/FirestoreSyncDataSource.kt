package com.healthyai.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.healthyai.app.domain.model.HabitLog
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Upload một HabitLog lên Firestore.
     * Cấu trúc: users/{uid}/habit_logs/{logId}
     */
    suspend fun uploadHabitLog(userId: String, log: HabitLog): Result<Unit> = runCatching {
        val data = mapOf(
            "habitId" to log.habitId,
            "habitName" to log.habitName,
            "category" to log.category.name,
            "value" to log.value,
            "unit" to log.unit,
            "targetValueAtTime" to log.targetValueAtTime,
            "loggedAt" to log.loggedAt,
            "note" to log.note
        )
        
        firestore.collection("users")
            .document(userId)
            .collection("habit_logs")
            .document(log.id.toString())
            .set(data)
            .await()
    }
}
