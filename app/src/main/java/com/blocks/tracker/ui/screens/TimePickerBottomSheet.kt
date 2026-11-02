package com.blocks.tracker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocks.tracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerBottomSheet(
    initialMinutes: Int,
    label: String,
    onDismiss: () -> Unit,
    onTimeSelected: (Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf((initialMinutes / 60).coerceIn(0, 23)) }
    var selectedMinute by remember { mutableStateOf((initialMinutes % 60).coerceIn(0, 59)) }

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
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Adjust $label",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Two Draggable / Scrollable Wheels (Hours & Minutes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Scrollable Wheel (0..23)
                ScrollableWheelColumn(
                    value = selectedHour,
                    range = 24,
                    onValueChange = { newHour ->
                        selectedHour = newHour
                        onTimeSelected(selectedHour * 60 + selectedMinute)
                    }
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
                    color = InkPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // Minute Scrollable Wheel (0..59, exact 1-min increments)
                ScrollableWheelColumn(
                    value = selectedMinute,
                    range = 60,
                    onValueChange = { newMin ->
                        selectedMinute = newMin
                        onTimeSelected(selectedHour * 60 + selectedMinute)
                    }
                )
            }

            // Quick minute adjustment shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val increments = listOf(-15, -5, 5, 15)
                for (delta in increments) {
                    OutlinedButton(
                        onClick = {
                            var total = selectedHour * 60 + selectedMinute + delta
                            if (total < 0) total = 0
                            if (total > 1440) total = 1440
                            selectedHour = total / 60
                            selectedMinute = total % 60
                            onTimeSelected(total)
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        val textStr = if (delta > 0) "+${delta}m" else "${delta}m"
                        Text(
                            text = textStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = InkPrimary
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onTimeSelected(selectedHour * 60 + selectedMinute)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(text = "Done", color = ColorWhite)
            }
        }
    }
}

/**
 * Ultra-Smooth Draggable & Interactive Wheel Column
 * - Allows dragging / swiping vertically to roll the numbers with momentum
 * - Tapping top or bottom increments/decrements by 1
 * - Numbers animate smoothly and wrap seamlessly
 */
@Composable
fun ScrollableWheelColumn(
    value: Int,
    range: Int,
    onValueChange: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val dragOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val stepPx = with(density) { 36.dp.toPx() }

    Box(
        modifier = Modifier
            .width(88.dp)
            .height(170.dp)
            .pointerInput(value, range) {
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            dragOffset.animateTo(
                                0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffset.animateTo(0f)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        coroutineScope.launch {
                            dragOffset.snapTo((accumulatedDrag % stepPx) * 0.4f)
                        }

                        // Dragging UP pulls numbers down (increases value)
                        while (accumulatedDrag <= -stepPx) {
                            accumulatedDrag += stepPx
                            val next = (value + 1) % range
                            onValueChange(next)
                        }

                        // Dragging DOWN pulls numbers up (decreases value)
                        while (accumulatedDrag >= stepPx) {
                            accumulatedDrag -= stepPx
                            val prev = (value - 1 + range) % range
                            onValueChange(prev)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffset.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Upper number (-1 step) - Tappable
            Text(
                text = String.format("%02d", (value - 1 + range) % range),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                color = InkPrimary.copy(alpha = 0.30f),
                modifier = Modifier
                    .clickable {
                        onValueChange((value - 1 + range) % range)
                    }
                    .padding(vertical = 2.dp)
            )

            // Center Selected number (Large & Bold)
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = InkPrimary,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Lower number (+1 step) - Tappable
            Text(
                text = String.format("%02d", (value + 1) % range),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                color = InkPrimary.copy(alpha = 0.30f),
                modifier = Modifier
                    .clickable {
                        onValueChange((value + 1) % range)
                    }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
