package com.example.ecotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "local_users")
data class LocalUser(
    @PrimaryKey val firebaseUid: String,
    val email: String,
    val passwordHash: String // Hashed locally for offline verification
)

@Dao
interface LocalUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: LocalUser)

    @Query("SELECT * FROM local_users WHERE email = :email LIMIT 1")
    suspend fun getLocalUser(email: String): LocalUser?
}
