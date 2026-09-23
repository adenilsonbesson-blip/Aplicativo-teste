package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.SmartHydrationCalculator
import com.example.alarm.PaceStatus
import com.example.data.model.DrinkType
import com.example.data.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AquaAlerta", appName)
  }

  @Test
  fun `test smart hydration pace calculation`() {
    val settings = UserSettings(
        dailyGoalMl = 2000,
        wakeUpHour = 8,
        bedHour = 22,
        smartRemindersEnabled = true
    )
    val paceInfo = SmartHydrationCalculator.calculatePace(
        todayTotalMl = 2500,
        settings = settings
    )
    assertEquals(PaceStatus.GOAL_REACHED, paceInfo.paceStatus)
    assertTrue(paceInfo.percentage >= 100)
  }

  @Test
  fun `test drink type effective hydration factor`() {
    assertEquals(1.0f, DrinkType.WATER.hydrationFactor)
    assertEquals(0.95f, DrinkType.TEA.hydrationFactor)
    assertEquals("Água", DrinkType.WATER.displayName)
  }
}
