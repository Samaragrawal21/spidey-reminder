package com.example.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.TaskEntity
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetadataGrey
import com.example.ui.theme.SpideyCrimson
import com.example.ui.theme.StatusCompletedGreen
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingBlue
import com.example.ui.theme.StatusSnoozedBg
import com.example.ui.theme.StatusSnoozedOrange
import com.example.ui.theme.SubtleBorderGrey
import com.example.ui.theme.WebWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActiveTasksScreen(
    tasks: List<TaskEntity>,
    onEditTask: (TaskEntity) -> Unit,
    onCompleteTaskEarly: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onTriggerTestAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Live ticker for countdown badges
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("active_tasks_list"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Upside-Down Hanging Spider-Man (spiderman.png) and Title "Active Tasks"
            item {
                SpideyHeroHeader(activeCount = tasks.size)
            }

            // Interactive Android 15 Permission Checklist & Test Alert Banner
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PermissionAndDiagnosticBanner(onTriggerTestAlert = onTriggerTestAlert)
                }
            }

            if (tasks.isEmpty()) {
                item {
                    EmptyActiveTasksCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(items = tasks, key = { it.id }) { task ->
                    ActiveTaskCard(
                        task = task,
                        currentMillis = currentMillis,
                        onEditClick = { onEditTask(task) },
                        onCompleteClick = { onCompleteTaskEarly(task) },
                        onDeleteClick = { onDeleteTask(task.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpideyHeroHeader(activeCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SpideyCrimson.copy(alpha = 0.22f),
                        MatteBlack
                    )
                )
            )
            .padding(bottom = 12.dp)
    ) {
        // Subtle Spider-Web Radial Linework
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            val topCenter = Offset(size.width * 0.80f, 0f)
            val webStroke = SpideyCrimson.copy(alpha = 0.16f)
            for (r in 1..4) {
                drawCircle(
                    color = webStroke,
                    radius = r * 48.dp.toPx(),
                    center = topCenter,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp)
            ) {
                Surface(
                    color = SpideyCrimson.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, SpideyCrimson)
                ) {
                    Text(
                        text = "$activeCount ARMED",
                        style = MaterialTheme.typography.labelMedium,
                        color = WebWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.header_active_tasks),
                    style = MaterialTheme.typography.displayLarge,
                    color = WebWhite,
                    modifier = Modifier.testTag("header_active_tasks_title")
                )
                Text(
                    text = stringResource(id = R.string.subtitle_active_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetadataGrey
                )
            }

            // Upside-down hanging Spider-Man icon descending from top edge
            Image(
                painter = painterResource(id = R.drawable.spiderman),
                contentDescription = stringResource(id = R.string.cd_hanging_spiderman),
                modifier = Modifier
                    .size(width = 112.dp, height = 152.dp)
                    .testTag("header_spiderman_image"),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionAndDiagnosticBanner(
    onTriggerTestAlert: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotifications by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasExactAlarms by remember { mutableStateOf(checkExactAlarmPermission(context)) }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifications = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifications = checkNotificationPermission(context)
                hasExactAlarms = checkExactAlarmPermission(context)
                hasOverlay = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SpideyCrimson.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .testTag("permission_diagnostic_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SpideyCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.permission_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = WebWhite
                    )
                }

                Button(
                    onClick = onTriggerTestAlert,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpideyCrimson,
                        contentColor = WebWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("test_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(id = R.string.button_test_alert),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionStatusChip(
                    label = "Notifications",
                    isGranted = hasNotifications,
                    onClick = {
                        if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                PermissionStatusChip(
                    label = "Exact Alarms",
                    isGranted = hasExactAlarms,
                    onClick = {
                        if (!hasExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                )

                PermissionStatusChip(
                    label = "Overlay Hijack",
                    isGranted = hasOverlay,
                    onClick = {
                        if (!hasOverlay) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusChip(
    label: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isGranted) StatusCompletedGreen else StatusSnoozedOrange
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.AccessTime,
            contentDescription = null,
            tint = borderColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = if (isGranted) "$label ✓" else "Grant $label",
            style = MaterialTheme.typography.labelMedium,
            color = WebWhite
        )
    }
}

@Composable
private fun ActiveTaskCard(
    task: TaskEntity,
    currentMillis: Long,
    onEditClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSnoozed = task.status == TaskEntity.STATUS_SNOOZED
    val borderColor = if (isSnoozed) StatusSnoozedOrange else SpideyCrimson
    val badgeBg = if (isSnoozed) StatusSnoozedBg else StatusPendingBg
    val badgeColor = if (isSnoozed) StatusSnoozedOrange else StatusPendingBlue

    val formattedScheduleLabel = remember(task.scheduledTimeMillis, currentMillis / 60_000L) {
        TaskViewModel.formatTaskCardScheduleLabel(task.scheduledTimeMillis, currentMillis)
    }

    val countdownText = remember(task.scheduledTimeMillis, currentMillis) {
        formatRelativeCountdown(task.scheduledTimeMillis, currentMillis)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("active_task_card_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 12-Hour AM/PM Scheduled Time Badge + Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ElevatedSlate,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = SpideyCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = formattedScheduleLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = WebWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Countdown Badge
                    Surface(
                        color = SpideyCrimson.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SpideyCrimson.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            color = WebWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                // Status Pill (PENDING or SNOOZED)
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        text = task.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = WebWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mark Done Early Button
                    IconButton(
                        onClick = onCompleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("complete_task_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(id = R.string.cd_complete_task),
                            tint = StatusCompletedGreen
                        )
                    }

                    // Edit Task Button
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_task_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.cd_edit_task),
                            tint = WebWhite
                        )
                    }

                    // Delete Individual Task Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_active_task_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.cd_delete_task),
                            tint = MetadataGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyActiveTasksCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorderGrey, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = SpideyCrimson,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.empty_active_title),
                style = MaterialTheme.typography.titleLarge,
                color = WebWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.empty_active_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MetadataGrey,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatRelativeCountdown(scheduledMillis: Long, nowMillis: Long): String {
    val diff = scheduledMillis - nowMillis
    if (diff <= 0L) return "Ringing now"
    val totalSeconds = diff / 1000L
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        days > 0L -> "in ${days}d ${hours}h"
        hours > 0L -> "in ${hours}h ${minutes}m"
        minutes > 0L -> "in ${minutes}m ${seconds}s"
        else -> "in ${seconds}s"
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}
