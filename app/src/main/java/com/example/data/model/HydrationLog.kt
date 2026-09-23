package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hydration_logs")
data class HydrationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val drinkType: String = DrinkType.WATER.name,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    val type: DrinkType
        get() = DrinkType.fromName(drinkType)

    val effectiveHydrationMl: Int
        get() = (amountMl * type.hydrationFactor).toInt()
}
