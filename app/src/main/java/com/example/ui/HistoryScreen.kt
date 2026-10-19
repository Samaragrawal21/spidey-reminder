package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TaskEntity
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetadataGrey
import com.example.ui.theme.SpideyCrimson
import com.example.ui.theme.StatusCompletedBg
import com.example.ui.theme.StatusCompletedGreen
import com.example.ui.theme.StatusMissedBg
import com.example.ui.theme.StatusMissedRed
import com.example.ui.theme.StatusSnoozedBg
import com.example.ui.theme.StatusSnoozedOrange
import com.example.ui.theme.SubtleBorderGrey
import com.example.ui.theme.WebWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    tasks: List<TaskEntity>,
    selectedFilter: HistoryFilter,
    onSelectFilter: (HistoryFilter) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.header_history_tasks),
                    style = MaterialTheme.typography.displayLarge,
                    color = WebWhite,
                    modifier = Modifier.testTag("header_history_title")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Swipe left or tap the trash icon to remove individual records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetadataGrey
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Chips at top: "Last 1 Day", "Last 7 Days", "Last 30 Days", "Last 6 Months"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        val selected = filter == selectedFilter
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFilter(filter) },
                            label = {
                                Text(
                                    text = stringResource(id = filter.labelRes),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSlate,
                                labelColor = MetadataGrey,
                                selectedContainerColor = SpideyCrimson,
                                selectedLabelColor = WebWhite,
                                selectedLeadingIconColor = WebWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = SubtleBorderGrey,
                                selectedBorderColor = SpideyCrimson
                            ),
                            modifier = Modifier.testTag("history_filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    EmptyHistoryCard()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_tasks_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart ||
                                    value == SwipeToDismissBoxValue.StartToEnd
                                ) {
                                    onDeleteTask(task.id)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            SpideyCrimson.copy(alpha = 0.3f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(id = R.string.cd_delete_task),
                                        tint = WebWhite
                                    )
                                }
                            }
                        ) {
                            HistoryTaskCard(
                                task = task,
                                onDeleteClick = { onDeleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTaskCard(
    task: TaskEntity,
    onDeleteClick: () -> Unit
) {
    // Status tag colors:
    // COMPLETED -> Green border / badge
    // MISSED -> Red border / badge
    // SNOOZED -> Orange border / badge
    val statusColors: Triple<Color, Color, String> = when (task.status) {
        TaskEntity.STATUS_COMPLETED -> Triple(
            StatusCompletedGreen,
            StatusCompletedBg,
            stringResource(id = R.string.status_completed)
        )
        TaskEntity.STATUS_SNOOZED -> Triple(
            StatusSnoozedOrange,
            StatusSnoozedBg,
            stringResource(id = R.string.status_snoozed)
        )
        else -> Triple(
            StatusMissedRed,
            StatusMissedBg,
            stringResource(id = R.string.status_missed)
        )
    }

    val (borderColor, badgeBg, statusText) = statusColors

    val formattedSchedule = remember(task.scheduledTimeMillis) {
        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            .format(Date(task.scheduledTimeMillis))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("history_task_card_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = borderColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = formattedSchedule,
                        style = MaterialTheme.typography.labelMedium,
                        color = MetadataGrey
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WebWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Single-item delete button (strictly no bulk delete)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(ElevatedSlate, RoundedCornerShape(10.dp))
                    .testTag("delete_history_task_button_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.cd_delete_task),
                    tint = SpideyCrimson
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier
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
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = SpideyCrimson,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.empty_history_title),
                style = MaterialTheme.typography.titleLarge,
                color = WebWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.empty_history_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MetadataGrey,
                textAlign = TextAlign.Center
            )
        }
    }
}
