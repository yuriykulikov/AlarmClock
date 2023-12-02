package com.better.alarm.data

interface DatastoreMigration {
  fun drop()

  fun insertDefaultAlarms()

  fun migrateDatabase()
}
