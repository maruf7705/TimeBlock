package com.blocks.tracker.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BlocksRepository(private val dao: EntryDao) {

    fun getEntriesForDate(date: String): Flow<List<EntryEntity>> =
        dao.getEntriesForDate(date)

    suspend fun getEntriesForDateSync(date: String): List<EntryEntity> =
        dao.getEntriesForDateSync(date)

    fun getAllEntries(): Flow<List<EntryEntity>> =
        dao.getAllEntries()

    suspend fun getAllEntriesSync(): List<EntryEntity> =
        dao.getAllEntriesSync()

    fun getUntitledCountForDate(date: String): Flow<Int> =
        dao.getUntitledCountForDate(date)

    suspend fun getUntitledEntries(): List<EntryEntity> =
        dao.getUntitledEntries()

    /**
     * Add entry with Gap auto-fill and Overlap auto-trim:
     * - Gap auto-fill: if from > last entry's end, the skipped interval is saved as an Untitled entry.
     * - Overlap auto-trim: if from < last entry's end, trim earlier entries and return trimmed for Undo.
     */
    suspend fun addEntry(
        date: String,
        fromMin: Int,
        toMin: Int,
        title: String
    ): List<EntryEntity> {
        val existing = dao.getEntriesForDateSync(date).sortedBy { it.startMin }
        val toInsert = mutableListOf<EntryEntity>()
        val toDelete = mutableListOf<String>()

        if (existing.isEmpty()) {
            if (fromMin > 0) {
                // Gap from 00:00 to fromMin
                toInsert.add(
                    EntryEntity(
                        id = UUID.randomUUID().toString(),
                        date = date,
                        startMin = 0,
                        endMin = fromMin,
                        title = "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        } else {
            val lastEntry = existing.last()
            if (fromMin > lastEntry.endMin) {
                // Gap auto-fill
                toInsert.add(
                    EntryEntity(
                        id = UUID.randomUUID().toString(),
                        date = date,
                        startMin = lastEntry.endMin,
                        endMin = fromMin,
                        title = "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else if (fromMin < lastEntry.endMin) {
                // Overlap auto-trim: trim or remove earlier entries
                for (e in existing) {
                    if (e.endMin <= fromMin) {
                        // stays intact
                    } else if (e.startMin < fromMin) {
                        // partly overlaps, trim its endMin
                        toInsert.add(e.copy(endMin = fromMin, updatedAt = System.currentTimeMillis()))
                    } else {
                        // fully covered, delete
                        toDelete.add(e.id)
                    }
                }
            }
        }

        // Add the new block
        val newBlock = EntryEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            startMin = fromMin,
            endMin = toMin,
            title = title.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        toInsert.add(newBlock)

        if (toDelete.isNotEmpty()) {
            dao.deleteByIds(toDelete)
        }
        dao.insertEntries(toInsert)
        return existing
    }

    /**
     * Day auto-close: entries never cross midnight.
     * If a day's last entry ends before 24:00 (1440), save the rest as an Untitled entry up to 24:00 (23:59).
     */
    suspend fun runDayAutoClose(date: String) {
        val entries = dao.getEntriesForDateSync(date).sortedBy { it.startMin }
        if (entries.isNotEmpty()) {
            val last = entries.last()
            if (last.endMin < 1440) {
                dao.insertEntry(
                    EntryEntity(
                        id = UUID.randomUUID().toString(),
                        date = date,
                        startMin = last.endMin,
                        endMin = 1440,
                        title = "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun updateEntryEdges(
        date: String,
        target: EntryEntity,
        newStart: Int,
        newEnd: Int,
        newTitle: String
    ) {
        val entries = dao.getEntriesForDateSync(date).sortedBy { it.startMin }.toMutableList()
        val index = entries.indexOfFirst { it.id == target.id }
        if (index == -1) {
            dao.updateEntry(target.copy(startMin = newStart, endMin = newEnd, title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
            return
        }

        val updatedTarget = target.copy(
            startMin = newStart,
            endMin = newEnd,
            title = newTitle.trim(),
            updatedAt = System.currentTimeMillis()
        )
        entries[index] = updatedTarget

        // Adjust previous neighbor's endMin
        if (index > 0) {
            val prev = entries[index - 1]
            entries[index - 1] = prev.copy(endMin = newStart, updatedAt = System.currentTimeMillis())
        }

        // Adjust next neighbor's startMin
        if (index < entries.size - 1) {
            val next = entries[index + 1]
            entries[index + 1] = next.copy(startMin = newEnd, updatedAt = System.currentTimeMillis())
        }

        dao.insertEntries(entries)
    }

    suspend fun splitEntry(entry: EntryEntity, splitMin: Int) {
        if (splitMin <= entry.startMin || splitMin >= entry.endMin) return

        val first = entry.copy(
            endMin = splitMin,
            updatedAt = System.currentTimeMillis()
        )
        val second = EntryEntity(
            id = UUID.randomUUID().toString(),
            date = entry.date,
            startMin = splitMin,
            endMin = entry.endMin,
            title = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertEntries(listOf(first, second))
    }

    suspend fun deleteEntry(entry: EntryEntity) {
        dao.deleteEntry(entry)
    }

    suspend fun restoreEntries(entries: List<EntryEntity>, date: String) {
        val current = dao.getEntriesForDateSync(date)
        dao.deleteByIds(current.map { it.id })
        dao.insertEntries(entries)
    }

    suspend fun insertDirectEntry(entry: EntryEntity) {
        dao.insertEntry(entry)
    }

    suspend fun updateTitle(entryId: String, newTitle: String) {
        val all = dao.getAllEntriesSync()
        all.find { it.id == entryId }?.let {
            dao.updateEntry(it.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
        }
    }
}
