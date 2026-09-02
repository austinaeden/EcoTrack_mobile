package com.example.ecotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Index
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "activity_logs",
    indices = [Index(value = ["userId"])]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // Firebase UID
    val title: String,
    val stepCount: Int,
    val temperature: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insertLog(log: ActivityLog)

    @Delete
    suspend fun deleteLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllLogs(userId: String): Flow<List<ActivityLog>>
}
