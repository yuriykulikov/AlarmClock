package com.better.alarm.model

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.better.alarm.logger.Logger
import com.better.alarm.presenter.AlarmsListActivity

/**
 * Created by Yuriy on 24.06.2017.
 */

interface AlarmSetter {

    fun removeRTCAlarm(previousHead: AlarmsScheduler.ScheduledAlarm)

    fun setUpRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm)

    fun fireNow(firedInThePastAlarm: AlarmsScheduler.ScheduledAlarm)

    class AlarmSetterImpl(private val log: Logger, private val am: AlarmManager, private val mContext: Context) : AlarmSetter {
        private val setAlarmStrategy: ISetAlarmStrategy

        init {
            this.setAlarmStrategy = initSetStrategyForVersion()
        }

        private val requestCode = 42

        override fun removeRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm) {
            val pendingIntent = Intent(ACTION_FIRED)
                    .apply {
                        setClass(mContext, AlarmsReceiver::class.java)
                        putExtra(EXTRA_ID, alarm.id)
                        putExtra(EXTRA_TYPE, alarm.type!!.name)
                    }
                    .let { PendingIntent.getBroadcast(mContext, requestCode, it, PendingIntent.FLAG_UPDATE_CURRENT) }

            am.cancel(pendingIntent)
        }

        override fun setUpRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm) {
            log.d("Set $alarm")
            val pendingIntent = Intent(ACTION_FIRED)
                    .apply {
                        setClass(mContext, AlarmsReceiver::class.java)
                        putExtra(EXTRA_ID, alarm.id)
                        putExtra(EXTRA_TYPE, alarm.type!!.name)
                    }
                    .let { PendingIntent.getBroadcast(mContext, requestCode, it, PendingIntent.FLAG_UPDATE_CURRENT) }

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
            log.d("SDK is " + Build.VERSION.SDK_INT)
            return when {
                Build.VERSION.SDK_INT >= 26 -> OreoSetter()
                Build.VERSION.SDK_INT >= 23 -> MarshmallowSetter()
                Build.VERSION.SDK_INT >= 19 -> KitKatSetter()
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
            override fun setRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm, pendingIntent: PendingIntent) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.calendar!!.timeInMillis, pendingIntent)
            }
        }

        /** 8.0  */
        @TargetApi(Build.VERSION_CODES.O)
        private inner class OreoSetter : ISetAlarmStrategy {
            override fun setRTCAlarm(alarm: AlarmsScheduler.ScheduledAlarm, sender: PendingIntent) {
                val showList = Intent(mContext, AlarmsListActivity::class.java)
                        .apply {
                            putExtra(EXTRA_ID, alarm.id)
                        }
                val showIntent = PendingIntent.getActivity(mContext, hashCode(), showList, PendingIntent.FLAG_UPDATE_CURRENT)
                am.setAlarmClock(AlarmManager.AlarmClockInfo(alarm.calendar!!.timeInMillis, showIntent), sender)
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
