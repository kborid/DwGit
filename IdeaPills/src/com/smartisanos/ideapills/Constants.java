package com.smartisanos.ideapills;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.receiver.DebugIntentReceiver;
import com.smartisanos.ideapills.sync.share.GlobalInvitationAction;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.view.FiltrateSetting;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

public class Constants {
    private static final LOG log = LOG.getInstance(Constants.class);

    public static final String SIDEBAR  = "com.smartisanos.sidebar";
    public static final String CALENDAR = "com.android.calendar";
    public static final String EMAIL    = "com.android.email";
    public static final String NOTES    = "com.smartisanos.notes";
    public static final String RECORDER = "com.smartisanos.recorder";
    public static final String BOOM     = "com.smartisanos.textboom";
    public static final String VOICE    = "com.smartisanos.sara";
    public static final String WECHAT   = "com.tencent.mm";
    public static final String BULLET_MESSENGER   = "com.bullet.messenger";

    public static final String EMAIL_COMPONENT = "com.android.email.activity.ComposeActivityEmail";
    public static final String WECHAT_SHARE_COMPONENT = "com.tencent.mm.ui.tools.ShareImgUI";
    public static final String CALENDAR_SHARE_COMPONENT = "com.android.calendar.event.EditEventActivity";

    public static final String BROADCAST_ACTION_INVITATION_SEND = "com.smartisanos.ideapills.share_invitation";
    public static final String LOCAL_BROADCAST_ACTION_BUBBLE_LIST_HIDE = "com.smartisanos.ideapills.bubble_list_hide";

    private static final String SYSTEM_DIALOG_REASON_EAT_HOME_KEY = "eathomekey";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String ACTION_REFRESH_BUBBLE_LIST = "action_refesh_bubble_list";
    public static final String VOICE_TODO_OVER_CYCLE_TYPE = "voice_todo_over_cycle_type";
    public static final String SYNC_ACCEPT_SHARE = "sync_accept_share";
    public static final String STATUS_BAR_EXPAND = "status_bar_expanded";
    public static final int STATUS_BAR_EXPANDED = 2;
    public static final long DAY_SECOND = 1000 * 60 * 60 * 24;
    public static final long WEEK_SECOND = 1000 * 60 * 60 * 24*7;
    public static final int ONE_HOUR_SECOND = 60 * 60 * 1000;
    public static final int MINITE_OF_HOUR = 60;
    public static final int VOICE_TODO_OVER_IMMEDIATELY = 0;
    public static final int VOICE_TODO_OVER_DAYLY = 1;
    public static final int VOICE_TODO_OVER_WEEKLY = 2;
    public static final int DEFAULT_HOUR = 4;
    public static final float DISABLE_ALPHA = 0.35f;
    public static final float ENABLE_ALPHA = 1f;
    public static final int BUBBLE_ANIM_DURATION = 250;
    public static int TODO_OVER_CLEAN_TYPE = VOICE_TODO_OVER_IMMEDIATELY;
    private static Context mContext;
    public static int WINDOW_WIDTH;
    public static int WINDOW_HEIGHT;
    public static int STATUS_BAR_HEIGHT;
    public static int SIDE_VIEW_WIDTH;
    public static int TOP_VIEW_WIDTH;
    public static int TOP_VIEW_HEIGHT;
    public static int BUBBLE_OPT_LAYOUT_WIDTH;
    public static int DRAG_TEXT_ARROW_OFFSET;
    public static int DRAG_TEXT_TOUCH_POINT_OFFSET;

    public static float WIDTH_SCALE;
    public static float HEIGHT_SCALE;

    public static boolean IS_IDEA_PILLS_ENABLE = false;
    public static int DEFAULT_BUBBLE_COLOR;
    public static long TODO_OVER_CLEANING_CYCLE = -1;

    public static int HORIZONTAL_SCROLL_RESPONSE_AREA_TOP;
    public static int HORIZONTAL_SCROLL_RESPONSE_AREA_BOTTOM;

    public static boolean BOOM_INSTALLED = false;
    public static boolean WECHAT_INSTALLED = false;
    public static boolean BULLET_MESSENGER_INSTALLED = false;
    public static volatile boolean WINDOW_READY = false;
    public static volatile boolean DATA_INIT_READY = false;
    public static final String CHINA_CODE = "+86";

    public static final String ClOUD_EVER_LOGIN = "cloud_ever_login"; //0  nerver login ;1 login
    public static final String CLOUD_IDEAPILL_CHECKED = "cloud_ideapill_checked";  //0 not check; 1 checked

    public static void initGlobalInfo(Context context) {
        //read system config
        IS_IDEA_PILLS_ENABLE = Utils.isIdeaPillsEnable(context);
        boolean isFeaturePhoneMode = settingGlobalGetInt(context, InterfaceDefine.SETTINGS_FEATURE_PHONE_MODE, 0) == 1;
        StatusManager.setStatus(StatusManager.FEATURE_PHONE_MODE, isFeaturePhoneMode);
        DEFAULT_BUBBLE_COLOR = settingGlobalGetInt(context, InterfaceDefine.SETTINGS_DEFAULT_BUBBLE_COLOR, GlobalBubble.COLOR_BLUE);
    }

    public static void init(Context context) {
        mContext = context;
        int[] size = getWindowSize(context);
        WINDOW_WIDTH  = size[0];
        WINDOW_HEIGHT = size[1];
        Resources resources = context.getResources();
        BUBBLE_OPT_LAYOUT_WIDTH = resources.getDimensionPixelSize(R.dimen.bubbleopt_layout_width);

        STATUS_BAR_HEIGHT = resources.getDimensionPixelSize(R.dimen.status_bar_height);

        SIDE_VIEW_WIDTH     = resources.getDimensionPixelSize(R.dimen.sidebar_width);
        float scaleRate     = 1.0f - SIDE_VIEW_WIDTH * 1.0f / Constants.WINDOW_WIDTH;
        TOP_VIEW_WIDTH      = Constants.WINDOW_WIDTH - SIDE_VIEW_WIDTH;
        TOP_VIEW_HEIGHT     = (int) (Constants.WINDOW_HEIGHT * (1.0f - scaleRate));

        WIDTH_SCALE = (WINDOW_WIDTH - SIDE_VIEW_WIDTH) * 1.0f / (WINDOW_WIDTH * 1.0f);
        HEIGHT_SCALE = (WINDOW_HEIGHT - TOP_VIEW_HEIGHT) * 1.0f / (WINDOW_HEIGHT * 1.0f);

        DRAG_TEXT_ARROW_OFFSET = resources.getDimensionPixelSize(com.smartisanos.internal.R.dimen.drag_normal_arrow_point_offset);
        DRAG_TEXT_TOUCH_POINT_OFFSET = resources.getDimensionPixelSize(com.smartisanos.internal.R.dimen.drag_touch_point_offset);
        HORIZONTAL_SCROLL_RESPONSE_AREA_TOP = resources.getDimensionPixelSize(R.dimen.horizontal_scroll_response_area_top);
        HORIZONTAL_SCROLL_RESPONSE_AREA_BOTTOM = resources.getDimensionPixelSize(R.dimen.horizontal_scroll_response_area_bottom);

        //update ui
        BubbleController.getInstance().requestBubbleOptLayoutUpdateRegion();
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                BubbleController.getInstance().updateVisibility();
            }
        });

        registerHomeKeyReceiver();

        if (LOG.DBG) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(DebugIntentReceiver.ACTION);
            mContext.registerReceiver(new DebugIntentReceiver(), filter);
        }

        if (PackageUtils.isAvilibleApp(context, WECHAT)) {
            WECHAT_INSTALLED = true;
        }

        if (PackageUtils.isAvilibleApp(context, BOOM)) {
            BOOM_INSTALLED = true;
        }

        if (PackageUtils.isAvilibleApp(context, BULLET_MESSENGER)) {
            BULLET_MESSENGER_INSTALLED = true;
        }
        updateTodoOverType();
    }

    public static int getVerticalScrollResponseAreaTop(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.vertical_scroll_response_area_top);
    }

    public static int getVerticalScrollResponseAreaBottom(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.vertical_scroll_response_area_bottom);
    }

    public static int settingGlobalGetInt(Context context, String key, int def) {
        try {
            ContentResolver resolver = context.getContentResolver();
            return Settings.Global.getInt(resolver, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static long settingGlobalGetLong(Context context, String key, long def) {
        try {
            ContentResolver resolver = context.getContentResolver();
            return Settings.Global.getLong(resolver, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static int[] getWindowSize(Context context) {
        int[] size = new int[2];
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        if (metrics.heightPixels > metrics.widthPixels) {
            size[0] = metrics.widthPixels;
            size[1] = metrics.heightPixels;
        } else {
            size[0] = metrics.heightPixels;
            size[1] = metrics.widthPixels;
        }
        return size;
    }

    private static final String ALARM_TASK_ACTION = "com.smartisanos.ideapills.AlarmTask";

    //todo : this feature is odd
    public static void startAlarmTask(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ALARM_TASK_ACTION);
        mContext.registerReceiver(mAlarmTaskBroadcastReceiver, filter);
        Calendar calendar = Calendar.getInstance();
        long systemtime = System.currentTimeMillis();
        calendar.setTimeInMillis(systemtime);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        long endTime = calendar.getTimeInMillis();
        long alarmTime = systemtime + (((long) (new Random().nextDouble() * (endTime - systemtime))));
        Intent intent = new Intent(ALARM_TASK_ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pi);
    }

    private static final BroadcastReceiver mAlarmTaskBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log.info("onReceive action:" + action);
            if (ALARM_TASK_ACTION.equals(action)) {
                if (!IS_IDEA_PILLS_ENABLE) {
                    return;
                }
                GlobalBubbleManager.getInstance().trackSettingsStatus();
            }
        }
    };

    private static void registerHomeKeyReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mContext.registerReceiver(mHomeKeyBroadcastReceiver, filter);
    }

    private static final BroadcastReceiver mHomeKeyBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                GlobalInvitationAction.getInstance().hideInvitationDialog();
                if (!Constants.IS_IDEA_PILLS_ENABLE) {
                    return;
                }
                if (!Constants.WINDOW_READY) {
                    log.error("window not init finished");
                    return;
                }
                String reason = intent.getStringExtra("reason");
                if (SYSTEM_DIALOG_REASON_EAT_HOME_KEY.equals(reason)
                        || SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            GlobalBubbleManager.getInstance().handleActionHome();
                        }
                    });
                    if (BubbleController.getInstance().isAlreadyShow()) {
                        UIHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BubbleController.getInstance().playHideAnimation(false);
                            }
                        }, 150L);
                    }
                }
            }
        }
    };

    public static void dumpStatus() {
        log.error("IS_IDEA_PILLS_ENABLE = " + IS_IDEA_PILLS_ENABLE);
        log.error("DEFAULT_BUBBLE_COLOR = " + DEFAULT_BUBBLE_COLOR);
    }

    public static void updateTodoOverType() {
        ContentResolver cr = mContext.getContentResolver();
        TODO_OVER_CLEAN_TYPE = Settings.Global.getInt(cr, VOICE_TODO_OVER_CYCLE_TYPE, VOICE_TODO_OVER_DAYLY);
    }

    public static long getHideBubblesTime() {
        long time = System.currentTimeMillis();
        switch (TODO_OVER_CLEAN_TYPE) {
            case VOICE_TODO_OVER_DAYLY:
                time = getDaylyTimeMills();
                break;
            case VOICE_TODO_OVER_WEEKLY:
                time = getWeeklyTimeMills();
                break;
        }
        return time;
    }

    public static long getDaylyTimeMills() {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());
        current.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH), Constants.DEFAULT_HOUR, 0);
        long time = cal.getTimeInMillis();
        if(current.get(Calendar.HOUR)< Constants.DEFAULT_HOUR){
            return time - DAY_SECOND;
        }
        return cal.getTimeInMillis();
    }

    public static long getWeeklyTimeMills() {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());
        current.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH), Constants.DEFAULT_HOUR, 0);
        int dayofweek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (dayofweek == 0)
            dayofweek = 7;
        cal.add(Calendar.DATE, -dayofweek + 1);
        return cal.getTimeInMillis();
    }

    public static int getNewBubbleColor() {
        int color = getDefaultBubbleColor();
        if (color == GlobalBubble.COLOR_SHARE) {
            return GlobalBubble.COLOR_BLUE;
        }
        return color;
    }

    public static int getDefaultBubbleColor() {
        int color = FiltrateSetting.getFiltrateColor(mContext);
        if (color == FiltrateSetting.FILTRATE_ALL) {
            color = Constants.DEFAULT_BUBBLE_COLOR;
        }
        return color;
    }
}