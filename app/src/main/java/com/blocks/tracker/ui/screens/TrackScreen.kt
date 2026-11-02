package com.blocks.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocks.tracker.data.EntryEntity
import com.blocks.tracker.notification.CheckInScheduler
import com.blocks.tracker.ui.theme.*
import com.blocks.tracker.ui.viewmodel.BlocksViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrackScreen(
    viewModel: BlocksViewModel,
    onOpenCalendar: () -> Unit,
    onOpenYesterday: (String) -> Unit,
    onOpenSettings: () -> Unit,
    focusTitleRequested: Boolean = false,
    onFocusTitleConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val untitledCount by viewModel.untitledCount.collectAsState()
    val fromMin by viewModel.fromMin.collectAsState()
    val toMin by viewModel.toMin.collectAsState()
    val isLiveClock by viewModel.isLiveClock.collectAsState()
    val currentDateStr by viewModel.currentDate.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    var localTitle by rememberSaveable { mutableStateOf(viewModel.stateStore.draftTitle) }

    var showTimePickerForFrom by remember { mutableStateOf(false) }
    var showTimePickerForTo by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<EntryEntity?>(null) }
    var showUntitledWalkthrough by remember { mutableStateOf(false) }

    // When opened from notification with focus title request
    LaunchedEffect(focusTitleRequested) {
        if (focusTitleRequested) {
            delay(250)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // safe
            }
            onFocusTitleConsumed()
        }
    }

    // Format display date: e.g. "Wed, 14 Oct"
    val formattedDate = remember(currentDateStr) {
        try {
            val date = LocalDate.parse(currentDateStr)
            date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
        } catch (e: Exception) {
            currentDateStr
        }
    }

    val yesterdayDateStr = remember(currentDateStr) {
        try {
            LocalDate.parse(currentDateStr).minusDays(1).toString()
        } catch (e: Exception) {
            ""
        }
    }

    // Decoupled Layout: Timeline & Floating Composer are isolated
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperSurface)
    ) {
        // 1. Content & Timeline Layer (Full Screen with bottom padding)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = InkPrimary
                        )
                    }
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Open Calendar",
                            tint = InkPrimary
                        )
                    }
                }
            }

            // Sub Header: Yesterday & N untitled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹ Yesterday",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    modifier = Modifier.clickable {
                        if (yesterdayDateStr.isNotBlank()) {
                            onOpenYesterday(yesterdayDateStr)
                        } else {
                            onOpenCalendar()
                        }
                    }
                )
                if (untitledCount > 0) {
                    Text(
                        text = "$untitledCount untitled",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentIndigo,
                        modifier = Modifier.clickable { showUntitledWalkthrough = true }
                    )
                } else {
                    Text(
                        text = "0 untitled",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }

            // Timeline Entries (Zero layout thrashing during keyboard animation)
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start logging your day below.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(items = entries, key = { _, entry -> entry.id }) { index, entry ->
                        val alpha = if (entries.size <= 1) 1f else 0.35f + (index.toFloat() / (entries.size - 1)) * 0.65f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { this.alpha = alpha }
                                .clickable {
                                    focusManager.clearFocus()
                                    editingEntry = entry
                                },
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
        }

        // 2. Bottom Composer Layer (Pure Hardware GPU Inset Translation)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(PaperSurface)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp, top = 6.dp)
        ) {
            val isEnabled = fromMin < toMin

            // Dual Synchronous Time Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // From Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePickerForFrom = true }
                ) {
                    Text(text = "from", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                    Text(
                        text = EntryEntity.formatMin(fromMin),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                        color = InkPrimary
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(PaperDivider)
                )

                // To / Now Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .clickable { showTimePickerForTo = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "to", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                        if (isLiveClock) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "now", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(NowDotColor, CircleShape)
                                )
                            }
                        } else {
                            Text(
                                text = "Now",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentIndigo,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { viewModel.resumeLiveClock() }
                            )
                        }
                    }
                    Text(
                        text = EntryEntity.formatMin(toMin),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                        color = InkPrimary
                    )
                }
            }

            // Inline Input Field (Zero keystroke lag + Focus Requester support)
            TextField(
                value = localTitle,
                onValueChange = {
                    localTitle = it
                    viewModel.stateStore.draftTitle = it
                },
                placeholder = { Text("What are you doing?", color = InkSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = AccentIndigo,
                    unfocusedIndicatorColor = PaperDivider
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isEnabled) {
                            viewModel.activityTitle.value = localTitle
                            viewModel.addBlock()
                            localTitle = ""
                            viewModel.stateStore.clearDraft()
                            CheckInScheduler.resetTimer(context)
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(vertical = 8.dp)
            )

            // Add Button (Classic Full-Width Pill, 52dp)
            Button(
                onClick = {
                    viewModel.activityTitle.value = localTitle
                    viewModel.addBlock()
                    localTitle = ""
                    viewModel.stateStore.clearDraft()
                    CheckInScheduler.resetTimer(context)
                    focusManager.clearFocus()
                },
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentIndigo,
                    disabledContainerColor = AccentIndigo.copy(alpha = 0.35f)
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorWhite
                )
            }
        }
    }

    if (showTimePickerForFrom) {
        TimePickerBottomSheet(
            initialMinutes = fromMin,
            label = "from",
            onDismiss = { showTimePickerForFrom = false },
            onTimeSelected = {
                viewModel.fromMin.value = it
            }
        )
    }

    if (showTimePickerForTo) {
        TimePickerBottomSheet(
            initialMinutes = toMin,
            label = "to",
            onDismiss = { showTimePickerForTo = false },
            onTimeSelected = {
                viewModel.toMin.value = it
                viewModel.isLiveClock.value = false
            }
        )
    }

    // Modal when user clicks an existing block to edit
    editingEntry?.let { entry ->
        EditBlockBottomSheet(
            entry = entry,
            viewModel = viewModel,
            onDismiss = { editingEntry = null }
        )
    }

    // Modal for walking through untitled blocks
    if (showUntitledWalkthrough) {
        val untitledEntries = remember(entries) {
            entries.filter { it.title.isBlank() }
        }
        UntitledWalkthroughBottomSheet(
            untitledEntries = untitledEntries,
            viewModel = viewModel,
            onDismiss = { showUntitledWalkthrough = false }
        )
    }
}
