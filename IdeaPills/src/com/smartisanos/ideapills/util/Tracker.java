package com.smartisanos.ideapills.util;

import com.smartisanos.ideapills.IdeaPillsApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import smartisanos.app.tracker.Agent;

public class Tracker {
    private static final LOG log = LOG.getInstance(Tracker.class);
    private static ExecutorService sAddDownloadTaskExecutor;
    private static final AtomicBoolean INIT_DONE = new AtomicBoolean(false);

    private static void runInTaskAddThread(Runnable runnable) {
        synchronized (Tracker.class) {
            if (sAddDownloadTaskExecutor == null || sAddDownloadTaskExecutor.isShutdown()) {
                sAddDownloadTaskExecutor = Executors.newSingleThreadExecutor();
            }
        }
        sAddDownloadTaskExecutor.execute(runnable);
    }

    private static boolean isInitAgentDone() {
        return INIT_DONE.get();
    }

    private static void initAgentInternal() {
        if (isInitAgentDone()) {
            LOG.d("already init return");
            return;
        }

        try {
            Agent.getInstance().init(IdeaPillsApp.getInstance());
            INIT_DONE.compareAndSet(false, true);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void init() {
        runInTaskAddThread(new TrackerRunnable(null));
    }


    public static void flush() {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().flush();
            }
        }));

    }

    public static void onLaunch() {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    Agent.getInstance().onLaunch();
                } catch (Exception e) {
                    // NA
                }
            }
        }));
    }

    public static void onBeginTimeing(final String eventId) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onBeginTiming(eventId);
            }
        }));
    }

    public static void onEndTiming(final String eventId) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onEndTiming(eventId);
            }
        }));
    }

    public static void onStatus(final String eventId, final Object... data) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onStatus(eventId, toJSONObject(data));
            }
        }));


    }

    public static void onStatus(final String eventId, final LinkedHashMap<String, Object> data) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onStatus(eventId, LinkedHashMapToJson(data).toString());
            }
        }));
    }

    public static void onEvent(final String eventId, final LinkedHashMap<String, Object> data) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onEvent(eventId, LinkedHashMapToJson(data).toString());
            }
        }));
    }

    public static void onEvent(final String eventId, final Object... data) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onEvent(eventId, toJSONObject(data));
            }
        }));
    }

    public static void onEvent(final String eventId) {
        runInTaskAddThread(new TrackerRunnable(new Runnable() {
            @Override
            public void run() {
                Agent.getInstance().onEvent(eventId);
            }
        }));
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
                return null;
            }
        }
        return jsonResult;
    }

    private static String toJSONObject(Object... data) {
        if (data.length > 0 && (data.length & 0x01) == 0) {
            JSONObject jsonObject = new JSONObject();
            for (int i = 1; i < data.length; i += 2) {
                String key = (String) data[i - 1];
                Object value = data[i];
                try {
                    jsonObject.put(key, value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return jsonObject.toString();
        }
        return null;
    }

    private static class TrackerRunnable implements Runnable {

        private Runnable mRunnable;

        TrackerRunnable(Runnable runnable) {
            mRunnable = runnable;
        }

        @Override
        public void run() {
            if (!isInitAgentDone()) {
                initAgentInternal();
            }
            if (isInitAgentDone() && mRunnable != null) {
                try {
                    mRunnable.run();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}