package com.better.alarm.presenter;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.better.alarm.R;
import com.better.alarm.alert.AlarmAlert;
import com.better.alarm.alert.AlarmAlertFullScreen;

public class DynamicThemeHandler {
    public static final String KEY_THEME = "theme";
    public static final String DEFAULT = "default";

    private static DynamicThemeHandler sInstance;
    private final Map<String, Map<String, Integer>> themes;
    private final SharedPreferences sp;

    private class HashMapWithDefault extends HashMap<String, Integer> {
        private static final long serialVersionUID = 6169875120194964563L;

        @Override
        public Integer get(Object key) {
            Object id = super.get(key);
            if (id == null) return super.get(DEFAULT);
            else return (Integer) id;
        }

        public HashMapWithDefault(Integer defaultValue) {
            super(5);
            put(DEFAULT, defaultValue);
        }
    }

    public int getIdForName(String name) {
        String activeThemeName = sp.getString(KEY_THEME, "green");
        Map<String, Integer> activeThemeMap = themes.get(activeThemeName);
        Integer themeForName = activeThemeMap.get(name);
        return themeForName;
    }

    public static void init(Context context) {
        sInstance = new DynamicThemeHandler(context);
    }

    public static DynamicThemeHandler getInstance() {
        return sInstance;
    }

    private DynamicThemeHandler(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        Map<String, Integer> darkThemes = new HashMapWithDefault(R.style.DefaultDarkTheme);
        darkThemes.put(AlarmAlert.class.getName(), R.style.AlarmAlertDarkTheme);
        darkThemes.put(AlarmAlertFullScreen.class.getName(), R.style.AlarmAlertFullScreenDarkTheme);
        darkThemes.put(TimePickerDialogFragment.class.getName(), R.style.TimePickerDialogFragmentDark);

        Map<String, Integer> lightThemes = new HashMapWithDefault(R.style.DefaultLightTheme);
        lightThemes.put(AlarmAlert.class.getName(), R.style.AlarmAlertLightTheme);
        lightThemes.put(AlarmAlertFullScreen.class.getName(), R.style.AlarmAlertFullScreenLightTheme);
        lightThemes.put(TimePickerDialogFragment.class.getName(), R.style.TimePickerDialogFragmentLight);

        Map<String, Integer> greenThemes = new HashMapWithDefault(R.style.GreenTheme);
        greenThemes.put(AlarmAlert.class.getName(), R.style.AlarmAlertGreenTheme);
        greenThemes.put(AlarmAlertFullScreen.class.getName(), R.style.AlarmAlertFullScreenGreenTheme);
        greenThemes.put(TimePickerDialogFragment.class.getName(), R.style.TimePickerDialogFragmentGreen);

        themes = new HashMap<String, Map<String, Integer>>(3);
        themes.put("light", lightThemes);
        themes.put("dark", darkThemes);
        themes.put("green", greenThemes);
        // fallback
        themes.put("Light", lightThemes);
        themes.put("Dark", darkThemes);
        themes.put("Green", greenThemes);
    }
}
