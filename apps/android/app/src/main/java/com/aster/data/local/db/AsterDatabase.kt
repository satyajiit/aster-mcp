package com.aster.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ToolCallLog::class],
    version = 2,
    exportSchema = false
)
abstract class AsterDatabase : RoomDatabase() {
    abstract fun toolCallLogDao(): ToolCallLogDao

    companion object {
        /**
         * Screen Control /goal P7 — add the four nullable audit columns to the
         * tool-call log. Non-destructive: the audit log is the security record,
         * so existing rows are preserved (no fallbackToDestructiveMigration).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tool_call_logs ADD COLUMN target TEXT")
                db.execSQL("ALTER TABLE tool_call_logs ADD COLUMN resolvedBy TEXT")
                db.execSQL("ALTER TABLE tool_call_logs ADD COLUMN risk TEXT")
                db.execSQL("ALTER TABLE tool_call_logs ADD COLUMN approval TEXT")
            }
        }
    }
}
