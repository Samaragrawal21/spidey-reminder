package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.TaskEntity
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetadataGrey
import com.example.ui.theme.SpideyCrimson
import com.example.ui.theme.SubtleBorderGrey
import com.example.ui.theme.WebWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class DateQuickChipMode {
    TODAY,
    TOMORROW,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreatorDialog(
    existingTask: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, scheduledTimeMillis: Long) -> Unit
) {
    val nowCal = remember { Calendar.getInstance() }
    val tomorrowCal = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    }

    val initialCal = remember(existingTask) {
        Calendar.getInstance().apply {
            if (existingTask != null) {
                timeInMillis = existingTask.scheduledTimeMillis
            } else {
                // Default time input +10m while keeping "Today" chip selected by default
                val plusTen = Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }
                set(Calendar.HOUR_OF_DAY, plusTen.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, plusTen.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    val initialMode = remember(existingTask) {
        if (existingTask == null) {
            DateQuickChipMode.TODAY
        } else {
            val taskCal = Calendar.getInstance().apply { timeInMillis = existingTask.scheduledTimeMillis }
            val isToday = taskCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                taskCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
            val isTomorrow = taskCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                taskCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
            when {
                isToday -> DateQuickChipMode.TODAY
                isTomorrow -> DateQuickChipMode.TOMORROW
                else -> DateQuickChipMode.CUSTOM
            }
        }
    }

    var selectedChipMode by remember(existingTask) { mutableStateOf(initialMode) }
    var selectedYear by remember(existingTask) { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember(existingTask) { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedDayOfMonth by remember(existingTask) { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    val initialHour12 = remember(initialCal) {
        val h = initialCal.get(Calendar.HOUR)
        if (h == 0) 12 else h
    }
    val initialMinute = remember(initialCal) { initialCal.get(Calendar.MINUTE) }
    val initialIsPm = remember(initialCal) { initialCal.get(Calendar.AM_PM) == Calendar.PM }

    var titleText by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var hourText by remember(existingTask) { mutableStateOf(String.format(Locale.US, "%02d", initialHour12)) }
    var minuteText by remember(existingTask) { mutableStateOf(String.format(Locale.US, "%02d", initialMinute)) }
    var isPm by remember(existingTask) { mutableStateOf(initialIsPm) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedHour = hourText.toIntOrNull()
    val parsedMinute = minuteText.toIntOrNull()
    val isTimeFormatValid = parsedHour != null && parsedHour in 1..12 &&
        parsedMinute != null && parsedMinute in 0..59

    val computedTargetMillis: Long? = remember(
        selectedYear,
        selectedMonth,
        selectedDayOfMonth,
        parsedHour,
        parsedMinute,
        isPm
    ) {
        if (isTimeFormatValid && parsedHour != null && parsedMinute != null) {
            TaskViewModel.computeScheduledTimeMillis(
                year = selectedYear,
                month = selectedMonth,
                dayOfMonth = selectedDayOfMonth,
                hour12 = parsedHour,
                minute = parsedMinute,
                isPm = isPm
            )
        } else {
            null
        }
    }

    val isPastSelection = computedTargetMillis != null && computedTargetMillis <= System.currentTimeMillis()
    val pastErrorString = stringResource(id = R.string.error_schedule_in_past)

    val customDateChipLabel = remember(selectedChipMode, selectedYear, selectedMonth, selectedDayOfMonth) {
        if (selectedChipMode == DateQuickChipMode.CUSTOM) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
            }
            SimpleDateFormat("MMM d", Locale.US).format(cal.time)
        } else {
            "Pick Date"
        }
    }

    val schedulePreviewText = remember(computedTargetMillis) {
        if (computedTargetMillis != null) {
            val formattedLabel = TaskViewModel.formatTaskCardScheduleLabel(computedTargetMillis)
            "Schedules for $formattedLabel"
        } else {
            "Enter valid 12-hour time (Hour 01–12, Minute 00–59)"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, SpideyCrimson, RoundedCornerShape(16.dp))
                .testTag("task_creator_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSlate)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(
                        id = if (existingTask == null) R.string.dialog_title_create else R.string.dialog_title_edit
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = WebWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Alphanumeric Task Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { input ->
                        titleText = input.filter { ch ->
                            ch.isLetterOrDigit() || ch.isWhitespace() || ch in ".,!?:;-'\"()/&#+"
                        }
                        validationError = null
                    },
                    label = { Text(stringResource(id = R.string.label_task_title)) },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.placeholder_task_title),
                            color = MetadataGrey.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = false,
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpideyCrimson,
                        unfocusedBorderColor = SubtleBorderGrey,
                        focusedLabelColor = SpideyCrimson,
                        unfocusedLabelColor = MetadataGrey,
                        focusedTextColor = WebWhite,
                        unfocusedTextColor = WebWhite,
                        cursorColor = SpideyCrimson
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selector Section (Quick Chips Pattern)
                Text(
                    text = stringResource(id = R.string.label_date_selector),
                    style = MaterialTheme.typography.labelMedium,
                    color = MetadataGrey
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip 1: Today (Selected by default)
                    DateQuickChip(
                        label = stringResource(id = R.string.chip_today),
                        selected = selectedChipMode == DateQuickChipMode.TODAY,
                        onClick = {
                            val today = Calendar.getInstance()
                            selectedChipMode = DateQuickChipMode.TODAY
                            selectedYear = today.get(Calendar.YEAR)
                            selectedMonth = today.get(Calendar.MONTH)
                            selectedDayOfMonth = today.get(Calendar.DAY_OF_MONTH)
                            validationError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("date_chip_today")
                    )

                    // Chip 2: Tomorrow
                    DateQuickChip(
                        label = stringResource(id = R.string.chip_tomorrow),
                        selected = selectedChipMode == DateQuickChipMode.TOMORROW,
                        onClick = {
                            val tomorrow = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            selectedChipMode = DateQuickChipMode.TOMORROW
                            selectedYear = tomorrow.get(Calendar.YEAR)
                            selectedMonth = tomorrow.get(Calendar.MONTH)
                            selectedDayOfMonth = tomorrow.get(Calendar.DAY_OF_MONTH)
                            validationError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("date_chip_tomorrow")
                    )

                    // Chip 3: Pick Date (shows e.g. "Oct 25" when selected)
                    DateQuickChip(
                        label = customDateChipLabel,
                        selected = selectedChipMode == DateQuickChipMode.CUSTOM,
                        showCalendarIcon = true,
                        onClick = {
                            showDatePickerModal = true
                            validationError = null
                        },
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("date_chip_pick_date")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MANUAL TIME ENTRY (12-HOUR)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MetadataGrey
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Numeric Time Entry: Hour + Minute + Segmented [AM | PM]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { newVal ->
                            hourText = newVal.filter { it.isDigit() }.take(2)
                            validationError = null
                        },
                        label = { Text("HH") },
                        placeholder = { Text("08", color = MetadataGrey.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpideyCrimson,
                            unfocusedBorderColor = SubtleBorderGrey,
                            focusedLabelColor = SpideyCrimson,
                            unfocusedLabelColor = MetadataGrey,
                            focusedTextColor = WebWhite,
                            unfocusedTextColor = WebWhite,
                            cursorColor = SpideyCrimson
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_hour_input")
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WebWhite
                    )

                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { newVal ->
                            minuteText = newVal.filter { it.isDigit() }.take(2)
                            validationError = null
                        },
                        label = { Text("MM") },
                        placeholder = { Text("30", color = MetadataGrey.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpideyCrimson,
                            unfocusedBorderColor = SubtleBorderGrey,
                            focusedLabelColor = SpideyCrimson,
                            unfocusedLabelColor = MetadataGrey,
                            focusedTextColor = WebWhite,
                            unfocusedTextColor = WebWhite,
                            cursorColor = SpideyCrimson
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_minute_input")
                    )

                    // Segmented Toggle Button for [AM | PM]
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MatteBlack)
                            .border(1.5.dp, SpideyCrimson, RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .width(48.dp)
                                .background(if (!isPm) SpideyCrimson else MatteBlack)
                                .clickable {
                                    isPm = false
                                    validationError = null
                                }
                                .testTag("toggle_am_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.period_am),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!isPm) WebWhite else MetadataGrey,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .width(48.dp)
                                .background(if (isPm) SpideyCrimson else MatteBlack)
                                .clickable {
                                    isPm = true
                                    validationError = null
                                }
                                .testTag("toggle_pm_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.period_pm),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isPm) WebWhite else MetadataGrey,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = ElevatedSlate,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = schedulePreviewText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetadataGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                val inlineError = validationError ?: if (isPastSelection) pastErrorString else null
                if (inlineError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = inlineError,
                        style = MaterialTheme.typography.labelMedium,
                        color = SpideyCrimson,
                        modifier = Modifier.testTag("task_creator_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_task_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SubtleBorderGrey)
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_cancel),
                            color = MetadataGrey
                        )
                    }

                    Button(
                        onClick = {
                            val cleanTitle = titleText.trim()
                            val h = hourText.toIntOrNull()
                            val m = minuteText.toIntOrNull()
                            when {
                                cleanTitle.isEmpty() -> {
                                    validationError = "Please enter a task title."
                                }
                                h == null || h !in 1..12 -> {
                                    validationError = "Hour must be between 01 and 12."
                                }
                                m == null || m !in 0..59 -> {
                                    validationError = "Minute must be between 00 and 59."
                                }
                                else -> {
                                    val targetMillis = TaskViewModel.computeScheduledTimeMillis(
                                        year = selectedYear,
                                        month = selectedMonth,
                                        dayOfMonth = selectedDayOfMonth,
                                        hour12 = h,
                                        minute = m,
                                        isPm = isPm
                                    )
                                    if (targetMillis <= System.currentTimeMillis()) {
                                        validationError = pastErrorString
                                    } else {
                                        onSave(cleanTitle, targetMillis)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("save_task_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpideyCrimson,
                            contentColor = WebWhite
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_save_task),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (showDatePickerModal) {
        val utcZone = remember { TimeZone.getTimeZone("UTC") }
        val todayStartUtcMillis = remember {
            val localNow = Calendar.getInstance()
            Calendar.getInstance(utcZone).apply {
                clear()
                set(
                    localNow.get(Calendar.YEAR),
                    localNow.get(Calendar.MONTH),
                    localNow.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0
                )
            }.timeInMillis
        }

        val initialSelectedUtcMillis = remember(selectedYear, selectedMonth, selectedDayOfMonth) {
            Calendar.getInstance(utcZone).apply {
                clear()
                set(selectedYear, selectedMonth, selectedDayOfMonth, 0, 0, 0)
            }.timeInMillis.coerceAtLeast(todayStartUtcMillis)
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= todayStartUtcMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                Button(
                    onClick = {
                        val pickedUtc = datePickerState.selectedDateMillis
                        if (pickedUtc != null) {
                            val pickedCal = Calendar.getInstance(utcZone).apply {
                                timeInMillis = pickedUtc
                            }
                            selectedYear = pickedCal.get(Calendar.YEAR)
                            selectedMonth = pickedCal.get(Calendar.MONTH)
                            selectedDayOfMonth = pickedCal.get(Calendar.DAY_OF_MONTH)
                            selectedChipMode = DateQuickChipMode.CUSTOM
                            validationError = null
                        }
                        showDatePickerModal = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpideyCrimson,
                        contentColor = WebWhite
                    ),
                    modifier = Modifier.testTag("confirm_date_picker_button")
                ) {
                    Text("Confirm Date")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePickerModal = false },
                    modifier = Modifier.testTag("dismiss_date_picker_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.button_cancel),
                        color = MetadataGrey
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = DarkSlate
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkSlate,
                    titleContentColor = MetadataGrey,
                    headlineContentColor = WebWhite,
                    weekdayContentColor = MetadataGrey,
                    subheadContentColor = WebWhite,
                    navigationContentColor = WebWhite,
                    yearContentColor = WebWhite,
                    currentYearContentColor = SpideyCrimson,
                    selectedYearContentColor = WebWhite,
                    selectedYearContainerColor = SpideyCrimson,
                    dayContentColor = WebWhite,
                    disabledDayContentColor = MetadataGrey.copy(alpha = 0.35f),
                    selectedDayContentColor = WebWhite,
                    selectedDayContainerColor = SpideyCrimson,
                    todayContentColor = SpideyCrimson,
                    todayDateBorderColor = SpideyCrimson
                )
            )
        }
    }
}

@Composable
private fun DateQuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCalendarIcon: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) SpideyCrimson else MatteBlack,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) WebWhite else SubtleBorderGrey
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showCalendarIcon) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (selected) WebWhite else SpideyCrimson,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) WebWhite else MetadataGrey,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
