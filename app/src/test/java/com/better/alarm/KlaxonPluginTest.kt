package com.better.alarm

import com.better.alarm.data.Alarmtone
import com.better.alarm.logger.Logger
import com.better.alarm.services.KlaxonPlugin
import com.better.alarm.services.Player
import com.better.alarm.services.PluginAlarmData
import com.better.alarm.services.TargetVolume
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.Test

class KlaxonPluginTest {
  private val playerMock = mockk<Player>(relaxed = true)
  private val scheduler = TestScheduler()
  private val klaxonPlugin =
      KlaxonPlugin(
          inCall = Observable.just(false),
          fadeInTimeInMillis = Observable.just(30 * 1000),
          log = Logger.create(),
          playerFactory = { playerMock },
          prealarmVolume = Observable.just(5),
          scheduler = scheduler)

  private fun Float.squared() = this * this

  private val closeEnough = Percentage.withPercentage(10f.toDouble())

  @Test
  fun `player should be configured when started`() {
    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default, ""), false, Observable.just(TargetVolume.FADED_IN))

    verify { playerMock.setPerceivedVolume(0f) }
    verify { playerMock.setDataSource(Alarmtone.defaultAlarmAlertUri) }
    verify { playerMock.startAlarm() }
  }

  @Test
  fun `clean up should be sequenced`() {
    klaxonPlugin
        .go(PluginAlarmData(1, Alarmtone.Default, ""), true, Observable.just(TargetVolume.FADED_IN))
        .dispose()

    verify { playerMock.stop() }
  }

  @Test
  fun `gradual fade in`() {
    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default, ""), false, Observable.just(TargetVolume.FADED_IN))

    scheduler.advanceTimeBy(15, TimeUnit.SECONDS)

    val values = mutableListOf<Float>()
    verify(atLeast = 1) { playerMock.setPerceivedVolume(capture(values)) }

    assertThat(values.last()).isCloseTo(0.5f.squared(), closeEnough)

    scheduler.advanceTimeBy(30, TimeUnit.SECONDS)

    verify(atLeast = 1) { playerMock.setPerceivedVolume(capture(values)) }
    assertThat(values.last()).isCloseTo(1.0f.squared(), closeEnough)
  }

  @Test
  fun `gradual fade in prealarm`() {
    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default, ""), true, Observable.just(TargetVolume.FADED_IN))

    scheduler.advanceTimeBy(15, TimeUnit.SECONDS)

    val values = mutableListOf<Float>()
    verify(atLeast = 1) { playerMock.setPerceivedVolume(capture(values)) }
    // prealarm is set to 5, which is a half, and it is halved again because it is a prealarm
    assertThat(values.last()).isCloseTo(0.25f.squared(), closeEnough)

    scheduler.advanceTimeBy(30, TimeUnit.SECONDS)

    verify { playerMock.setPerceivedVolume(capture(values)) }
    assertThat(values.last()).isCloseTo(0.5f.squared(), closeEnough)
  }

  @Test
  fun `fallback should be used if failed to play default`() {

    every { playerMock.setDataSource(any()) } throws NullPointerException("Test IOE")

    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default, ""), true, Observable.just(TargetVolume.FADED_IN))

    verify { playerMock.setDataSourceFromResource(R.raw.fallbackring) }
  }
}
