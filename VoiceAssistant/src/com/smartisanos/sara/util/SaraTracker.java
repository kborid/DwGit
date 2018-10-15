package com.smartisanos.sanbox.utils;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;

import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import smartisanos.api.SettingsSmt;
import smartisanos.app.tracker.Agent;
import smartisanos.util.LogTag;

/**
 * cloud Doc:
 * http://git.smartisan.com/wangjunwei/docs/blob/master/data-platform/events-42-flashidear.adoc#props-0002
 */
public class SaraTracker {

    private static final String TAG = "SaraTracker";

    private static ExecutorService sAddDownloadTaskExecutor;

    private static void runInTaskAddThread(Runnable runnable) {
        synchronized (SaraTracker.class) {
            if (sAddDownloadTaskExecutor == null || sAddDownloadTaskExecutor.isShutdown()) {
                sAddDownloadTaskExecutor = Executors.newSingleThreadExecutor();
            }
        }
        sAddDownloadTaskExecutor.execute(runnable);
    }

    public static void init(Application context) {
        Agent.getInstance().init(context);
    }

    public static void onLaunch() {
        runInTaskAddThread(new Runnable() {
            @Override
            public void run() {
                LogTag.d(TAG, "onLaunch ");
                try {
                    Agent.getInstance().onLaunch();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void trackAllIfNeeded(final Context context) {
        runInTaskAddThread(new Runnable() {
            @Override
            public void run() {
                String currentVersion = SystemProperties.get("ro.smartisan.version");
                String oldVersion = SharePrefUtil.getString(context, SaraConstant.PREF_SYSTEM_VERSION, null);
                if (oldVersion == null || !TextUtils.equals(currentVersion, oldVersion)) {
                    SharePrefUtil.savePref(context, SaraConstant.PREF_SYSTEM_VERSION, currentVersion);
                    trackVoiceAssistSettings(context);
                    flush();
                }
            }
        });
    }

    private static JSONObject LinkedHashMapToJson(LinkedHashMap<String, Object> data) {
        JSONObject jsonResult = new JSONObject();
        for (LinkedHashMap.Entry<String, Object> entry : data.entrySet()) {
            try {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    jsonResult.put(entry.getKey(), value);
                } else {
                    jsonResult.put(entry.getKey(), String.valueOf(value));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                LogTag.d(TAG, "LinkedHashMapToJson catch JSONException " + e.getMessage());
                return null;
            }
        }
        return jsonResult;
    }

    public static void onEvent(final String eventId) {
        runInTaskAddThread(new Runnable() {
            @Override
            public void run() {
                LogTag.d(TAG, "onEvent " + eventId);
                Agent.getInstance().onEvent(eventId);
            }
        });
    }

    public static void onEvent(String eventId, String key, Object value) {
        LogTag.d(TAG, "onEvent " + eventId);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put(key, value);
        onEvent(eventId, data);
    }

    public static void onEvent(final String eventId, final LinkedHashMap<String, Object> data) {
        runInTaskAddThread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = LinkedHashMapToJson(data);
                if (jsonObject != null) {
                    LogTag.d(TAG, "onEvent " + eventId + " jsonObject:\n" + jsonObject.toString());
                    Agent.getInstance().onEvent(eventId, jsonObject.toString());
                }
            }
        });
    }

    public static void onStatus(String eventId, String key, Object value) {
        LogTag.d(TAG, "onStatus " + eventId);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put(key, value);
        onStatus(eventId, data);
    }

    public static void onStatus(final String eventId, final LinkedHashMap<String, Object> data) {
        runInTaskAddThread(new Runnable() {
            @Override
            public void run() {
                if (data.isEmpty()) {
                    return;
                }
                JSONObject jsonObject = LinkedHashMapToJson(data);
                if (jsonObject != null) {
                    LogTag.d(TAG, "onStatus " + eventId + " jsonObject:\n" + jsonObject.toString());
                    Agent.getInstance().onStatus(eventId, jsonObject.toString());
                }
            }
        });
    }

    public static void flush() {
        runInTaskAddThread(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().flush();
            }
        });
    }

    public static void trackVoiceAssistSettings(Context context) {
        ContentResolver cr = context.getContentResolver();
        LinkedHashMap<String, Object> trackerData = new LinkedHashMap<String, Object>();
        trackerData.put("switch", Settings.Global.getInt(cr, SettingsSmt.Global.VOICE_INPUT, 1));
        trackerData.put("quick_open", SaraUtils.getLeftSlideLunchGloblePillEnabled(context) ? 1 : 0);
        trackerData.put("default_input", 0);
        trackerData.put("todo", SaraUtils.getDefaultBubbleColor(context) == GlobalBubble.COLOR_ORANGE ? 1 : 0);
        trackerData.put("local_search", SaraUtils.getLocalInputEnabled(context) ? 1 : 0);
        trackerData.put("web_search", SaraUtils.getWebInputEnabled(context) ? 1 : 0);
        JSONObject jsonData = LinkedHashMapToJson(trackerData);
        final String KEY_SETTINGS_TRACKER_LAST = "settings_tracker_last";
        String lastTrackJson = SharePrefUtil.getString(context, KEY_SETTINGS_TRACKER_LAST, null);
        if (jsonData != null && !TextUtils.equals(lastTrackJson, jsonData.toString())) {
            SharePrefUtil.savePref(context, KEY_SETTINGS_TRACKER_LAST, jsonData.toString());
            onStatus("A420002", trackerData);
        }

    }
}
