package com.better.alarm.stores

import android.content.SharedPreferences
import com.better.alarm.data.stores.InMemoryRxDataStoreFactory
import com.better.alarm.data.stores.PrimitiveDataStoreFactory
import com.better.alarm.data.stores.SharedRxDataStoreFactory
import com.better.alarm.data.stores.intStringDataStore
import com.better.alarm.data.stores.modify
import io.mockk.every
import io.mockk.mockk
import java.util.WeakHashMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PrimitiveDataStoresMediumTest(val name: String, private val inMemory: Boolean) {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun params(): Array<Array<Any>> =
        arrayOf(arrayOf("in memory", true), arrayOf("stubbed preferences", false))
  }

  private var stores: PrimitiveDataStoreFactory =
      if (inMemory) {
        InMemoryRxDataStoreFactory()
      } else {
        SharedRxDataStoreFactory.createFromSharedPreferences(
            stubSharedPreferences(), mockk(relaxed = false))
      }

  @Test
  fun `booleanDataStore has default value when created`() {
    assertThat(stores.booleanDataStore("foo", true).value).isEqualTo(true)
    assertThat(stores.booleanDataStore("bar", false).value).isEqualTo(false)
  }

  @Test
  fun `intDataStore has default value when created`() {
    assertThat(stores.intDataStore("foo", 33).value).isEqualTo(33)
    assertThat(stores.intDataStore("bar", 52).value).isEqualTo(52)
  }

  @Test
  fun `intStringDataStore has default value when created`() {
    assertThat(stores.intStringDataStore("foo", 33).value).isEqualTo(33)
    assertThat(stores.intStringDataStore("bar", 52).value).isEqualTo(52)
  }

  @Test
  fun `stringDataStore has default value when created`() {
    assertThat(stores.stringDataStore("foo", "baz").value).isEqualTo("baz")
    assertThat(stores.stringDataStore("bar", "qux").value).isEqualTo("qux")
  }

  @Test
  fun `booleanDataStore can be changed and observed`() {
    val store = stores.booleanDataStore("foo", true)

    val test = store.observe().test()
    test.assertValue(true)
    assertThat(store.value).isEqualTo(true)

    store.value = false
    test.assertValues(true, false)
    assertThat(store.value).isEqualTo(false)

    store.modify { !it }
    test.assertValues(true, false, true)
    assertThat(store.value).isEqualTo(true)
  }

  @Test
  fun `string DataStore can be shared`() {
    val one = stores.stringDataStore("foo", "bar")
    val two = stores.stringDataStore("foo", "bar")

    val testOne = one.observe().test()
    val testTwo = two.observe().test()

    testOne.assertValue("bar")
    testTwo.assertValue("bar")

    one.value = "hello"

    testOne.assertValues("bar", "hello")
    testTwo.assertValues("bar", "hello")

    two.modify { "$it, world" }

    assertThat(one.value).isEqualTo("hello, world")
    assertThat(two.value).isEqualTo("hello, world")
  }

  /**
   * Creates a working stub of [SharedPreferences] which behaves close to the real implementation.
   */
  private fun stubSharedPreferences(): SharedPreferences {
    return mockk {
      val prefs = this
      val values: MutableMap<String, Any> = WeakHashMap()
      val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()
      every { getBoolean(any(), any()) } answers
          {
            values.getOrElse(firstArg()) { lastArg() } as Boolean
          }
      every { getString(any(), any()) } answers
          {
            values.getOrElse(firstArg()) { lastArg() } as String
          }
      every { getInt(any(), any()) } answers { values.getOrElse(firstArg()) { lastArg() } as Int }
      every { registerOnSharedPreferenceChangeListener(any()) } answers
          {
            listeners.add(firstArg())
          }

      every { edit() } answers
          {
            object : SharedPreferences.Editor {
              private val changes = mutableMapOf<String, Any>()

              override fun clear(): SharedPreferences.Editor = apply {}

              override fun remove(key: String) = apply {}

              override fun putLong(key: String, value: Long) = apply { changes[key] = value }

              override fun putInt(key: String, value: Int) = apply { changes[key] = value }

              override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }

              override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {
                changes[key!!] = value
              }

              override fun putFloat(key: String?, value: Float) = apply { changes[key!!] = value }

              override fun putString(key: String, value: String?) = apply { changes[key] = value!! }

              override fun commit(): Boolean {
                apply()
                return true
              }

              override fun apply() {
                values.putAll(changes)
                changes.keys.forEach { key ->
                  listeners.forEach { listener -> listener.onSharedPreferenceChanged(prefs, key) }
                }
              }
            }
          }
    }
  }
}
