package com.example.ecotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_steps")
data class DailyStep(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val userId: String, // Firebase UID
    val steps: Int
)

@Dao
interface DailyStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(dailyStep: DailyStep)

    @Query("SELECT * FROM daily_steps WHERE userId = :userId ORDER BY date DESC")
    fun getDailyStepsForUser(userId: String): Flow<List<DailyStep>>

    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getStepsForDate(userId: String, date: String): DailyStep?
}
