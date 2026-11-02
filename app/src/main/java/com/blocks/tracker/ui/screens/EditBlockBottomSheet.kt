package com.blocks.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blocks.tracker.data.EntryEntity
import com.blocks.tracker.ui.theme.*
import com.blocks.tracker.ui.viewmodel.BlocksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBlockBottomSheet(
    entry: EntryEntity,
    viewModel: BlocksViewModel,
    onDismiss: () -> Unit
) {
    var title by remember(entry) { mutableStateOf(entry.title) }
    var startMin by remember(entry) { mutableStateOf(entry.startMin) }
    var endMin by remember(entry) { mutableStateOf(entry.endMin) }

    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaperSurfaceSheet,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(PaperDivider, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Block",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary
                )
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color(0xFFBA1A1A))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title (empty for Untitled)", color = InkSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = AccentIndigo,
                    unfocusedIndicatorColor = PaperDivider
                ),
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start & End adjustments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(PaperContainer, RoundedCornerShape(12.dp))
                        .clickable { showTimePickerForStart = true }
                        .padding(14.dp)
                ) {
                    Text(text = "from", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = EntryEntity.formatMin(startMin),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(PaperContainer, RoundedCornerShape(12.dp))
                        .clickable { showTimePickerForEnd = true }
                        .padding(14.dp)
                ) {
                    Text(text = "to", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = EntryEntity.formatMin(endMin),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Split Action Row
            val canSplit = (endMin - startMin) >= 2
            OutlinedButton(
                onClick = { showSplitDialog = true },
                enabled = canSplit,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Split Block at Midpoint", color = if (canSplit) AccentIndigo else InkSecondary)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Cancel", color = InkSecondary)
                }

                Button(
                    onClick = {
                        if (startMin < endMin) {
                            viewModel.updateEdges(entry, startMin, endMin, title)
                            onDismiss()
                        }
                    },
                    enabled = startMin < endMin,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Save", color = ColorWhite)
                }
            }
        }
    }

    if (showTimePickerForStart) {
        TimePickerBottomSheet(
            initialMinutes = startMin,
            label = "start time",
            onDismiss = { showTimePickerForStart = false },
            onTimeSelected = { newMin ->
                if (newMin < endMin) startMin = newMin
            }
        )
    }

    if (showTimePickerForEnd) {
        TimePickerBottomSheet(
            initialMinutes = endMin,
            label = "end time",
            onDismiss = { showTimePickerForEnd = false },
            onTimeSelected = { newMin ->
                if (newMin > startMin) endMin = newMin
            }
        )
    }

    if (showSplitDialog) {
        val midMin = (startMin + endMin) / 2
        AlertDialog(
            onDismissRequest = { showSplitDialog = false },
            title = { Text("Split Block", color = InkPrimary) },
            text = {
                Text(
                    "Split this block at ${EntryEntity.formatMin(midMin)} into two separate entries?",
                    color = InkSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.split(entry, midMin)
                        showSplitDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Split", color = AccentIndigo)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitDialog = false }) {
                    Text("Cancel", color = InkSecondary)
                }
            },
            containerColor = PaperSurfaceSheet
        )
    }
}
