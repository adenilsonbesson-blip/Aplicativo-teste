package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.ui.graphics.vector.ImageVector

enum class DrinkType(
    val displayName: String,
    val hydrationFactor: Float, // Water multiplier for effective hydration
    val iconEmoji: String,
    val defaultAmountMl: Int
) {
    WATER("Água", 1.0f, "💧", 250),
    TEA("Chá", 0.95f, "🍵", 200),
    COCONUT_WATER("Água de Coco", 1.0f, "🥥", 300),
    JUICE("Suco Natural", 0.85f, "🍊", 250),
    COFFEE("Café", 0.8f, "☕", 150),
    ISOTONIC("Isotônico", 1.0f, "⚡", 500);

    companion object {
        fun fromName(name: String?): DrinkType {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: WATER
        }
    }
}
