package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaPrimaryLight
import com.example.ui.theme.AquaSecondary
import com.example.ui.theme.AquaSuccess
import kotlin.math.sin

@Composable
fun FluidWaveProgress(
    percentage: Int,
    currentMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    val clampedPercent = percentage.coerceIn(0, 100) / 100f
    val isGoalReached = percentage >= 100

    Box(
        modifier = modifier
            .size(size)
            .shadow(16.dp, CircleShape, spotColor = AquaPrimary.copy(alpha = 0.5f))
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE),
                        Color(0xFFBAE6FD)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Water Wave Animation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val waterLevelY = height * (1f - clampedPercent)

            // Back wave (cyan lighter)
            val backWavePath = Path()
            backWavePath.moveTo(0f, height)
            for (x in 0..width.toInt() step 5) {
                val waveHeight = 12.dp.toPx()
                val y = waterLevelY + waveHeight * sin(x * 0.025f + waveOffset)
                backWavePath.lineTo(x.toFloat(), y)
            }
            backWavePath.lineTo(width, height)
            backWavePath.close()

            drawPath(
                path = backWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AquaPrimaryLight.copy(alpha = 0.7f),
                        AquaSecondary.copy(alpha = 0.85f)
                    ),
                    startY = waterLevelY - 20f,
                    endY = height
                )
            )

            // Front wave (vibrant aqua blue)
            val frontWavePath = Path()
            frontWavePath.moveTo(0f, height)
            for (x in 0..width.toInt() step 5) {
                val waveHeight = 10.dp.toPx()
                val y = waterLevelY + waveHeight * sin(x * 0.022f - waveOffset + 1.5f)
                frontWavePath.lineTo(x.toFloat(), y)
            }
            frontWavePath.lineTo(width, height)
            frontWavePath.close()

            val primaryWaterColor = if (isGoalReached) AquaSuccess else AquaPrimary

            drawPath(
                path = frontWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryWaterColor.copy(alpha = 0.85f),
                        AquaPrimary
                    ),
                    startY = waterLevelY - 15f,
                    endY = height
                )
            )

            // Border glow
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Center Content Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (isGoalReached) "🎉 $percentage%" else "$percentage%",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = if (clampedPercent > 0.55f) Color.White else Color(0xFF0C4A6E)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$currentMl / $goalMl ml",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (clampedPercent > 0.55f) Color.White.copy(alpha = 0.95f) else Color(0xFF0369A1)
            )
            val remaining = (goalMl - currentMl).coerceAtLeast(0)
            Text(
                text = if (isGoalReached) "Meta Concluída!" else "Faltam ${remaining}ml",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (clampedPercent > 0.55f) Color.White.copy(alpha = 0.8f) else Color(0xFF0284C7)
            )
        }
    }
}
