package com.better.alarm.compose

import androidx.compose.animation.Crossfade
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.better.alarm.compose.sharedelement.SharedElementsRoot
import com.better.alarm.compose.theme.ColoredTheme
import com.better.alarm.compose.theme.toColors
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.stores.RxDataStore
import io.reactivex.Observable
import java.util.Calendar

sealed class Screen {
  object List : Screen()
  object New : Screen()
  data class Details(val alarmValue: AlarmValue) : Screen()
}

@Composable
fun AlarmsApp(
  backs: Backs,
  editor: Editor,
  alarms: Observable<List<AlarmValue>>,
  themeStore: RxDataStore<String>,
) {
  val colors = rememberRxStateBlocking { themeStore.observe().map { it.toColors() } }

  ColoredTheme(colors.value) {
    SharedElementsRoot {
      AppContent(editor, alarms, backs, themeStore)
    }
  }
}

@Composable
private fun AppContent(
  editor: Editor,
  alarms: Observable<List<AlarmValue>>,
  backs: Backs,
  colors: RxDataStore<String>,
) {
  val currentScreen = remember { mutableStateOf<Screen>(Screen.List) }
  val mutLayout = remember { mutableStateOf(LayoutType.Bold) }
  // TODO use it in screens
  backs.backPressed.CommitSubscribe { currentScreen.value = Screen.List }

  Crossfade(mutLayout.value) { layout ->
    Crossfade(currentScreen.value) { screen ->
      Surface(color = MaterialTheme.colors.background) {
        when (screen) {
          is Screen.List -> ListScreen(
            showDetails = { currentScreen.value = Screen.Details(it) },
            createNew = { currentScreen.value = Screen.New },
            editor = editor,
            alarms = alarms,
            colors = colors,
            layout = mutLayout,
          )
          is Screen.Details -> DetailsScreen(
            onSave = { currentScreen.value = Screen.List },
            onCancel = { currentScreen.value = Screen.List },
            alarm = screen.alarmValue,
            layout = layout,
            editor = editor,
          )
          is Screen.New -> DetailsScreen(
            onSave = { currentScreen.value = Screen.List },
            onCancel = { currentScreen.value = Screen.List },
            alarm = createAlarmData(),
            layout = layout,
            editor = editor,
          )
        }
      }
    }
  }
}

@Composable
private fun createAlarmData(): AlarmValue {
  return AlarmValue(
    id = -1,
    isEnabled = true,
    hour = 0,
    minutes = 0,
    daysOfWeek = DaysOfWeek(0),
    isVibrate = true,
    isPrealarm = false,
    label = "",
    alarmtone = Alarmtone.Default(),
    state = "",
    nextTime = Calendar.getInstance()
  )
}
