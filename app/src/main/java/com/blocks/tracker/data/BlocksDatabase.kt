package com.blocks.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EntryEntity::class], version = 1, exportSchema = false)
abstract class BlocksDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: BlocksDatabase? = null

        fun getDatabase(context: Context): BlocksDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlocksDatabase::class.java,
                    "blocks_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
