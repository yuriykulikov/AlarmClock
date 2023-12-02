package com.better.alarm.data

import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.better.alarm.logger.Logger
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.time.measureDuration
import org.koin.core.time.measureTimedValue

/**
 * [AlarmsRepository] which stores [AlarmValue]s in a [DataStore].
 *
 * [AlarmsRepository] uses active records ([AlarmStore]s) to read and write values.
 *
 * ## Extra care required
 * Both reading and writing are asynchronous which requires extra care in certain situations.
 *
 * ### Reading
 * When reading values to dispatch an event to an alarm from a broadcast receiver, a blocking read
 * is required because the application can destroyed before the asynchronous read can complete.
 *
 * ### Writing
 * Writing is done in the background and we can assume then the last value will eventually be
 * written. However, we must ensure that the application is not destroyed before the write
 * completes. This is done by calling [awaitStored] before the application can be destroyed. Since
 * there are no callbacks for application destruction, we must call [awaitStored] and blocking await
 * in certain key places. These places are:
 * - [android.app.Activity.onPause] before returning from the method
 * - [android.content.BroadcastReceiver.onReceive] before returning from the method
 */
class DataStoreAlarmsRepository(
    private val logger: Logger,
    private val ioScope: CoroutineScope,
    initial: AlarmValues,
    private val dataStore: DataStore<AlarmValues>,
    override val initialized: Boolean
) : AlarmsRepository {
  companion object {
    fun createBlocking(
        datastoreDir: File,
        logger: Logger,
        ioScope: CoroutineScope
    ): AlarmsRepository {
      val initialized = datastoreDir.resolve("alarms").exists()
      val dataStore: DataStore<AlarmValues> =
          DataStoreFactory.create(
              serializer = ProtobufSerializer,
              produceFile = { datastoreDir.resolve("alarms") },
              scope = ioScope,
              corruptionHandler = ReplaceFileCorruptionHandler { AlarmValues() },
          )

      val (restoredValues, duration) =
          measureTimedValue {
            runBlocking { withTimeout(5000) { ioScope.async { dataStore.data.first() }.await() } }
          }

      logger.debug { "create() took ${duration.toInt()}ms" }

      return DataStoreAlarmsRepository(
              logger = logger,
              ioScope = ioScope,
              initial = restoredValues,
              dataStore = dataStore,
              initialized = initialized,
          )
          .also { it.launch() }
    }
  }

  private val alarmsByIdState: MutableStateFlow<Map<Int, AlarmValue>> =
      MutableStateFlow(initial.alarms)

  override fun create(): AlarmStore {
    val usedIds = alarmsByIdState.value.keys
    val id = (0..Int.MAX_VALUE).first { it !in usedIds }
    alarmsByIdState.update { alarmsById ->
      val created = id to AlarmValue(id = id)
      alarmsById.plus(created)
    }
    return createStoreView(id)
  }

  override fun query(): List<AlarmStore> {
    return alarmsByIdState.value.map { (id, _) -> createStoreView(id) }
  }

  private fun launch() {
    alarmsByIdState
        .onEach { newData ->
          val storeDuration = measureDuration {
            dataStore.updateData { prev ->
              val duration = measureDuration {
                // changed
                prev.alarms
                    .filter { (id, value) -> id in newData && value != newData[id] }
                    .forEach { (id, prevValue) ->
                      logger.debug { "changed $prevValue => ${newData[id]}" }
                    }
                // added
                newData
                    .filter { (id, value) -> id !in prev.alarms }
                    .forEach { logger.debug { "added ${it.value}" } }
                // removed
                prev.alarms
                    .filter { (id, value) -> id !in newData }
                    .forEach { logger.debug { "removed ${it.value}" } }
              }
              logger.debug { "Logging took ${duration.toInt()}ms" }
              AlarmValues(alarms = newData)
            }
          }
          logger.debug { "Store took ${storeDuration.toInt()}ms" }
        }
        .launchIn(ioScope)
  }

  override fun awaitStored() {
    val duration = measureDuration {
      runBlocking {
        withTimeout(5000) {
          dataStore.data.first { stored -> stored.alarms == alarmsByIdState.value }
        }
      }
    }
    logger.debug { "awaitStored() took ${duration.toInt()}ms" }
  }

  /**
   * Creates a view of the [alarmsByIdState] for the given id. [AlarmStore] is backed by the
   * [alarmsByIdState] value associated with this id, therefore changes to [AlarmStore] are
   * reflected in the state and are stored.
   */
  private fun createStoreView(id: Int): AlarmStore {
    return object : AlarmStore {
      override val id: Int = id
      override var value: AlarmValue
        get() {
          check(Looper.getMainLooper() == Looper.myLooper()) { "Must be called on main thread" }
          return requireNotNull(alarmsByIdState.value).getValue(id)
        }
        set(value) {
          check(Looper.getMainLooper() == Looper.myLooper()) { "Must be called on main thread" }
          alarmsByIdState.update { it.plus(id to value) }
        }

      override fun delete() {
        check(Looper.getMainLooper() == Looper.myLooper()) { "Must be called on main thread" }
        alarmsByIdState.update { it.minus(id) }
      }
    }
  }
}

/** [ProtoBuf] [AlarmValues] serializer for [DataStore]. */
@OptIn(ExperimentalSerializationApi::class)
object ProtobufSerializer : Serializer<AlarmValues> {
  override val defaultValue: AlarmValues = AlarmValues()

  override suspend fun readFrom(input: InputStream): AlarmValues {
    val bytes = input.readBytes()
    try {
      return ProtoBuf.decodeFromByteArray(bytes)
    } catch (e: Exception) {
      error("Failed to read from ${bytes.decodeToString()}")
    }
  }

  @Suppress("BlockingMethodInNonBlockingContext")
  override suspend fun writeTo(t: AlarmValues, output: OutputStream) {
    output.write(ProtoBuf.encodeToByteArray(AlarmValues.serializer(), t))
  }
}
