package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PiTelemetry
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
    assertEquals("RaspiDash", appName)
  }

  @Test
  fun `verify pi telemetry calculations`() {
    val telemetry = PiTelemetry(
      cpuTemp = 52.4,
      cpuUsage = 25.0,
      ramUsedMb = 2048,
      ramTotalMb = 8192,
      diskUsedGb = 16.0,
      diskTotalGb = 64.0
    )
    assertEquals(25.0, telemetry.ramPercent, 0.1)
    assertEquals(25.0, telemetry.diskPercent, 0.1)
    assertTrue(telemetry.formattedUptime.isNotEmpty())
  }
}
