package com.word.block.puzzle.free.relax.helper.notify;

import static com.word.block.puzzle.free.relax.helper.notify.NotificationUtils.getAppVersionCode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.word.block.puzzle.free.relax.helper.FirebaseManager;
import com.word.block.puzzle.free.relax.helper.NativeHelper;
import com.word.block.puzzle.free.relax.helper.R;
import com.word.block.puzzle.free.relax.helper.fcm.FCMReceiver;
import com.word.block.puzzle.free.relax.helper.fcm.FCMService;
import com.word.block.puzzle.free.relax.helper.utils.SharedPreferencesUtils;
import com.word.block.puzzle.free.relax.helper.utils.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class NotificationHelper {

    public static final String KEY_GAME_TODAY_FIRST_OPEN_DATE_KEY = "game_today_first_open_date_key";//用户当天第一次打开

    public static final String EXTRA_REQUEST_CODE = "request_code"; //请求CODE
    public static final int REQUEST_CODE_NOTIFICATION = 0x1001;//推送

    public static final String KEY_ALARM_DAILY_DATA = "alarm_daily_data";
    public static final String KEY_ALARM_IS_EVERYDAY = "alarm_is_everyday";

    public static final String KEY_DAILY_PUSH_ALARM_CACHE = "daily_push_alarm_cache";

    public static final String KEY_SDK_IS_OPEN_FCM = "sdk_is_open_fcm";//是否开启FCM
    public static final String KEY_SDK_NOTIFICATION_IS_OPENED = "sdk_is_open_noti";//是否开启FCM
    public static final String KEY_SDK_DC_OPEN = "key_dc_open";//是否开启dc

    public static final String KEY_LOCALE_M_NOTI_FINISH = "local_morning_noti_finish";//本日早上已推送
    public static final String KEY_LOCALE_N_NOTI_FINISH = "local_noon_noti_finish";//本日中午已推送
    public static final String KEY_LOCALE_E_NOTI_FINISH = "local_night_noti_finish";//本日晚上已推送


    //推送样式实验
    public static final String noti_test_group_key = "noti_test_group";

    //推送清除实验
    public static final String noti_test_clear_key = "noti_test_clear";

    //--------------------------------

    public static final String LOG_TAG = "[NOTI_LOG]";

    private static NotificationHelper notiHelper;
    private AlarmManager manager;
    public List<MsgInfo> msgInfos;
    public List<MsgInfo> msgInfosNotDaily;
    private NotificationManager mNotiMgr;
    private Context context;

    public NotificationManager getNotificationManager(Context context) {
        if (mNotiMgr == null)
            mNotiMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        return mNotiMgr;
    }

    public static NotificationHelper getInstance(Context context) {

        if (notiHelper == null) {
            notiHelper = new NotificationHelper();
        }
        notiHelper.context = context;
        if (!notiHelper.isInitialized()) {
            notiHelper.init(context);
        }
        return notiHelper;
    }

    public void setAlarm(DailyAlarmInfo info, int requestCode, boolean isEveryday) {
        long alarmTime = NotificationUtils.getNextSignInPushTime(info.hour, info.minute);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_ALARM_RECEIVER);
        intent.putExtra(NotificationHelper.EXTRA_REQUEST_CODE, requestCode);
        intent.putExtra(KEY_ALARM_DAILY_DATA, new Gson().toJson(info));
        intent.putExtra(KEY_ALARM_IS_EVERYDAY, isEveryday);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, info.id, intent, PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// 6.0及以上
            manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {// 4.4及以上
            manager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent);
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        }
    }

    //for android
    public boolean isInitialized() {
        boolean ready = context != null && manager != null && msgInfos != null;

//        Log.i(LOG_TAG, "noti init ready:" + ready);
        return ready;
    }

    public boolean isNotiOpen(Context context) {
        boolean open = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.KEY_SDK_NOTIFICATION_IS_OPENED, true);
        Log.i(LOG_TAG, "noti open:" + open);
        return open;
    }

    public boolean isFCMOpen(Context context) {
        boolean open = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.KEY_SDK_IS_OPEN_FCM, true);
        Log.i(LOG_TAG, "fcm open:" + open);
        return open;
    }

    //创建推送对象
    public Notification buildNotification(Context context, DailyAlarmInfo info, int requestCode,MsgInfo msg) {

        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        } else {
            intent = new Intent(context, NotificationReceiver.class);
        }

        intent.setAction(NotificationReceiver.ACTION_NOTIFY_CLICK);
        intent.putExtra(NotificationHelper.KEY_ALARM_DAILY_DATA, new Gson().toJson(info));
        intent.putExtra(NotificationHelper.EXTRA_REQUEST_CODE, requestCode);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            pendingIntent = PendingIntent.getActivity(context, info.id, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        }
        else
        {
            pendingIntent = PendingIntent.getBroadcast(context, info.id, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        RemoteViews view = getDailyNotifyView(context, msg.title, msg.content);
        if (NotificationUtils.isTestView(context) && !NotificationUtils.isSDKBig12()) {
            if (NotificationUtils.isMorning(info.hour)) {
                view.setImageViewResource(R.id.ic_icon, R.mipmap.push_morning);
            } else if (NotificationUtils.isNoon(info.hour)) {
                view.setImageViewResource(R.id.ic_icon, R.mipmap.push_noon);
            } else {
                view.setImageViewResource(R.id.ic_icon, R.mipmap.push_night);
            }

        }

        @SuppressLint("NotificationTrampoline") NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, "WordCrush")
                        .setSmallIcon(R.mipmap.notify_icon)
                        .setContentTitle(msg.title)
                        .setContentText(msg.content)
                        .setAutoCancel(true)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.app_icon))
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setColor(0xFF8F4A1B)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCustomBigContentView(view)
                        .setContentIntent(pendingIntent);

        //不做测试就全是展开的样式,做测试安卓12以下
        if (!NotificationUtils.isTestView(context) || NotificationUtils.isTestView(context) && !NotificationUtils.isSDKBig12()) {
            Log.i(LOG_TAG, "set custom view");
            notificationBuilder.setCustomContentView(view);
        }

        //强推送
        if (NotificationUtils.isTestForcePush(context) && NotificationUtils.isNight(info.hour)) {
            Log.i(LOG_TAG, "set force noti");
            notificationBuilder.setFullScreenIntent(pendingIntent, true)
                    .setContent(view);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("WordCrush",
                    TextUtils.isEmpty(msg.title) ? "WordCrush" : msg.title,
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        return notificationBuilder.build();
    }

    public RemoteViews getDailyNotifyView(Context context, String title, String content) {

        int viewName = R.layout.view_daily_notify_new;
        if (NotificationUtils.isTestView(context)) {
            if (NotificationUtils.isSDKBig12()) {
                viewName = R.layout.view_daily_notify_big_12;
            } else {
                viewName = R.layout.view_daily_notify_big;
            }
        }

        RemoteViews view = new RemoteViews(context.getPackageName(), viewName);

        view.setTextViewText(R.id.tv_notify_title, title);
        view.setTextViewText(R.id.tv_notify_title_content, content);
        return view;
    }

    public void sendNotification(Context context, DailyAlarmInfo info, int code,MsgInfo msg) {
        Notification notification = NotificationHelper.getInstance(context).buildNotification(context, info, code,msg);
        NotificationHelper.getInstance(context).getNotificationManager(context).notify(info.id, notification);
        String suffix = "";
        int id = -1;
        if (info != null) {
            suffix = info.eventSuffix;
            id = info.id;
        }
        FirebaseManager.getInstance(context).logDailyNotificationEvent(FirebaseManager.EVENT_PUSH_DAILY_NOTIFICATION, suffix, id);
    }

    public void init(Context context) {
        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        initMsg(context);
        Log.i(LOG_TAG, "noti helper init");
    }

    public void initMsg(Context context) {
        msgInfos = new ArrayList<>();
        msgInfos.add(new MsgInfo(1, context.getResources().getString(R.string.notify_title_1), context.getResources().getString(R.string.notify_content_1), false, false));
        msgInfos.add(new MsgInfo(2, context.getResources().getString(R.string.notify_title_2), context.getResources().getString(R.string.notify_content_2), false, false));
        msgInfos.add(new MsgInfo(3, context.getResources().getString(R.string.notify_title_3), context.getResources().getString(R.string.notify_content_3), false, false));
        msgInfos.add(new MsgInfo(4, context.getResources().getString(R.string.notify_title_4), context.getResources().getString(R.string.notify_content_4), false, false));
        msgInfos.add(new MsgInfo(5, context.getResources().getString(R.string.notify_title_5), context.getResources().getString(R.string.notify_content_5), false, false));
        msgInfos.add(new MsgInfo(6, context.getResources().getString(R.string.notify_title_6), context.getResources().getString(R.string.notify_content_6), false, false));
        msgInfos.add(new MsgInfo(7, context.getResources().getString(R.string.notify_title_7), context.getResources().getString(R.string.notify_content_7), false, false));

        msgInfos.add(new MsgInfo(8, context.getResources().getString(R.string.notify_title_8), context.getResources().getString(R.string.notify_content_8), true, true));
        msgInfos.add(new MsgInfo(9, context.getResources().getString(R.string.notify_title_9), context.getResources().getString(R.string.notify_content_9), true, true));
        msgInfos.add(new MsgInfo(10, context.getResources().getString(R.string.notify_title_10), context.getResources().getString(R.string.notify_content_10), true, true));
        msgInfos.add(new MsgInfo(11, context.getResources().getString(R.string.notify_title_11), context.getResources().getString(R.string.notify_content_11), true, true));
        msgInfos.add(new MsgInfo(12, context.getResources().getString(R.string.notify_title_12), context.getResources().getString(R.string.notify_content_12), true, true));
        msgInfos.add(new MsgInfo(13, context.getResources().getString(R.string.notify_title_13), context.getResources().getString(R.string.notify_content_13), true, false));
        msgInfos.add(new MsgInfo(14, context.getResources().getString(R.string.notify_title_14), context.getResources().getString(R.string.notify_content_14), true, false));

        msgInfos.add(new MsgInfo(15, context.getResources().getString(R.string.notify_title_15), context.getResources().getString(R.string.notify_content_15), false, true));
        msgInfos.add(new MsgInfo(16, context.getResources().getString(R.string.notify_title_16), context.getResources().getString(R.string.notify_content_16), false, true));
        msgInfos.add(new MsgInfo(17, context.getResources().getString(R.string.notify_title_17), context.getResources().getString(R.string.notify_content_17), false, true));
        msgInfos.add(new MsgInfo(18, context.getResources().getString(R.string.notify_title_18), context.getResources().getString(R.string.notify_content_18), false, true));
        msgInfos.add(new MsgInfo(19, context.getResources().getString(R.string.notify_title_19), context.getResources().getString(R.string.notify_content_19), false, true));
        msgInfos.add(new MsgInfo(20, context.getResources().getString(R.string.notify_title_20), context.getResources().getString(R.string.notify_content_20), false, true));
        msgInfos.add(new MsgInfo(21, context.getResources().getString(R.string.notify_title_21), context.getResources().getString(R.string.notify_content_21), false, true));

        msgInfosNotDaily = new ArrayList<>();
        msgInfosNotDaily.add(new MsgInfo(22, context.getResources().getString(R.string.notify_title_22), context.getResources().getString(R.string.notify_content_22), false, false));
    }

    //for unity
    public void setDailyNotificationAlarm(int id, int hour, int minute, String eventSuffix, boolean isNoon, boolean isNight) {
        DailyAlarmInfo info = NotificationUtils.formatNotificationDailyAlarmInfo(context, id, hour, minute, eventSuffix, isNoon, isNight);
        setAlarm(info, REQUEST_CODE_NOTIFICATION, true);
        NotificationUtils.addDailyAlarmInfoToCache(context, info);
        Log.i(LOG_TAG, "set alarm" + id);
    }

    //for unity活动闹钟 infoId 指定推送内容
    public void setDailyActivityNotificationAlarm(int id, int hour, int minute, int infoId) {
        DailyAlarmInfo info = NotificationUtils.formatNotificationDailyActivityAlarmInfo(context, id, hour, minute, infoId);
        setAlarm(info, REQUEST_CODE_NOTIFICATION, true);
        NotificationUtils.addDailyAlarmInfoToCache(context, info);
        Log.i(LOG_TAG, "set activity alarm" + id);
    }

    //设置指定时间
    public void setNotificationAlarm(int id,int month,int day, int hour, int minute, int infoId) {
        DailyAlarmInfo info = new DailyAlarmInfo();
        info.level = -1;
        info.day = day;
        info.hour = hour;
        info.minute = minute;
        info.id = id;
        info.version = getAppVersionCode(context);
        info.isActivity = true;
        info.once = true;
        info.msgId = infoId;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH,Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH,day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        long alarmTime = calendar.getTimeInMillis();

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_ALARM_RECEIVER);
        intent.putExtra(NotificationHelper.EXTRA_REQUEST_CODE, REQUEST_CODE_NOTIFICATION);
        intent.putExtra(KEY_ALARM_DAILY_DATA, new Gson().toJson(info));
        intent.putExtra(KEY_ALARM_IS_EVERYDAY, false);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, info.id, intent, PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// 6.0及以上
            manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {// 4.4及以上
            manager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent);
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        }

        Log.i(LOG_TAG, "set alarm" + id+" month="+month + " day="+day +" hour="+hour +" timemilsec="+alarmTime);
    }

    //每日打开的日期
    public void initDailyKeys(String todayOpenDate) {
        Log.i(LOG_TAG, "init daily keys:" + todayOpenDate);
        SharedPreferencesUtils.getInstance(context).save(KEY_GAME_TODAY_FIRST_OPEN_DATE_KEY, todayOpenDate);
        SharedPreferencesUtils.getInstance(context).clear(KEY_DAILY_PUSH_ALARM_CACHE);
    }

    //period id 0 morning,1 noon ,2 night
    public void addMsg(int id, String title, String content, int period) {
        MsgInfo info = new MsgInfo(id, title, content, period == 1, period == 2);
        if (msgInfos == null) {
            msgInfos = new ArrayList<>();
        }
        msgInfos.add(info);
        Log.i(LOG_TAG, "add noti msg " + id);
    }

    public void setFCMOpen(boolean open) {
        SharedPreferencesUtils.getInstance(context).save(NotificationHelper.KEY_SDK_IS_OPEN_FCM, open);
        Log.i(LOG_TAG, "set fcm:" + open);
    }

    public void setNotiOpen(boolean open) {
        SharedPreferencesUtils.getInstance(context).save(NotificationHelper.KEY_SDK_NOTIFICATION_IS_OPENED, open);
        Log.i(LOG_TAG, "set noti:" + open);
    }

    public boolean checkHasPushed(String key) {
        String pushTime = SharedPreferencesUtils.getInstance(context).get(key, "");

        //本地今天没推送过
        boolean pushed = NotificationUtils.checkIsToday(pushTime);
        Log.i(NotificationHelper.LOG_TAG, "today pushed:" + pushed + ",time:" + pushTime);
        return pushed;
    }

    public void setPushed(String key, String date) {
        SharedPreferencesUtils.getInstance(context).save(key, date);
    }

    //for test
    public void sendMsgTest(int id, String title, String content) {
        DailyAlarmInfo info = new DailyAlarmInfo();
        info.id = id;
        MsgInfo msg = new MsgInfo(title, content);
        sendNotification(context, info, REQUEST_CODE_NOTIFICATION,msgInfos.get(0));
    }

    //安卓13权限申请
    public static final String POST_NOTIFICATIONS="android.permission.POST_NOTIFICATIONS";
    public static void requestNotificationPermission(Activity activity) {

        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(activity, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale( activity, POST_NOTIFICATIONS)) {
                    enableNotification(activity);
                }else{
                    ActivityCompat.requestPermissions( activity,new String[]{POST_NOTIFICATIONS},100);
                }
            }
        } else {
            boolean enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled();
            if (!enabled) {
                enableNotification(activity);
            }
        }
    }

    private static void enableNotification(Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE,context. getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package",context. getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }

    private static ArrayList<Integer> alarmIds = new ArrayList<>();
    private static ArrayList<String> fcmPeriods = new ArrayList<>();

    /**
     * 12以上 处理推送点击
     */
    public static void handlerAppLaunch(Context context) {
        Log.i(LOG_TAG,"handle activity noti");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context instanceof Activity) {
                Intent intent = ((Activity) context).getIntent();

                Log.i(LOG_TAG,"handle activity noti action="+intent.getAction());

                Bundle bundle = intent.getExtras();
                if (bundle != null && bundle.size() > 0) {
                    if (NotificationReceiver.ACTION_NOTIFY_CLICK.equals(intent.getAction())) {

                        String json = intent.getStringExtra(NotificationHelper.KEY_ALARM_DAILY_DATA);
                        DailyAlarmInfo info = new Gson().fromJson(json, DailyAlarmInfo.class);

                        if(info!=null){

                            if(alarmIds.contains(info.id))
                                return;

                            alarmIds.add(info.id);
                            intent.setClass(context, NotificationReceiver.class);
                            intent.putExtra(NotificationHelper.EXTRA_REQUEST_CODE, REQUEST_CODE_NOTIFICATION);
                            intent.setAction(NotificationReceiver.ACTION_NOTIFY_CLICK);
                            intent.putExtra(NotificationHelper.KEY_ALARM_DAILY_DATA, new Gson().toJson(info));
                            context.sendBroadcast(intent);

                            Log.i(LOG_TAG,"local activity broadcast");
                        }
                    }
                    else if(FCMReceiver.ACTION_NOTIFY_CLICK.equals(intent.getAction())){

                        String period = intent.getStringExtra(FCMService.EXTRA_PERIOD_CODE);

                        if(fcmPeriods.contains(period))
                            return;
                        fcmPeriods.add(period);

                        int msgId = intent.getIntExtra(FCMService.PUSH_INFO_ID,-1);

                        intent.setClass(context, FCMReceiver.class);
                        intent.setAction(FCMReceiver.ACTION_NOTIFY_CLICK);
                        intent.putExtra(EXTRA_REQUEST_CODE, FCMService.FCM_REQUEST_CODE);
                        intent.putExtra(FCMService.PUSH_INFO_ID,msgId);
                        intent.putExtra(FCMService.EXTRA_PERIOD_CODE, period);

                        context.sendBroadcast(intent);

                        Log.i(LOG_TAG,"fcm activity broadcast");
                    }
                }
            }
        }
    }
}
