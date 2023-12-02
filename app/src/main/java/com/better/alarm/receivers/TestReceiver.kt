package com.better.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.better.alarm.bootstrap.globalInject
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.DatastoreMigration
import com.better.alarm.logger.Logger

class TestReceiver : BroadcastReceiver() {
  private val log: Logger by globalLogger("TestReceiver")
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
    intent.getStringExtra("CB")?.let { cbAction -> context?.sendBroadcast(Intent(cbAction)) }
  }

  companion object {
    const val ACTION_DROP = com.better.alarm.BuildConfig.APPLICATION_ID + ".ACTION_DROP"
    const val ACTION_DROP_AND_INSERT_DEFAULTS =
        com.better.alarm.BuildConfig.APPLICATION_ID + ".ACTION_DROP_AND_INSERT_DEFAULTS"
    const val ACTION_DROP_AND_MIGRATE_DATABASE =
        com.better.alarm.BuildConfig.APPLICATION_ID + ".ACTION_DROP_AND_MIGRATE_DATABASE"
  }
}
