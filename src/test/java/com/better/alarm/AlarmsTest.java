package com.better.alarm;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;

import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.logger.SysoutLogWriter;
import com.better.alarm.model.AlarmContainer;
import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.AlarmsScheduler;
import com.better.alarm.model.CalendarType;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.ImmutableAlarmContainer;
import com.better.alarm.model.ImmutableDaysOfWeek;
import com.better.alarm.persistance.DatabaseQuery;
import com.better.alarm.statemachine.HandlerFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AlarmsTest {
    private Injector guice;
    private AlarmCore.IStateNotifier stateNotifierMock;
    private AlarmSetter alarmSetterMock;
    private TestScheduler testScheduler;
    private ImmutableStore store;
    private ImmutablePrefs prefs;
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

        prefs = ImmutablePrefs.builder()
                .preAlarmDuration(BehaviorSubject.createDefault(10))
                .snoozeDuration(BehaviorSubject.createDefault(10))
                .autoSilence(BehaviorSubject.createDefault(10))
                .is24HoutFormat(Single.just(true))
                .build();

        store = ImmutableStore.builder()
                .alarmsSubject(BehaviorSubject.<List<AlarmValue>>createDefault(new ArrayList<AlarmValue>()))
                .next(BehaviorSubject.createDefault(Optional.<Store.Next>absent()))
                .sets(PublishSubject.<Store.AlarmSet>create())
                .build();

        stateNotifierMock = mock(AlarmCore.IStateNotifier.class);
        alarmSetterMock = mock(AlarmSetter.class);

        guice = Guice.createInjector(Modules
                .override(new AlarmApplication.AppModule(logger, prefs, store))
                .with(new TestModule()));
    }

    private class TestModule implements Module {
        @Override
        public void configure(Binder binder) {
            //stubs
            binder.bind(ContainerFactory.class).to(TestContainerFactory.class).asEagerSingleton();
            binder.bind(Scheduler.class).toInstance(testScheduler);
            binder.bind(HandlerFactory.class).to(TestHandlerFactory.class);
            binder.bind(Context.class).toInstance(mock(Context.class));
            binder.bind(DatabaseQuery.class).toInstance(mockQuery());
            binder.bind(AlarmSetter.class).toInstance(alarmSetterMock);
            binder.bind(PowerManager.class).toInstance(mock(PowerManager.class));

            //mocks for verification
            binder.bind(AlarmCore.IStateNotifier.class).toInstance(stateNotifierMock);
        }
    }

    @android.support.annotation.NonNull
    private DatabaseQuery mockQuery() {
        final DatabaseQuery query = mock(DatabaseQuery.class);
        List<AlarmContainer> list = Lists.newArrayList();
        when(query.query()).thenReturn(Single.just(list));
        return query;
    }

    @Test
    public void create() {
        //when
        IAlarmsManager instance = guice.getInstance(IAlarmsManager.class);
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
        IAlarmsManager instance = guice.getInstance(IAlarmsManager.class);
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
        IAlarmsManager instance = guice.getInstance(IAlarmsManager.class);
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
        IAlarmsManager instance = guice.getInstance(IAlarmsManager.class);
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

        @Inject
        public DatabaseQueryMock(ContainerFactory factory) {
            super(mock(ContentResolver.class), factory);
            this.factory = factory;
        }

        @Override
        public Single<List<AlarmContainer>> query() {
            AlarmContainer container =
                    ImmutableAlarmContainer.copyOf(factory.create())
                            .withId(100500)
                            .withIsEnabled(true)
                            .withLabel("hello");

            List<AlarmContainer> item = Lists.newArrayList(container);
            return Single.just(item);
        }
    }

    @Test
    public void alarmsFromMemoryMustBePresentInTheList() {
        //when
        Alarms instance = Guice.createInjector(Modules
                .override(Modules
                        .override(new AlarmApplication.AppModule(logger, prefs, store))
                        .with(new TestModule()))
                .with(new Module() {
                    @Override
                    public void configure(Binder binder) {
                        binder.bind(DatabaseQuery.class).to(DatabaseQueryMock.class);
                    }
                }))
                .getInstance(Alarms.class);

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
        IAlarmsManager instance = guice.getInstance(IAlarmsManager.class);
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
        Alarms instance = guice.getInstance(Alarms.class);
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
        Alarms instance = guice.getInstance(Alarms.class);
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.edit().withIsEnabled(true).withDaysOfWeek(ImmutableDaysOfWeek.of(1)).commit();
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
        Alarms instance = guice.getInstance(Alarms.class);
        Alarm newAlarm = instance.createNewAlarm();
        newAlarm.enable(true);
        testScheduler.triggerActions();

        instance.onAlarmFired((AlarmCore) newAlarm, CalendarType.NORMAL);
        testScheduler.triggerActions();
        verify(stateNotifierMock).broadcastAlarmState(eq(newAlarm.getId()), eq(Intents.ALARM_ALERT_ACTION));

        newAlarm.edit().withDaysOfWeek(ImmutableDaysOfWeek.of(1)).withIsPrealarm(true).commit();
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

        ArgumentCaptor<AlarmsScheduler.ScheduledAlarm> captor = ArgumentCaptor.forClass(AlarmsScheduler.ScheduledAlarm.class);
        verify(alarmSetterMock, atLeastOnce()).setUpRTCAlarm(captor.capture());

        assertEquals(newAlarm.getId(), captor.getValue().id);
    }


    @Test
    public void firedAlarmShouldBeStillEnabledAfterSnoozed() {
        //given
        Alarms instance = guice.getInstance(Alarms.class);
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(ImmutableDaysOfWeek.of(1)).withIsPrealarm(true).commit();
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
        Alarms instance = guice.getInstance(Alarms.class);
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(ImmutableDaysOfWeek.of(1)).commit();
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
        Alarms instance = guice.getInstance(Alarms.class);
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(ImmutableDaysOfWeek.of(1)).withIsPrealarm(true).commit();
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
        Alarms instance = guice.getInstance(Alarms.class);
        Alarm newAlarm = instance.createNewAlarm();
        //TODO circle the time, otherwise the tests may fail around 0 hours
        newAlarm.edit().withIsEnabled(true).withHour(0).withDaysOfWeek(ImmutableDaysOfWeek.of(1)).withIsPrealarm(true).commit();
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