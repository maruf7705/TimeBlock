package com.blocks.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE date = :date ORDER BY startMin ASC")
    fun getEntriesForDate(date: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY startMin ASC")
    suspend fun getEntriesForDateSync(date: String): List<EntryEntity>

    @Query("SELECT * FROM entries ORDER BY date ASC, startMin ASC")
    fun getAllEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY date ASC, startMin ASC")
    suspend fun getAllEntriesSync(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE title = '' OR title IS NULL ORDER BY date DESC, startMin DESC")
    suspend fun getUntitledEntries(): List<EntryEntity>

    @Query("SELECT COUNT(*) FROM entries WHERE (title = '' OR title IS NULL) AND date = :date")
    fun getUntitledCountForDate(date: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity)

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
