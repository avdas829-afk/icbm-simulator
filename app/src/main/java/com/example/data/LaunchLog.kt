package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "launch_logs")
data class LaunchLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetName: String,
    val targetX: Float,
    val targetY: Float,
    val hitX: Float,
    val hitY: Float,
    val distanceErrorMeters: Float,
    val status: String, // "DIRECT HIT", "NEAR MISS", "MISSED"
    val missileVelocityMach: Float,
    val apogeeAltitudeKm: Float
)

@Dao
interface LaunchLogDao {
    @Query("SELECT * FROM launch_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LaunchLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LaunchLog)

    @Query("DELETE FROM launch_logs")
    suspend fun clearLogs()
}

@Database(entities = [LaunchLog::class], version = 1, exportSchema = false)
abstract class LaunchDatabase : RoomDatabase() {
    abstract fun launchLogDao(): LaunchLogDao

    companion object {
        @Volatile
        private var INSTANCE: LaunchDatabase? = null

        fun getDatabase(context: Context): LaunchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LaunchDatabase::class.java,
                    "launch_simulator_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class LaunchRepository(private val launchLogDao: LaunchLogDao) {
    val allLogs: Flow<List<LaunchLog>> = launchLogDao.getAllLogs()

    suspend fun insert(log: LaunchLog) {
        launchLogDao.insertLog(log)
    }

    suspend fun clearAll() {
        launchLogDao.clearLogs()
    }
}
