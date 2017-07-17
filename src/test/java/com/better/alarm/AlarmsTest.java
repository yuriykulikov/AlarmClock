package com.better.alarm;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;

import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.logger.Logger;
import com.better.alarm.logger.SysoutLogWriter;
import com.better.alarm.model.AlarmContainer;
import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.ImmutableAlarmContainer;
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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlarmsTest {
    private Injector guice;
    private AlarmCore.IStateNotifier stateNotifierMock;
    private TestScheduler testScheduler;
    private ImmutableStore store;
    private ImmutablePrefs prefs;
    private Logger logger;

    @Before
    public void setUp() {
        testScheduler = new TestScheduler();
        logger = Logger.getDefaultLogger().addLogWriter(new SysoutLogWriter());

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
            binder.bind(AlarmSetter.class).to(TestAlarmSetter.class).asEagerSingleton();
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
}