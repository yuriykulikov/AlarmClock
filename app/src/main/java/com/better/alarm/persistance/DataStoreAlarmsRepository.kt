package com.better.alarm.persistance

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmStore
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.AlarmValues
import com.better.alarm.model.AlarmsRepository
import com.better.alarm.model.modify
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

/** Stores [AlarmValue]s in [DataStore]s. */
class DataStoreAlarmsRepository(
    private val datastoreDir: File,
    private val logger: Logger,
    ioDispatcher: CoroutineContext,
) : AlarmsRepository {
  val ioScope: CoroutineScope = CoroutineScope(ioDispatcher)
  private val dataStore: DataStore<AlarmValues> =
      DataStoreFactory.create(
          serializer = ProtobufSerializer,
          produceFile = { datastoreDir.resolve("alarms") },
          scope = ioScope,
          corruptionHandler = ReplaceFileCorruptionHandler { AlarmValues() },
      )

  private val stores = ConcurrentHashMap<Int, AlarmStore>()
  private val initJob: Job =
      ioScope.launch {
        val restoredValues = dataStore.data.first()
        val newStores =
            restoredValues.alarms
                .filter { it.value.state != "DeletedState" } //
                .mapValues { (id, value) -> createProtobufStore(id, value) }
        stores.putAll(newStores)
      }

  override fun create(): AlarmStore {
    val id = (0..Int.MAX_VALUE).first { it !in stores.keys }
    val store = createProtobufStore(id, AlarmValue())
    stores[store.id] = store
    store.modify { AlarmValue(id = id) }
    return store
  }

  override fun query(): List<AlarmStore> = runBlocking {
    initJob.join()
    stores.values.toList()
  }

  override val initialized: Boolean
    get() = datastoreDir.resolve("alarms").exists()

  override suspend fun awaitStored() {
    stores.forEach { (_, store) -> store.awaitStored() }
  }
  private fun createProtobufStore(id: Int, value: AlarmValue) =
      ProtobufAlarmStore(
          datastore = dataStore,
          logger = logger,
          id = id,
          initialValue = value,
          scope = ioScope,
          onDelete = { deletedId, awaitDeleted ->
            ioScope.launch {
              awaitDeleted()
              logger.debug { "Removed $deletedId from stores map" }
              stores.remove(id)
            }
          },
      )
}

/**
 * # ProtobufAlarmStore: AlarmStore
 *
 * Persistent [AlarmStore] for [AlarmValue]. Stores [AlarmValue] associated by [AlarmValue.id] (
 * [AlarmValues.alarms]) using a [DataStore].
 *
 * ## Threading
 *
 * Changing the [value] is instant, [value] is available synchronously. Access to the [value] is
 * thread-safe.
 */
private class ProtobufAlarmStore(
    private val datastore: DataStore<AlarmValues>,
    private val logger: Logger,
    override val id: Int,
    initialValue: AlarmValue,
    scope: CoroutineScope,
    private val onDelete: (Int, awaitDelete: suspend () -> Unit) -> Unit,
) : AlarmStore {
  private var cached: AtomicReference<AlarmValue> = AtomicReference(initialValue)
  override var value: AlarmValue
    get() {
      return requireNotNull(cached.get()) { "Alarm was deleted!" }
    }
    set(value) {
      requireNotNull(cached.get()) { "Alarm was deleted!" }
      cached.set(value)
      require(writes.tryEmit(value))
    }

  override fun delete() {
    if (cached.get() != null) {
      cached.set(null)
      require(writes.tryEmit(null))
      onDelete(id) { awaitStored() }
    }
  }

  override suspend fun awaitStored() {
    writtenState.first { it == cached.get() }
  }

  /** Updated when data is stored to disc. Use in tests and perhaps when not foreground. */
  private val writtenState: MutableStateFlow<AlarmValue?> = MutableStateFlow(initialValue)

  private val writes =
      MutableSharedFlow<AlarmValue?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  init {
    writes
        .distinctUntilChanged()
        .conflate()
        .map { next ->
          val updated =
              datastore.updateData { alarms ->
                val modified =
                    when {
                      next != null -> alarms.alarms.plus(id to next)
                      else -> alarms.alarms.minus(id)
                    }
                alarms.copy(alarms = modified)
              }
          if (id in updated.alarms) {
            logger.debug { "Stored $next on disc" }
          } else {
            logger.debug { "Deleted $id on disc" }
          }
          writtenState.value = next
          updated
        }
        .takeWhile { id in it.alarms }
        .launchIn(scope)
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
