package com.blocks.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entry model matching specifications:
 * Entry(id, date, startMin, endMin, title, createdAt, updatedAt)
 * - date: 'yyyy-MM-dd'
 * - startMin / endMin: 0 to 1440
 * - empty title = 'Untitled'
 * - endMin 1440 is shown as 23:59
 */
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val startMin: Int,
    val endMin: Int,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    val displayTitle: String
        get() = if (title.isBlank()) "Untitled" else title

    val durationMinutes: Int
        get() = (endMin - startMin).coerceAtLeast(0)

    val formattedDuration: String
        get() {
            val h = durationMinutes / 60
            val m = durationMinutes % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }

    val formattedRange: String
        get() = "${formatMin(startMin)}–${formatMin(endMin)}"

    companion object {
        fun formatMin(min: Int): String {
            if (min >= 1440) return "23:59"
            val safe = min.coerceIn(0, 1440)
            val h = safe / 60
            val m = safe % 60
            return String.format("%02d:%02d", h, m)
        }
    }
}
