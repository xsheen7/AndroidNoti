package com.word.block.puzzle.free.relax.helper.notify;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.word.block.puzzle.free.relax.helper.FirebaseManager;
import com.word.block.puzzle.free.relax.helper.utils.SharedPreferencesUtils;
import com.word.block.puzzle.free.relax.helper.utils.Utils;

import java.util.Date;
import java.util.Objects;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_ALARM_RECEIVER = "com.word.block.puzzle.free.relax.helper.ACTION_ALARM_RECEIVER";
    public static final String ACTION_NOTIFY_CLICK = "com.word.block.puzzle.free.relax.helper.ACTION_CLICK_NOTIFICATION";

    @SuppressLint("NotificationTrampoline")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(NotificationHelper.LOG_TAG, "receive:" + intent.getAction());

        String json = intent.getStringExtra(NotificationHelper.KEY_ALARM_DAILY_DATA);
        DailyAlarmInfo info = new Gson().fromJson(json, DailyAlarmInfo.class);

        if (info == null)
            return;

        //收到每日消息埋点
        String suffix = info.eventSuffix;
        if (!Objects.isNull(suffix) && !suffix.isEmpty()) {
            int id = info.id;
            FirebaseManager.getInstance(context).logDailyNotificationEvent(FirebaseManager.EVENT_RECEIVE_DAILY_NOTIFICATION, suffix, id);
        }

        Log.i(NotificationHelper.LOG_TAG, "receive :" + info.id);

        if (info.isActivity) {

        }

        if (ACTION_ALARM_RECEIVER.equals(intent.getAction())) {
            handlerPush(context, intent);
        } else if (ACTION_NOTIFY_CLICK.equals(intent.getAction())) {
            handleClickNotify(context, intent);
        }
    }

    private void handlerPush(Context context, Intent intent) {
        Log.i(NotificationHelper.LOG_TAG, "local handle push");
        if (!NotificationHelper.getInstance(context).isNotiOpen(context))
            return;

        if (!NotificationHelper.getInstance(context).isInitialized()) {
            NotificationHelper.getInstance(context).init(context);
        }

        int code = intent.getIntExtra(NotificationHelper.EXTRA_REQUEST_CODE, 0);
        String json = intent.getStringExtra(NotificationHelper.KEY_ALARM_DAILY_DATA);
        DailyAlarmInfo info = new Gson().fromJson(json, DailyAlarmInfo.class);
        if (info == null) {
            Log.e(NotificationHelper.LOG_TAG, "info err");
            return;
        }

        String key = NotificationHelper.KEY_LOCALE_M_NOTI_FINISH;
        if (info.isNoon) {
            key = NotificationHelper.KEY_LOCALE_N_NOTI_FINISH;
        } else if (info.isNight) {
            key = NotificationHelper.KEY_LOCALE_E_NOTI_FINISH;
        }

        //先判断活动
        if (info.isActivity) {
            Log.i(NotificationHelper.LOG_TAG, "activity noti");
            //再次设置活动闹钟
            setActivityAlarm(context, intent);

            Log.i(NotificationHelper.LOG_TAG, "check activity push:" + info.id);
        } else {
            //本地时间段已将推送过了
            boolean canPush = !NotificationHelper.getInstance(context).checkHasPushed(key);

            if (!canPush)
                return;

            startAlarm(context, intent);
        }

        if (code == NotificationHelper.REQUEST_CODE_NOTIFICATION) {

            //测试推送清除推送
            if (NotificationUtils.isTestClearPush(context)) {
//            int notificationId = 1;
                NotificationManager notificationManager = NotificationHelper.getInstance(context).getNotificationManager(context);
                notificationManager.cancelAll();
            }

            MsgInfo msg = NotificationUtils.getDailyNotificationContent(context, info.isNoon, info.isNight);

            //推送通知
            NotificationHelper.getInstance(context).sendNotification(context, info, code, msg);
            Log.i(NotificationHelper.LOG_TAG, "local noti push=" + info.hour);

            if (!info.isActivity) {
                String date = Utils.formatDate(new Date());
                NotificationHelper.getInstance(context).setPushed(key, date);
            }

        } else if (code > 0) {
            FirebaseManager.getInstance(context).logEvent(FirebaseManager.EVENT_RECEIVE_INVALID_CODE, "code", code);
        }
    }

    private void handleClickNotify(Context context, Intent intent) {
        Log.i(NotificationHelper.LOG_TAG, "local noti click");

        int code = intent.getIntExtra(NotificationHelper.EXTRA_REQUEST_CODE, 0);
        String json = intent.getStringExtra(NotificationHelper.KEY_ALARM_DAILY_DATA);
        DailyAlarmInfo info = new Gson().fromJson(json, DailyAlarmInfo.class);
        if (info == null) return;
        if (code != NotificationHelper.REQUEST_CODE_NOTIFICATION) return;

        String suffix = "";
        int id = -1;
        if (info != null) {
            suffix = info.eventSuffix;
            id = info.id;
        }

        if (info.isActivity) {

        } else {
            FirebaseManager.getInstance(context).logDailyNotificationEvent(FirebaseManager.EVENT_CLICK_DAILY_NOTIFICATION, suffix, id);
            //first click
            String todayOpenDate = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.KEY_GAME_TODAY_FIRST_OPEN_DATE_KEY, "");
            if (!NotificationUtils.checkIsToday(todayOpenDate)) {
                FirebaseManager.getInstance(context).logDailyNotificationEvent(FirebaseManager.EVENT_OPEN_WITH_DAILY_NOTIFICATION, suffix, id);
            }
        }

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        context.startActivity(launch);
    }

    //本地收到通知后，再次设置闹钟
    private void startAlarm(Context context, Intent intent) {
        boolean open = intent.getBooleanExtra(NotificationHelper.KEY_ALARM_IS_EVERYDAY, false);
        if (!open) return;
        if (!NotificationHelper.getInstance(context).isInitialized()) {
            NotificationHelper.getInstance(context).init(context);
        }
        int requestCode = intent.getIntExtra(NotificationHelper.EXTRA_REQUEST_CODE, 0);
        String json = intent.getStringExtra(NotificationHelper.KEY_ALARM_DAILY_DATA);
        DailyAlarmInfo info = new Gson().fromJson(json, DailyAlarmInfo.class);
        info = NotificationUtils.formatNotificationDailyAlarmInfo(context, info.id, info.hour, info.minute, info.eventSuffix, info.isNoon, info.isNight);
        NotificationHelper.getInstance(context).setAlarm(info, requestCode, true);
    }


    //再次设置活动闹钟
    private void setActivityAlarm(Context context, Intent intent) {
        if (!NotificationHelper.getInstance(context).isInitialized()) {
            NotificationHelper.getInstance(context).init(context);
        }
        int requestCode = intent.getIntExtra(NotificationHelper.EXTRA_REQUEST_CODE, 0);
        String json = intent.getStringExtra(NotificationHelper.KEY_ALARM_DAILY_DATA);
        DailyAlarmInfo info = new Gson().fromJson(json, DailyAlarmInfo.class);
        NotificationHelper.getInstance(context).setAlarm(info, requestCode, true);
    }
}

