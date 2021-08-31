package com.better.alarm

import com.better.alarm.background.KlaxonPlugin
import com.better.alarm.background.Player
import com.better.alarm.background.PluginAlarmData
import com.better.alarm.background.TargetVolume
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarmtone
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class KlaxonPluginTest {
  private val playerMock = mock(Player::class.java)
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
        PluginAlarmData(1, Alarmtone.Default(), ""), false, Observable.just(TargetVolume.FADED_IN))

    verify(playerMock).setPerceivedVolume(0f)
    verify(playerMock).setDataSource(Alarmtone.Default())
    verify(playerMock).startAlarm()
  }

  @Test
  fun `clean up should be sequenced`() {
    klaxonPlugin
        .go(
            PluginAlarmData(1, Alarmtone.Default(), ""),
            true,
            Observable.just(TargetVolume.FADED_IN))
        .dispose()

    verify(playerMock).stop()
  }

  @Test
  fun `gradual fade in`() {
    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default(), ""), false, Observable.just(TargetVolume.FADED_IN))

    scheduler.advanceTimeBy(15, TimeUnit.SECONDS)

    ArgumentCaptor.forClass(Float::class.java).run {
      verify(playerMock, atLeastOnce()).setPerceivedVolume(capture())
      assertThat(value).isCloseTo(0.5f.squared(), closeEnough)
    }

    scheduler.advanceTimeBy(30, TimeUnit.SECONDS)

    ArgumentCaptor.forClass(Float::class.java).run {
      verify(playerMock, atLeastOnce()).setPerceivedVolume(capture())
      assertThat(value).isCloseTo(1.0f, closeEnough)
    }
  }

  @Test
  fun `gradual fade in prealarm`() {
    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default(), ""), true, Observable.just(TargetVolume.FADED_IN))

    scheduler.advanceTimeBy(15, TimeUnit.SECONDS)

    ArgumentCaptor.forClass(Float::class.java).run {
      verify(playerMock, atLeastOnce()).setPerceivedVolume(capture())
      // prealarm is set to 5, which is a half, and it is halved again because it is a prealarm

      assertThat(value).isCloseTo(0.25f.squared(), closeEnough)
    }

    scheduler.advanceTimeBy(30, TimeUnit.SECONDS)

    ArgumentCaptor.forClass(Float::class.java).run {
      verify(playerMock, atLeastOnce()).setPerceivedVolume(capture())
      println(value)
      assertThat(value).isCloseTo(0.5f.squared(), closeEnough)
    }
  }

  @Test
  fun `fallback should be used if failed to play default`() {
    any(Alarmtone::class.java)
    `when`(playerMock.setDataSource(Alarmtone.Default()))
        .thenThrow(NullPointerException("Test IOE"))

    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Default(), ""), true, Observable.just(TargetVolume.FADED_IN))

    verify(playerMock).setDataSourceFromResource(R.raw.fallbackring)
  }

  @Test
  fun `fallback should be used if failed to play`() {
    any(Alarmtone::class.java)
    `when`(playerMock.setDataSource(Alarmtone.Sound("")))
        .thenThrow(NullPointerException("Test IOE"))

    klaxonPlugin.go(
        PluginAlarmData(1, Alarmtone.Sound(""), ""), true, Observable.just(TargetVolume.FADED_IN))

    verify(playerMock).setDataSourceFromResource(R.raw.fallbackring)
  }
}
