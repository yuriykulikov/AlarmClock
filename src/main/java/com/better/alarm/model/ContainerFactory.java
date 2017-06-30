package com.better.alarm.model;

import android.content.Context;

import com.better.alarm.persistance.AlarmContainer;
import com.better.alarm.logger.Logger;
import com.google.inject.Inject;

/**
 * Created by Yuriy on 24.06.2017.
 */

public interface ContainerFactory {
    IAlarmContainer create();

    class ContainerFactoryImpl implements ContainerFactory {
        private Context context;
        private Logger logger;

        @Inject
        public ContainerFactoryImpl(Context context, Logger logger) {
            this.context = context;
            this.logger = logger;
        }

        @Override
        public IAlarmContainer create() {
            return new AlarmContainer(logger, context);
        }
    }
}
