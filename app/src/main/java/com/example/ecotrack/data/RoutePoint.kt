package com.example.ecotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "route_points")
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface RoutePointDao {
    @Insert
    suspend fun insertPoint(point: RoutePoint)

    @Query("SELECT * FROM route_points WHERE userId = :userId ORDER BY timestamp ASC")
    fun getPointsForUser(userId: String): Flow<List<RoutePoint>>

    @Query("DELETE FROM route_points WHERE userId = :userId")
    suspend fun deletePointsForUser(userId: String)

    @Query("DELETE FROM route_points WHERE timestamp < :threshold")
    suspend fun deleteOldPoints(threshold: Long)
}
