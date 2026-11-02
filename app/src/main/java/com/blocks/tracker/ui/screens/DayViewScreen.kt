package com.blocks.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.blocks.tracker.data.EntryEntity
import com.blocks.tracker.ui.theme.*
import com.blocks.tracker.ui.viewmodel.BlocksViewModel

@Composable
fun DayViewScreen(
    date: String,
    viewModel: BlocksViewModel,
    onBack: () -> Unit
) {
    val allEntries by viewModel.allEntries.collectAsState()
    val dayEntries = remember(allEntries, date) {
        allEntries.filter { it.date == date }.sortedBy { it.startMin }
    }

    var editingEntry by remember { mutableStateOf<EntryEntity?>(null) }

    val totalTrackedMinutes = dayEntries.sumOf { it.durationMinutes }
    val totalHours = totalTrackedMinutes / 60
    val totalMins = totalTrackedMinutes % 60
    val totalDisplay = "${totalHours}h ${totalMins}m"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperSurface)
            .padding(horizontal = 24.dp)
    ) {
        // Navigation Bar
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
            Text(text = date, style = MaterialTheme.typography.bodyLarge, color = InkPrimary)
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Editorial Total Tracked Metric
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)) {
            Text(text = "tracked", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            Text(
                text = totalDisplay,
                style = MaterialTheme.typography.displayLarge,
                color = InkPrimary
            )
        }

        // Entries
        if (dayEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No entries for this date.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(dayEntries) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingEntry = entry },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.formattedRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            modifier = Modifier.width(96.dp)
                        )
                        Text(
                            text = entry.displayTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = if (entry.title.isBlank()) FontStyle.Italic else FontStyle.Normal,
                            color = if (entry.title.isBlank()) InkSecondary else InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = entry.formattedDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }
            }
        }

        // Quiet Endmark Dot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(InkSecondary.copy(alpha = 0.4f), CircleShape)
            )
        }
    }

    editingEntry?.let { entry ->
        EditBlockBottomSheet(
            entry = entry,
            viewModel = viewModel,
            onDismiss = { editingEntry = null }
        )
    }
}
