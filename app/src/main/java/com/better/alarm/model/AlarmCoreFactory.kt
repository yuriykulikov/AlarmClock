package com.better.alarm.model

import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Store
import com.better.alarm.logger.Logger
import com.better.alarm.statemachine.HandlerFactory

/**
 * Created by Yuriy on 09.08.2017.
 */

class AlarmCoreFactory(
        private val logger: Logger,
        private val alarmsScheduler: IAlarmsScheduler,
        private val broadcaster: AlarmCore.IStateNotifier,
        private val handlerFactory: HandlerFactory,
        private val prefs: Prefs,
        private val store: Store,
        private val calendars: Calendars
) {
    fun create(container: AlarmActiveRecord): AlarmCore {
        return AlarmCore(container, logger, alarmsScheduler, broadcaster, handlerFactory, prefs, store, calendars)
    }
}
