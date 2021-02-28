package com.better.alarm.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.better.alarm.R
import com.better.alarm.compose.theme.ColoredTheme
import com.better.alarm.compose.theme.themeColors
import com.better.alarm.view.TimePickerPresenter

private val ONE = TimePickerPresenter.Key.ONE
private val TWO = TimePickerPresenter.Key.TWO
private val THREE = TimePickerPresenter.Key.THREE
private val FOUR = TimePickerPresenter.Key.FOUR
private val FIVE = TimePickerPresenter.Key.FIVE
private val SIX = TimePickerPresenter.Key.SIX
private val EIGHT = TimePickerPresenter.Key.EIGHT
private val NINE = TimePickerPresenter.Key.NINE
private val SEVEN = TimePickerPresenter.Key.SEVEN
private val ZERO = TimePickerPresenter.Key.ZERO
private val OK = TimePickerPresenter.Key.OK
private val LEFT = TimePickerPresenter.Key.LEFT
private val RIGHT = TimePickerPresenter.Key.RIGHT
private val DELETE = TimePickerPresenter.Key.DELETE

fun TimePickerPresenter.State.asString(): String {
  val reversed = input.reversed()
  val i3 = reversed.getOrElse(3) { "-" }
  val i2 = reversed.getOrElse(2) { "-" }
  val i1 = reversed.getOrElse(1) { "-" }
  val i0 = reversed.getOrElse(0) { "-" }
  return "$i3$i2:$i1$i0"
}

@Preview
@Composable
fun PickerPreview() {
  ColoredTheme(colors = themeColors().first().colors) {
    Surface {
      Picker({}, { _, _, _ -> })
    }
  }
}

@Composable
fun Picker(
  onCancel: () -> Unit,
  onResult: (hour: Int, minute: Int, amPm: TimePickerPresenter.AmPm) -> Unit,
) {
  val presenter = remember { TimePickerPresenter(true) }

  @Composable
  fun enabled(key: TimePickerPresenter.Key): State<Boolean> {
    return rememberRxStateBlocking {
      presenter.state.map { it.enabled.contains(key) }
    }
  }

  val state: State<String> = rememberRxState(initial = "--:--") {
    presenter.state.map { it.asString() }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().weight(1f)
    ) {

      Row(
        modifier = Modifier.weight(2f).fillMaxHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = state.value,
          style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.Thin),
          textAlign = TextAlign.Center
        )
      }

      IconButton(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        onClick = { presenter.onClick(DELETE) },
      ) {
        LoadingVectorImage(
          id = R.drawable.ic_baseline_backspace_24,
          tint = colors.primary,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
      PickerButton("1", enabled(ONE).value, onClick = { presenter.onClick(ONE) })
      PickerButton("2", enabled(TWO).value, onClick = { presenter.onClick(TWO) })
      PickerButton("3", enabled(THREE).value, onClick = { presenter.onClick(THREE) })
    }
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
      PickerButton("4", enabled(FOUR).value, onClick = { presenter.onClick(FOUR) })
      PickerButton("5", enabled(FIVE).value, onClick = { presenter.onClick(FIVE) })
      PickerButton("6", enabled(SIX).value, onClick = { presenter.onClick(SIX) })
    }
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
      PickerButton("7", enabled(SEVEN).value, onClick = { presenter.onClick(SEVEN) })
      PickerButton("8", enabled(EIGHT).value, onClick = { presenter.onClick(EIGHT) })
      PickerButton("9", enabled(NINE).value, onClick = { presenter.onClick(NINE) })
    }
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
      PickerButton(":00", enabled(LEFT).value, onClick = { presenter.onClick(LEFT) })
      PickerButton("0", enabled(ZERO).value, onClick = { presenter.onClick(ZERO) })
      PickerButton(":30", enabled(RIGHT).value, onClick = { presenter.onClick(RIGHT) })
    }
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
      TextButton(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        onClick = onCancel
      ) {
        Text("Cancel")
      }
      TextButton(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        enabled = enabled(OK).value,
        onClick = {
          with(presenter.snapshot) {
            onResult(hours, minutes, amPm)
          }
        }) {
        Text("Ok")
      }
    }
  }
}

@Composable
private fun RowScope.PickerButton(
  text: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  TextButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.weight(1f).fillMaxHeight(),
  ) {
    Text(
      textAlign = TextAlign.Center,
      text = text,
      style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Thin),
    )
  }
}
