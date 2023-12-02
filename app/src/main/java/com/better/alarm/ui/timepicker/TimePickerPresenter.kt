package com.better.alarm.ui.timepicker

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/** Created by Yuriy on 17.08.2017. */
open class TimePickerPresenter(private val is24HoursMode: Boolean) {
  enum class Key {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    ZERO,
    OK,
    LEFT,
    RIGHT,
    DELETE
  }

  enum class AmPm {
    AM,
    PM,
    NONE
  }

  // TODO visibility
  private val subject: BehaviorSubject<State> =
      BehaviorSubject.createDefault(State(is24HoursMode = is24HoursMode))
  val state: Observable<State> = subject

  private var input: List<Int> = arrayListOf()
  private var amPm: AmPm = AmPm.NONE
  private var leftRightEntered: Boolean = false

  fun onClick(key: Key) {
    when (key) {
      Key.ONE -> input = input.plus(1)
      Key.TWO -> input = input.plus(2)
      Key.THREE -> input = input.plus(3)
      Key.FOUR -> input = input.plus(4)
      Key.FIVE -> input = input.plus(5)
      Key.SIX -> input = input.plus(6)
      Key.SEVEN -> input = input.plus(7)
      Key.EIGHT -> input = input.plus(8)
      Key.NINE -> input = input.plus(9)
      Key.ZERO -> input = input.plus(0)
      Key.LEFT,
      Key.RIGHT -> leftRightEntered = true
      else -> {}
    }

    when {
      key == Key.LEFT && is24HoursMode -> input = input.plus(0).plus(0)
      key == Key.RIGHT && is24HoursMode -> input = input.plus(3).plus(0)
      key == Key.LEFT && input.hoursForAmPmEntered() -> {
        amPm = AmPm.AM
        input = input.plus(0).plus(0)
      }
      key == Key.LEFT -> amPm = AmPm.AM
      key == Key.RIGHT && input.hoursForAmPmEntered() -> {
        amPm = AmPm.PM
        input = input.plus(0).plus(0)
      }
      key == Key.RIGHT -> amPm = AmPm.PM
    }

    when {
      key == Key.DELETE && leftRightEntered && is24HoursMode -> {
        input = input.dropLast(2)
        leftRightEntered = false
      }
      key == Key.DELETE && leftRightEntered && !is24HoursMode -> {
        amPm = AmPm.NONE
        leftRightEntered = false
      }
      key == Key.DELETE -> input = input.dropLast(1)
    }

    subject.onNext(
        State(
            input = this.input,
            amPm = this.amPm,
            is24HoursMode = is24HoursMode,
            leftRightEntered = leftRightEntered))
  }

  fun reset() {
    input = arrayListOf()
    amPm = AmPm.NONE
    subject.onNext(State(is24HoursMode = is24HoursMode))
  }

  data class State(
      val input: List<Int> = arrayListOf(),
      val amPm: AmPm = AmPm.NONE,
      val is24HoursMode: Boolean,
      val leftRightEntered: Boolean = false
  ) {
    val any: List<Key> =
        arrayListOf(
            Key.ZERO,
            Key.ONE,
            Key.TWO,
            Key.THREE,
            Key.FOUR,
            Key.FIVE,
            Key.SIX,
            Key.SEVEN,
            Key.EIGHT,
            Key.NINE)
    private val zeroToFive: List<Key> =
        arrayListOf(Key.ZERO, Key.ONE, Key.TWO, Key.THREE, Key.FOUR, Key.FIVE)
    private val none: List<Key> = arrayListOf()

    private val inverted = input.asReversed()
    val hoursTensDigit: Int by lazy { inverted.getOrElse(3) { -1 } }
    val hoursOnesDigit: Int by lazy { inverted.getOrElse(2) { -1 } }
    val minutesTensDigit: Int by lazy { inverted.getOrElse(1) { -1 } }
    val minutesOnesDigit: Int by lazy { inverted.getOrElse(0) { -1 } }
    val asText: String by lazy {
      inverted.run {
        "${getOrElse(3) { '-' }}${getOrElse(2) { '-' }}:${inverted.getOrElse(1) { '-' }}${inverted.getOrElse(0) { '-' }}"
      }
    }

    val minutes: Int by lazy { minutesTensDigit * 10 + minutesOnesDigit }
    val hours: Int by lazy {
      val hours = inverted.getOrElse(3, { 0 }) * 10 + hoursOnesDigit
      when {
        is24HoursMode -> hours
        hours == 12 && amPm == AmPm.AM -> 0
        hours == 12 && amPm == AmPm.PM -> 12
        amPm == AmPm.PM -> hours + 12
        else -> hours
      }
    }

    private fun isTimeValid(): Boolean =
        when {
          is24HoursMode -> inverted.size >= 3 && hours <= 23 && minutes <= 60
          else -> inverted.size >= 3 && minutes <= 60 && leftRightEntered
        }

    private fun front(): Int = input.getOrElse(0) { 0 } * 10 + input.getOrElse(1) { 0 }

    val enabled: List<Key> by lazy {
      var enabled: List<Key> =
          when {
            is24HoursMode -> enable24Digits().plus(wholeAndHalf())
            else -> enable12Digits().plus(amPm())
          }

      if (input.isNotEmpty()) enabled = enabled.plus(Key.DELETE)
      if (isTimeValid()) enabled = enabled.plus(Key.OK)

      enabled
    }

    private fun enable24Digits(): List<Key> =
        when {
          leftRightEntered -> none
          input.isEmpty() -> any
          // After first 0 or 1, second can be any (e.g. 07:15, 19:35, 1:34)
          input.size == 1 && input.first() <= 1 -> any
          // After first 2 or more, second can be zeroToFive (e.g. 23:15, 2:58)
          input.size == 1 && input.first() > 1 -> zeroToFive
          // Third can be any, like in 7:49 or 12:52 or 1:29
          input.size == 2 -> any
          // last number can be minutes if front() is less than 24
          input.size == 3 && front() < 24 && input[2] <= 5 -> any
          else -> none
        }

    private fun wholeAndHalf(): List<Key> =
        when {
          leftRightEntered -> none
          input.size == 1 -> arrayListOf(Key.RIGHT, Key.LEFT)
          input.size == 2 && front() <= 23 -> arrayListOf(Key.RIGHT, Key.LEFT)
          else -> none
        }

    private fun enable12Digits(): List<Key> =
        when {
          leftRightEntered -> none
          input.isEmpty() -> any.minus(Key.ZERO)
          // second number can be tens of hours or tens of minutes
          input.size == 1 -> zeroToFive
          // third number can tens of minutes or minutes (any)
          input.size == 2 -> any
          // last number can be minutes if front() is less than 12
          input.size == 3 && front() <= 12 && input[2] <= 5 -> any
          else -> none
        }

    private fun amPm(): List<Key> =
        when {
          leftRightEntered -> none
          // enable am pm keys if only hours are entered
          input.size == 1 -> arrayListOf(Key.RIGHT, Key.LEFT)
          // 10, 11, 12 am/pm
          input.hoursForAmPmEntered() -> arrayListOf(Key.RIGHT, Key.LEFT)
          input.size == 2 && input[0] == 1 && input[1] in (0..2) -> arrayListOf(Key.RIGHT, Key.LEFT)
          input.size >= 3 -> arrayListOf(Key.RIGHT, Key.LEFT)
          else -> none
        }
  }
}

private fun List<Int>.hoursForAmPmEntered(): Boolean {
  return when {
    size == 1 -> true
    size == 2 && first() == 1 && this[1] in (0..2) -> true
    else -> false
  }
}
