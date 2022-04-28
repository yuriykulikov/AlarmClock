package com.better.alarm.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.better.alarm.configuration.globalInject
import com.better.alarm.configuration.globalLogger
import com.better.alarm.logger.Logger
import com.better.alarm.persistance.DatastoreMigration

class TestReceiver : BroadcastReceiver() {
  private val log: Logger by globalLogger("AlarmsReceiver")
  private val migration: DatastoreMigration by globalInject()

  override fun onReceive(context: Context?, intent: Intent?) {
    log.debug { intent?.action.orEmpty() }
    when (intent?.action) {
      ACTION_DROP -> {
        migration.drop()
      }
      ACTION_DROP_AND_INSERT_DEFAULTS -> {
        migration.drop()
        migration.insertDefaultAlarms()
      }
      ACTION_DROP_AND_MIGRATE_DATABASE -> {
        migration.drop()
        migration.migrateDatabase()
      }
      else -> error("Unexpected $intent")
    }
  }

  companion object {
    const val ACTION_DROP = com.better.alarm.BuildConfig.APPLICATION_ID + ".ACTION_DROP"
    const val ACTION_DROP_AND_INSERT_DEFAULTS =
        com.better.alarm.BuildConfig.APPLICATION_ID + ".ACTION_DROP_AND_INSERT_DEFAULTS"
    const val ACTION_DROP_AND_MIGRATE_DATABASE =
        com.better.alarm.BuildConfig.APPLICATION_ID + ".ACTION_DROP_AND_MIGRATE_DATABASE"
  }
}
