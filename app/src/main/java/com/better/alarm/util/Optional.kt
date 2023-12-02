package com.better.alarm.util

data class Optional<T>(val of: T?) {
  fun isPresent(): Boolean = of != null

  fun get(): T = of!!

  fun getOrNull(): T? = of

  fun or(defaultValue: T): T = of ?: defaultValue

  fun <O : Any> map(function: (T).(T) -> O): Optional<O> {
    return of?.let { Optional(function(of, of)) } ?: absent()
  }

  companion object {
    @JvmStatic fun <T> absent(): Optional<T> = Optional(null)

    @JvmStatic fun <T> fromNullable(value: T?): Optional<T> = Optional(value)

    @JvmStatic fun <T> of(value: T): Optional<T> = Optional(value)
  }
}
