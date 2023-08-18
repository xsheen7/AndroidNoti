package com.word.block.puzzle.free.relax.helper.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Utils {

    public static int String2Int(String str) {
        int num = 0;
        try {
            num = Integer.valueOf(str);
        } catch (Exception e) {
        }
        return num;
    }

    public static String formatDate(Date date){
        if(date==null)
            return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String time = sdf.format(date);
        return time;
    }

    /**
     * view截图
     *
     * @param view
     * @return
     */
    public static Bitmap screenShot(View view) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            view.draw(c);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.gc();
        }
        if (bitmap == null) {
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap drawingCache = view.getDrawingCache();
            if (drawingCache != null) {
                bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, drawingCache.getWidth(), drawingCache.getHeight());
            }
        }
        return bitmap;
    }

    public static String getDiskCacheRootDirPath(Context context){
        File cacheRootDir = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/Android/data/"+ context.getPackageName());
        if (!cacheRootDir.exists()) {
            cacheRootDir.mkdirs();
        }
        return cacheRootDir.getPath();
    }

    public static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
