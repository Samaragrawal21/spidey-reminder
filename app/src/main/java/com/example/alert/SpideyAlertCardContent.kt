package com.example.alert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.TaskEntity
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetadataGrey
import com.example.ui.theme.SpideyCrimson
import com.example.ui.theme.StatusCompletedGreen
import com.example.ui.theme.StatusSnoozedOrange
import com.example.ui.theme.WebWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpideyAlertCardContent(
    task: TaskEntity,
    queuedCount: Int,
    onDoneClick: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var customSnoozeInput by remember(task.id) { mutableStateOf("") }
    var customSnoozeError by remember(task.id) { mutableStateOf(false) }

    val formattedTime = remember(task.scheduledTimeMillis) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(task.scheduledTimeMillis))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Top Hanging Spider-Man Graphic (spiderman.png)
        Image(
            painter = painterResource(id = R.drawable.spiderman),
            contentDescription = stringResource(id = R.string.cd_hanging_spiderman),
            modifier = Modifier
                .size(width = 150.dp, height = 195.dp)
                .testTag("alert_spiderman_graphic"),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Alert Card with bold 2dp Crimson Red border
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, SpideyCrimson, RoundedCornerShape(16.dp))
                .testTag("alert_task_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = SpideyCrimson.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SpideyCrimson)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = SpideyCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.alert_header_badge),
                                style = MaterialTheme.typography.labelLarge,
                                color = WebWhite
                            )
                        }
                    }

                    if (queuedCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = StatusSnoozedOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, StatusSnoozedOrange)
                        ) {
                            Text(
                                text = "+$queuedCount QUEUED",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusSnoozedOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scheduled time badge
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelLarge,
                    color = MetadataGrey
                )

                Spacer(modifier = Modifier.height(6.dp))

                // High-contrast Task Title Box with 2dp Crimson Red border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MatteBlack, RoundedCornerShape(12.dp))
                        .border(2.dp, SpideyCrimson, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        color = WebWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("alert_task_title")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Primary "Done / Complete" Button
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("alert_done_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCompletedGreen,
                        contentColor = MatteBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.button_done_complete),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "QUICK SNOOZE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetadataGrey
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Fixed Snooze Chips: +15m, +30m, +1h, +2h
                val snoozePresets = listOf(
                    15 to stringResource(id = R.string.snooze_15m),
                    30 to stringResource(id = R.string.snooze_30m),
                    60 to stringResource(id = R.string.snooze_1h),
                    120 to stringResource(id = R.string.snooze_2h)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    snoozePresets.forEach { (mins, label) ->
                        OutlinedButton(
                            onClick = { onSnoozeMinutes(mins) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("snooze_chip_${mins}m"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, SpideyCrimson),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ElevatedSlate,
                                contentColor = WebWhite
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SpideyCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = WebWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Custom Snooze Input Field + Confirm Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSnoozeInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }.take(4)
                            customSnoozeInput = filtered
                            customSnoozeError = false
                        },
                        label = {
                            Text(
                                text = stringResource(id = R.string.label_custom_snooze),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        placeholder = {
                            Text(text = "45", color = MetadataGrey.copy(alpha = 0.6f))
                        },
                        isError = customSnoozeError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpideyCrimson,
                            unfocusedBorderColor = MetadataGrey.copy(alpha = 0.5f),
                            focusedLabelColor = SpideyCrimson,
                            unfocusedLabelColor = MetadataGrey,
                            focusedTextColor = WebWhite,
                            unfocusedTextColor = WebWhite,
                            cursorColor = SpideyCrimson
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_snooze_input")
                    )

                    Button(
                        onClick = {
                            val mins = customSnoozeInput.toIntOrNull()
                            if (mins != null && mins > 0) {
                                onSnoozeMinutes(mins)
                            } else {
                                customSnoozeError = true
                            }
                        },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("custom_snooze_confirm_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpideyCrimson,
                            contentColor = WebWhite
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_confirm_snooze),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
