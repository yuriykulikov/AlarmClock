package com.better.alarm.domain.statemachine

abstract class ComplexTransition<T> : State<T>() {
  abstract fun performComplexTransition()

  override fun enter(reason: T?) {
    performComplexTransition()
  }

  override fun onEvent(event: T): Boolean {
    throw RuntimeException("performComplexTransition() must transit immediately")
  }

  override fun exit(reason: T?) {
    // nothing to do
  }
}
