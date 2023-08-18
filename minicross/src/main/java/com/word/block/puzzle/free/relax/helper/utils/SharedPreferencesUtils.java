package com.word.block.puzzle.free.relax.helper.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {
    private static SharedPreferencesUtils sUtils;
    private SharedPreferences sharedPreferences;

    private SharedPreferencesUtils(Context context) {
        String name = String.format("%s.v2.playerprefs", context.getPackageName());
        sharedPreferences = context.getSharedPreferences(name, 0);
    }

    public static SharedPreferencesUtils getInstance(Context context) {
        if (sUtils == null) {
            sUtils = new SharedPreferencesUtils(context);
        }
        return sUtils;
    }

    public <T> T get(String key, T value) {
        Object obj = null;
        if ((value instanceof String)) {
            obj = sharedPreferences.getString(key, (String) value);
        } else if ((value instanceof Boolean)) {
            obj = sharedPreferences.getBoolean(key, (Boolean) value);
        } else if ((value instanceof Long)) {
            obj = sharedPreferences.getLong(key, (Long) value);
        } else if ((value instanceof Float)) {
            obj = sharedPreferences.getFloat(key, (Float) value);
        } else if ((value instanceof Integer)) {
            obj = sharedPreferences.getInt(key, Utils.String2Int(value.toString()));
        }
        return obj == null ? null : (T) obj;
    }

    public void clear(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (editor != null) {
            editor.remove(key);
            editor.apply();
        }
    }

    public synchronized void save(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if ((value instanceof String)) {
            editor.putString(key, value.toString());
        }
        if ((value instanceof Boolean)) {
            editor.putBoolean(key, (Boolean) value);
        }
        if ((value instanceof Long)) {
            editor.putLong(key, (Long) value);
        }
        if ((value instanceof Float)) {
            editor.putFloat(key, (Float) value);
        }
        if ((value instanceof Integer)) {
            editor.putInt(key, (Integer) value);
        }
        editor.apply();
    }


}
