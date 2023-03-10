package com.better.alarm.model

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import com.better.alarm.logger.DateTransformer
import com.better.alarm.logger.Logger
import com.better.alarm.logger.StringUtils
import com.better.alarm.pendingIntentUpdateCurrentFlag
import com.better.alarm.presenter.AlarmsListActivity
import java.util.*

/** Created by Yuriy on 24.06.2017. */
interface AlarmSetter {

  fun removeRTCAlarm(id: Int)

  fun setUpRTCAlarm(id: Int, calendar: Calendar)

  fun fireNow(id: Int, )

  fun removeInexactAlarm(id: Int)

  fun setInexactAlarm(id: Int, calendar: Calendar)

  class AlarmSetterImpl(
      private val am: AlarmManager,
      private val mContext: Context
  ) : AlarmSetter {
    private val setAlarmStrategy: ISetAlarmStrategy

    init {
      this.setAlarmStrategy = initSetStrategyForVersion()
    }


      override fun removeRTCAlarm(id: Int) {
      val pendingAlarm =
          PendingIntent.getBroadcast(
              mContext,
              id,
              Intent(ACTION_FIRED).apply {
                // must be here, otherwise replace does not work
                setClass(mContext, AlarmsReceiver::class.java)
              },
              pendingIntentUpdateCurrentFlag())
      sysCancel(pendingAlarm)
    }

      private fun sysCancel(pendingIntent: PendingIntent) {
          pendingIntent.cancel()
          am.cancel(pendingIntent)
      }

    override fun setUpRTCAlarm(id: Int, calendar: Calendar) {
      val pendingAlarm =
          Intent(ACTION_FIRED)
              .apply {
                setClass(mContext, AlarmsReceiver::class.java)
                putExtra(EXTRA_ID, id)
              }
              .let {
                PendingIntent.getBroadcast(
                    mContext,
                    id,
                    it, pendingIntentUpdateCurrentFlag())
              }

      setAlarmStrategy.setRTCAlarm(id, calendar, pendingAlarm)
    }

    override fun fireNow(id: Int) {
      val intent =
          Intent(ACTION_FIRED).apply {
            setClass(mContext, AlarmsReceiver::class.java)
            putExtra(EXTRA_ID, id)
          }
      mContext.sendBroadcast(intent)
    }

    override fun setInexactAlarm(id: Int, calendar: Calendar) {
      val pendingAlarm =
          Intent(ACTION_INEXACT_FIRED)
              .apply {
                setClass(mContext, AlarmsReceiver::class.java)
                putExtra(EXTRA_ID, id)
              }
              .let {
                PendingIntent.getBroadcast(mContext, id, it, pendingIntentUpdateCurrentFlag())
              }

      setAlarmStrategy.setInexactAlarm(calendar, pendingAlarm)
    }

    override fun removeInexactAlarm(id: Int) {
      val pendingAlarm =
          PendingIntent.getBroadcast(
              mContext,
              id,
              Intent(ACTION_INEXACT_FIRED).apply {
                // must be here, otherwise replace does not work
                setClass(mContext, AlarmsReceiver::class.java)
              },
              pendingIntentUpdateCurrentFlag())
      sysCancel(pendingAlarm)
    }

    private fun initSetStrategyForVersion(): ISetAlarmStrategy {
        val strat : ISetAlarmStrategy = when {
        Build.VERSION.SDK_INT >= 26 -> OreoSetter()
        Build.VERSION.SDK_INT >= 23 -> MarshmallowSetter()
        Build.VERSION.SDK_INT >= 19 -> KitKatSetter()
        else -> IceCreamSetter()
      }
        return object : ISetAlarmStrategy {
            override fun setRTCAlarm(id: Int, calendar: Calendar, pendingIntent: PendingIntent) {
                strat.setRTCAlarm(id, calendar, pendingIntent)
            }

            override fun setInexactAlarm(calendar: Calendar, pendingIntent: PendingIntent) {
                strat.setInexactAlarm(calendar, pendingIntent)
            }
        }
    }

    private inner class IceCreamSetter : ISetAlarmStrategy {
      override fun setRTCAlarm(id: Int, calendar: Calendar, pendingIntent: PendingIntent) {
        am.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }

      override fun setInexactAlarm(calendar: Calendar, pendingIntent: PendingIntent) {
        am.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private inner class KitKatSetter : ISetAlarmStrategy {
      override fun setRTCAlarm(id: Int, calendar: Calendar, pendingIntent: PendingIntent) {
        am.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }

      override fun setInexactAlarm(calendar: Calendar, pendingIntent: PendingIntent) {
        am.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }
    }

    @TargetApi(23)
    private inner class MarshmallowSetter : ISetAlarmStrategy {
      override fun setRTCAlarm(
          id: Int, calendar: Calendar, pendingIntent: PendingIntent
      ) {
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
        )
      }
    }

    /** 8.0 */
    @TargetApi(Build.VERSION_CODES.O)
    private inner class OreoSetter : ISetAlarmStrategy {
      override fun setRTCAlarm(id: Int, calendar: Calendar, pendingIntent: PendingIntent) {
          val pendingShowList =
              PendingIntent.getActivity(
                  mContext,
                  id,
//                  100500,
                  Intent(mContext, AlarmsListActivity::class.java),
                  pendingIntentUpdateCurrentFlag()
              )
          am.setAlarmClock(
              AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingShowList), pendingIntent
          )
      }

      override fun setInexactAlarm(calendar: Calendar, pendingIntent: PendingIntent) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      }
    }

      private interface ISetAlarmStrategy {
      fun setRTCAlarm(id: Int, calendar: Calendar, pendingIntent: PendingIntent)
      fun setInexactAlarm(calendar: Calendar, pendingIntent: PendingIntent) {
        setRTCAlarm(0, calendar, pendingIntent)
      }
    }

  }

  companion object {
    const val ACTION_FIRED = AlarmsScheduler.ACTION_FIRED
    const val ACTION_INEXACT_FIRED = AlarmsScheduler.ACTION_INEXACT_FIRED
    const val EXTRA_ID = AlarmsScheduler.EXTRA_ID
  }
}
