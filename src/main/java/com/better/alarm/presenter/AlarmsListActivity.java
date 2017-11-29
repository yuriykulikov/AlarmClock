/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.better.alarm.presenter;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.better.alarm.R;
import com.better.alarm.configuration.EditedAlarm;
import com.better.alarm.configuration.ImmutableEditedAlarm;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.better.alarm.configuration.AlarmApplication.container;
import static com.better.alarm.configuration.AlarmApplication.themeHandler;

/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
public class AlarmsListActivity extends AppCompatActivity {
    private ActionBarHandler mActionBarHandler;
    private final Logger logger = container().logger();
    private final IAlarmsManager alarms = container().alarms();

    private Disposable sub = Disposables.disposed();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //in order to apply
        finish();
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(themeHandler().getIdForName(AlarmsListActivity.class.getName()));
        super.onCreate(savedInstanceState);
        logger.d(AlarmsListActivity.this);
        this.mActionBarHandler = new ActionBarHandler(this, store, alarms);

        boolean isTablet = !getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.list_activity);

        if (getIntent() != null && getIntent().hasExtra(Intents.EXTRA_ID)) {
            //jump directly to editor
            store.edit(getIntent().getIntExtra(Intents.EXTRA_ID, -1));
        }
    }

    @Override
    protected void onStart() {
        logger.d(AlarmsListActivity.this);
        super.onStart();
        configureTransactions();
    }

    @Override
    protected void onStop() {
        logger.d(AlarmsListActivity.this);
        super.onStop();
        this.sub.dispose();
    }

    @Override
    protected void onDestroy() {
        logger.d(AlarmsListActivity.this);
        super.onDestroy();
        this.mActionBarHandler.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mActionBarHandler.onCreateOptionsMenu(menu, getMenuInflater(), getSupportActionBar());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mActionBarHandler.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        store.onBackPressed().onNext(AlarmsListActivity.class.getSimpleName());
    }

    @TargetApi(21)
    private void configureTransactions() {
        sub = store.editing()
                .distinctUntilChanged(new Function<EditedAlarm, Boolean>() {
                    @Override
                    public Boolean apply(@NonNull EditedAlarm editedAlarm) throws Exception {
                        return editedAlarm.isEdited();
                    }
                })
                .subscribe(new Consumer<EditedAlarm>() {
                    @Override
                    public void accept(@NonNull EditedAlarm edited) throws Exception {
                        if (isDestroyed()) {
                            return;
                        } else if (edited.isEdited()) {
                            showDetails(edited);
                        } else {
                            showList(edited);
                        }
                    }
                });
    }

    @TargetApi(21)
    private void showList(@NonNull EditedAlarm edited) {
        Fragment detailsFragment = getFragmentManager().findFragmentById(R.id.main_fragment_container);
        if (detailsFragment != null) {
            detailsFragment.setExitTransition(new Fade());
        }

        Transition moveShared = TransitionInflater.from(AlarmsListActivity.this).inflateTransition(android.R.transition.move);

        Fragment listFragment = new AlarmsListFragment();
        listFragment.setSharedElementEnterTransition(moveShared);
        listFragment.setEnterTransition(new Fade());

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        if (edited.holder().isPresent()) {
            RowHolder viewHolder = edited.holder().get();
            transaction.addSharedElement(viewHolder.digitalClock(), "clock" + viewHolder.alarmId());
            transaction.addSharedElement(viewHolder.container(), "onOff" + viewHolder.alarmId());
        }

        transaction.replace(R.id.main_fragment_container, listFragment).commitAllowingStateLoss();
    }

    @TargetApi(21)
    private void showDetails(@NonNull EditedAlarm edited) {
        Fragment listFragment = getFragmentManager().findFragmentById(R.id.main_fragment_container);
        if (listFragment != null) {
            //Explode explode = new Explode();
            //if (edited.holder().isPresent()) {
            //    explode.setEpicenterCallback(edited.holder().get().epicenter());
            //}
            listFragment.setExitTransition(new Fade());
        }

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        Slide enterSlide = new Slide();

        if (edited.holder().isPresent()) {
            final RowHolder viewHolder = edited.holder().get();
            fragmentTransaction.addSharedElement(viewHolder.digitalClock(), "clock" + viewHolder.alarmId());
            fragmentTransaction.addSharedElement(viewHolder.container(), "onOff" + viewHolder.alarmId());
            enterSlide.setEpicenterCallback(viewHolder.epicenter());
            enterSlide.setSlideEdge(Gravity.TOP);
        }

        Fragment detailsFragment = new AlarmDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(Intents.EXTRA_ID, edited.id());
        args.putBoolean(Store.IS_NEW_ALARM, edited.isNew());
        detailsFragment.setArguments(args);
        detailsFragment.setSharedElementEnterTransition(TransitionInflater.from(AlarmsListActivity.this).inflateTransition(android.R.transition.move));
        detailsFragment.setEnterTransition(enterSlide);

        fragmentTransaction
                .replace(R.id.main_fragment_container, detailsFragment)
                .commitAllowingStateLoss();
    }

    public static UiStore uiStore(Fragment fragment) {
        return ((AlarmsListActivity) fragment.getActivity()).store;
    }

    private UiStore store = new UiStore() {
        public PublishSubject<String> onBackPressed = PublishSubject.<String>create();
        public Subject<EditedAlarm> editing = BehaviorSubject.createDefault((EditedAlarm) ImmutableEditedAlarm.builder().build());

        @Override
        public Subject<EditedAlarm> editing() {
            return editing;
        }

        @Override
        public PublishSubject<String> onBackPressed() {
            return onBackPressed;
        }

        @Override
        public void createNewAlarm() {
            AlarmValue newAlarm = alarms.createNewAlarm().edit();
            editing().onNext(ImmutableEditedAlarm.builder()
                    .isNew(true)
                    .id(newAlarm.getId())
                    .isEdited(true)
                    .build());
        }

        @Override
        public void edit(int id) {
            editing().onNext(ImmutableEditedAlarm.builder()
                    .isNew(false)
                    .id(id)
                    .isEdited(true)
                    .build());
        }

        @Override
        public void edit(int id, RowHolder holder) {
            editing().onNext(ImmutableEditedAlarm.builder()
                    .isNew(false)
                    .holder(holder)
                    .id(id)
                    .isEdited(true)
                    .build());
        }

        @Override
        public void hideDetails() {
            editing().onNext(ImmutableEditedAlarm.builder()
                    .isNew(false)
                    .isEdited(false)
                    .build());
        }

        @Override
        public void hideDetails(RowHolder holder) {
            editing().onNext(ImmutableEditedAlarm.builder()
                    .isNew(false)
                    .id(holder.alarmId())
                    .isEdited(false)
                    .holder(holder)
                    .build());
        }
    };
}
