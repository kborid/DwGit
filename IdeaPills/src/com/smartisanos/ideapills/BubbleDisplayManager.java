package com.smartisanos.ideapills;

import android.app.SmtPCUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.view.Display;

import com.smartisanos.ideapills.common.util.TaskUtils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.util.LOG;

import smartisanos.api.SettingsSmt;

public enum BubbleDisplayManager {
    INSTANCE;

    private final static String PERMISSION_BROADCAST_SWITCH_MODE = "com.android.permission.SWITCH_MODE";
    private static final String ACTION_NOTIFY_SWITCH_MODE = "com.android.server.pc.action.SWITCH_MODE";
    private static final LOG log = LOG.getInstance(BubbleDisplayManager.class);
    private int mDisplayId = -1;
    private Context mExtContext;
    private final BroadcastReceiver mNotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                log.d("mNotifyReceiver received a null intent");
                return;
            }
            String action = intent.getAction();
            if (ACTION_NOTIFY_SWITCH_MODE.equals(action)) {
                switchPCMode();
            }
        }
    };
    private ContentObserver mDisplayObserver = new ContentObserver(UIHandler.getHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            int extDisplayOn = Settings.Global.getInt(IdeaPillsApp.getInstance().getContentResolver(),
                    SettingsSmt.Global.GLOBAL_PC_MODE_SETTINGS, 0);
            if (extDisplayOn == 1) {
                onDisplayAdded(SmtPCUtils.getExtDisplayId(IdeaPillsApp.getInstance()));
            } else if (extDisplayOn == 0) {
                onDisplayRemoved(mDisplayId);
            }
        }
    };

    BubbleDisplayManager() {
    }

    public void start() {
        Uri uri = Settings.Global.getUriFor(SettingsSmt.Global.GLOBAL_PC_MODE_SETTINGS);
        IdeaPillsApp.getInstance().getContentResolver().registerContentObserver(uri, false, mDisplayObserver);

        int extDisplayOn = Settings.Global.getInt(IdeaPillsApp.getInstance().getContentResolver(),
                SettingsSmt.Global.GLOBAL_PC_MODE_SETTINGS, 0);
        if (extDisplayOn == 1) {
            int extDisplayId = SmtPCUtils.getExtDisplayId(IdeaPillsApp.getInstance());
            if (extDisplayId != -1) {
                onDisplayAdded(extDisplayId);
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFY_SWITCH_MODE);
        IdeaPillsApp.getInstance().registerReceiver(mNotifyReceiver, filter, PERMISSION_BROADCAST_SWITCH_MODE, null);
    }

    public void stop() {
        try {
            IdeaPillsApp.getInstance().getContentResolver().unregisterContentObserver(mDisplayObserver);
        } catch (Exception e) {
            e.printStackTrace();
            //ignore
        }
        try {
            IdeaPillsApp.getInstance().unregisterReceiver(mNotifyReceiver);
        } catch (Exception e) {
            e.printStackTrace();
            //ignore
        }
    }

    public void release() {
        stop();
        if (mDisplayId != -1) {
            onDisplayRemoved(mDisplayId);
        }
    }

    public boolean switchDisplayIfNeeded(boolean fromExtDisplay) {
        if (fromExtDisplay) {
            if (mExtContext != null) {
                TaskUtils.removeTask(mExtContext, ProxyActivity.PACKAGE_NAME, ProxyActivity.CLASS_NAME);
                BubbleController.switchInstance(true);
                return true;
            }
            log.d("switchDisplayIfNeeded:" + mExtContext);
        } else {
            TaskUtils.removeTask(mExtContext, ProxyActivity.PACKAGE_NAME, ExtDisplayProxyActivity.EXT_CLASS_NAME);
            BubbleController.switchInstance(false);
            return true;
        }
        return false;
    }

    public void onDisplayAdded(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            return;
        }

        // display not support
        if (!SmtPCUtils.isValidExtDisplay(IdeaPillsApp.getInstance(), displayId)) {
            return;
        }
        if (mDisplayId != -1) {
            onDisplayRemoved(mDisplayId);
        }
        Context extContext = SmtPCUtils.getDisplayContext(IdeaPillsApp.getInstance(), displayId);
        if (extContext != null) {
            try {
                mDisplayId = displayId;
                mExtContext = extContext;
                BubbleController.createExtInstance(mExtContext);
                BubbleController.getExtInstance().addIdeaPillsWindow();
                log.d("onDisplayAdded:" + mDisplayId + ",context:" + mExtContext);
            } catch (Exception e) {
                log.error(e);
                mDisplayId = -1;
                mExtContext = null;
            }
        }
    }

    public void onDisplayRemoved(int displayId) {
        log.d("onDisplayRemoved:" + displayId);
        if (displayId == Display.DEFAULT_DISPLAY
                || displayId != mDisplayId) {
            return;
        }
        if (BubbleController.getExtInstance() != null) {
            try {
                BubbleController.getExtInstance().removeIdeaPillsWindow();
            } catch (Exception e) {
                // ignore
            }
            BubbleController.destroyExtInstance();
        }
        mDisplayId = -1;
        mExtContext = null;
    }

    public void onDisplayChanged(int displayId) {
        log.d("onDisplayChanged:" + displayId);
    }

    private void switchPCMode() {
        if (mDisplayId != -1) {
            onDisplayRemoved(mDisplayId);
        } else {
            onDisplayAdded(SmtPCUtils.getExtDisplayId());
        }
    }

    public Context getExtContext(Context context) {
        if (mExtContext != null) {
            return mExtContext;
        }
        return SmtPCUtils.getDisplayContext(context, SmtPCUtils.getExtDisplayId());
    }
}
