package com.better.alarm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
fun setMainUnconfined() {
  Dispatchers.setMain(Dispatchers.Unconfined)
}
