package com.better.alarm.model

import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Store
import com.better.alarm.logger.Logger

/** Created by Yuriy on 09.08.2017. */
class AlarmCoreFactory(
    private val logger: Logger,
    private val alarmsScheduler: IAlarmsScheduler,
    private val broadcaster: AlarmCore.IStateNotifier,
    private val prefs: Prefs,
    private val store: Store,
    private val calendars: Calendars
) {
  fun create(container: AlarmStore): AlarmCore {
    return AlarmCore(container, logger, alarmsScheduler, broadcaster, prefs, store, calendars)
  }
}
