
package com.smartisanos.ideapills.common.remind.util;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CalendarContract.CalendarCache;
import android.text.TextUtils;
import android.text.format.DateUtils;


import com.smartisanos.ideapills.common.remind.Time;

import java.util.Calendar;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Locale;

public class CalendarUtils {

    public static final int EVENT_TYPE_NORMAL = 1 << 1;
    public static final int MIN_CALENDAR_YEAR = 1970;
    public static final int MAX_CALENDAR_YEAR = 2037;

    private static final String CN = "CN";

    /**
     * Param time will be modified. adjustToBeginningOfWeek(yyyy/mm/dd
     * Wednesday, TIME.SUNDAY) -> yyyy/mm/(dd - 3) Sunday
     *
     * @param time
     * @param weekStart
     */
    public static void adjustToBeginningOfWeek(Time time, int weekStart) {
        int dayOfWeek = time.weekDay;
        int diff = dayOfWeek - weekStart;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            time.monthDay -= diff;
            time.normalize(true /* ignore isDst */);
        }
    }

    public static final int MONDAY_BEFORE_JULIAN_EPOCH = Time.EPOCH_JULIAN_DAY - 3;

    /**
     * Takes a number of weeks since the epoch and calculates the Julian day of
     * the Monday for that week. This assumes that the week containing the
     * {@link Time#EPOCH_JULIAN_DAY} is considered week 0. It returns the Julian
     * day for the Monday {@code week} weeks after the Monday of the week
     * containing the epoch.
     *
     * @param week Number of weeks since the epoch
     * @return The julian day for the Monday of the given week since the epoch
     */
    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeekInCalendar(Context context) {
        if (getDefaultWeekStartWithLocale(context) == Calendar.SUNDAY) {
            return Calendar.SUNDAY;
        } else {
            return Calendar.MONDAY;
        }
    }

    public static int getFirstDayOfWeekInTime(Context context) {
        if (getDefaultWeekStartWithLocale(context) == Calendar.SUNDAY) {
            return Time.SUNDAY;
        } else {
            return Time.MONDAY;
        }
    }

    public static int getDefaultWeekStartWithLocale(Context context) {
        int result = Calendar.MONDAY;
        Locale locale = context.getResources().getConfiguration().locale;
        if (Locale.US.getCountry().equals(locale.getCountry()) ||
                "ID".equals(locale.getCountry())) {
            result = Calendar.SUNDAY;
        }
        return result;
    }

    /**
     * Returns the week since {@link Time#EPOCH_JULIAN_DAY} (Jan 1, 1970)
     * adjusted for first day of week. This takes a julian day and the week
     * start day and calculates which week since {@link Time#EPOCH_JULIAN_DAY}
     * that day occurs in, starting at 0. *Do not* use this to compute the ISO
     * week number for the year.
     *
     * @param julianDay      The julian day to calculate the week number for
     * @param firstDayOfWeek Which week day is the first day of the week, see
     *                       {@link Time#SUNDAY}
     * @return Weeks since the epoch
     */
    public static int getWeeksSinceEpochFromJulianDay(int julianDay,
                                                      int firstDayOfWeek) {
        int diff = Time.THURSDAY - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        int refDay = Time.EPOCH_JULIAN_DAY - diff;
        return (julianDay - refDay) / 7;
    }

    /**
     * check the day if in the range of our rule
     *
     * @param millitime
     * @return
     */
    public static boolean isValidDay(long millitime) {
        Time minTime = new Time();
        minTime.set(1, 0, 1970);
        long minMin = minTime.normalize(true);
        Time maxTime = new Time();
        maxTime.set(1, 0, 2038);
        long maxMin = maxTime.normalize(true);
        return millitime <= maxMin && millitime >= minMin;
    }

    public static boolean isTheSameDay(Time a, Time b) {
        if (a == null || b == null) {
            return false;
        }
        return a.year == b.year && a.month == b.month && a.monthDay == b.monthDay;
    }

    /**
     * setFisrtDayOfWeek according to the our own setting.
     * First day of week may be {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    public static void setFisrtDayOfWeek(Context context, Calendar... cals) {
        int firstDayOfWeekInCalendar = getFirstDayOfWeekInCalendar(context);
        for (Calendar c : cals) {
            c.setFirstDayOfWeek(firstDayOfWeekInCalendar);
            c.setMinimalDaysInFirstWeek(4);
            // clear this field so that it can be recalculated.
            c.clear(Calendar.WEEK_OF_YEAR);
        }
    }

    public static boolean turningMonthIsValid(Time time, boolean isToNext) {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time.toMillis(true));

        int filed = Calendar.MONTH;
        int value = 1;

        if (isToNext) {
            cal.add(filed, value);
            return cal.get(Calendar.YEAR) <= MAX_CALENDAR_YEAR;
        } else {
            cal.add(filed, 0 - value);
            return cal.get(Calendar.YEAR) >= MIN_CALENDAR_YEAR;
        }
    }

    public static String addEnglishNumberSuffix(int n) {
        return n + getEnglishNumberSuffix(n);
    }

    public static String getEnglishNumberSuffix(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    private static final TimeZoneUtils mTZUtils = new TimeZoneUtils(
            "com.android.calendar_preferences");

    /**
     * Gets the time zone that Calendar should be displayed in This is a helper
     * method to get the appropriate time zone for Calendar. If this is the
     * first time this method has been called it will initiate an asynchronous
     * query to verify that the data in preferences is correct. The callback
     * supplied will only be called if this query returns a value other than
     * what is stored in preferences and should cause the calling activity to
     * refresh anything that depends on calling this method.
     *
     * @param context  The calling activity
     * @param callback The runnable that should execute if a query returns new values
     * @return The string value representing the time zone Calendar should
     * display
     */
    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }

    /**
     * This class contains methods specific to reading and writing time zone
     * values.
     */
    public static class TimeZoneUtils {
        private static final String[] TIMEZONE_TYPE_ARGS = {CalendarCache.KEY_TIMEZONE_TYPE};
        private static final String[] TIMEZONE_INSTANCES_ARGS = {CalendarCache.KEY_TIMEZONE_INSTANCES};
        public static final String[] CALENDAR_CACHE_POJECTION = {
                CalendarCache.KEY, CalendarCache.VALUE};

        private static StringBuilder mSB = new StringBuilder(50);
        private static Formatter mF = new Formatter(mSB, Locale.getDefault());
        private volatile static boolean mFirstTZRequest = true;
        private volatile static boolean mTZQueryInProgress = false;

        private volatile static boolean mUseHomeTZ = false;
        private volatile static String mHomeTZ = Time.getCurrentTimezone();

        private static HashSet<Runnable> mTZCallbacks = new HashSet<Runnable>();
        private static int mToken = 1;
        private static AsyncTZHandler mHandler;

        // The name of the shared preferences file. This name must be maintained
        // for historical
        // reasons, as it's what PreferenceManager assigned the first time the
        // file was created.
        private final String mPrefsName;

        /**
         * This is the key used for writing whether or not a home time zone
         * should be used in the Calendar app to the Calendar Preferences.
         */
        public static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
        /**
         * This is the key used for writing the time zone that should be used if
         * home time zones are enabled for the Calendar app.
         */
        public static final String KEY_HOME_TZ = "preferences_home_tz";

        /**
         * This is a helper class for handling the async queries and updates for
         * the time zone settings in Calendar.
         */
        private class AsyncTZHandler extends AsyncQueryHandler {
            public AsyncTZHandler(ContentResolver cr) {
                super(cr);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie,
                                           Cursor cursor) {
                synchronized (mTZCallbacks) {
                    if (cursor == null) {
                        mTZQueryInProgress = false;
                        mFirstTZRequest = true;
                        return;
                    }

                    boolean writePrefs = false;
                    // Check the values in the db
                    int keyColumn = cursor
                            .getColumnIndexOrThrow(CalendarCache.KEY);
                    int valueColumn = cursor
                            .getColumnIndexOrThrow(CalendarCache.VALUE);
                    while (cursor.moveToNext()) {
                        String key = cursor.getString(keyColumn);
                        String value = cursor.getString(valueColumn);
                        if (TextUtils.equals(key,
                                CalendarCache.KEY_TIMEZONE_TYPE)) {
                            boolean useHomeTZ = !TextUtils.equals(value,
                                    CalendarCache.TIMEZONE_TYPE_AUTO);
                            if (useHomeTZ != mUseHomeTZ) {
                                writePrefs = true;
                                mUseHomeTZ = useHomeTZ;
                            }
                        } else if (TextUtils.equals(key,
                                CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS)) {
                            if (!TextUtils.isEmpty(value)
                                    && !TextUtils.equals(mHomeTZ, value)) {
                                writePrefs = true;
                                mHomeTZ = value;
                            }
                        }
                    }
                    cursor.close();
                    if (writePrefs) {
                        SharedPreferences prefs = getSharedPreferences(
                                (Context) cookie, mPrefsName);
                        // Write the prefs
                        setSharedPreference(prefs, KEY_HOME_TZ_ENABLED,
                                mUseHomeTZ);
                        setSharedPreference(prefs, KEY_HOME_TZ, mHomeTZ);
                    }

                    mTZQueryInProgress = false;
                    for (Runnable callback : mTZCallbacks) {
                        if (callback != null) {
                            callback.run();
                        }
                    }
                    mTZCallbacks.clear();
                }
            }
        }

        /**
         * The name of the file where the shared prefs for Calendar are stored
         * must be provided. All activities within an app should provide the
         * same preferences name or behavior may become erratic.
         *
         * @param prefsName
         */
        public TimeZoneUtils(String prefsName) {
            mPrefsName = prefsName;
        }

        /**
         * Formats a date or a time range according to the local conventions.
         * This formats a date/time range using Calendar's time zone and the
         * local conventions for the region of the device. If the
         * {@link DateUtils#FORMAT_UTC} flag is used it will pass in the UTC
         * time zone instead.
         *
         * @param context     the context is required only if the time is shown
         * @param startMillis the start time in UTC milliseconds
         * @param endMillis   the end time in UTC milliseconds
         * @param flags       a bit mask of options See
         *                    {@link DateUtils#formatDateRange(Context, Formatter, long, long, int, String)
         *                    formatDateRange}
         * @return a string containing the formatted date/time range.
         */
        public String formatDateRange(Context context, long startMillis,
                                      long endMillis, int flags) {
            String date;
            String tz;
            if ((flags & DateUtils.FORMAT_UTC) != 0) {
                tz = Time.TIMEZONE_UTC;
            } else {
                tz = getTimeZone(context, null);
            }
            synchronized (mSB) {
                mSB.setLength(0);
                date = DateUtils.formatDateRange(context, mF, startMillis,
                        endMillis, flags, tz).toString();
            }
            return date;
        }

        /**
         * Writes a new home time zone to the db. Updates the home time zone in
         * the db asynchronously and updates the local cache. Sending a time
         * zone of {@link CalendarCache#TIMEZONE_TYPE_AUTO} will cause it to be
         * set to the device's time zone. null or empty tz will be ignored.
         *
         * @param context  The calling activity
         * @param timeZone The time zone to set Calendar to, or
         *                 {@link CalendarCache#TIMEZONE_TYPE_AUTO}
         */
        public void setTimeZone(Context context, String timeZone) {
            if (TextUtils.isEmpty(timeZone)) {
                return;
            }
            boolean updatePrefs = false;
            synchronized (mTZCallbacks) {
                if (CalendarCache.TIMEZONE_TYPE_AUTO.equals(timeZone)) {
                    if (mUseHomeTZ) {
                        updatePrefs = true;
                    }
                    mUseHomeTZ = false;
                } else {
                    if (!mUseHomeTZ || !TextUtils.equals(mHomeTZ, timeZone)) {
                        updatePrefs = true;
                    }
                    mUseHomeTZ = true;
                    mHomeTZ = timeZone;
                }
            }
            if (updatePrefs) {
                // Write the prefs
                SharedPreferences prefs = getSharedPreferences(context,
                        mPrefsName);
                setSharedPreference(prefs, KEY_HOME_TZ_ENABLED, mUseHomeTZ);
                setSharedPreference(prefs, KEY_HOME_TZ, mHomeTZ);

                // Update the db
                ContentValues values = new ContentValues();
                if (mHandler != null) {
                    mHandler.cancelOperation(mToken);
                }

                mHandler = new AsyncTZHandler(context.getContentResolver());

                // skip 0 so query can use it
                if (++mToken == 0) {
                    mToken = 1;
                }

                // Write the use home tz setting
                values.put(CalendarCache.VALUE,
                        mUseHomeTZ ? CalendarCache.TIMEZONE_TYPE_HOME
                                : CalendarCache.TIMEZONE_TYPE_AUTO);
                mHandler.startUpdate(mToken, null, CalendarCache.URI, values,
                        "key=?", TIMEZONE_TYPE_ARGS);

                // If using a home tz write it to the db
                if (mUseHomeTZ) {
                    ContentValues values2 = new ContentValues();
                    values2.put(CalendarCache.VALUE, mHomeTZ);
                    mHandler.startUpdate(mToken, null, CalendarCache.URI,
                            values2, "key=?", TIMEZONE_INSTANCES_ARGS);
                }
            }
        }

        /**
         * Gets the time zone that Calendar should be displayed in This is a
         * helper method to get the appropriate time zone for Calendar. If this
         * is the first time this method has been called it will initiate an
         * asynchronous query to verify that the data in preferences is correct.
         * The callback supplied will only be called if this query returns a
         * value other than what is stored in preferences and should cause the
         * calling activity to refresh anything that depends on calling this
         * method.
         *
         * @param context  The calling activity
         * @param callback The runnable that should execute if a query returns new
         *                 values
         * @return The string value representing the time zone Calendar should
         * display
         */
        public String getTimeZone(Context context, Runnable callback) {
            synchronized (mTZCallbacks) {
                if (mFirstTZRequest) {
                    mTZQueryInProgress = true;
                    mFirstTZRequest = false;

                    SharedPreferences prefs = getSharedPreferences(context,
                            mPrefsName);
                    mUseHomeTZ = prefs.getBoolean(KEY_HOME_TZ_ENABLED, false);
                    mHomeTZ = prefs.getString(KEY_HOME_TZ,
                            Time.getCurrentTimezone());

                    // When the async query returns it should synchronize on
                    // mTZCallbacks, update mUseHomeTZ, mHomeTZ, and the
                    // preferences, set mTZQueryInProgress to false, and call
                    // all
                    // the runnables in mTZCallbacks.
                    if (mHandler == null) {
                        mHandler = new AsyncTZHandler(
                                context.getContentResolver());
                    }
                    mHandler.startQuery(0, context, CalendarCache.URI,
                            CALENDAR_CACHE_POJECTION, null, null, null);
                }
                if (mTZQueryInProgress) {
                    mTZCallbacks.add(callback);
                }
            }
            return mUseHomeTZ ? mHomeTZ : Time.getCurrentTimezone();
        }

        /**
         * Forces a query of the database to check for changes to the time zone.
         * This should be called if another app may have modified the db. If a
         * query is already in progress the callback will be added to the list
         * of callbacks to be called when it returns.
         *
         * @param context  The calling activity
         * @param callback The runnable that should execute if a query returns new
         *                 values
         */
        public void forceDBRequery(Context context, Runnable callback) {
            synchronized (mTZCallbacks) {
                if (mTZQueryInProgress) {
                    mTZCallbacks.add(callback);
                    return;
                }
                mFirstTZRequest = true;
                getTimeZone(context, callback);
            }
        }
    }

    /**
     * A helper method for writing a String value to the preferences
     * asynchronously.
     */
    private static void setSharedPreference(SharedPreferences prefs, String key,
                                            String value) {
        // SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * A helper method for writing a boolean value to the preferences
     * asynchronously.
     */
    private static void setSharedPreference(SharedPreferences prefs, String key,
                                            boolean value) {
        // SharedPreferences prefs = getSharedPreferences(context, prefsName);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Return a properly configured SharedPreferences instance
     */
    private static SharedPreferences getSharedPreferences(Context context,
                                                          String prefsName) {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    /**
     * if all day, need set hour,minute,second to 0 and UTC timezone for calendar event
     */
    public static long convertAlldayLocalToUTC(long localTime) {
        Time time = new Time();
        time.set(localTime);
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        time.timezone = Time.TIMEZONE_UTC;
        return time.normalize(true);
    }

    public static boolean isCNLanguage(Context context){
        return context.getResources().getConfiguration().locale.getCountry().equals(CN);
    }
}
