package com.better.alarm.compose

import android.media.RingtoneManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.Checkbox
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.better.alarm.R
import com.better.alarm.compose.theme.ColoredTheme
import com.better.alarm.compose.theme.themeColors
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.model.ringtoneManagerString
import kotlinx.coroutines.launch

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
  val saveDetails: () -> Unit = {
    editor.change(
      alarm.copy(
        hour = hour.value,
        minutes = minutes.value,
        isEnabled = enabled.value,
        label = label.value.text,
        isPrealarm = prealarm.value,
        daysOfWeek = repeat.value,
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
            Details(alarm, label, prealarm, repeat)
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
  repeat: MutableState<DaysOfWeek>
) {
  Column {
    TextWithLabel(
      repeat.value.toString(AmbientContext.current, false),
      "Repeat",
      Modifier.clickable(onClick = { /* showDialog.value = true */ })
    )
    ListDivider()
    // RepeatButtons(repeat.value, Modifier.padding(16.dp).fillMaxWidth())
    // ListDivider()
    val ringtone = remember { mutableStateOf("") }
    val context = AmbientContext.current
    rememberCoroutineScope().launch {
      ringtone.value = RingtoneManager.getRingtone(context, alarm.alarmtone.ringtoneManagerString())
        .getTitle(context)
    }
    TextWithLabel(
      ringtone.value,
      "Ringtone",
      Modifier.clickable(onClick = { /* showDialog.value = true */ })
    )
    ListDivider()
    CheckboxWithText(prealarm)
    ListDivider()
    LabelTextField(label)
  }
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
  Row {
    val text = "Gentle alarm is ${if (checked.value) "on" else "off"}"
    TextWithLabel(text, "Prealarm", Modifier.weight(1f))
    Checkbox(
      checked = checked.value,
      onCheckedChange = { checked.value = !checked.value },
      modifier = Modifier.preferredHeight(56.dp).padding(16.dp),
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
        TextWithLabel(
          "Mo, Tue, Wed, Sun",
          "Repeat",
        )
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
