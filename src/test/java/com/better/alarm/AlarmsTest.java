package com.better.alarm;

import android.content.Context;

import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.AlarmCoreFactory;
import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.AlarmsScheduler;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.IAlarmsScheduler;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.persistance.AlarmContainer;
import com.better.alarm.persistance.DatabaseQuery;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.logger.SysoutLogWriter;
import com.github.androidutils.statemachine.HandlerFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlarmsTest {
    private Injector guice;
    private AlarmCore.IStateNotifier stateNotifierMock;
    private TestScheduler testScheduler;
    private ImmutableStore store;
    private ImmutablePrefs prefs;

    @Before
    public void setup() {
        testScheduler = new TestScheduler();
        final Logger logger = Logger.getDefaultLogger().addLogWriter(new SysoutLogWriter());

        prefs = ImmutablePrefs.builder()
                .preAlarmDuration(BehaviorSubject.createDefault(10))
                .snoozeDuration(BehaviorSubject.createDefault(10))
                .autoSilence(BehaviorSubject.createDefault(10))
                .build();

        store = ImmutableStore.builder()
                .alarms(BehaviorSubject.<List<AlarmValue>>createDefault(new ArrayList<AlarmValue>()))
                .next(BehaviorSubject.createDefault(Optional.<Store.Next>absent()))
                .build();

        stateNotifierMock = mock(AlarmCore.IStateNotifier.class);

        guice = Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                //initial config
                binder.requireExplicitBindings();
                binder.requireAtInjectOnConstructors();
                binder.requireExactBindingAnnotations();

                //real stuff
                binder.bind(Logger.class).toInstance(logger);
                binder.bind(Logger.class).annotatedWith(Names.named("debug")).toInstance(new Logger());
                binder.bind(IAlarmsManager.class).to(Alarms.class).asEagerSingleton();
                binder.bind(IAlarmsScheduler.class).to(AlarmsScheduler.class).asEagerSingleton();
                binder.bind(AlarmCoreFactory.class).asEagerSingleton();

                //stubs
                binder.bind(ContainerFactory.class).to(TestAlarmContainerFactory.class).asEagerSingleton();
                binder.bind(Scheduler.class).toInstance(testScheduler);
                binder.bind(HandlerFactory.class).to(TestHandlerFactory.class);
                binder.bind(Context.class).toInstance(mock(Context.class));
                binder.bind(DatabaseQuery.class).toInstance(mockQuery());
                binder.bind(AlarmSetter.class).to(TestAlarmSetter.class).asEagerSingleton();

                //stores and settings without persistance
                binder.bind(Prefs.class).toInstance(prefs);
                binder.bind(Store.class).toInstance(store);

                //mocks for verification
                binder.bind(AlarmCore.IStateNotifier.class).toInstance(stateNotifierMock);
            }
        });

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
        newAlarm.delete();
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
        newAlarm.delete();
        testScheduler.triggerActions();
        //verify
        store.alarms().test().assertValue(new Predicate<List<AlarmValue>>() {
            @Override
            public boolean test(@NonNull List<AlarmValue> alarmValues) throws Exception {
                return alarmValues.size() == 0;
            }
        });
    }
}