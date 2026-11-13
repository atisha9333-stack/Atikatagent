package com.atik.aiagent.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val appName: String,
    val senderDisplay: String,
    val outcome: String,
    val detailNote: String,
    val previewSnippet: String? // Null unless user explicitly enables preview storage in privacy settings
)

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestampMillis DESC LIMIT 200")
    fun observeRecentLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM activity_logs WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}

@Database(
    entities = [ActivityLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AtikDatabase : RoomDatabase() {
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AtikDatabase? = null

        fun getInstance(context: Context): AtikDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AtikDatabase::class.java,
                    "atik_ai_agent_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
