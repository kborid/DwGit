package com.smartisanos.sara.setting.recycle;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;
import com.smartisanos.sara.R;
import com.smartisanos.sara.setting.RecycleBinActivity;
import com.smartisanos.sara.providers.SaraProvider;
import com.smartisanos.sara.providers.SaraSettings.GlobleBubbleColumns;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.ideapills.common.util.TimeUtils;

public class RecycleItem {
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;
    private static final long ONE_MONTH_DAYS = 30;
    private static final int FUTURE = 0;
    private static final int SINCE_TODAY = 1;
    private static final int SINCE_YESTERDAY = 2;
    private static final int SINCE_LAST_WEEK = 3;
    private static final int SINCE_LAST_1_MONTH = 4;
    private static final int SINCE_LONG_TIME_AGO = 5;

    public int bubbleId;
    public String bubbleText;
    public String bubblePath = "";
    public int bubbleType;
    public int bubbleColor;
    public int bubbleTodo;
    private long bubbleRecycleDate;
    public int section;
    public long remindTime;
    public long dueDate;
    public CharSequence recycleFormattedDate;
    public CharSequence mFormattedDate;
    public List<GlobalBubbleAttach> bubbleAttaches;
    private boolean mAttachLoaded = false;

    public void setRecycleDate(Context context, long recycleDate) {
        this.bubbleRecycleDate = recycleDate;
        this.section = getRecycleDateSince(recycleDate);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date(recycleDate);
        this.recycleFormattedDate = sdf.format(date) + " " + TimeUtils.buildDetailTime(context, recycleDate);
        SimpleDateFormat format = new SimpleDateFormat(SaraConstant.ABBREV_YEAR_MONTH);
        if (bubbleRecycleDate > SaraUtils.getFirstDayOfYear()) {
            format = new SimpleDateFormat(SaraConstant.ABBREV_MONTH_NO_YEAR);
        }
        mFormattedDate = format.format(date);
    }

    public long getRecycleDate() {
        return bubbleRecycleDate;
    }

    public List<GlobalBubbleAttach> getBubbleAttaches() {
        return bubbleAttaches;
    }

    public void setBubbleAttaches(List<GlobalBubbleAttach> bubbleAttaches) {
        this.bubbleAttaches = bubbleAttaches;
        mAttachLoaded = true;
    }

    public boolean isAttachLoaded(){
        return mAttachLoaded;
    }

    public void fillRestoreOperations(ArrayList<ContentProviderOperation> operationList) {
        final int backReferenceIndex = operationList.size();
        Uri uri = ContentUris.withAppendedId(SaraProvider.CONTENT_URI_GLOBLE_BUBBLE, bubbleId);
        ContentValues value = new ContentValues();
        value.put(GlobleBubbleColumns.BUBBLE_DELETE_TIME, "0");
        operationList.add(ContentProviderOperation.newUpdate(uri).withValues(value).build());
    }

    public String getSectionHeader(Context context) {
        String header;
        switch (section) {
            case FUTURE:
                header = context.getString(R.string.recycle_future);
                break;
            case SINCE_TODAY:
                header = context.getString(R.string.recycle_today);
                break;
            case SINCE_YESTERDAY:
                header = context.getString(R.string.recycle_yesterday);
                break;
            case SINCE_LAST_WEEK:
                header = context.getString(R.string.recycle_since_week);
                break;
            case SINCE_LAST_1_MONTH:
                header = context.getString(R.string.recycle_since_one_month);
                break;
            default:
                header = context.getString(R.string.recycle_since_long_ago);
        }
        return header;
    }

    public String getTodoOverSectionHeader(Context context) {
        String header;
        switch (section) {
            case FUTURE:
                header = context.getString(R.string.recycle_future);
                break;
            case SINCE_TODAY:
                header = context.getString(R.string.recycle_today);
                break;
            case SINCE_YESTERDAY:
                header = context.getString(R.string.recycle_yesterday);
                break;
            case SINCE_LAST_WEEK:
                header = context.getString(R.string.recycle_since_week);
                break;
            default:
                header = mFormattedDate.toString();
        }
        return header;
    }
    public boolean search(String searchKey) {
        if (TextUtils.isEmpty(searchKey)) {
            return false;
        }

        Pattern   pattern = Pattern.compile(".*" + StringUtils.handleSpecialCharacter(searchKey) + ".*", Pattern.CASE_INSENSITIVE);
        if (bubbleText != null && pattern.matcher(bubbleText).matches()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RecycleItem that = (RecycleItem) o;

        return bubbleId == that.bubbleId;

    }

    @Override
    public int hashCode() {
        return (int) (bubbleId ^ (bubbleId >>> 32));
    }

    @Override
    public String toString() {
        return "RecycleItem{" +
                "bubbleId=" + bubbleId +
                ", bubbleText='" + bubbleText + '\'' +
                ", bubblePath='" + bubblePath + '\'' +
                ", bubbleType='" + bubbleType + '\'' +
                ", bubbleColor='" + bubbleColor + '\'' +
                ", bubbleTodo='" + bubbleTodo + '\'' +
                ", section=" + section +
                ", bubbleRecycleDate=" + bubbleRecycleDate +
                ", recycleFormattedDate=" + recycleFormattedDate +
                ", mRemindTime=" + remindTime +
                ", mDueDate=" + dueDate +
                '}';
    }

    private int getRecycleDateSince(long recycleDate) {
        long now = System.currentTimeMillis();
        if (recycleDate > now) {
            return FUTURE;
        }
        int dayNum = gapDaysFromNow(recycleDate);
        if (dayNum < 7) {
            switch (dayNum) {
                case 0:
                    return SINCE_TODAY;
                case 1:
                    return SINCE_YESTERDAY;
                default:
                    return SINCE_LAST_WEEK;
            }
        } else {
            if (RecycleBinActivity.sBubbleDirection == RecycleBinActivity.BUBBLE_RIRECTION_USED) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(recycleDate);
                calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                return calendar.get(Calendar.YEAR) << 4 + calendar.get(Calendar.MONTH);
            } else {
                if (dayNum < ONE_MONTH_DAYS) {
                    return SINCE_LAST_1_MONTH;
                }
                return SINCE_LONG_TIME_AGO;
            }
        }
    }

    private static int gapDaysFromNow(long time) {
        int dayNum;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == c.get(Calendar.MONTH)) {
            dayNum = now.get(Calendar.DATE) - c.get(Calendar.DATE);
        } else {
            long days = (now.getTimeInMillis() - c.getTimeInMillis()) / ONE_DAY_MILLIS;
            long runningDays = days + 1;
            long probableEndMillis = c.getTimeInMillis() + ONE_DAY_MILLIS * days;
            c.setTimeInMillis(probableEndMillis);
            if (c.get(Calendar.DAY_OF_WEEK) != now.get(Calendar.DAY_OF_WEEK)) {
                runningDays++;
            }
            dayNum = (int) (runningDays - 1);
        }

        return dayNum;
    }
}
