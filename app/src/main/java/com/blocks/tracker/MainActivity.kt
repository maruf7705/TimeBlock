package com.blocks.tracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import com.blocks.tracker.data.BlocksRepository
import com.blocks.tracker.notification.CheckInNotificationHelper
import com.blocks.tracker.notification.CheckInScheduler
import com.blocks.tracker.ui.screens.*
import com.blocks.tracker.ui.theme.BlocksTheme
import com.blocks.tracker.ui.viewmodel.BlocksViewModel

class MainActivity : ComponentActivity() {

    private val focusTitleRequestedState = mutableStateOf(false)
    private var activeViewModel: BlocksViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BlocksApplication
        val repository = BlocksRepository(app.database.entryDao())
        val viewModel = BlocksViewModel(repository, app.stateStore)
        activeViewModel = viewModel

        if (intent?.getBooleanExtra(CheckInNotificationHelper.EXTRA_FOCUS_TITLE, false) == true) {
            focusTitleRequestedState.value = true
        }

        setContent {
            BlocksTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val undoMsg by viewModel.undoMessage.collectAsState()

                LaunchedEffect(undoMsg) {
                    undoMsg?.let { msg ->
                        val result = snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = "Undo"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undo()
                        } else {
                            viewModel.clearUndoMessage()
                        }
                    }
                }

                // App Navigation State: Restored across process death and phone memory pressure
                val stateStore = app.stateStore
                var currentScreen by rememberSaveable { mutableStateOf(stateStore.currentScreen) }
                var previousScreen by rememberSaveable { mutableStateOf("TRACK") }
                var selectedDateForDayView by rememberSaveable { mutableStateOf(stateStore.selectedDayViewDate) }
                var showExportSheet by remember { mutableStateOf(false) }
                var showSettingsSheet by remember { mutableStateOf(false) }

                // 1. Permission request launcher (Android 13+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        stateStore.checkInNotificationsEnabled = true
                        CheckInScheduler.resetTimer(this@MainActivity)
                    }
                }

                // First time opening permission bottom sheet prompt
                var showPermissionSheet by remember {
                    mutableStateOf(!stateStore.hasSeenPermissionPrompt)
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.statusBars,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "TRACK" -> {
                                TrackScreen(
                                    viewModel = viewModel,
                                    onOpenCalendar = {
                                        previousScreen = "TRACK"
                                        currentScreen = "CALENDAR"
                                        stateStore.currentScreen = "CALENDAR"
                                    },
                                    onOpenYesterday = { yesterdayDate ->
                                        selectedDateForDayView = yesterdayDate
                                        stateStore.selectedDayViewDate = yesterdayDate
                                        previousScreen = "TRACK"
                                        currentScreen = "DAY_VIEW"
                                        stateStore.currentScreen = "DAY_VIEW"
                                    },
                                    onOpenSettings = {
                                        showSettingsSheet = true
                                    },
                                    focusTitleRequested = focusTitleRequestedState.value,
                                    onFocusTitleConsumed = {
                                        focusTitleRequestedState.value = false
                                    }
                                )
                            }
                            "CALENDAR" -> {
                                CalendarScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = "TRACK"
                                        stateStore.currentScreen = "TRACK"
                                    },
                                    onSelectDate = { date ->
                                        selectedDateForDayView = date
                                        stateStore.selectedDayViewDate = date
                                        previousScreen = "CALENDAR"
                                        currentScreen = "DAY_VIEW"
                                        stateStore.currentScreen = "DAY_VIEW"
                                    },
                                    onOpenExport = { showExportSheet = true }
                                )
                            }
                            "DAY_VIEW" -> {
                                DayViewScreen(
                                    date = selectedDateForDayView,
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = previousScreen
                                        stateStore.currentScreen = previousScreen
                                    }
                                )
                            }
                        }

                        if (showExportSheet) {
                            ExportBottomSheet(
                                viewModel = viewModel,
                                onDismiss = { showExportSheet = false }
                            )
                        }

                        if (showSettingsSheet) {
                            SettingsBottomSheet(
                                stateStore = stateStore,
                                onDismiss = { showSettingsSheet = false }
                            )
                        }

                        if (showPermissionSheet) {
                            PermissionBottomSheet(
                                onDismiss = {
                                    stateStore.hasSeenPermissionPrompt = true
                                    showPermissionSheet = false
                                },
                                onRequestPermission = {
                                    stateStore.hasSeenPermissionPrompt = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }

                        // Next-day unlogged gap recovery modal
                        val pendingGap by viewModel.pendingGap.collectAsState()
                        pendingGap?.let { gap ->
                            YesterdayGapBottomSheet(
                                gapInfo = gap,
                                onSkip = {
                                    viewModel.dismissPendingGap(autoFillAsSleep = false)
                                },
                                onSaveBlock = { title ->
                                    viewModel.savePendingGapWithTitle(title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(CheckInNotificationHelper.EXTRA_FOCUS_TITLE, false)) {
            focusTitleRequestedState.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Check day transition when app resumes/wakes up
        activeViewModel?.checkAndHandleDayTransition()
        // Reset timer on app open/resume: cancels pending reminders and starts fresh 45m timer
        CheckInScheduler.resetTimer(this)
    }
}
