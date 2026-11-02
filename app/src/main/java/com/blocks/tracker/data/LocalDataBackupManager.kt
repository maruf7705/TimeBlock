package com.blocks.tracker.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalDataBackupManager {

    /**
     * Creates an internal atomic JSON snapshot of all entries in app's internal filesDir.
     * Guaranteed safe across app updates and process deaths.
     */
    suspend fun createInternalSnapshot(context: Context, repository: BlocksRepository) {
        try {
            val allEntries = repository.getAllEntriesSync()
            val jsonArray = JSONArray()
            for (entry in allEntries) {
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("date", entry.date)
                    put("startMin", entry.startMin)
                    put("endMin", entry.endMin)
                    put("title", entry.title)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                }
                jsonArray.put(obj)
            }

            val file = File(context.filesDir, "focus_local_backup.json")
            val tempFile = File(context.filesDir, "focus_local_backup.json.tmp")
            FileOutputStream(tempFile).use { out ->
                out.write(jsonArray.toString(2).toByteArray(Charsets.UTF_8))
                out.flush()
            }
            tempFile.renameTo(file)
        } catch (e: Exception) {
            // Fail-safe
        }
    }

    /**
     * Exports full database backup JSON to user-selected Uri via Storage Access Framework (SAF).
     * The user can save this to Phone Storage, Downloads, Google Drive, SD Card, etc.
     */
    suspend fun exportToUri(context: Context, uri: Uri, repository: BlocksRepository): Boolean {
        return try {
            val allEntries = repository.getAllEntriesSync()
            val root = JSONObject().apply {
                put("app", "Focus")
                put("version", "1.0.0")
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("totalEntries", allEntries.size)

                val entriesArray = JSONArray()
                for (entry in allEntries) {
                    val obj = JSONObject().apply {
                        put("id", entry.id)
                        put("date", entry.date)
                        put("startMin", entry.startMin)
                        put("endMin", entry.endMin)
                        put("formattedRange", entry.formattedRange)
                        put("formattedDuration", entry.formattedDuration)
                        put("title", entry.title)
                        put("createdAt", entry.createdAt)
                        put("updatedAt", entry.updatedAt)
                    }
                    entriesArray.put(obj)
                }
                put("entries", entriesArray)
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restores entries from a user-selected JSON backup file without losing existing records.
     * Uses REPLACE or conflict resolution so existing entries are merged cleanly.
     */
    suspend fun restoreFromUri(context: Context, uri: Uri, repository: BlocksRepository): Int {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return 0

            val entriesToInsert = mutableListOf<EntryEntity>()
            if (jsonString.trim().startsWith("[")) {
                // Direct array format
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    entriesToInsert.add(parseEntry(obj))
                }
            } else {
                // Root object with "entries" key
                val root = JSONObject(jsonString)
                val array = root.optJSONArray("entries") ?: JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    entriesToInsert.add(parseEntry(obj))
                }
            }

            if (entriesToInsert.isNotEmpty()) {
                val dao = BlocksDatabase.getDatabase(context).entryDao()
                dao.insertEntries(entriesToInsert)
            }
            entriesToInsert.size
        } catch (e: Exception) {
            0
        }
    }

    private fun parseEntry(obj: JSONObject): EntryEntity {
        return EntryEntity(
            id = obj.getString("id"),
            date = obj.getString("date"),
            startMin = obj.getInt("startMin"),
            endMin = obj.getInt("endMin"),
            title = obj.optString("title", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}
