package com.better.alarm

import com.better.alarm.view.TimePickerPresenter
import com.better.alarm.view.TimePickerPresenter.Key
import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Created by Yuriy on 17.08.2017.
 */

class TimePickerPresenterTest {
    @Rule
    @JvmField
    var watcher: TestRule = object : TestWatcher() {
        override fun starting(description: Description) {
            println("---- ${description.methodName} ----")
        }
    }

    @Test
    fun enter4digits24() {
        val presenter = TimePickerPresenter(true)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)

        val last = presenter.state.test().values().last()

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.hours).isEqualTo(12)
        assertThat(last.minutes).isEqualTo(35)
    }

    @Test
    fun enter4digitsAm() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.LEFT)

        val last = presenter.state.test().values().last()

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.hours).isEqualTo(0)
        assertThat(last.minutes).isEqualTo(35)
    }

    @Test
    fun enter4digitsPm() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.RIGHT)

        val last = presenter.state.test().values().last()

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.hours).isEqualTo(12)
        assertThat(last.minutes).isEqualTo(35)
    }

    @Test
    fun delete() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.DELETE)
        presenter.onClick(Key.DELETE)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursTensDigit).isEqualTo(-1)
        assertThat(last.hoursOnesDigit).isEqualTo(-1)
        assertThat(last.minutesTensDigit).isEqualTo(1)
        assertThat(last.minutesOnesDigit).isEqualTo(2)
    }

    @Test
    fun left24() {
        val presenter = TimePickerPresenter(true)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.LEFT)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(0)
        assertThat(last.minutesOnesDigit).isEqualTo(0)
        assertThat(last.hours).isEqualTo(12)
        assertThat(last.minutes).isEqualTo(0)
    }

    @Test
    fun right24() {
        val presenter = TimePickerPresenter(true)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.RIGHT)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(0)
        assertThat(last.hours).isEqualTo(12)
        assertThat(last.minutes).isEqualTo(30)
    }

    @Test
    fun right12() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.SEVEN)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.RIGHT)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursOnesDigit).isEqualTo(7)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.amPm).isEqualTo(TimePickerPresenter.AmPm.PM)
        assertThat(last.hours).isEqualTo(19)
        assertThat(last.minutes).isEqualTo(35)
    }

    @Test
    fun when1239pm() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.RIGHT)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.amPm).isEqualTo(TimePickerPresenter.AmPm.PM)
        assertThat(last.hours).isEqualTo(12)
        assertThat(last.minutes).isEqualTo(35)
    }

    @Test
    fun when1239am() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.TWO)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.LEFT)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(2)
        assertThat(last.minutesTensDigit).isEqualTo(3)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.amPm).isEqualTo(TimePickerPresenter.AmPm.AM)
        assertThat(last.hours).isEqualTo(0)
        assertThat(last.minutes).isEqualTo(35)
    }

    @Test
    fun when1605() {
        val presenter = TimePickerPresenter(true)

        presenter.onClick(Key.ONE)
        presenter.onClick(Key.SIX)
        presenter.onClick(Key.ZERO)
        presenter.onClick(Key.FIVE)

        val last = presenter.state.test().values().last()

        System.out.println(last)

        assertThat(last.hoursTensDigit).isEqualTo(1)
        assertThat(last.hoursOnesDigit).isEqualTo(6)
        assertThat(last.minutesTensDigit).isEqualTo(0)
        assertThat(last.minutesOnesDigit).isEqualTo(5)
        assertThat(last.amPm).isEqualTo(TimePickerPresenter.AmPm.NONE)
        assertThat(last.hours).isEqualTo(16)
        assertThat(last.minutes).isEqualTo(5)
    }

    @Test
    fun ok() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.SEVEN)
        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)
        presenter.onClick(Key.LEFT)

        val last = presenter.state.test().values().last()

        assertThat(last.enabled.contains(Key.OK)).isTrue()
    }

    @Test
    fun nok() {
        val presenter = TimePickerPresenter(false)

        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)

        val last = presenter.state.test().values().last()

        assertThat(last.enabled.contains(Key.OK)).isFalse()
    }

    @Test
    fun nok2() {
        val presenter = TimePickerPresenter(true)

        presenter.onClick(Key.THREE)
        presenter.onClick(Key.FIVE)

        val last = presenter.state.test().values().last()

        assertThat(last.enabled.contains(Key.OK)).isFalse()
    }

    @Test
    fun after0in24Mode() {
        val presenter = TimePickerPresenter(true)

        presenter.onClick(Key.ZERO)

        val last = presenter.state.test().values().last()

        assertThat(last.enabled.containsAll(last.any)).isTrue()
    }
}
