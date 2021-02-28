package com.better.alarm.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ConstraintLayout
import androidx.compose.foundation.layout.Dimension
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.better.alarm.R
import com.better.alarm.compose.sharedelement.SharedElement
import com.better.alarm.compose.sharedelement.SharedElementType
import com.better.alarm.compose.theme.ColoredTheme
import com.better.alarm.compose.theme.themeColors

private fun Modifier.debugBorder() = composed { debugBorder(false) }

/**
 * Create a list row from an separated params.
 *
 * Wraps with [SharedElement]
 */
@Composable
fun BoldAlarmListRow(
  hour: Int,
  minutes: Int,
  isEnabled: Boolean,
  label: String? = null,
  repeat: String? = null,
  onClick: () -> Unit,
  onOffChange: (Boolean) -> Unit,
  onTimeClick: () -> Unit,
  isDetails: Boolean,
  tag: String,
  layout: LayoutType,
) {
  SharedElement(tag = tag, type = if (isDetails) SharedElementType.TO else SharedElementType.FROM) {
    BoldAlarmListRow(hour, minutes, isEnabled, label, repeat, onClick, onOffChange, onTimeClick, layout)
  }
}

@Composable
private fun BoldAlarmListRow(
  hour: Int,
  minutes: Int,
  isEnabled: Boolean,
  label: String? = null,
  repeat: String? = null,
  onClick: () -> Unit,
  onOffChange: (Boolean) -> Unit,
  onTimeClick: () -> Unit,
  layout: LayoutType,
) {
  ConstraintLayout(
    modifier = Modifier
      .debugBorder()
      .fillMaxWidth()
      .padding(8.dp)
      .clickable(onClick = { onClick() })
  ) {
    val (labelText, timeText, weekDaysText, switch, subText) = createRefs()

    val hourStr = hour.toString().padStart(2, '0')
    val minuteStr = minutes.toString().padStart(2, '0')

    // label
    Text(
      text = if (label.isNullOrEmpty()) " " else label,
      color = MaterialTheme.colors.primary,
      style = MaterialTheme.typography.subtitle1,
      modifier = Modifier.constrainAs(labelText) {
        start.linkTo(parent.start)
        top.linkTo(parent.top)
        end.linkTo(switch.start)
        width = Dimension.fillToConstraints
      }
        .debugBorder()
    )

    Text(
      text = "$hourStr:$minuteStr",
      style = if (layout == LayoutType.Bold) {
        MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Thin)
      } else {
        MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Thin)
      },
      color = MaterialTheme.colors.onBackground,
      modifier = Modifier.constrainAs(timeText) {
        top.linkTo(labelText.bottom)
        start.linkTo(parent.start)
        width = Dimension.percent(0.5f)
      }
        .clickable(onClick = onTimeClick)
        .debugBorder()
    )

    // repeat
    Text(
      text = if (repeat.isNullOrEmpty()) " " else repeat,
      color = MaterialTheme.colors.primary,
      style = MaterialTheme.typography.subtitle2,
      modifier = Modifier
        .constrainAs(weekDaysText) {
          top.linkTo(timeText.bottom)
          start.linkTo(parent.start)
          end.linkTo(switch.start)
          bottom.linkTo(parent.bottom)
          width = Dimension.fillToConstraints
        }
        .debugBorder()
    )

    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .constrainAs(switch) {
          top.linkTo(parent.top)
          end.linkTo(parent.end)
          start.linkTo(subText.start)
          bottom.linkTo(subText.top)
          width = Dimension.percent(0.2f)
          height = Dimension.percent(0.5f)
        }
        .clickable(onClick = { onOffChange(!isEnabled) }, indication = null)
        .debugBorder()
    ) {
      Switch(
        checked = isEnabled,
        // TODO this is strange that I need this
        onCheckedChange = { if (isEnabled != it) onOffChange(it) },
        modifier = Modifier.debugBorder()
      )
    }
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.constrainAs(subText) {
        top.linkTo(switch.bottom)
        end.linkTo(parent.end)
        bottom.linkTo(parent.bottom)
        width = Dimension.percent(0.2f)
        height = Dimension.percent(0.5f)
      }
        .debugBorder()
    ) {
      LoadingVectorImage(
        id = R.drawable.ic_more_vertical,
        modifier = Modifier.debugBorder(),
        tint = MaterialTheme.colors.primary
      )
    }
  }
}

@Preview
@Composable
fun ListRowPreview() {
  ColoredTheme(colors = themeColors().first().colors) {
    Surface {
      Column(Modifier.sidePadding()) {
        BoldAlarmListRow(
          hour = 12,
          minutes = 35,
          isEnabled = true,
          label = "Label",
          onClick = { },
          onOffChange = { },
          onTimeClick = { },
          layout = LayoutType.Bold,
        )
        ListDivider()
        BoldAlarmListRow(
          hour = 14,
          minutes = 55,
          isEnabled = false,
          label = "Label",
          onClick = { },
          onOffChange = { },
          onTimeClick = { },
          layout = LayoutType.Classic,
        )
      }
    }
  }
}