package com.better.alarm.compose

import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Colors
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.better.alarm.R
import com.better.alarm.compose.theme.themeColors
import com.better.alarm.model.AlarmValue
import com.better.alarm.stores.RxDataStore
import io.reactivex.Observable

private fun Modifier.debugBorder() = composed { debugBorder(false) }

interface Editor {
  fun change(alarm: AlarmValue) {}
}

@Composable
fun ListScreen(
  showDetails: (AlarmValue) -> Unit,
  createNew: () -> Unit,
  editor: Editor,
  alarms: Observable<List<AlarmValue>>,
  colors: RxDataStore<String>,
  layout: MutableState<LayoutType>,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(text = "Alarms")
        },
        actions = {
          IconButton(onClick = {}) {
            LoadingVectorImage(R.drawable.ic_more_vertical, tint = MaterialTheme.colors.onSurface)
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = createNew,
        backgroundColor = MaterialTheme.colors.primaryVariant,
      ) {
        LoadingVectorImage(R.drawable.ic_fab_plus)
      }
    },
    bodyContent = {
      AlarmsList(showDetails, editor, alarms, layout.value)
    },
    bottomBar = {
      ThemeSelectionBottomBar(colors, layout)
    }
  )
}

enum class LayoutType {
  Bold, Classic;

  fun next(): LayoutType = if (this == Bold) Classic else Bold
}

@Composable
private fun ThemeSelectionBottomBar(colorsStore: RxDataStore<String>, layout: MutableState<LayoutType>) {
  BottomAppBar {
    ScrollableRow(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
      themeColors().forEach { (name, key, colors) ->
        ThemeSelector({ colorsStore.value = key }, name, colors)
      }
      // LayoutToggle({ layout.value = layout.value.next() }, layout.value)
    }
  }
}

@Composable
private fun ThemeSelector(onClick: () -> Unit, text: String, colors: Colors) {
  TextButton(onClick, modifier = Modifier
    .padding(4.dp)
    .background(
      color = colors.background,
      shape = CircleShape,
    )
  ) {
    Text(
      text,
      style = typography.button,
      color = colors.onBackground,
    )
  }
}

@Composable
fun LayoutToggle(onClick: () -> Unit, value: LayoutType) {
  TextButton(onClick, modifier = Modifier
    .padding(4.dp)
    .background(color = MaterialTheme.colors.background, shape = CircleShape)
  ) {
    Text("$value", style = typography.button, color = MaterialTheme.colors.onBackground)
  }
}

@Composable
private fun AlarmsList(
  showDetails: (AlarmValue) -> Unit,
  editor: Editor,
  alarms: Observable<List<AlarmValue>>,
  layout: LayoutType,
) {
  val data = rememberRxState(emptyList()) { alarms }
  LazyColumn(modifier = Modifier
    .fillMaxSize()
    .sidePadding()
    .debugBorder()) {
    items(data.value,
      itemContent = { alarm ->
        val showPicker = remember { mutableStateOf(false) }
        BoldAlarmListRow(
          alarm = alarm,
          onClick = { showDetails(alarm) },
          onOffChange = { editor.change(alarm.copy(isEnabled = it)) },
          onTimeClick = { showPicker.value = true },
          layout = layout,
        )
        ListDivider()
        if (showPicker.value) {
          Dialog(onDismissRequest = { showPicker.value = false }) {
            Surface(modifier = Modifier.padding(vertical = 64.dp)) {
              Column {
                Picker(
                  onCancel = { showPicker.value = false },
                  onResult = { resHour, resMinute, _ ->
                    editor.change(
                      alarm.copy(
                        hour = resHour,
                        minutes = resMinute,
                        isEnabled = true,
                      )
                    )
                    showPicker.value = false
                  },
                )
              }
            }
          }
        }
      })
  }
}

/**
 * Create a list row from an [AlarmValue]
 */
@Composable
private fun BoldAlarmListRow(
  alarm: AlarmValue,
  onClick: () -> Unit,
  onOffChange: (Boolean) -> Unit,
  onTimeClick: () -> Unit,
  layout: LayoutType,
) {
  BoldAlarmListRow(
    hour = alarm.hour,
    minutes = alarm.minutes,
    isEnabled = alarm.isEnabled,
    label = alarm.label,
    repeat = alarm.daysOfWeek.toString(AmbientContext.current, false),
    onClick = onClick,
    onOffChange = onOffChange,
    onTimeClick = onTimeClick,
    isDetails = false,
    tag = "${alarm.id}",
    layout = layout,
  )
}

@Composable
fun ListDivider() = Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))