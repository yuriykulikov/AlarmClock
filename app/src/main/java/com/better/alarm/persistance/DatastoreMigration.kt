package com.better.alarm.persistance

interface DatastoreMigration {
  fun drop()
  fun insertDefaultAlarms()
  fun migrateDatabase()
}
