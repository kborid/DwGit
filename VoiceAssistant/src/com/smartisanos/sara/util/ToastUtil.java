package com.smartisanos.sara.util;
import com.smartisanos.sara.SaraApplication;
import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    private static Toast toast;

    public static void showToast(Context context, String content, int duration) {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }

        toast = Toast.makeText(context.getApplicationContext(), content, duration);
        toast.show();
    }

    public static void showToast(Context context, int resId, int duration) {
        showToast(context, context.getString(resId), duration);
    }

    public static void showToast(Context context, String content) {
        showToast(context, content, Toast.LENGTH_SHORT);
    }

    public static void showToast(int resId) {
        Context context = SaraApplication.getInstance().getApplicationContext();
        showToast(context, (String) context.getResources().getText(resId));
    }

}
