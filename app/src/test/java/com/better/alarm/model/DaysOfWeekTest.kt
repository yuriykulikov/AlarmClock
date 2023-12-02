package com.better.alarm.model

import android.content.Context
import com.better.alarm.R
import com.better.alarm.data.DaysOfWeek
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DaysOfWeekTest {
  private val context = mockk<Context>()

  init {
    every { context.getText(R.string.day_concat) } returns ", "
    every { context.getText(R.string.never) } returns "Never"
  }

  @Test
  fun `toString property prints`() {
    Locale.setDefault(Locale.ENGLISH)
    assertThat(DaysOfWeek(1 shl 0).toString(context, false)).isEqualTo("Monday")
    assertThat(DaysOfWeek(1 shl 1).toString(context, false)).isEqualTo("Tuesday")
    assertThat(DaysOfWeek(1 shl 2).toString(context, false)).isEqualTo("Wednesday")
    assertThat(DaysOfWeek(1 shl 3).toString(context, false)).isEqualTo("Thursday")
    assertThat(DaysOfWeek(1 shl 4).toString(context, false)).isEqualTo("Friday")
    assertThat(DaysOfWeek(1 shl 5).toString(context, false)).isEqualTo("Saturday")
    assertThat(DaysOfWeek(1 shl 6).toString(context, false)).isEqualTo("Sunday")
  }

  @Test
  fun `toString property prints many days`() {
    Locale.setDefault(Locale.ENGLISH)
    assertThat(DaysOfWeek((1 shl 0) or (1 shl 1)).toString(context, false)).isEqualTo("Mon, Tue")
    assertThat(
            DaysOfWeek(arrayOf(0, 1, 2, 3, 4).fold(0) { acc, day -> acc or (1 shl day) })
                .toString(context, false))
        .isEqualTo("Mon, Tue, Wed, Thu, Fri")
    assertThat(
            DaysOfWeek(arrayOf(5, 6).fold(0) { acc, day -> acc or (1 shl day) })
                .toString(context, false))
        .isEqualTo("Sat, Sun")
  }
}
