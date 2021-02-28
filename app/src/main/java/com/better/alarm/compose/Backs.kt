package com.better.alarm.compose

import io.reactivex.subjects.PublishSubject

class Backs {
  val backPressed = PublishSubject.create<String>()
}