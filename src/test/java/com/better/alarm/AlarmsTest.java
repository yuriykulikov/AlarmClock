package com.better.alarm;

import android.content.Context;

import com.better.alarm.logger.Logger;
import com.better.alarm.logger.SysoutLogWriter;
import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.IAlarmContainer;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.persistance.DatabaseQuery;
import com.better.alarm.statemachine.HandlerFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
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
    public void setup() {
        testScheduler = new TestScheduler();
        logger = Logger.getDefaultLogger().addLogWriter(new SysoutLogWriter());

        prefs = ImmutablePrefs.builder()
                .preAlarmDuration(BehaviorSubject.createDefault(10))
                .snoozeDuration(BehaviorSubject.createDefault(10))
                .autoSilence(BehaviorSubject.createDefault(10))
                .build();

        store = ImmutableStore.builder()
                .alarms(BehaviorSubject.<List<AlarmValue>>createDefault(new ArrayList<AlarmValue>()))
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
            binder.bind(ContainerFactory.class).to(TestAlarmContainerFactory.class).asEagerSingleton();
            binder.bind(Scheduler.class).toInstance(testScheduler);
            binder.bind(HandlerFactory.class).to(TestHandlerFactory.class);
            binder.bind(Context.class).toInstance(mock(Context.class));
            binder.bind(DatabaseQuery.class).toInstance(mockQuery());
            binder.bind(AlarmSetter.class).to(TestAlarmSetter.class).asEagerSingleton();

            //mocks for verification
            binder.bind(AlarmCore.IStateNotifier.class).toInstance(stateNotifierMock);
        }
    }

    @android.support.annotation.NonNull
    private DatabaseQuery mockQuery() {
        final DatabaseQuery query = mock(DatabaseQuery.class);
        List<IAlarmContainer> list = Lists.newArrayList();
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

    @Test
    public void alarmsFromMemoryMustBePresentInTheList() {
        //when
        IAlarmsManager instance = Guice.createInjector(Modules
                .override(Modules
                        .override(new AlarmApplication.AppModule(logger, prefs, store))
                        .with(new TestModule()))
                .with(new Module() {
                    @Override
                    public void configure(Binder binder) {
                        final DatabaseQuery query = mock(DatabaseQuery.class);
                        TestAlarmContainer existingContainer = new TestAlarmContainer(100500);
                        existingContainer.setEnabled(true);
                        existingContainer.setLabel("hello");
                        List<IAlarmContainer> list = Lists.<IAlarmContainer>newArrayList(existingContainer);
                        when(query.query()).thenReturn(Single.just(list));
                        binder.bind(DatabaseQuery.class).toInstance(query);
                    }
                }))
                .getInstance(IAlarmsManager.class);

        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
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