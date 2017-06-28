package com.better.alarm;

import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.IAlarmContainer;
import com.google.inject.Inject;

/**
 * Created by Yuriy on 25.06.2017.
 */
public class TestAlarmContainerFactory implements ContainerFactory {
    int idCounter;

    @Inject
    public TestAlarmContainerFactory() {

    }

    @Override
    public IAlarmContainer create() {
        return new TestAlarmContainer(idCounter++);
    }
}
