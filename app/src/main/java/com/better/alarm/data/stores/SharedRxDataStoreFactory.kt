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

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.better.alarm.logger.Logger
import io.reactivex.Observable

/** Implementation of [PrimitiveDataStoreFactory] which uses [SharedPreferences] to store values. */
class SharedRxDataStoreFactory
private constructor(private val preferences: SharedPreferences, private val logger: Logger) :
    PrimitiveDataStoreFactory {
  companion object {
    fun create(context: Context, logger: Logger): PrimitiveDataStoreFactory {
      return SharedRxDataStoreFactory(
          PreferenceManager.getDefaultSharedPreferences(context), logger)
    }

    fun createFromSharedPreferences(
        preferences: SharedPreferences,
        logger: Logger
    ): PrimitiveDataStoreFactory {
      return SharedRxDataStoreFactory(preferences, logger)
    }
  }

  private val keyChanges =
      Observable.create<String> { emitter ->
            // we need to hold a strong reference to this listener
            val listener =
                object : SharedPreferences.OnSharedPreferenceChangeListener {
                  override fun onSharedPreferenceChanged(
                      preferences: SharedPreferences,
                      key: String
                  ) {
                    emitter.onNext(key)
                  }

                  protected fun finalize() {
                    logger.error { "finalized ${javaClass.name}" }
                  }
                }

            emitter.setCancellable {
              preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }

            preferences.registerOnSharedPreferenceChangeListener(listener)
          }
          .share()

  override fun booleanDataStore(key: String, defaultValue: Boolean): RxDataStore<Boolean> {
    return object : RxDataStore<Boolean> {
      override var value: Boolean
        get() = preferences.getBoolean(key, defaultValue)
        set(value) = preferences.edit().putBoolean(key, value).apply()

      override fun observe(): Observable<Boolean> =
          keyChanges.startWith(key).filter { it == key }.map { value }
    }
  }

  override fun stringDataStore(key: String, defaultValue: String): RxDataStore<String> {
    return object : RxDataStore<String> {
      override var value: String
        get() = preferences.getString(key, defaultValue) ?: defaultValue
        set(value) = preferences.edit().putString(key, value).apply()

      override fun observe(): Observable<String> =
          keyChanges.startWith(key).filter { it == key }.map { value }
    }
  }

  override fun intDataStore(key: String, defaultValue: Int): RxDataStore<Int> {
    return object : RxDataStore<Int> {
      override var value: Int
        get() = preferences.getInt(key, defaultValue)
        set(value) = preferences.edit().putInt(key, value).apply()

      override fun observe(): Observable<Int> =
          keyChanges.startWith(key).filter { it == key }.map { value }
    }
  }
}
