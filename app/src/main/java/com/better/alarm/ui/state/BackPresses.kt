package com.better.alarm.ui.state

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class BackPresses {
  private var handlerStack = mutableListOf<() -> Unit>()

  fun backPressed(reason: String) {
    handlerStack.lastOrNull()?.invoke()
  }

  fun onBackPressed(lifecycle: Lifecycle, function: () -> Unit) {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
          override fun onResume(owner: LifecycleOwner) {
            handlerStack.add(function)
          }

          override fun onPause(owner: LifecycleOwner) {
            handlerStack.remove(function)
          }
        })
  }
}
