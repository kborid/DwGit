package com.smartisanos.sara.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.smartisanos.sara.bullet.widget.AvatarImageViewControl;
import com.smartisanos.sara.storage.DrawerDataRepository;

public class LazyService extends IntentService {
    public static void initRes(Context context) {
        startService(context, Actions.ACTION_INIT_RESOURCE);
    }

    public static void startService(Context context, String action) {
        Intent intent = new Intent(context, LazyService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    public LazyService() {
        super("LazyService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (TextUtils.equals(Actions.ACTION_INIT_RESOURCE, action)) {
                AvatarImageViewControl.getInstances(getApplication());
            }
        }
    }
}
