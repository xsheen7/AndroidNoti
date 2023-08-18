package com.word.block.puzzle.free.relax.helper;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.word.block.puzzle.free.relax.helper.notify.DailyAlarmInfo;
import com.word.block.puzzle.free.relax.helper.utils.Utils;

import java.util.Objects;

public class FirebaseManager {
    private static FirebaseManager sManager;
    private FirebaseAnalytics mFirebaseAnalytics;

    public static final String EVENT_RECEIVE_DAILY_NOTIFICATION ="receive_daily_msg";//收到推送请求
    public static final String EVENT_PUSH_DAILY_NOTIFICATION = "push_daily_msg";//发送推送
    public static final String EVENT_CLICK_DAILY_NOTIFICATION = "click_daily_msg";//点击推送
    public static final String EVENT_OPEN_WITH_DAILY_NOTIFICATION = "open_with_daily_msg";//第一次的打开APP是通过推送

    public static final String EVENT_RECEIVE_INVALID_CODE = "receive_invalid_code";//收到无效的code

    //fcm打点
    public static final String EVENT_FCMPUSH_M_RECEIVE = "FCMPush_M_Receive";
    public static final String EVENT_FCMPUSH_M_PUSHED = "FCMPush_M_Pushed";
    public static final String EVENT_FCMPUSH_M_OPEN = "FCMPush_M_Open";
    public static final String EVENT_FCMPUSH_N_RECEIVE = "FCMPush_N_Receive";
    public static final String EVENT_FCMPUSH_N_PUSHED = "FCMPush_N_Pushed";
    public static final String EVENT_FCMPUSH_N_OPEN = "FCMPush_N_Open";
    public static final String EVENT_FCMPUSH_E_RECEIVE = "FCMPush_E_Receive";
    public static final String EVENT_FCMPUSH_E_PUSHED = "FCMPush_E_Pushed";
    public static final String EVENT_FCMPUSH_E_OPEN = "FCMPush_E_Open";

    private FirebaseManager(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public synchronized static FirebaseManager getInstance(Context context) {
        if (sManager == null) sManager = new FirebaseManager(context);
        return sManager;
    }

    public void logEvent(String event) {
        mFirebaseAnalytics.logEvent(event, null);
    }

    public void logEvent(String event, String name, int value) {
        Bundle bundle = new Bundle();
        bundle.putInt(name, value);
        mFirebaseAnalytics.logEvent(event, bundle);
    }

    public void logEvent(String event, String name, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(name, value);
        mFirebaseAnalytics.logEvent(event, bundle);
    }

    public void logDailyNotificationEvent(String event, String eventSuffix, int id) {
        String eventName = event;
        if (!TextUtils.isEmpty(eventSuffix)) {
            eventName = String.format("%s_%s", event, eventSuffix);
        }
        logEvent(eventName, "notify_id", id);
    }

    public void logPopupEvent(String event, String name, int value, String eventSuffix) {
        if (TextUtils.isEmpty(eventSuffix)) {
            logEvent(event, name, value);
        } else {
            logEvent(String.format("%s_%s", event, eventSuffix), name, value);
        }
    }
}
