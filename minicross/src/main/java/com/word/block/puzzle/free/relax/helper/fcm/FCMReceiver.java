package com.word.block.puzzle.free.relax.helper.fcm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.word.block.puzzle.free.relax.helper.FirebaseManager;
import com.word.block.puzzle.free.relax.helper.notify.NotificationHelper;
import com.word.block.puzzle.free.relax.helper.notify.NotificationUtils;
import com.word.block.puzzle.free.relax.helper.utils.SharedPreferencesUtils;

public class FCMReceiver extends BroadcastReceiver {
    public static final String ACTION_NOTIFY_CLICK = ".fcm.ACTION_CLICK_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(NotificationHelper.LOG_TAG,"fcm receive click");

        if(!NotificationHelper.getInstance(context).isNotiOpen(context))
            return;

        if(!NotificationHelper.getInstance(context).isFCMOpen(context))
            return;

        if ((context.getPackageName() + ACTION_NOTIFY_CLICK).equals(intent.getAction())) {

            handleClickNotify(context,intent);
        }
    }

    private void handleClickNotify(Context context, Intent intent) {
        int code = intent.getIntExtra(FCMService.EXTRA_REQUEST_CODE, 0);
        if (code != FCMService.FCM_REQUEST_CODE)
            return;

        //点击
        String period = intent.getStringExtra(FCMService.EXTRA_PERIOD_CODE);
        int id = intent.getIntExtra(FCMService.PUSH_INFO_ID,-1);
        String eventName = FirebaseManager.EVENT_FCMPUSH_M_OPEN;
        String suffix = "morning";
        if (period.equals("1")) {
            eventName = FirebaseManager.EVENT_FCMPUSH_N_OPEN;
            suffix = "noon";
        } else if (period.equals("2")) {
            eventName = FirebaseManager.EVENT_FCMPUSH_E_OPEN;
            suffix="night";
        }

        FirebaseManager.getInstance(context).logDailyNotificationEvent(eventName,"",id);
        FirebaseManager.getInstance(context).logDailyNotificationEvent(FirebaseManager.EVENT_CLICK_DAILY_NOTIFICATION,suffix,id);

        //check first click
        String todayOpenDate = SharedPreferencesUtils.getInstance(context).get(NotificationHelper.KEY_GAME_TODAY_FIRST_OPEN_DATE_KEY, "");
        if (!NotificationUtils.checkIsToday(todayOpenDate)) {
            FirebaseManager.getInstance(context).logDailyNotificationEvent(FirebaseManager.EVENT_OPEN_WITH_DAILY_NOTIFICATION,"",id);
        }

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        context.startActivity(launch);

        Log.i(NotificationHelper.LOG_TAG,"fcm click");
    }
}
