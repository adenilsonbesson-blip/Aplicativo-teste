package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ActiveAlarmOverlay
import com.example.ui.screens.AlarmSettingsScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TodayDashboardScreen
import com.example.ui.viewmodel.HydrationViewModel

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY("Hoje", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop, "tab_today"),
    STATS("Histórico", Icons.Filled.BarChart, Icons.Outlined.BarChart, "tab_stats"),
    ALARMS("Alarmes", Icons.Filled.Alarm, Icons.Outlined.Alarm, "tab_alarms"),
    PROFILE("Perfil", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@Composable
fun MainScreen(
    viewModel: HydrationViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }

    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val paceInfo by viewModel.paceInfo.collectAsStateWithLifecycle()
    val isAlarmRinging by viewModel.isAlarmRinging.collectAsStateWithLifecycle()
    val activeAlarmMessage by viewModel.activeAlarmMessage.collectAsStateWithLifecycle()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    AppTab.TODAY -> TodayDashboardScreen(
                        todayLogs = todayLogs,
                        paceInfo = paceInfo,
                        userSettings = userSettings,
                        onLogWater = { amount, type, note -> viewModel.logWater(amount, type, note) },
                        onDeleteLog = { id -> viewModel.deleteLog(id) },
                        onTestAlarm = { viewModel.triggerAlarmTestNow() },
                        onToggleAlarms = { enabled -> viewModel.toggleAlarmsEnabled(enabled) }
                    )

                    AppTab.STATS -> StatisticsScreen(
                        allLogs = allLogs,
                        userSettings = userSettings
                    )

                    AppTab.ALARMS -> AlarmSettingsScreen(
                        userSettings = userSettings,
                        onUpdateSettings = { updated -> viewModel.updateSettings(updated) },
                        onTestAlarm = { viewModel.triggerAlarmTestNow() }
                    )

                    AppTab.PROFILE -> ProfileSettingsScreen(
                        userSettings = userSettings,
                        onUpdateSettings = { updated -> viewModel.updateSettings(updated) },
                        onClearHistory = { viewModel.clearAllHistory() },
                        onCalculateGoal = { weight, activity ->
                            viewModel.calculateRecommendedWaterGoal(weight, activity)
                        }
                    )
                }
            }

            // High Urgency Persistent Alarm Ringing Overlay (Modal)
            ActiveAlarmOverlay(
                isRinging = isAlarmRinging,
                message = activeAlarmMessage,
                onConfirmHydration = { amount, type ->
                    viewModel.logWater(amount, type, "Confirmado no alarme")
                },
                onSnooze = { minutes ->
                    viewModel.snoozeAlarm(minutes)
                },
                onDismiss = {
                    viewModel.stopAlarmDirectly()
                }
            )
        }
    }
}
