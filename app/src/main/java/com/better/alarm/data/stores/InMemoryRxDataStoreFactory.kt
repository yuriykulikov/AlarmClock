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
import io.reactivex.subjects.BehaviorSubject

class InMemoryRxDataStoreFactory : PrimitiveDataStoreFactory {
  companion object {
    @JvmStatic fun create(): PrimitiveDataStoreFactory = InMemoryRxDataStoreFactory()

    fun <T : Any> inMemoryRxDataStore(defaultValue: T): RxDataStore<T> {
      return object : RxDataStore<T> {
        private val subject: BehaviorSubject<T> = BehaviorSubject.createDefault(defaultValue)
        override var value: T
          get() = requireNotNull(subject.value)
          set(value) = subject.onNext(value)

        override fun observe(): Observable<T> = subject.hide()
      }
    }
  }

  private val booleans = mutableMapOf<String, RxDataStore<Boolean>>()
  private val strings = mutableMapOf<String, RxDataStore<String>>()
  private val ints = mutableMapOf<String, RxDataStore<Int>>()

  override fun booleanDataStore(key: String, defaultValue: Boolean): RxDataStore<Boolean> {
    return booleans.getOrPut(key) { inMemoryRxDataStore(defaultValue) }
  }

  override fun stringDataStore(key: String, defaultValue: String): RxDataStore<String> {
    return strings.getOrPut(key) { inMemoryRxDataStore(defaultValue) }
  }

  override fun intDataStore(key: String, defaultValue: Int): RxDataStore<Int> {
    return ints.getOrPut(key) { inMemoryRxDataStore(defaultValue) }
  }
}
