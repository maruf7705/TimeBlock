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
fun UntitledWalkthroughBottomSheet(
    untitledEntries: List<EntryEntity>,
    viewModel: BlocksViewModel,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var currentTitle by remember(currentIndex, untitledEntries) {
        mutableStateOf(if (untitledEntries.isNotEmpty() && currentIndex < untitledEntries.size) untitledEntries[currentIndex].title else "")
    }

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
                .padding(bottom = 36.dp)
        ) {
            if (untitledEntries.isEmpty() || currentIndex >= untitledEntries.size) {
                Text(
                    text = "All blocks are titled",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You have no untitled blocks left for today.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Done", color = ColorWhite)
                }
            } else {
                val entry = untitledEntries[currentIndex]
                val stepText = "Block ${currentIndex + 1} of ${untitledEntries.size}"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Name untitled block",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time info badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PaperContainer, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Time interval",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                        Text(
                            text = entry.formattedRange,
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                    }
                    Text(
                        text = entry.formattedDuration,
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextField(
                    value = currentTitle,
                    onValueChange = { currentTitle = it },
                    placeholder = { Text("What were you doing?", color = InkSecondary) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AccentIndigo,
                        unfocusedIndicatorColor = PaperDivider
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentIndex + 1 < untitledEntries.size) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Skip", color = InkSecondary)
                    }

                    Button(
                        onClick = {
                            if (currentTitle.isNotBlank()) {
                                viewModel.updateTitle(entry.id, currentTitle.trim())
                            }
                            if (currentIndex + 1 < untitledEntries.size) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Save", color = ColorWhite)
                    }
                }
            }
        }
    }
}
