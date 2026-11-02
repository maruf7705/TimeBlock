package com.blocks.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocks.tracker.data.EntryEntity
import com.blocks.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YesterdayGapBottomSheet(
    gapInfo: PendingGapInfo,
    onSkip: () -> Unit,
    onSaveBlock: (title: String) -> Unit
) {
    var blockTitle by remember { mutableStateOf("") }
    val timeRangeStr = "${EntryEntity.formatMin(gapInfo.startMin)}–${EntryEntity.formatMin(gapInfo.endMin)}"
    val durationMin = (gapInfo.endMin - gapInfo.startMin).coerceAtLeast(0)
    val h = durationMin / 60
    val m = durationMin % 60
    val durationStr = when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }

    ModalBottomSheet(
        onDismissRequest = onSkip,
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
            Text(
                text = "Unlogged time from yesterday",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = InkPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Yesterday ended with unlogged time. Would you like to label this block or leave it as sleep / rest?",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = InkSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Time Capsule Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PaperContainer, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = gapInfo.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    Text(
                        text = timeRangeStr,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = InkPrimary
                    )
                }

                Text(
                    text = durationStr,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AccentIndigo
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            TextField(
                value = blockTitle,
                onValueChange = { blockTitle = it },
                placeholder = {
                    Text("Activity name (e.g., Sleep, Reading, Movies)", color = InkSecondary)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = AccentIndigo,
                    unfocusedIndicatorColor = PaperDivider
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Skip", color = InkSecondary, fontSize = 15.sp)
                }

                Button(
                    onClick = {
                        onSaveBlock(blockTitle)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = if (blockTitle.isBlank()) "Save as Sleep" else "Save Block",
                        color = ColorWhite,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
