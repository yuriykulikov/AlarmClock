package com.better.alarm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain

fun setMainUnconfined() {
  Dispatchers.setMain(Dispatchers.Unconfined)
}
