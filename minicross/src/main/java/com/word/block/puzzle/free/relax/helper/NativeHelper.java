package com.word.block.puzzle.free.relax.helper;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.word.block.puzzle.free.relax.helper.notify.NotificationHelper;
import com.word.block.puzzle.free.relax.helper.utils.DeviceIdGenerator;

import java.util.UUID;

public class NativeHelper {

    public static String getUUID(Context context) {
        String deviceid = null;
        try {
            deviceid = DeviceIdGenerator.readDeviceId(context);
        } catch (Exception e) {
        }
        if (TextUtils.isEmpty(deviceid)) {
            deviceid = UUID.randomUUID().toString();
        }
        return deviceid;
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    //unity清除通知
    public static void cleanAllNoti(Context context) {
        NotificationManager notificationManager = NotificationHelper.getInstance(context).getNotificationManager(context);
        notificationManager.cancelAll();
    }
}
