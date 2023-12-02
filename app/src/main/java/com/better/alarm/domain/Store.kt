package com.better.alarm.domain

import com.better.alarm.data.AlarmValue
import com.better.alarm.services.Event
import com.better.alarm.util.Optional
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/** Created by Yuriy on 10.06.2017. */
data class Store(
    val alarmsSubject: BehaviorSubject<List<AlarmValue>>,
    val next: BehaviorSubject<Optional<Next>>,
    val sets: Subject<AlarmSet>,
    val events: Subject<Event>,
) {
  val uiVisible: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

  fun alarms(): Observable<List<AlarmValue>> {
    return alarmsSubject().distinctUntilChanged()
  }

  fun alarmsSubject(): BehaviorSubject<List<AlarmValue>> = alarmsSubject

  fun next(): BehaviorSubject<Optional<Next>> = next

  fun sets(): Subject<AlarmSet> = sets

  data class Next(
      val isPrealarm: Boolean,
      val alarm: AlarmValue,
      val nextNonPrealarmTime: Long,
  ) {
    fun alarm(): AlarmValue = alarm

    fun nextNonPrealarmTime(): Long = nextNonPrealarmTime
  }

  data class AlarmSet(
      val alarm: AlarmValue,
      val millis: Long,
      val fromUserInteraction: Boolean,
  ) {
    fun alarm(): AlarmValue = alarm

    fun millis(): Long = millis
  }
}
