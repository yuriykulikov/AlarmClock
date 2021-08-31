package com.better.alarm.test;

import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Locale;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Created by Yuriy on 01.07.2017. */
public class ForceLocaleRule implements TestRule {

  private final Locale mTestLocale;
  private Locale mDeviceLocale;

  public ForceLocaleRule(Locale testLocale) {
    mTestLocale = testLocale;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      public void evaluate() throws Throwable {
        try {
          System.out.println("Setting locale to " + mTestLocale);
          if (mTestLocale != null) {
            mDeviceLocale = Locale.getDefault();
            setLocale(mTestLocale);
          }

          base.evaluate();
        } finally {
          if (mDeviceLocale != null) {
            setLocale(mDeviceLocale);
          }
        }
      }
    };
  }

  public void setLocale(Locale locale) {
    Resources resources =
        InstrumentationRegistry.getInstrumentation().getTargetContext().getResources();
    Locale.setDefault(locale);
    Configuration config = resources.getConfiguration();
    config.locale = locale;
    resources.updateConfiguration(config, resources.getDisplayMetrics());
  }
}
