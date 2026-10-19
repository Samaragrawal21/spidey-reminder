package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alert.AlertQueueManager
import com.example.alert.AudioAssetManager
import com.example.data.TaskEntity
import com.example.ui.ActiveTasksScreen
import com.example.ui.HistoryScreen
import com.example.ui.TaskCreatorDialog
import com.example.ui.TaskViewModel
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetadataGrey
import com.example.ui.theme.SpideyCrimson
import com.example.ui.theme.SpideyReminderTheme
import com.example.ui.theme.WebWhite

enum class SpideyTab {
    ACTIVE,
    HISTORY
}

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertQueueManager.ensureNotificationChannels(applicationContext)
        AudioAssetManager.ensureAlertSoundDownloadedAsync(applicationContext)
        enableEdgeToEdge()

        setContent {
            SpideyReminderTheme {
                SpideyReminderApp(viewModel = taskViewModel)
            }
        }
    }
}

@Composable
fun SpideyReminderApp(viewModel: TaskViewModel) {
    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val historyTasks by viewModel.historyTasks.collectAsStateWithLifecycle()
    val selectedHistoryFilter by viewModel.selectedHistoryFilter.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(SpideyTab.ACTIVE) }
    var isCreatorDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    // BackHandler on secondary tab to return to Active Tasks home tab
    BackHandler(enabled = currentTab != SpideyTab.ACTIVE) {
        currentTab = SpideyTab.ACTIVE
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MatteBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            if (currentTab == SpideyTab.ACTIVE) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        isCreatorDialogOpen = true
                    },
                    containerColor = SpideyCrimson,
                    contentColor = WebWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_task")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.fab_create_task)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSlate,
                contentColor = WebWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == SpideyTab.ACTIVE,
                    onClick = { currentTab = SpideyTab.ACTIVE },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == SpideyTab.ACTIVE) {
                                Icons.Filled.NotificationsActive
                            } else {
                                Icons.Outlined.NotificationsActive
                            },
                            contentDescription = stringResource(id = R.string.tab_active_tasks)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.tab_active_tasks),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WebWhite,
                        selectedTextColor = WebWhite,
                        indicatorColor = SpideyCrimson,
                        unselectedIconColor = MetadataGrey,
                        unselectedTextColor = MetadataGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_active")
                )

                NavigationBarItem(
                    selected = currentTab == SpideyTab.HISTORY,
                    onClick = { currentTab = SpideyTab.HISTORY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == SpideyTab.HISTORY) {
                                Icons.Filled.History
                            } else {
                                Icons.Outlined.History
                            },
                            contentDescription = stringResource(id = R.string.tab_history)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.tab_history),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WebWhite,
                        selectedTextColor = WebWhite,
                        indicatorColor = SpideyCrimson,
                        unselectedIconColor = MetadataGrey,
                        unselectedTextColor = MetadataGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            SpideyTab.ACTIVE -> {
                ActiveTasksScreen(
                    tasks = pendingTasks,
                    onEditTask = { task ->
                        editingTask = task
                        isCreatorDialogOpen = true
                    },
                    onCompleteTaskEarly = { task ->
                        viewModel.markTaskCompletedEarly(task)
                    },
                    onDeleteTask = { id ->
                        viewModel.deleteTaskById(id)
                    },
                    onTriggerTestAlert = {
                        viewModel.triggerDiagnosticAlert()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            SpideyTab.HISTORY -> {
                HistoryScreen(
                    tasks = historyTasks,
                    selectedFilter = selectedHistoryFilter,
                    onSelectFilter = { filter ->
                        viewModel.setHistoryFilter(filter)
                    },
                    onDeleteTask = { id ->
                        viewModel.deleteTaskById(id)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        if (isCreatorDialogOpen) {
            TaskCreatorDialog(
                existingTask = editingTask,
                onDismiss = {
                    isCreatorDialogOpen = false
                    editingTask = null
                },
                onSave = { title, scheduledTimeMillis ->
                    viewModel.saveOrUpdateTask(
                        existingTask = editingTask,
                        title = title,
                        scheduledTimeMillis = scheduledTimeMillis
                    )
                    isCreatorDialogOpen = false
                    editingTask = null
                }
            )
        }
    }
}
