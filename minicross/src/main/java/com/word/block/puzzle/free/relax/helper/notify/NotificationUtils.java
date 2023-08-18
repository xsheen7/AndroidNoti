package com.word.block.puzzle.free.relax.helper.notify;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.word.block.puzzle.free.relax.helper.R;
import com.word.block.puzzle.free.relax.helper.utils.SharedPreferencesUtils;
import com.word.block.puzzle.free.relax.helper.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class NotificationUtils {

    public static long getTimeInMillisForHourMinute(int hour, int minue) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minue);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getTomorrowTimeInMillisForHourMinute(int hour, int minue) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, +1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minue);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    public static long getNextSignInPushTime(int hour, int minute) {
        long time = getTimeInMillisForHourMinute(hour, minute);
        if (time < System.currentTimeMillis()) {
            time = getTomorrowTimeInMillisForHourMinute(hour, minute);
        }
        return time;
    }

    public static boolean checkIsToday(String time) {
        if (TextUtils.isEmpty(time))
            return false;
        String dateTime = Utils.formatDate(new Date());
        return dateTime.equals(time);
    }

    public static List<DailyAlarmInfo> getDailyPushAlarmInfos(Context context) {
        List<DailyAlarmInfo> infos = new ArrayList<>();
        if (context == null) return infos;
        String json = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.KEY_DAILY_PUSH_ALARM_CACHE, "");
        if (!TextUtils.isEmpty(json)) {
            try {
                List<DailyAlarmInfo> temp = new Gson().fromJson(json, new TypeToken<List<DailyAlarmInfo>>() {
                }.getType());
                if (temp != null && temp.size() > 0) infos.addAll(temp);
            } catch (Exception e) {
            }
        }
        return infos;
    }


    public static DailyAlarmInfo formatNotificationDailyAlarmInfo(Context context, int id, int hour, int minute, String eventSuffix, boolean isNoon, boolean isNight) {
        DailyAlarmInfo info = new DailyAlarmInfo();
        MsgInfo content = getDailyNotificationContent(context, isNoon, isNight);
        info.msgInfo = content;
        info.level = -1;
        info.hour = hour;
        info.minute = minute;
        info.eventSuffix = eventSuffix;
        info.id = id;
        info.isNoon = isNoon;
        info.isNight = isNight;
        info.version = getAppVersionCode(context);
        return info;
    }

    public static DailyAlarmInfo formatNotificationDailyActivityAlarmInfo(Context context, int id, int hour, int minute, int infoId) {
        DailyAlarmInfo info = new DailyAlarmInfo();
        MsgInfo content = getLocalMsgInfo(context, infoId);
        if (content == null) {
            Log.e(NotificationHelper.LOG_TAG, "msg info err:" + infoId);
        }
        info.msgInfo = content;
        info.level = -1;
        info.hour = hour;
        info.minute = minute;
        info.id = id;
        info.version = getAppVersionCode(context);
        info.isActivity = true;
        return info;
    }

    public static void addDailyAlarmInfoToCache(Context context, DailyAlarmInfo info) {
        if (context == null) return;
        String json = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.KEY_DAILY_PUSH_ALARM_CACHE, "");
        List<DailyAlarmInfo> infos = new ArrayList<>();
        if (!TextUtils.isEmpty(json)) {
            try {
                List<DailyAlarmInfo> temp = new Gson().fromJson(json, new TypeToken<List<DailyAlarmInfo>>() {
                }.getType());
                if (temp != null && temp.size() > 0) infos.addAll(temp);
            } catch (Exception e) {
            }
        }
        infos.add(info);
        SharedPreferencesUtils.getInstance(context).save(NotificationHelper.KEY_DAILY_PUSH_ALARM_CACHE, new Gson().toJson(infos));
    }

    public static MsgInfo getDailyNotificationContent(Context context, boolean isNoon, boolean isNight) {
        Random random = new Random();
        List<MsgInfo> tmp = new ArrayList<>();

        List<MsgInfo> infos = NotificationHelper.getInstance(context).msgInfos;
        if (infos == null) {
            NotificationHelper.getInstance(context).initMsg(context);
            infos = NotificationHelper.getInstance(context).msgInfos;
        }

        try {
            for (MsgInfo info : infos) {
                boolean add = isNoon == info.isNoon && isNight == info.isNight;

                if (add) {
                    tmp.add(info);
                }
            }
        } catch (Exception e) {
            return null;
        }

        int count = tmp.size();
        Log.i(NotificationHelper.LOG_TAG, "msg count" + count);
        if (count == 0) return null;

        // 获取当前时间
        Calendar calendar = Calendar.getInstance();
        // 获取今天是星期几，星期日为 1，星期一为 2，依次类推
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        int index = dayOfWeek - 1;
        if(index<0)
        {
            index = 6;
        }
        if (index > tmp.size() - 1) {
            index = 0;
        }

        Log.i(NotificationHelper.LOG_TAG, "day of week:" + dayOfWeek);
        return tmp.get(index);
    }

    public static MsgInfo getLocalMsgInfo(Context context, int id) {
        List<MsgInfo> infos = NotificationHelper.getInstance(context).msgInfos;
        if (infos == null) {
            NotificationHelper.getInstance(context).initMsg(context);
            infos = NotificationHelper.getInstance(context).msgInfos;
        }

        for (MsgInfo info : infos) {
            if (info.id == id)
                return info;
        }

        return null;
    }

    public static int getAppVersionCode(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean isRushRewardShowed(Context context) {
        String showed = SharedPreferencesUtils.getInstance(context).get("rush_showed", "");
        if (NotificationUtils.checkIsToday(showed)) {
            return true;
        }
        return false;
    }

    public static boolean isTestView(Context context) {
        String value = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.noti_test_group_key, "-1");
        if (!TextUtils.isEmpty(value)) {
            if (value.equals("1")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isTestForcePush(Context context) {
        String value = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.noti_test_group_key, "-1");
        if (!TextUtils.isEmpty(value)) {
            if (value.equals("2")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSDKBig12() {
        boolean yes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        return yes;
    }

    public static boolean isMorning(int hour) {
        boolean yes = hour < 11;
        return yes;
    }

    public static boolean isNoon(int hour) {
        boolean yes = hour >= 11 && hour < 19;
        return yes;
    }

    public static boolean isNight(int hour) {
        boolean yes = hour >= 19;
        return yes;
    }


    public static boolean isTestClearPush(Context context) {
        String group = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.noti_test_clear_key, "");

        if (TextUtils.isEmpty(group))
            return false;

        boolean yes = group.equals("2");

        Log.i(NotificationHelper.LOG_TAG, yes + ":clear group=" + group);
        return yes;
    }
}
