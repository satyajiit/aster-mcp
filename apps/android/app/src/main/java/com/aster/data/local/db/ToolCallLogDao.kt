package com.aster.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolCallLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ToolCallLog)

    @Query("SELECT * FROM tool_call_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ToolCallLog>>

    @Query("SELECT * FROM tool_call_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<ToolCallLog>>

    @Query("SELECT COUNT(*) FROM tool_call_logs")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tool_call_logs WHERE success = 1")
    fun getSuccessCount(): Flow<Int>

    @Query("DELETE FROM tool_call_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM tool_call_logs")
    suspend fun clearAll()
}
