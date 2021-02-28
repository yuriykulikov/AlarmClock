package com.better.alarm.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp

fun Modifier.sidePadding() = composed { padding(horizontal = 16.dp) }