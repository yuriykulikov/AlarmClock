package com.better.alarm.compose

import android.media.RingtoneManager
import android.media.RingtoneManager.ID_COLUMN_INDEX
import android.media.RingtoneManager.TITLE_COLUMN_INDEX
import android.media.RingtoneManager.URI_COLUMN_INDEX
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.unit.dp
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.ringtoneManagerString

@Composable
fun AlarmtonePicker(
  initial: Alarmtone,
  onCancel: () -> Unit,
  onResult: (Alarmtone) -> Unit,
) {
  val selection = remember { mutableStateOf(initial) }
  val current = AmbientContext.current
  val rm = remember {
    RingtoneManager(current).apply {
      setType(RingtoneManager.TYPE_ALARM)
    }.apply { stopPreviousRingtone = true }
  }

  Column(Modifier.fillMaxSize()) {
    ScrollableColumn(Modifier.weight(1f)) {
      // TODO async load

      val cursor = rm.cursor
      generateSequence {
        if (cursor.moveToNext()) {
          val id = cursor.getInt(ID_COLUMN_INDEX)
          val uri = cursor.getString(URI_COLUMN_INDEX)
          val title = cursor.getString(TITLE_COLUMN_INDEX)
          title to Alarmtone.fromString("$uri/$id")
        } else {
          null
        }
      }.forEach { (title, tone) ->
        val onClick = {
          selection.value = tone

          rm.getRingtone(rm.getRingtonePosition(tone.ringtoneManagerString()))
            .apply {
              // isLooping TODO
            }
            .play()
        }
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(16.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
        ) {
          Text(text = title)
          RadioButton(selected = selection.value == tone, onClick = onClick)
        }
      }
    }
    ListDivider()
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      modifier = Modifier.fillMaxWidth().preferredHeight(64.dp)
    ) {
      TextButton(onClick = {
        rm.stopPreviousRingtone()
        onCancel()
      }, Modifier.weight(1f)) {
        Text(text = "Cancel")
      }
      TextButton(onClick = {
        rm.stopPreviousRingtone()
        onResult(selection.value)
      }, Modifier.weight(1f)) {
        Text(text = "OK")
      }
    }
  }
}