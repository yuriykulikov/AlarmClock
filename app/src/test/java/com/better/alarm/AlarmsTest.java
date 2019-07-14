package com.better.alarm;

import android.content.ContentResolver;

import com.better.alarm.background.Event;
import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.logger.SysoutLogWriter;
import com.better.alarm.model.AlarmActiveRecord;
import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.AlarmCoreFactory;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.AlarmsScheduler;
import com.better.alarm.model.CalendarType;
import com.better.alarm.model.Calendars;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.persistance.DatabaseQuery;
import com.better.alarm.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import kotlin.collections.CollectionsKt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AlarmsTest {
    private AlarmCore.IStateNotifier stateNotifierMock;
    private AlarmSetter alarmSetterMock;
    private TestScheduler testScheduler;
    private Store store;
    private Prefs prefs;
    private Logger logger;
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("---- " + description.getMethodName() + " ----");
        }
    };


    @Before
    public void setUp() {
        testScheduler = new TestScheduler();
        logger = Logger.create().addLogWriter(new SysoutLogWriter());

        prefs = new Prefs(
                /* is24HoutFormat */ Single.just(true),
                /* preAlarmDuration */ BehaviorSubject.createDefault(10),
                /* snoozeDuration */ BehaviorSubject.createDefault(10),
                /* listRowLayout */ BehaviorSubject.createDefault("bold"),
                /* autoSilence */ BehaviorSubject.createDefault(10));

        store = new Store(
                /* alarmsSubject */ BehaviorSubject.<List<AlarmValue>>createDefault(new ArrayList<AlarmValue>()),
                /* next */ BehaviorSubject.createDefault(Optional.<Store.Next>absent()),
                /* sets */ PublishSubject.<Store.AlarmSet>create(),
                /* events */ PublishSubject.<Event>create());

        stateNotifierMock = mock(AlarmCore.IStateNotifier.class);
        alarmSetterMock = mock(AlarmSetter.class);

    }

    private Alarms createAlarms(DatabaseQuery query) {
        Calendars calendars = new Calendars() {
            @Override
            public Calendar now() {
                return Calendar.getInstance();
            }
        };
        AlarmsScheduler alarmsScheduler = new AlarmsScheduler(alarmSetterMock, logger, store, prefs, calendars);
        Alarms alarms = new Alarms(alarmsScheduler, query, new AlarmCoreFactory(logger,
                alarmsScheduler,
                stateNotifierMock,
                new TestHandlerFactory(testScheduler),
                prefs,
                store,
                calendars
        ),
                new TestContainerFactory(calendars),
                logger);
        return alarms;
    }

    private Alarms createAlarms() {
        return createAlarms(mockQuery());
    }

    @android.support.annotation.NonNull
    private DatabaseQuery mockQuery() {
        final DatabaseQuery query = mock(DatabaseQuery.class);
        List<AlarmActiveRecord> list = new ArrayList<>();
        when(query.query()).thenReturn(Single.just(list));
        return query;
    }

    @Test
    public void create() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1 && alarmValues.get(0).isEnabled();
            }
        });
    }

    @Test
    public void deleteDisabledAlarm() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        testScheduler.triggerActions();
        instance.delete(newAlarm);
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 0;
            }
        });
    }

    @Test
    public void deleteEnabledAlarm() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        testScheduler.triggerActions();
        newAlarm.enable(true);
        testScheduler.triggerActions();
        instance.getAlarm(0).delete();
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 0;
            }
        });
    }

    @Test
    public void createThreeAlarms() {
        //when
        IAlarmsManager instance = createAlarms();
        instance.createNewAlarm();
        testScheduler.triggerActions();
        instance.createNewAlarm().enable(true);
        testScheduler.triggerActions();
        instance.createNewAlarm();
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValueAt(0, new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                System.out.println(alarmValues);
                return alarmValues.size() == 3
                        && !alarmValues.get(0).isEnabled()
                        && alarmValues.get(1).isEnabled()
                        && !alarmValues.get(2).isEnabled();
            }
        });
    }

    static class DatabaseQueryMock extends DatabaseQuery {
        private ContainerFactory factory;

        public DatabaseQueryMock(ContainerFactory factory) {
            super(mock(ContentResolver.class), factory);
            this.factory = factory;
        }

        @Override
        public Single<List<AlarmActiveRecord>> query() {
            AlarmActiveRecord container =
                    factory.create()
                            .withId(100500)
                            .withIsEnabled(true)
                            .withLabel("hello");

            List<AlarmActiveRecord> item = CollectionsKt.arrayListOf(container);
            return Single.just(item);
        }
    }

    @Test
    public void alarmsFromMemoryMustBePresentInTheList() {
        //when
        Alarms instance = createAlarms(new DatabaseQueryMock(new TestContainerFactory(new Calendars() {
            @Override
            public Calendar now() {
                return Calendar.getInstance();
            }
        })));

        instance.start();

        //verify
        store.alarms().test()
                .assertValue(new Predicate<List<AlarmValue>>() {
                    @Override
                    public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                        System.out.println(alarmValues);
                        return alarmValues.size() == 1
                                && alarmValues.get(0).isEnabled()
                                && alarmValues.get(0).getLabel().equals("hello");
                    }
                });
    }

    @Test
    public void editAlarm() {
        //when
        IAlarmsManager instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit().withIsEnabled(true).withHour(7).commit();
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && alarmValues.get(0).isEnabled()
                        && alarmValues.get(0).getHour() == 7;
            }
        });
    }

    @Test
    public void firedAlarmShouldBeDisabledIfNoRepeatingIsSet() {
        //when
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.dismiss();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && !alarmValues.get(0).isEnabled();
            }
        });
    }

    @Test
    public void firedAlarmShouldBeRescheduledIfRepeatingIsSet() {
        //when
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit().withIsEnabled(true).withDaysOfWeek(new DaysOfWeek(1)).commit();
        testScheduler.triggerActions();

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.dismiss();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && alarmValues.get(0).isEnabled();
            }
        });
    }

    @Test
    public void changingAlarmWhileItIsFiredShouldReschedule() {
        //when
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.edit().withDaysOfWeek(new DaysOfWeek(1)).withIsPrealarm(true).commit();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 1
                        && alarmValues.get(0).isEnabled();
            }
        });

        verify(alarmSetterMock, atLeastOnce()).setUpRTCAlarm(eq(newAlarm.getId()), eq("NORMAL"), any(Calendar.class));
    }


    @Test
    public void firedAlarmShouldBeStillEnabledAfterSnoozed() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(new DaysOfWeek(1)).withIsPrealarm(true).commit();
        testScheduler.triggerActions();
        //TODO verify

        //when pre-alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        //when pre-alarm-snoozed
        newAlarm.snooze();
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_SNOOZE_ACTION));

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        //when alarm is snoozed
        newAlarm.snooze();
        testScheduler.triggerActions();
        verify(stateNotifierMock, times(2)).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));

        newAlarm.delete();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ACTION_CANCEL_SNOOZE));
    }


    @Test
    public void snoozeToTime() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(new DaysOfWeek(1)).commit();
        testScheduler.triggerActions();
        //TODO verify

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        //when pre-alarm-snoozed
        newAlarm.snooze(23, 59);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_SNOOZE_ACTION));
    }

    @Test
    public void snoozePreAlarmToTime() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(new DaysOfWeek(1)).withIsPrealarm(true).commit();
        testScheduler.triggerActions();
        //TODO verify

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        //when pre-alarm-snoozed
        newAlarm.snooze(23, 59);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_SNOOZE_ACTION));

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.SNOOZE);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));
    }


    @Test
    public void prealarmTimedOutAndThenDisabled() {
        //given
        Alarms instance = createAlarms();
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(new DaysOfWeek(1)).withIsPrealarm(true).commit();
        testScheduler.triggerActions();
        //TODO verify

        //when alarm fired
        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.PREALARM);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_PREALARM_ACTION));

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        //when pre-alarm-snoozed
        newAlarm.enable(false);
        testScheduler.triggerActions();
        verify(stateNotifierMock, atLeastOnce()).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_DISMISS_ACTION));
    }
}