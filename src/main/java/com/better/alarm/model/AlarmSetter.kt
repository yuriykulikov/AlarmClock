package com.better.alarm.model

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.better.alarm.logger.Logger

/**
 * Created by Yuriy on 24.06.2017.
 */

interface AlarmSetter {

    fun removeRTCAlarm()

    fun setUpRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm)

    fun fireNow(firedInThePastAlarm: AlarmsScheduler.ScheduledAlarm)

    class AlarmSetterImpl(private val log: Logger, private val am: AlarmManager, private val mContext: Context) : AlarmSetter {
        private val setAlarmStrategy: ISetAlarmStrategy

        init {
            this.setAlarmStrategy = initSetStrategyForVersion()
            log.d("Using $setAlarmStrategy")
        }

        override fun removeRTCAlarm() {
            val pendingIntent = PendingIntent.getBroadcast(mContext, 0, Intent(ACTION_FIRED), 0)
            am.cancel(pendingIntent)
        }

        override fun setUpRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm) {
            log.d("Set " + alarm.toString())
            val pendingIntent = Intent(ACTION_FIRED)
                    .apply {
                        putExtra(EXTRA_ID, alarm.id)
                        putExtra(EXTRA_TYPE, alarm.type!!.name)
                    }
                    .let { PendingIntent.getBroadcast(mContext, 0, it, PendingIntent.FLAG_UPDATE_CURRENT) }

            setAlarmStrategy.setRTCAlarm(alarm, pendingIntent)
        }

        override fun fireNow(firedInThePastAlarm: AlarmsScheduler.ScheduledAlarm) {
            val intent = Intent(ACTION_FIRED).apply {
                putExtra(EXTRA_ID, firedInThePastAlarm.id)
                putExtra(EXTRA_TYPE, firedInThePastAlarm.type!!.name)
            }
            mContext.sendBroadcast(intent)
        }

        private fun initSetStrategyForVersion(): ISetAlarmStrategy {
            log.d("SDK is " + android.os.Build.VERSION.SDK_INT)
            return when {
                android.os.Build.VERSION.SDK_INT >= 23 -> MarshmallowSetter()
                android.os.Build.VERSION.SDK_INT >= 19 -> KitKatSetter()
                else -> IceCreamSetter()
            }
        }

        private inner class IceCreamSetter : ISetAlarmStrategy {
            override fun setRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm, pendingIntent: PendingIntent) {
                am.set(AlarmManager.RTC_WAKEUP, alarm.calendar!!.timeInMillis, pendingIntent)
            }
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        private inner class KitKatSetter : ISetAlarmStrategy {
            override fun setRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm, pendingIntent: PendingIntent) {
                am.setExact(AlarmManager.RTC_WAKEUP, alarm.calendar!!.timeInMillis, pendingIntent)
            }
        }

        @TargetApi(23)
        private inner class MarshmallowSetter : ISetAlarmStrategy {
            val setExactAndAllowWhileIdle: (Long, PendingIntent) -> Unit = try {
                am.javaClass.getMethod(
                        "setExactAndAllowWhileIdle",
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType,
                        PendingIntent::class.java
                ).let {
                    { timeInMillis: Long, pendingIntent: PendingIntent -> it.invoke(am, AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent) }
                }
            } catch (e: ReflectiveOperationException) {
                { timeInMillis, pendingIntent -> am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent) }
            }

            override fun setRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm, pendingIntent: PendingIntent) {
                setExactAndAllowWhileIdle(alarm.calendar!!.timeInMillis, pendingIntent)
            }
        }

        private interface ISetAlarmStrategy {
            fun setRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm, pendingIntent: PendingIntent)
        }
    }

    companion object {
        const val ACTION_FIRED = AlarmsScheduler.ACTION_FIRED
        const val EXTRA_ID = AlarmsScheduler.EXTRA_ID
        const val EXTRA_TYPE = AlarmsScheduler.EXTRA_TYPE
    }
}
