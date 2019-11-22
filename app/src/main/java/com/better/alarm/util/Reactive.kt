package com.better.alarm.util

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject


fun <T, O> Observable<T>.mapNotNull(func: (T) -> O?): Observable<O> {
    return map { Optional.fromNullable(func(it)) }
            .filter { it.isPresent() }
            .map { it.get() }
}

fun <T : Any> BehaviorSubject<T>.modify(func: (T).() -> T) {
    value?.let {
        onNext(func(it))
    }
}