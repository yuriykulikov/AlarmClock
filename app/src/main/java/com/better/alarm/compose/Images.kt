package com.better.alarm.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.loadVectorResource

@Composable
fun LoadingVectorImage(id: Int, modifier: Modifier = Modifier, tint: Color? = null) {
  loadVectorResource(id = id)
    .resource.resource?.let { vector ->
      val colorFilter = tint?.let { ColorFilter.tint(it) }
      Image(imageVector = vector, modifier = modifier, colorFilter = colorFilter)
    }
}