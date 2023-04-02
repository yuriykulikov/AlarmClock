package com.better.alarm.persistence

import com.better.alarm.logger.Logger
import com.better.alarm.model.modify
import com.better.alarm.persistance.DataStoreAlarmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.junit.Test

class DataStoreAlarmsRepositoryTest {
  private val datastoreDir = Files.newTemporaryFolder().apply { deleteOnExit() }

  @Test
  fun `changes are cached`() {
    val repository =
        DataStoreAlarmsRepository(
            datastoreDir = datastoreDir, logger = logger(), ioDispatcher = Dispatchers.IO)
    repository.create().run {
      modify { copy(hour = 20) }
      modify { copy(minutes = 30) }

      assertThat(value.hour).isEqualTo(20)
      assertThat(value.minutes).isEqualTo(30)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `changes are written to file`() =
      runBlocking<Unit> {
        val scheduler = Dispatchers.IO
        val firstRepository = DataStoreAlarmsRepository(datastoreDir, logger(), scheduler)
        val alarmStore = firstRepository.create()
        // when
        alarmStore.modify { copy(hour = 20) }
        alarmStore.modify { copy(minutes = 30) }
        firstRepository.awaitStored()
        firstRepository.ioScope.cancel()
        firstRepository.ioScope.coroutineContext.job.join()

        // then
        DataStoreAlarmsRepository(datastoreDir, logger(), scheduler).query().run {
          assertThat(first().value.hour).isEqualTo(20)
          assertThat(first().value.minutes).isEqualTo(30)
        }
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `new alarms are deleted from the file when delete is called`() =
      runBlocking<Unit> {
        val scheduler = Dispatchers.IO
        with(DataStoreAlarmsRepository(datastoreDir, logger(), scheduler)) {
          create()
          val store = create()
          awaitStored()
          store.delete()
          awaitStored()
          cancelAndJoin(ioScope)
        }

        // then
        DataStoreAlarmsRepository(datastoreDir, logger(), scheduler).query().run {
          assertThat(this).hasSize(1)
          assertThat(first().value.hour).isEqualTo(0)
          assertThat(first().value.minutes).isEqualTo(0)
        }
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `alarms are written to file when create is called`() =
      runBlocking<Unit> {
        val scheduler = Dispatchers.IO
        with(DataStoreAlarmsRepository(datastoreDir, logger(), scheduler)) {
          create()
          // when
          awaitStored()
          cancelAndJoin(ioScope)
        }

        // then
        DataStoreAlarmsRepository(datastoreDir, logger(), scheduler).query().run {
          assertThat(first().value.hour).isEqualTo(0)
          assertThat(first().value.minutes).isEqualTo(0)
        }
      }

  private fun logger(): Logger {
    return Logger.create()
  }
}

private suspend fun cancelAndJoin(scope: CoroutineScope) {
  scope.cancel()
  scope.coroutineContext.job.join()
}
