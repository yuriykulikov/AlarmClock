package com.better.alarm.util

import io.reactivex.Observable


fun <T, O> Observable<T>.mapNotNull(func: (T) -> O?): Observable<O> {
    return map { Optional.fromNullable(func(it)) }
            .filter { it.isPresent() }
            .map { it.get() }
}