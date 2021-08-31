package com.better.alarm.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.assertj.core.api.Assertions.assertThat;

import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import java.util.List;
import org.assertj.core.api.AbstractListAssert;

/** Created by Yuriy on 09.07.2017. */
public class ListAsserts<T> {
  public interface ListAssert<T> {
    AbstractListAssert<?, ? extends List<? extends T>, T> items();

    ListAssert<T> filter(Predicate<T> predicate);
  }

  public static <T> ListAssert<T> assertThatList(final int id) {
    return createListAssert(ListAsserts.<T>listObservable(id));
  }

  public static <T> Observable<T> listObservable(final int id) {
    return Observable.create(
        new ObservableOnSubscribe<T>() {
          @Override
          public void subscribe(@NonNull final ObservableEmitter<T> e) throws Exception {
            onView(withId(id))
                .check(
                    new ViewAssertion() {
                      @Override
                      public void check(View view, NoMatchingViewException noViewFoundException) {
                        if (noViewFoundException != null) {
                          e.onError(noViewFoundException);
                        } else {
                          ListAdapter adapter = ((ListView) view).getAdapter();
                          for (int i = 0; i < adapter.getCount(); i++) {
                            e.onNext((T) adapter.getItem(i));
                          }
                          e.onComplete();
                        }
                      }
                    });
          }
        });
  }

  private static <T> ListAssert<T> createListAssert(final Observable<T> observable) {
    return new ListAssert<T>() {
      @Override
      public AbstractListAssert<?, ? extends List<? extends T>, T> items() {
        return assertThat(observable.toList().blockingGet());
      }

      @Override
      public ListAssert<T> filter(Predicate<T> predicate) {
        return createListAssert(observable.filter(predicate));
      }
    };
  }
}
