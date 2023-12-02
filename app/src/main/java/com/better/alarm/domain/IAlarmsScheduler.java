/*
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.better.alarm.domain;

import com.better.alarm.data.AlarmValue;
import com.better.alarm.data.CalendarType;
import java.util.Calendar;

public interface IAlarmsScheduler {
  /** remove all Calendars of the AlarmCore with given id */
  public void removeAlarm(int id);

  /**
   * Set all from a map.
   *
   * @param id
   */
  public void setAlarm(int id, CalendarType calendarType, Calendar calendar, AlarmValue alarmValue);

  void setInexactAlarm(int id, Calendar calendar);

  void removeInexactAlarm(int id);
}
