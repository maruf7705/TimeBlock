package com.blocks.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blocks.tracker.ui.theme.*
import com.blocks.tracker.ui.viewmodel.BlocksViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: BlocksViewModel,
    onBack: () -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenExport: () -> Unit
) {
    val allEntries by viewModel.allEntries.collectAsState()
    val entryDates = remember(allEntries) { allEntries.map { it.date }.toSet() }

    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now().toString() }

    val monthName = remember(currentYearMonth) {
        currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
    val yearStr = remember(currentYearMonth) {
        currentYearMonth.year.toString()
    }

    val daysInMonth = remember(currentYearMonth) {
        currentYearMonth.lengthOfMonth()
    }

    // Monday start: 1 (Mon) .. 7 (Sun) -> offset: 0 .. 6
    val firstDayOffset = remember(currentYearMonth) {
        val firstDay = currentYearMonth.atDay(1).dayOfWeek.value
        (firstDay - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperSurface)
            .padding(horizontal = 24.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = InkPrimary
                )
            }
            TextButton(onClick = onOpenExport) {
                Text(text = "Export", color = AccentIndigo)
            }
        }

        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = InkPrimary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = yearStr, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text(text = monthName, style = MaterialTheme.typography.titleLarge, color = InkPrimary)
            }
            IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = InkPrimary
                )
            }
        }

        // Days of week (Monday start)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(text = day, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }

        // Grid of dates with proper offset
        val totalCells = firstDayOffset + daysInMonth
        val weeks = (0 until totalCells).chunked(7)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { cellIndex ->
                        val dayNumber = cellIndex - firstDayOffset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val dateString = String.format("%04d-%02d-%02d", currentYearMonth.year, currentYearMonth.monthValue, dayNumber)
                            val hasEntry = entryDates.contains(dateString)
                            val isToday = dateString == today

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onSelectDate(dateString) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isToday) AccentIndigo else InkPrimary
                                )
                                if (hasEntry) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .size(4.dp)
                                            .background(InkSecondary, CircleShape)
                                    )
                                }
                            }
                        } else {
                            // Blank filler box for alignment
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                    // Fill remaining week spaces if last week has < 7 cells
                    if (week.size < 7) {
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}
