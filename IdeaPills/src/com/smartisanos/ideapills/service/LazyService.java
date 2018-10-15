package com.smartisanos.ideapills.service;

import android.annotation.NonNull;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.PhoneState;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.interfaces.LocalInterface;
import com.smartisanos.ideapills.receiver.PackageActionReceiver;
import com.smartisanos.ideapills.util.GlobalBubbleManager;

public class LazyService extends IntentService implements IActions {

    public static void startService(@NonNull Context context, String action) {
        Intent intent = new Intent(context, LazyService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public LazyService() {
        super("LazyService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_INIT_CONSTANTS.equals(action)) {
                performInitConstants();
            } else if (ACTION_INIT_DEVICE_STATS.equals(action)) {
                performInitDeviceStats();
            }
        }
    }

    private void performInitConstants() {
        Constants.init(getApplication());
    }

    private void performInitDeviceStats() {
        final Context appContext = getApplication();
        PhoneState phoneState = new PhoneState(appContext);
        phoneState.registerTelephonyState();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        PackageActionReceiver receiver = new PackageActionReceiver();
        appContext.registerReceiver(receiver, filter);
        IntentFilter dateFilter = new IntentFilter();
        dateFilter.addAction(Intent.ACTION_DATE_CHANGED);
        appContext.registerReceiver(mDateChangeReceiver, dateFilter);
        Constants.startAlarmTask(this);
    }

    private final BroadcastReceiver mDateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_DATE_CHANGED.equals(action)) {
                if (Constants.IS_IDEA_PILLS_ENABLE) {
                    Constants.startAlarmTask(context);
                    GlobalBubbleManager.getInstance().reportBubbleNum();
                }
            }
        }
    };

}
