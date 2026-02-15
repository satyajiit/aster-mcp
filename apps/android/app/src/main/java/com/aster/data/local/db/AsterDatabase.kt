package com.aster.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ToolCallLog::class],
    version = 1,
    exportSchema = false
)
abstract class AsterDatabase : RoomDatabase() {
    abstract fun toolCallLogDao(): ToolCallLogDao
}
