package com.word.block.puzzle.free.relax.helper.fcm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.word.block.puzzle.free.relax.helper.FirebaseManager;
import com.word.block.puzzle.free.relax.helper.R;
import com.word.block.puzzle.free.relax.helper.notify.MsgInfo;
import com.word.block.puzzle.free.relax.helper.notify.NotificationHelper;
import com.word.block.puzzle.free.relax.helper.notify.NotificationUtils;
import com.word.block.puzzle.free.relax.helper.utils.SharedPreferencesUtils;
import com.word.block.puzzle.free.relax.helper.utils.Utils;

import java.util.Date;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {

    public static final String NOTIFICATION_CHANNEL_NAME = "wordcrush";
    public static final String NOTIFICATION_CHANNEL_ID = "wordcrush";

    public static final String KEY_ACTION = "action";
    public static final String ACTION_NOTI_MORNING = "action_noti_morning";//早上推送
    public static final String ACTION_NOTI_NOON = "action_noti_noon";//中午推送
    public static final String ACTION_NOTI_NIGHT = "action_noti_night";//晚上推送

    public static final String EXTRA_REQUEST_CODE = "request_code"; //请求CODE
    public static final int FCM_REQUEST_CODE = 0x3001;

    public static final String EXTRA_PERIOD_CODE = "period_code"; //请求period
    public static final String PUSH_INFO_ID = "push_noti_id";//推送消息的id


    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i(NotificationHelper.LOG_TAG, "fcm receive msg=" + remoteMessage);

        String suffix="";
        //fcm receive
        if (remoteMessage != null) {
            Map<String, String> data = remoteMessage.getData();
            if (data != null && data.size() > 0) {
                if (data.containsKey(KEY_ACTION)) {
                    String action = data.get(KEY_ACTION);

                    Log.i(NotificationHelper.LOG_TAG, "fcm msg action:"+action);

                    if (ACTION_NOTI_MORNING.equals(action)) {
                        suffix ="morning";
                    } else if (ACTION_NOTI_NOON.equals(action)) {
                        suffix = "noon";
                    } else if (ACTION_NOTI_NIGHT.equals(action)) {
                        suffix = "night";
                    }
                }
            }
        }

        FirebaseManager.getInstance(getApplicationContext()).logDailyNotificationEvent(FirebaseManager.EVENT_RECEIVE_DAILY_NOTIFICATION,suffix,-1);

        //noti close
        if (!NotificationHelper.getInstance(getApplicationContext()).isNotiOpen(getApplicationContext()))
            return;

        //fcm close
        if (!NotificationHelper.getInstance(getApplicationContext()).isFCMOpen(getApplicationContext())) {
            return;
        }

        if(suffix.equals("morning")){
            if(SharedPreferencesUtils.getInstance(getApplicationContext()).get("only_20", "").equals("1")){
                Log.e(NotificationHelper.LOG_TAG,"only_20");
                return;
            }

            if(SharedPreferencesUtils.getInstance(getApplicationContext()).get("only_12_and_20", "").equals("1")){
                Log.e(NotificationHelper.LOG_TAG,"only_12_and_20");
                return;
            }

            if(NotificationUtils.isRushRewardShowed(getApplicationContext())){
                return;
            }

            FirebaseManager.getInstance(getApplicationContext()).logDailyNotificationEvent(FirebaseManager.EVENT_FCMPUSH_M_RECEIVE,"",-1);
            sendNotiFromFCM(0);
        }
        else if(suffix.equals("noon")){

            if(SharedPreferencesUtils.getInstance(getApplicationContext()).get("only_20", "").equals("1")){
                Log.e(NotificationHelper.LOG_TAG,"only_20");
                return;
            }

            FirebaseManager.getInstance(getApplicationContext()).logDailyNotificationEvent(FirebaseManager.EVENT_FCMPUSH_N_RECEIVE,"",-1);
            sendNotiFromFCM(1);
        }
        else if(suffix.equals("night")){
            FirebaseManager.getInstance(getApplicationContext()).logDailyNotificationEvent(FirebaseManager.EVENT_FCMPUSH_E_RECEIVE,"",-1);
            sendNotiFromFCM(2);
        }
    }

    //0 morning 1 noon 2 night
    private void sendNotiFromFCM(int period) {

        String eventName = FirebaseManager.EVENT_FCMPUSH_M_PUSHED;
        String suffix = "morning";
        String key = NotificationHelper.KEY_LOCALE_M_NOTI_FINISH;
        if (period == 1) {
            eventName = FirebaseManager.EVENT_FCMPUSH_N_PUSHED;
            suffix = "noon";
            key = NotificationHelper.KEY_LOCALE_N_NOTI_FINISH;
        } else if (period == 2) {
            eventName = FirebaseManager.EVENT_FCMPUSH_E_PUSHED;
            suffix = "night";
            key = NotificationHelper.KEY_LOCALE_E_NOTI_FINISH;
        }

        //本地今天没推送过
        boolean canPush = !NotificationHelper.getInstance(getApplicationContext()).checkHasPushed(key);

        if (!canPush)
            return;

        MsgInfo info = NotificationUtils.getDailyNotificationContent(getApplicationContext(), period == 1, period == 2);
        if(info==null)
            return;

        sendNotification(getApplicationContext(),info.id, info.title, info.content, period);

        FirebaseManager.getInstance(getApplicationContext()).logDailyNotificationEvent(eventName,"",info.id);
        FirebaseManager.getInstance(getApplicationContext()).logDailyNotificationEvent(FirebaseManager.EVENT_PUSH_DAILY_NOTIFICATION,suffix,info.id);

        String date = Utils.formatDate(new Date());
        NotificationHelper.getInstance(getApplicationContext()).setPushed(key,date);
        Log.i(NotificationHelper.LOG_TAG, "send noti");
    }

    private void sendNotification(Context context,int id, String title,String content, int period) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content))
            return;

        //测试推送清除推送
        if(NotificationUtils.isTestClearPush(context)){
//            int notificationId = 1;
            NotificationManager notificationManager = NotificationHelper.getInstance(context).getNotificationManager(context);
            notificationManager.cancelAll();
        }

        Notification notify = buildNotification(context, id, title, content, period);
        NotificationHelper.getInstance(context).getNotificationManager(context).notify(0, notify);
    }

    private Notification buildNotification(Context context, int id, String title, String content, int period) {
        Intent intent = new Intent(context, FCMReceiver.class);
        intent.setAction(context.getPackageName() + FCMReceiver.ACTION_NOTIFY_CLICK);
        intent.putExtra(PUSH_INFO_ID,id);
        intent.putExtra(EXTRA_REQUEST_CODE, FCM_REQUEST_CODE);
        intent.putExtra(EXTRA_PERIOD_CODE, Integer.toString(period));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, FCM_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE);

        RemoteViews view = NotificationHelper.getInstance(context).getDailyNotifyView(context, title, content);
        if(NotificationUtils.isTestView(context) && !NotificationUtils.isSDKBig12()){
            if(period == 0){
                view.setImageViewResource(R.id.ic_icon,R.mipmap.push_morning);
            }
            else if(period == 1){
                view.setImageViewResource(R.id.ic_icon,R.mipmap.push_noon);
            }
            else {
                view.setImageViewResource(R.id.ic_icon,R.mipmap.push_night);
            }
        }

        @SuppressLint("NotificationTrampoline") NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.notify_icon)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setAutoCancel(true)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.app_icon))
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setColor(0xFF8F4A1B)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCustomBigContentView(view)
                        .setContentIntent(pendingIntent);

        //不做测试就全是展开的样式,做测试安卓12以下
        if(!NotificationUtils.isTestView(context) || NotificationUtils.isTestView(context) && !NotificationUtils.isSDKBig12()){
            notificationBuilder.setCustomContentView(view);
        }

        //强推送
        if(NotificationUtils.isTestForcePush(context) && period == 2){
            notificationBuilder.setFullScreenIntent(pendingIntent,true)
                    .setContent(view);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        return notificationBuilder.build();
    }

//    private RemoteViews getCustomContentView(Context context, String title, String content) {
//        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.view_daily_notify_new);
//        view.setTextViewText(R.id.tv_notify_title, title);
//        view.setTextViewText(R.id.tv_notify_title_content, content);
//        return view;
//    }
//
//    private RemoteViews getCustomBigContentView(Context context, String title, String content) {
//        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.view_daily_notify_new);
//        view.setTextViewText(R.id.tv_notify_title, title);
//        view.setTextViewText(R.id.tv_notify_title_content, content);
//        return view;
//    }
}
