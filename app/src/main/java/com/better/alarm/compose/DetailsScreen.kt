package com.better.alarm.compose

import android.media.RingtoneManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.better.alarm.R
import com.better.alarm.compose.theme.ColoredTheme
import com.better.alarm.compose.theme.themeColors
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.model.ringtoneManagerString
import java.text.DateFormatSymbols
import java.util.Calendar

private fun Modifier.debugBorder() = composed { debugBorder(false) }

@Composable
fun DetailsScreen(
  onSave: () -> Unit,
  onCancel: () -> Unit,
  alarm: AlarmValue,
  layout: LayoutType,
  editor: Editor,
  newAlarm: Boolean = false,
) {
  val showPicker = remember { mutableStateOf(newAlarm) }
  val label = remember { mutableStateOf(TextFieldValue(alarm.label)) }
  val hour = remember { mutableStateOf(alarm.hour) }
  val minutes = remember { mutableStateOf(alarm.minutes) }
  val enabled = remember { mutableStateOf(alarm.isEnabled) }
  val prealarm = remember { mutableStateOf(alarm.isPrealarm) }
  val repeat = remember { mutableStateOf(alarm.daysOfWeek) }
  val alarmtone = remember { mutableStateOf(alarm.alarmtone) }
  val saveDetails: () -> Unit = {
    editor.change(
      alarm.copy(
        hour = hour.value,
        minutes = minutes.value,
        isEnabled = enabled.value,
        label = label.value.text,
        isPrealarm = prealarm.value,
        daysOfWeek = repeat.value,
        alarmtone = alarmtone.value,
      )
    )
    onSave()
  }
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = "Edit alarm") },
        actions = {
          IconButton(onClick = saveDetails) {
            LoadingVectorImage(id = R.drawable.ic_baseline_done_24, tint = colors.secondary)
          }
        }
      )
    },
    bodyContent = {
      Column(modifier = Modifier.debugBorder()
        .sidePadding()
        .fillMaxSize()
      ) {
        BoldAlarmListRow(
          hour = hour.value,
          minutes = minutes.value,
          isEnabled = enabled.value,
          onClick = saveDetails,
          onTimeClick = { showPicker.value = true },
          onOffChange = { enabled.value = it },
          isDetails = true,
          tag = "${alarm.id}",
          layout = layout,
        )
        ListDivider()
        Crossfade(showPicker.value) { picker ->
          if (picker) {
            Picker(
              onCancel = { showPicker.value = false },
              onResult = { resHour, resMinute, _ ->
                hour.value = resHour
                minutes.value = resMinute
                enabled.value = true
                showPicker.value = false
              },
            )
          } else {
            Details(alarm, label, prealarm, repeat, alarmtone)
          }
        }
      }
    }
  )
}

@Composable
private fun Details(
  alarm: AlarmValue,
  label: MutableState<TextFieldValue>,
  prealarm: MutableState<Boolean>,
  repeat: MutableState<DaysOfWeek>,
  alarmtone: MutableState<Alarmtone>,
) {
  Column {
    RepeatRow(repeat)
    ListDivider()
    AlarmToneRow(alarmtone.value, onChange = { alarmtone.value = it })
    ListDivider()
    CheckboxWithText(prealarm)
    ListDivider()
    LabelTextField(label)
  }
}

@Composable
fun AlarmToneRow(current: Alarmtone, onChange: (Alarmtone) -> Unit) {
  val showPicker = remember { mutableStateOf(false) }
  val toneTitle = remember { mutableStateOf("") }
  val context = AmbientContext.current

  LaunchedEffect(current) {
    toneTitle.value = RingtoneManager.getRingtone(context, current.ringtoneManagerString())
      .getTitle(context)
  }

  TextWithLabel(
    toneTitle.value,
    "Ringtone",
    Modifier
      .fillMaxWidth()
      .clickable(onClick = { showPicker.value = true })
  )
  if (showPicker.value) {
    Dialog(onDismissRequest = { showPicker.value = false }) {
      Surface(modifier = Modifier.padding(vertical = 64.dp)) {
        Column {
          AlarmtonePicker(
            initial = current,
            onCancel = { showPicker.value = false },
            onResult = { alarmtone ->
              onChange(alarmtone)
              showPicker.value = false
            },
          )
        }
      }
    }
  }
}

@Composable
private fun RepeatRow(repeat: MutableState<DaysOfWeek>) {
  val showRepeatPicker = remember { mutableStateOf(false) }
  if (showRepeatPicker.value) {
    Dialog(onDismissRequest = { showRepeatPicker.value = false }) {
      Surface(modifier = Modifier.padding(vertical = 64.dp)) {
        Column {
          RepeatPicker(
            initial = repeat.value,
            onCancel = { showRepeatPicker.value = false },
            onResult = { daysOfWeek ->
              repeat.value = daysOfWeek
              showRepeatPicker.value = false
            },
          )
        }
      }
    }
  }
  TextWithLabel(
    repeat.value.toString(AmbientContext.current, true),
    "Repeat",
    Modifier.clickable(onClick = { showRepeatPicker.value = true }).fillMaxWidth()
  )
}

@Preview
@Composable
fun RepeatPickerPreview() {
  ColoredTheme(colors = themeColors().first().colors) {
    Surface {
      RepeatPicker(initial = DaysOfWeek(0x07), onCancel = { }, onResult = { })
    }
  }
}


@Composable
fun RepeatPicker(initial: DaysOfWeek, onCancel: () -> Unit, onResult: (DaysOfWeek) -> Unit) {
  val weekdays = DateFormatSymbols().weekdays
  val entries = listOf(
    weekdays[Calendar.MONDAY],
    weekdays[Calendar.TUESDAY],
    weekdays[Calendar.WEDNESDAY],
    weekdays[Calendar.THURSDAY],
    weekdays[Calendar.FRIDAY],
    weekdays[Calendar.SATURDAY],
    weekdays[Calendar.SUNDAY]
  )
  // TODO use locale for the order (Sunday/Monday)
  val state = remember { mutableStateOf(initial) }
  ScrollableColumn(Modifier.sidePadding().fillMaxHeight()) {
    entries.forEachIndexed { index, day ->
      val toggle: () -> Unit = {
        state.value = state.value.toggle(index)
      }
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
          .weight(1f)
          .clickable(onClick = toggle)
      ) {
        Text(text = day)
        Checkbox(checked = state.value.isDaySet(index), onCheckedChange = { toggle() })
      }
    }
    ListDivider()
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
      TextButton(onClick = onCancel, Modifier.fillMaxHeight().weight(1f)) {
        Text(text = "Cancel")
      }
      TextButton(onClick = { onResult(state.value) }, Modifier.fillMaxHeight().weight(1f)) {
        Text(text = "OK")
      }
    }
  }
}

private fun DaysOfWeek.toggle(index: Int): DaysOfWeek {
  return copy(coded = coded xor (1 shl index))
}

@Composable
private fun LabelTextField(label: MutableState<TextFieldValue>) {
  TextField(
    label = { Text("Label", color = colors.primary) },
    value = label.value,
    onValueChange = { label.value = it },
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 56.dp),
    backgroundColor = Color.Transparent,
  )
}

@Composable
private fun TextWithLabel(text: String, label: String, modifier: Modifier = Modifier) {
  Row(modifier.heightIn(min = 56.dp)
    .padding(top = 16.dp, bottom = 8.dp)
    // same as TextField padding
    .padding(horizontal = 16.dp)) {
    Column(verticalArrangement = Arrangement.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.caption,
        color = colors.primary
      )
      Text(
        text = text,
        style = MaterialTheme.typography.body1,
      )
    }
  }
}

@Composable
fun CheckboxWithText(checked: MutableState<Boolean>) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().clickable(onClick = {
      checked.value = !checked.value
    }),
  ) {
    val text = "Gentle alarm is ${if (checked.value) "on" else "off"}"
    TextWithLabel(text, "Prealarm", Modifier.weight(1f))
    Checkbox(
      checked = checked.value,
      onCheckedChange = { checked.value = !checked.value },
      modifier = Modifier.padding(end = 16.dp)
    )
  }
}

@Preview
@Composable
fun TextWithLabelPreview() {
  ColoredTheme(colors = themeColors().first().colors) {
    Surface {
      Column(Modifier.sidePadding()) {
        ListDivider()
        RepeatRow(repeat = remember { mutableStateOf(DaysOfWeek(0x07)) })
        ListDivider()
        TextWithLabel(
          "Happy song",
          "Ringtone",
        )
        ListDivider()
        CheckboxWithText(remember { mutableStateOf(true) })
        ListDivider()
        LabelTextField(remember { mutableStateOf(TextFieldValue("Time to code!")) })
      }
    }
  }
}
