package com.smartisanos.ideapills.remind.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;

import com.smartisanos.ideapills.common.remind.util.CalendarUtils;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.data.Table;
import com.smartisanos.ideapills.remind.RemindBubbleShowActivity;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AlarmUtils {
    private static final LOG log = LOG.getInstance(AlarmUtils.class);
    public static final String KEY_ALARM_ID = "alarmId";

    public static final String CALENDAR_IDEA_PILL = "idea pill";
    public static final String RELATION_APP = "relation_app";
    public static final String RELATION_ID = "relation_id";

    public static void scheduleNextAlarm(Context context, AlarmManager manager) {
        BubbleItem nextAlarmBubbleItem = GlobalBubbleManager.getInstance().getNextAlarmBubble();
        cancelAlarm(context, manager);
        if (nextAlarmBubbleItem != null) {
            log.info("scheduleNextAlarm:" + nextAlarmBubbleItem.getText()
                    + "," + nextAlarmBubbleItem.getId() + ",time:" + nextAlarmBubbleItem.getRemindTime());
            scheduleAlarm(context, manager, nextAlarmBubbleItem);
        } else {
            log.info("scheduleNextAlarm no alarm");
        }
    }

    public static void scheduleAlarm(Context context, AlarmManager manager, BubbleItem bubbleItem) {
        boolean quietUpdate = false;
        if (manager == null) {
            manager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
        }

        int alarmType = AlarmManager.RTC_WAKEUP;
        if (quietUpdate) {
            alarmType = AlarmManager.RTC;
        }

        Intent intent = new Intent(context, RemindBubbleShowActivity.class);
        intent.putExtra(RemindBubbleShowActivity.EXTRA_FROM, true);
        intent.putExtra(RemindBubbleShowActivity.EXTRA_ID, bubbleItem.getId());
        intent.putExtra(RemindBubbleShowActivity.EXTRA_TIME, bubbleItem.getRemindTime());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent remindPendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        manager.setExact(alarmType, bubbleItem.getRemindTime(), remindPendingIntent);
    }

    public static void cancelAlarm(Context context, AlarmManager manager) {
        if (manager == null) {
            manager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
        }

        Intent intent = new Intent(context, RemindBubbleShowActivity.class);
        PendingIntent remindPendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        manager.cancel(remindPendingIntent);
    }

    public static void deleteAlarmFromCalendar(Context context, BubbleItem bubbleItem) {
        String calId = readCalendar(context);
        if (TextUtils.isEmpty(calId)) {
            return;
        }
        String eventId = getEventId(context, bubbleItem, calId);
        if (!TextUtils.isEmpty(eventId)) {
            deleteEvent(context, eventId);
        }
    }

    public static void deleteAlarmFromCalendar(Context context, List<String> createDates) {
        if (createDates == null || createDates.isEmpty()) {
            return;
        }
        String calId = readCalendar(context);
        if (TextUtils.isEmpty(calId)) {
            return;
        }
        String inSql = Table.inSql(RELATION_ID, createDates);
        context.getContentResolver().delete(CalendarContract.Events.CONTENT_URI,
                CalendarContract.Events.CALENDAR_ID + "=? AND " + inSql, new String[]{calId});
    }

    public static void deleteAllAlarmFromCalendar(Context context) {
        String calId = readCalendar(context);
        if (TextUtils.isEmpty(calId)) {
            return;
        }
        context.getContentResolver().delete(CalendarContract.Events.CONTENT_URI,
                CalendarContract.Events.CALENDAR_ID + "=?", new String[]{calId});
    }

    public static void replaceAlarmToCalendar(final Context context, final List<BubbleItem> bubbleItems) {
        if (bubbleItems == null || bubbleItems.isEmpty()) {
            return;
        }
        String calId = readCalendar(context);
        if (TextUtils.isEmpty(calId)) {
            BubbleItem bubbleItem = bubbleItems.get(0);
            final List<BubbleItem> otherItems = new ArrayList<BubbleItem>();
            if (bubbleItems.size() > 1) {
                for (int i = 1; i < bubbleItems.size(); i++) {
                    otherItems.add(bubbleItems.get(i));
                }
            }
            final Intent addEventIntent = genAddEventBroadcast(bubbleItem);
            if (Looper.getMainLooper() == Looper.myLooper()) {
                context.sendBroadcast(addEventIntent);
                if (!otherItems.isEmpty()) {
                    // delay to wait add ideaPill calendar success, then bulk insert
                    DataHandler.postDelayed(new Runnable() {
                        public void run() {
                            replaceAlarmToCalendar(context, otherItems);
                        }
                    }, 50);
                }
            } else {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            context.sendBroadcast(addEventIntent);
                            if (!otherItems.isEmpty()) {
                                // delay to wait add ideaPill calendar success, then bulk insert
                                DataHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        replaceAlarmToCalendar(context, otherItems);
                                    }
                                }, 50);
                            }
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                });
            }
        } else {
            Map<String, String> relationMap = getEventIds(context, bubbleItems, calId);
            for (BubbleItem bubbleItem : bubbleItems) {
                if (bubbleItem.getCreateAt() > 0 && !TextUtils.isEmpty(bubbleItem.getText())) {
                    String eventId = relationMap.get(String.valueOf(bubbleItem.getCreateAt()));
                    if (!TextUtils.isEmpty(eventId)) {
                        updateEvent(context, bubbleItem, eventId);
                    } else {
                        addEvent(context, calId, bubbleItem);
                    }
                }
            }
        }
    }

    public static void replaceAlarmToCalendar(final Context context, BubbleItem bubbleItem) {
        String calId = readCalendar(context);
        if (TextUtils.isEmpty(calId)) {
            if (bubbleItem.getCreateAt() > 0 && !TextUtils.isEmpty(bubbleItem.getText())) {
                final Intent addEventIntent = genAddEventBroadcast(bubbleItem);
                if (Looper.getMainLooper() == Looper.myLooper()) {
                    context.sendBroadcast(addEventIntent);
                } else {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                context.sendBroadcast(addEventIntent);
                            } catch (Exception e) {
                                //ignore
                            }
                        }
                    });
                }

            }
        } else {
            if (bubbleItem.getCreateAt() > 0 && !TextUtils.isEmpty(bubbleItem.getText())) {
                String eventId = getEventId(context, bubbleItem, calId);
                if (!TextUtils.isEmpty(eventId)) {
                    updateEvent(context, bubbleItem, eventId);
                } else {
                    addEvent(context, calId, bubbleItem);
                }
            }
        }
    }

    private static String readCalendar(Context context) {
        Cursor userCursor = null;
        try {
            userCursor = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, null,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + "=? and "
                            + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + "=? and "
                            + CalendarContract.Calendars.DELETED + "=?",
                    new String[]{CALENDAR_IDEA_PILL, CalendarContract.Calendars.CAL_ACCESS_READ + "", "0"}, null);

            if (userCursor != null && userCursor.moveToFirst()) {
                int idColumn = userCursor.getColumnIndex(CalendarContract.Calendars._ID);
                return userCursor.getString(idColumn); //日历id
            }
        } catch (Exception e) {
            //ignore
        } finally {
            if (userCursor != null) {
                userCursor.close();
            }
        }
        return null;
    }

    private static Intent genAddEventBroadcast(BubbleItem bubbleItem) {
        long dueDate = bubbleItem.getRemindTime() == 0 ? CalendarUtils.convertAlldayLocalToUTC(bubbleItem.getDueDate()) : bubbleItem.getDueDate();
        long dueDateEnd = bubbleItem.getRemindTime() == 0 ? dueDate + 24 * 60 * 60 * 1000 : dueDate + 60 * 1000;
        final Intent addEventIntent = new Intent();
        addEventIntent.setAction("com.smartisanos.calendar.ADD_EVENT");
        addEventIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        addEventIntent.putExtra(CalendarContract.Events.DTSTART, dueDate);
        addEventIntent.putExtra(CalendarContract.Events.DTEND, dueDateEnd);
        addEventIntent.putExtra(CalendarContract.Events.TITLE, bubbleItem.getText());
        addEventIntent.putExtra(RELATION_APP, "com.smartisanos.ideapills");
        addEventIntent.putExtra(RELATION_ID, String.valueOf(bubbleItem.getCreateAt()));
        addEventIntent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, bubbleItem.getRemindTime() == 0 ? Time.TIMEZONE_UTC : TimeZone.getDefault().getID());
        addEventIntent.putExtra(CalendarContract.Events.ALL_DAY, bubbleItem.getRemindTime() == 0 ? 1 : 0);
        return addEventIntent;
    }

    /**
     * 添加日历事件
     */
    private static boolean addEvent(Context context, String calId, BubbleItem bubbleItem) {
        if (bubbleItem.getDueDate() <= 0) {
            return false;
        }
        ContentValues value = new ContentValues();
        //必传
        value.put(CalendarContract.Events.CALENDAR_ID, calId);
        long dueDate = bubbleItem.getRemindTime() == 0 ? CalendarUtils.convertAlldayLocalToUTC(bubbleItem.getDueDate()) : bubbleItem.getDueDate();
        long dueDateEnd = bubbleItem.getRemindTime() == 0 ? dueDate + 24 * 60 * 60 * 1000 : dueDate + 60 * 1000;
        value.put(CalendarContract.Events.DTSTART, dueDate);
        value.put(CalendarContract.Events.DTEND, dueDateEnd);
        value.put(CalendarContract.Events.TITLE, bubbleItem.getText() == null ? "" : bubbleItem.getText());
        value.put(RELATION_APP, "com.smartisanos.ideapills");
        value.put(RELATION_ID, String.valueOf(bubbleItem.getCreateAt()));
        value.put(CalendarContract.Events.EVENT_TIMEZONE, bubbleItem.getRemindTime() == 0 ? Time.TIMEZONE_UTC : TimeZone.getDefault().getID());

        //非必传
        value.put(CalendarContract.Events.HAS_ALARM, 0);
        value.put(CalendarContract.Events.ALL_DAY, bubbleItem.getRemindTime() == 0 ? 1 : 0);

        Uri result = context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, value);
        long id = result == null ? -1 : ContentUris.parseId(result); //返回的事件id
        return id >= 0;
    }

    /**
     * 删除日历事件
     */
    private static boolean deleteEvent(Context context, String eventId) {
        int result = context.getContentResolver().delete(CalendarContract.Events.CONTENT_URI,
                CalendarContract.Events._ID + "=?", new String[]{eventId});
        return result > 0;
    }

    /**
     * 更新日历事件
     */
    private static boolean updateEvent(Context context, BubbleItem bubbleItem, String eventId) {
        ContentValues value = new ContentValues();
        if (bubbleItem.getDueDate() > 0) {
            long dueDate = bubbleItem.getRemindTime() == 0 ? CalendarUtils.convertAlldayLocalToUTC(bubbleItem.getDueDate()) : bubbleItem.getDueDate();
            long dueDateEnd = bubbleItem.getRemindTime() == 0 ? dueDate + 24 * 60 * 60 * 1000 : dueDate + 60 * 1000;
            value.put(CalendarContract.Events.DTSTART, dueDate);
            value.put(CalendarContract.Events.DTEND, dueDateEnd);
            value.put(CalendarContract.Events.EVENT_TIMEZONE, bubbleItem.getRemindTime() == 0 ? Time.TIMEZONE_UTC : TimeZone.getDefault().getID());
        }
        value.put(CalendarContract.Events.TITLE, bubbleItem.getText() == null ? "" : bubbleItem.getText());
        value.put(CalendarContract.Events.ALL_DAY, bubbleItem.getRemindTime() == 0 ? 1 : 0);
        int result = context.getContentResolver().update(CalendarContract.Events.CONTENT_URI, value,
                CalendarContract.Events._ID + "=?", new String[]{eventId});
        return result > 0;
    }

    private static String getEventId(Context context, BubbleItem bubbleItem, String calId) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                    new String[]{CalendarContract.Events._ID},
                    RELATION_ID + "=? AND " + CalendarContract.Events.CALENDAR_ID + " = ?",
                    new String[]{String.valueOf(bubbleItem.getCreateAt()), calId}, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex(CalendarContract.Events._ID);
                return cursor.getString(idColumn);
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static Map<String, String> getEventIds(Context context, List<BubbleItem> bubbleItems, String calId) {
        if (bubbleItems == null || bubbleItems.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> relateMap = new HashMap<>();
        List<String> createDates = new ArrayList<>();
        for (BubbleItem bubbleItem : bubbleItems) {
            createDates.add(String.valueOf(bubbleItem.getCreateAt()));
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                    new String[]{RELATION_ID, CalendarContract.Events._ID},
                    Table.inSql(RELATION_ID, createDates) + " AND " + CalendarContract.Events.CALENDAR_ID + " = ?",
                    new String[]{calId}, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex(CalendarContract.Events._ID);
                int relationIdColumn = cursor.getColumnIndex(RELATION_ID);
                relateMap.put(cursor.getString(relationIdColumn), cursor.getString(idColumn));
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return relateMap;
    }
}
