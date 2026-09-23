package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrinkType
import com.example.data.model.HydrationLog
import com.example.data.model.UserSettings
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaPrimaryLight
import com.example.ui.theme.AquaSecondary
import com.example.ui.theme.AquaSuccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    allLogs: List<HydrationLog>,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    // Group logs by past 7 days
    val past7DaysData = remember(allLogs, userSettings.dailyGoalMl) {
        val daysList = mutableListOf<DayStat>()
        val calendar = Calendar.getInstance()
        val dayNameFormatter = SimpleDateFormat("EEE", Locale("pt", "BR"))
        val dayNumFormatter = SimpleDateFormat("dd/MM", Locale("pt", "BR"))

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val start = (targetCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val end = (targetCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val logsInDay = allLogs.filter { it.timestamp in start..end }
            val totalMl = logsInDay.sumOf { it.amountMl }
            val isToday = (i == 0)

            daysList.add(
                DayStat(
                    dayLabel = if (isToday) "Hoje" else dayNameFormatter.format(Date(start)).replace(".", "").uppercase(),
                    dateLabel = dayNumFormatter.format(Date(start)),
                    totalMl = totalMl,
                    goalMl = userSettings.dailyGoalMl,
                    isGoalMet = totalMl >= userSettings.dailyGoalMl,
                    isToday = isToday
                )
            )
        }
        daysList
    }

    val total7DaysMl = past7DaysData.sumOf { it.totalMl }
    val averageDailyMl = total7DaysMl / 7
    val goalMetDaysCount = past7DaysData.count { it.isGoalMet }
    val bestDay = past7DaysData.maxByOrNull { it.totalMl }

    // Breakdown by drink type
    val drinkBreakdown = remember(allLogs) {
        val totalVolume = allLogs.sumOf { it.amountMl }.coerceAtLeast(1)
        DrinkType.entries.map { type ->
            val vol = allLogs.filter { it.drinkType.equals(type.name, ignoreCase = true) }.sumOf { it.amountMl }
            Pair(type, (vol.toFloat() / totalVolume * 100).toInt())
        }.filter { it.second > 0 }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("statistics_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Estatísticas de Hidratação",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Acompanhe sua regularidade e volume consumido nos últimos 7 dias",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. KPI Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Média Diária",
                    value = "${averageDailyMl} ml",
                    subtitle = "Últimos 7 dias",
                    icon = Icons.Default.AutoGraph,
                    accentColor = AquaPrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Metas Atingidas",
                    value = "$goalMetDaysCount de 7 dias",
                    subtitle = "Consistência",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = AquaSuccess
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Ingerido",
                    value = String.format(Locale.getDefault(), "%.1f L", total7DaysMl / 1000f),
                    subtitle = "Volume total 7d",
                    icon = Icons.Default.WaterDrop,
                    accentColor = AquaSecondary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Melhor Dia",
                    value = bestDay?.let { "${it.totalMl} ml" } ?: "0 ml",
                    subtitle = bestDay?.dayLabel ?: "-",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = Color(0xFFF59E0B)
                )
            }
        }

        // 2. Weekly Bar Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Histórico Semanal (7 Dias)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AquaPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Meta: ${userSettings.dailyGoalMl}ml",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AquaPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Custom Bar Chart
                    val maxChartValue = (past7DaysData.maxOfOrNull { it.totalMl } ?: userSettings.dailyGoalMl)
                        .coerceAtLeast(userSettings.dailyGoalMl)
                        .coerceAtLeast(1000)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        past7DaysData.forEach { stat ->
                            val heightFraction = (stat.totalMl.toFloat() / maxChartValue).coerceIn(0.04f, 1f)
                            val isGoalMet = stat.totalMl >= stat.goalMl

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Value on top
                                if (stat.totalMl > 0) {
                                    Text(
                                        text = "${stat.totalMl}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isGoalMet) AquaSuccess else AquaPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                // Bar
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = if (isGoalMet) {
                                                    listOf(AquaSuccess, Color(0xFF059669))
                                                } else if (stat.isToday) {
                                                    listOf(AquaPrimaryLight, AquaPrimary)
                                                } else {
                                                    listOf(AquaPrimary.copy(alpha = 0.6f), AquaPrimary.copy(alpha = 0.85f))
                                                }
                                            )
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Day Label
                                Text(
                                    text = stat.dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.Medium
                                    ),
                                    color = if (stat.isToday) AquaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Drink Type Breakdown
        if (drinkBreakdown.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Distribuição por Tipo de Bebida",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        drinkBreakdown.forEach { (type, percentage) ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = type.iconEmoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = type.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AquaPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { percentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AquaPrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

data class DayStat(
    val dayLabel: String,
    val dateLabel: String,
    val totalMl: Int,
    val goalMl: Int,
    val isGoalMet: Boolean,
    val isToday: Boolean
)

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
