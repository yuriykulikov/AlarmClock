package com.better.alarm.util

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

fun <T, O> Observable<T>.mapNotNull(func: (T) -> O?): Observable<O> {
  return map { Optional.fromNullable(func(it)) }.filter { it.isPresent() }.map { it.get() }
}

fun <T : Any> BehaviorSubject<T>.modify(func: (T).(T) -> T) {
  value?.let { onNext(func(it, it)) }
}

fun <T : Any> BehaviorSubject<T>.requireValue(): T {
  return requireNotNull(value)
}

fun <T : Any> Observable<T>.subscribeIn(scope: CompositeDisposable, func: (T) -> Unit) {
  scope.add(subscribe(func))
}

fun <T : Any> Observable<T>.subscribeForever(consumer: (T) -> Unit) {
  subscribe { consumer(it) }.apply {}
}
