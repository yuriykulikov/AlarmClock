package com.better.alarm.model

import android.content.Context
import com.better.alarm.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class DaysOfWeekTest {
    private val context = mock(Context::class.java)

    init {
        `when`(context.getText(R.string.day_concat)).thenReturn(", ")
        `when`(context.getText(R.string.never)).thenReturn("")
    }

    @Test
    fun `toString property prints`() {
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
        assertThat(DaysOfWeek((1 shl 0) or (1 shl 1)).toString(context, false)).isEqualTo("Mon, Tue")
        assertThat(DaysOfWeek(arrayOf(0, 1, 2, 3, 4).fold(0) { acc, day -> acc or (1 shl day) }).toString(context, false)).isEqualTo("Mon, Tue, Wed, Thu, Fri")
        assertThat(DaysOfWeek(arrayOf(5, 6).fold(0) { acc, day -> acc or (1 shl day) }).toString(context, false)).isEqualTo("Sat, Sun")
    }

    @Test
    fun `getNextAlarm`() {

    }
}