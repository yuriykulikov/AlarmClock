/*
 * MIT License
 *
 * Copyright (c) 2020 Yuriy Kulikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.better.alarm.data.stores

import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.asFlow

/** Represents a single store. This an observable and persistent container for a single value. */
interface RxDataStore<T : Any> {
  var value: T

  fun observe(): Observable<T>

  fun flow(): Flow<T> {
    return observe().asFlow()
  }
}

/** Change the contents of the [RxDataStore] given the previous value. */
fun <T : Any> RxDataStore<T>.modify(func: T.(prev: T) -> T) {
  value = func(value, value)
}

/** Factory to create [RxDataStore] objects. */
interface PrimitiveDataStoreFactory {
  /**
   * Creates a new [RxDataStore]<[Boolean]>. [defaultValue] will be used if the RxDataStore was not
   * initialized yet.
   */
  fun booleanDataStore(key: String, defaultValue: Boolean): RxDataStore<Boolean>

  /**
   * Creates a new [RxDataStore]<[String]>. [defaultValue] will be used if the RxDataStore was not
   * initialized yet.
   */
  fun stringDataStore(key: String, defaultValue: String): RxDataStore<String>

  /**
   * Creates a new [RxDataStore]<[Int]>. [defaultValue] will be used if the RxDataStore was not
   * initialized yet.
   */
  fun intDataStore(key: String, defaultValue: Int): RxDataStore<Int>
}

/**
 * Creates a new [RxDataStore]<[Int]>. [defaultValue] will be used if the RxDataStore was not
 * initialized yet.
 */
fun PrimitiveDataStoreFactory.intStringDataStore(key: String, defaultValue: Int): RxDataStore<Int> {
  val stringStore = stringDataStore(key, defaultValue.toString())
  return object : RxDataStore<Int> {
    override var value: Int
      get() = stringStore.value.toInt()
      set(value) {
        stringStore.value = value.toString()
      }

    override fun observe(): Observable<Int> = stringStore.observe().map { it.toInt() }
  }
}
