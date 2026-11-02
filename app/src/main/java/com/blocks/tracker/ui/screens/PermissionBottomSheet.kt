package com.blocks.tracker.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blocks.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionBottomSheet(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
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
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "সময়মতো মনে করিয়ে দিতে notification চালু করো",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp
                ),
                color = InkPrimary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    onRequestPermission()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "চালু করো",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                    color = ColorWhite
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "পরে",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
    }
}
