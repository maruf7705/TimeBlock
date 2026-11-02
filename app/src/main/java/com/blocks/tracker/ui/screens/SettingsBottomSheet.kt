package com.blocks.tracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.blocks.tracker.data.BlocksDatabase
import com.blocks.tracker.data.BlocksRepository
import com.blocks.tracker.data.FocusStateStore
import com.blocks.tracker.data.LocalDataBackupManager
import com.blocks.tracker.notification.CheckInScheduler
import com.blocks.tracker.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    stateStore: FocusStateStore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(stateStore.checkInNotificationsEnabled) }
    var isAlarmStreamEnabled by remember { mutableStateOf(stateStore.useAlarmStream) }
    var isDebugMode by remember { mutableStateOf(stateStore.checkInDebugMode) }

    // SAF Document Launchers for export and import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = BlocksRepository(BlocksDatabase.getDatabase(context).entryDao())
                val success = LocalDataBackupManager.exportToUri(context, it, repo)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = BlocksRepository(BlocksDatabase.getDatabase(context).entryDao())
                val count = LocalDataBackupManager.restoreFromUri(context, it, repo)
                withContext(Dispatchers.Main) {
                    if (count > 0) {
                        Toast.makeText(context, "Restored $count entries successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No entries restored", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Check battery optimization restriction
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val isBatteryRestricted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            false
        }
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
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = InkPrimary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 1. Notification Toggle (No extra clutter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = InkPrimary
                )

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                            if (!systemEnabled) {
                                // Open phone's app notification settings
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                } else {
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            } else {
                                isEnabled = true
                                stateStore.checkInNotificationsEnabled = true
                                CheckInScheduler.resetTimer(context)
                            }
                        } else {
                            isEnabled = false
                            stateStore.checkInNotificationsEnabled = false
                            CheckInScheduler.cancelAllReminders(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorWhite,
                        checkedTrackColor = AccentIndigo,
                        uncheckedThumbColor = InkSecondary,
                        uncheckedTrackColor = PaperDivider
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Alarm stream priority alerts toggle (enabled by default)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Alarm stream alerts",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = InkPrimary
                    )
                    Text(
                        text = "Vibrates during calls & uses alarm volume",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = InkSecondary
                    )
                }

                Switch(
                    checked = isAlarmStreamEnabled,
                    onCheckedChange = { checked ->
                        isAlarmStreamEnabled = checked
                        stateStore.useAlarmStream = checked
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorWhite,
                        checkedTrackColor = AccentIndigo,
                        uncheckedThumbColor = InkSecondary,
                        uncheckedTrackColor = PaperDivider
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Localhost Permanent Backup & Restore Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(PaperContainer, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Localhost Backup & Restore",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "All data is securely saved in local SQLite. You can export a snapshot to phone storage or import anytime.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("Focus_Backup_${System.currentTimeMillis()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Export Backup", color = ColorWhite, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Restore", color = InkPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Battery optimization minimal tip (if phone restricts background execution)
            if (isBatteryRestricted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(PaperContainer, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Battery saver may delay reminders",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = InkSecondary
                            )
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            // safe
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Allow",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = ColorWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Debug option for testing (1 min first, 20 sec follow-up)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Debug test mode",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = InkPrimary
                    )
                    Text(
                        text = "1m first, 20s reminder",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = InkSecondary
                    )
                }

                Switch(
                    checked = isDebugMode,
                    onCheckedChange = { checked ->
                        isDebugMode = checked
                        stateStore.checkInDebugMode = checked
                        CheckInScheduler.resetTimer(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorWhite,
                        checkedTrackColor = AccentIndigo,
                        uncheckedThumbColor = InkSecondary,
                        uncheckedTrackColor = PaperDivider
                    )
                )
            }
        }
    }
}
