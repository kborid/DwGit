package com.smartisanos.ideapills.sync.share;

import android.annotation.NonNull;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.ProxyActivity;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.data.BubbleDB;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.entity.SyncPushItem;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;

public enum SyncShareManager {
    INSTANCE;

    private static final long PUSH_MERGE_DELAY = 20 * 1000;

    private static final long CACHE_DURATION = 10 * 60 * 1000;
    private static final int NOTIFICATION_MAX_WORD_SIZE = 48;
    public static final String ACTION_INVITE_STATUS_CHANGE = "action_invite_status_change";
    private List<SyncShareInvitation> mInvitationList = new ArrayList<>();
    private long mInvitationCacheLastTime;
    private long mInvitationRequestLastTime;
    private volatile boolean mRefreshing;
    private boolean mInviteSwitch;

    private Map<Long, Integer> mChangedToNotShareColorMap = new HashMap<>();

    private final Object mPushListLock = new Object();
    private int mLastDelay = 0;
    @NonNull
    private Map<Long, Long> mPillSharePushIdsInOneCycle = new HashMap<>();
    @NonNull
    private Map<SyncPushItem, Long> mPillEditPushList = new HashMap<>();
    private Runnable mPillEditPushRunnable = new Runnable() {
        @Override
        public void run() {
            Map<SyncPushItem, Long> thisCyclePushItems;
            Map<Long, Long> thisCycleSharePushIds;
            synchronized (mPushListLock) {
                thisCyclePushItems = mPillEditPushList;
                mPillEditPushList = new HashMap<>();
                thisCycleSharePushIds = mPillSharePushIdsInOneCycle;
                mPillSharePushIdsInOneCycle = new HashMap<>();
            }
            LinkedHashMap<String, SyncPushItem> mergePushItems = new LinkedHashMap<>();
            if (thisCyclePushItems != null && !thisCyclePushItems.isEmpty()) {
                for (SyncPushItem pushItem : thisCyclePushItems.keySet()) {
                    if (pushItem == null) {
                        continue;
                    }
                    if (pushItem.pid != 0 && thisCycleSharePushIds.containsKey(pushItem.pid)) {
                        long sharePushItemTime = thisCycleSharePushIds.get(pushItem.pid);
                        long itemTime = thisCyclePushItems.get(pushItem);
                        // share change item in 5 seconds, ignore edit notify
                        if (sharePushItemTime + 5000 > itemTime) {
                            continue;
                        }
                    }
                    String key = "" + pushItem.fid + pushItem.pid + pushItem.action;
                    mergePushItems.put(key, pushItem);
                }
            }
            int delay = 0;
            for (final SyncPushItem mergePushItem : mergePushItems.values()) {
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendIdeapillsNotifyDirectly(IdeaPillsApp.getInstance(), mergePushItem);
                    }
                }, delay);
                delay += 1000;
            }
            mLastDelay = delay;
        }
    };
    @NonNull
    private List<SyncPushItem> mPillSharePushList = new ArrayList<>();
    private Runnable mPillSharePushRunnable = new Runnable() {
        @Override
        public void run() {
            mLastDelay = 0;
            List<SyncPushItem> nowPushItems ;
            synchronized (mPushListLock) {
                nowPushItems = mPillSharePushList;
                mPillSharePushList = new ArrayList<>();
            }
            int delay = mLastDelay;
            for (final SyncPushItem pushItem : nowPushItems) {
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendIdeapillsNotifyDirectly(IdeaPillsApp.getInstance(), pushItem);
                    }
                }, delay);
                delay += 1000;
            }
        }
    };

    public boolean isInviteSwitchOn() {
        return mInviteSwitch;
    }

    void setInviteSwitch(boolean inviteSwitch) {
        this.mInviteSwitch = inviteSwitch;
    }

    public void clearAll() {
        mInvitationCacheLastTime = 0;
        mInvitationRequestLastTime = 0;
        mInviteSwitch = false;
        mInvitationList = new ArrayList<>();
        clearToNotShareFromOtherItems();
        UIHandler.removeCallbacks(mPillSharePushRunnable);
        UIHandler.removeCallbacks(mPillEditPushRunnable);
        synchronized (mPushListLock) {
            mPillSharePushList.clear();
            mPillEditPushList.clear();
            mPillSharePushIdsInOneCycle.clear();
        }
        LocalBroadcastManager.getInstance(IdeaPillsApp.getInstance())
                .sendBroadcast(new Intent(SyncShareManager.ACTION_INVITE_STATUS_CHANGE));
    }

    public void getInvitationList(SyncBundleRepository.RequestListener<List<SyncShareInvitation>> requestListener) {
        if (isInvitationListCacheDirty() || mInvitationList == null || mInvitationList.isEmpty()) {
            SyncShareRepository.getInviteList(requestListener);
        } else {
            if (requestListener != null) {
                requestListener.onResponse(mInvitationList);
            }
        }
    }

    public void refreshInvitationListCycled() {
        if (isInvitationRequestDirty() && !mRefreshing) {
            refreshInvitationList();
        }
    }

    public void refreshInvitationList() {
        mInvitationCacheLastTime = 0;
        mInvitationRequestLastTime = 0;
        getInvitationList(new SyncBundleRepository.RequestListener<List<SyncShareInvitation>>() {
            @Override
            public void onRequestStart() {
                mRefreshing = true;
            }

            @Override
            public void onResponse(List<SyncShareInvitation> response) {
                mRefreshing = false;
                LocalBroadcastManager.getInstance(IdeaPillsApp.getInstance())
                        .sendBroadcast(new Intent(SyncShareManager.ACTION_INVITE_STATUS_CHANGE));
            }

            @Override
            public void onError(SyncBundleRepository.DataException e) {
                mRefreshing = false;
            }
        });
    }

    private boolean isInvitationListCacheDirty() {
        long now = System.currentTimeMillis();
        return now - mInvitationCacheLastTime > CACHE_DURATION || now < mInvitationCacheLastTime;
    }

    private boolean isInvitationRequestDirty() {
        long now = System.currentTimeMillis();
        return now - mInvitationRequestLastTime > CACHE_DURATION || now < mInvitationRequestLastTime;
    }

    public boolean hasInvitationListCached() {
        return mInvitationCacheLastTime > 0 || (mInvitationList != null && !mInvitationList.isEmpty());
    }

    public List<SyncShareInvitation> getCachedInvitationList() {
        return mInvitationList;
    }

    void markInvitationListRequested() {
        mInvitationRequestLastTime = System.currentTimeMillis();
    }

    void setInvitationList(List<SyncShareInvitation> invitationList) {
        final boolean lastHasSuccessInvitationList = SyncShareUtils.hasSuccessParticipant(mInvitationList);
        boolean nowHasSuccessInvitationList = SyncShareUtils.hasSuccessParticipant(invitationList);
        this.mInvitationList = invitationList;
        if (lastHasSuccessInvitationList != nowHasSuccessInvitationList) {
            if (!nowHasSuccessInvitationList) {
                GlobalBubbleManager.getInstance().removeShareItems();
            } else {
                SyncManager.requestSync();
            }
        }
        mInvitationCacheLastTime = System.currentTimeMillis();
        mInvitationRequestLastTime = mInvitationCacheLastTime;
    }

    public SyncShareInvitation getInvitation(long inviterId) {
        if (mInvitationList != null) {
            for (SyncShareInvitation invitation : mInvitationList) {
                if (invitation.inviter.id == inviterId) {
                    return invitation;
                }
            }
        }
        return null;
    }

    void addInvitation(SyncShareInvitation invitation) {
        if (invitation != null) {
            for(SyncShareInvitation invite : mInvitationList){
                if (invite.inviter.id == invitation.inviter.id && invite.invitee.id == invitation.invitee.id) {
                    mInvitationList.remove(invite);
                    break;
                }
            }
            mInvitationList.add(invitation);
        }
    }

    void modifyInvitation(long inviterId, int inviteStatus) {
        if (mInvitationList != null) {
            for (SyncShareInvitation syncShareInvitation : mInvitationList) {
                if (syncShareInvitation.inviter != null && syncShareInvitation.inviter.id == inviterId) {
                    syncShareInvitation.inviteStatus = inviteStatus;
                    break;
                }
            }
        }
    }

    void removeInvitation(long inviterId, long inviteeId) {
        if (mInvitationList != null) {
            SyncShareInvitation toRemoved = null;
            for (SyncShareInvitation syncShareInvitation : mInvitationList) {
                if (syncShareInvitation.inviter != null && syncShareInvitation.inviter.id == inviterId
                        && syncShareInvitation.invitee != null && syncShareInvitation.invitee.id == inviteeId) {
                    toRemoved = syncShareInvitation;
                    break;
                }
            }
            if (toRemoved != null) {
                mInvitationList.remove(toRemoved);
            }
        }
    }

    public int getChangeToNotShareFromOtherItemColor(Long createDate) {
        Integer color = mChangedToNotShareColorMap.get(createDate);
        return color == null ? -1 : color;
    }

    public void addChangeToNotShareFromOtherItem(Long createDate, int color) {
        mChangedToNotShareColorMap.put(createDate, color);
    }

    public void clearToNotShareFromOtherItems() {
        mChangedToNotShareColorMap.clear();
    }

    public void sendNotification(Context context, String title, String content, PendingIntent pendingIntent, int id) {
        String strUri = Settings.System.getString(context.getContentResolver(),
                smartisanos.api.SettingsSmt.System.IDEA_PILLS_RINGTONE_URI);
        final Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.pill_small_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.pill_large_icon))
                .setWhen(System.currentTimeMillis())
                .setTicker(title)
                .setAutoCancel(true)
                .setSound(TextUtils.isEmpty(strUri) ? null : Uri.parse(strUri));

        String channelId = "idea_pills_sync_Id"+ SystemClock.currentThreadTimeMillis();
        CharSequence name = "IdeaPillsName"+ SystemClock.currentThreadTimeMillis();
        MultiSdkUtils.sendNotification(context, channelId, name, id, builder);
    }

    public void sendRemoveShareNotify(Context context, long fid, String fname) {
        String title = context.getString(R.string.sync_cancel_notification_title, fname);
        Intent notifyActivity = new Intent(context, ShareMainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)fid, notifyActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);
        sendNotification(context, title, null, pendingIntent, (int)fid);
    }

    public void sendIdeapillsEditNotify(Context context, SyncPushItem syncPushItem) {
        synchronized (mPushListLock) {
            mPillEditPushList.put(syncPushItem, System.nanoTime() / (1000 * 1000));
        }
        UIHandler.removeCallbacks(mPillEditPushRunnable);
        UIHandler.postDelayed(mPillEditPushRunnable, PUSH_MERGE_DELAY);
    }

    public void sendIdeapillsShareNotify(Context context, SyncPushItem syncPushItem) {
        synchronized (mPushListLock) {
            mPillSharePushIdsInOneCycle.put(syncPushItem.pid, System.nanoTime() / (1000 * 1000));
            mPillSharePushList.add(syncPushItem);
        }
    }

    public void popAllIdeapillsShareNotify() {
        if (!mPillSharePushList.isEmpty()) {
            UIHandler.removeCallbacks(mPillSharePushRunnable);
            UIHandler.post(mPillSharePushRunnable);
        }
    }

    private void sendIdeapillsNotifyDirectly(Context context, SyncPushItem syncPushItem) {
        String title = getShareIdeaPillsNotiTitle(context, syncPushItem.action, syncPushItem.op, syncPushItem.fName);
        String content = getShareIdeaPillsNotiContent(syncPushItem.pid);
        Intent notifyActivity = new Intent(context, ProxyActivity.class);
        notifyActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notifyActivity.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_OPEN_IDEAPILLS);
        notifyActivity.putExtra(ProxyActivity.EXTRA_BUBBLE_SYNC_ID, syncPushItem.pid);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) syncPushItem.pid, notifyActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);
        int notifyId = (int) syncPushItem.pid;
        if (notifyId == 0) {
            notifyId = Long.valueOf(System.currentTimeMillis()).intValue();
        }
        sendNotification(context, title, content, pendingIntent, notifyId);
    }

    public void sendInvitationNotify(Context context, long fid, SyncShareInvitation invitation) {
        String nickName;
        if (!TextUtils.isEmpty(invitation.inviter.nickname)) {
            nickName = invitation.inviter.nickname;
        } else if (!TextUtils.isEmpty(invitation.invitee.nickname)) {
            nickName = invitation.invitee.nickname;
        } else {
            nickName = String.valueOf(invitation.inviter.id);
        }
        String title = getInviteNotifyTitle(context, invitation.inviteStatus, nickName);
        Intent notifyActivity = new Intent(context, SyncAccountDetailActivity.class);
        notifyActivity.putExtra(SyncAccountDetailActivity.EXTRA_INVITEE, invitation.toContentValues());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) fid, notifyActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);
        sendNotification(context, title, null, pendingIntent, (int) fid);
    }

    private static String getInviteNotifyTitle(Context context, int status, String fname) {
        int resId = R.string.sync_invitation_notification_title;
        switch (status) {
            case SyncShareInvitation.INVITE_ACCEPT:
                resId = R.string.sync_accept_notification_title;
                break;
            case SyncShareInvitation.INVITE_DECLINE:
                resId = R.string.sync_decline_notification_title;
                break;
            case SyncShareInvitation.INVITE_CANCEL:
                resId = R.string.sync_cancel_notification_title;
                break;
        }
        return context.getString(resId, fname);
    }

    private static String getShareIdeaPillsNotiTitle(Context context, int status, int op, String fname) {
        int resId = R.string.sync_share_ideapills_notification_title;
        switch (status) {
            case SyncPushItem.SHARE_ACTION_START_SHARE:
                resId = R.string.sync_share_ideapills_notification_title;
                break;
            case SyncPushItem.SHARE_ACTION_EDIT_SHARED_PILL:
                if (op == SyncPushItem.OP_DELETE) {
                    resId = R.string.sync_trash_ideapills_notification_title;
                } else if (op == SyncPushItem.OP_MARK_FINISHED) {
                    resId = R.string.sync_todo_over_ideapills_notification_title;
                } else {
                    resId = R.string.sync_edit_ideapills_notification_title;
                }
                break;
            case SyncPushItem.SHARE_ACTION_CANCEL_SHARE:
                resId = R.string.sync_cancel_ideapills_notification_title;
                break;
        }
        return context.getString(resId, fname);
    }

    private static String getShareIdeaPillsNotiContent(long pid) {
        if (pid > 0) {
            BubbleItem item = GlobalBubbleManager.getInstance().getBubbleBySyncId(String.valueOf(pid));
            String text = null;
            if (item != null) {
                text = item.getText();
            } else {
                text = BubbleDB.listBubbleText(BUBBLE.SYNC_ID + "=" + pid);
            }
            if (text != null && text.length() > NOTIFICATION_MAX_WORD_SIZE) {
                text = text.substring(0, NOTIFICATION_MAX_WORD_SIZE);
            }
            return text;
        }
        return null;
    }

    public String getInvitationShowName(long userId,SyncShareInvitation invitation) {
        if (invitation.inviter.id == userId) {
            String number = invitation.invitee.getShowName();
            if (TextUtils.isEmpty(invitation.inviter.remark)) {
                return number;
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(invitation.inviter.remark).append("(").append(number).append(")");
                return builder.toString();
            }
        } else {
            String number = invitation.inviter.getShowName();
            if (TextUtils.isEmpty(invitation.invitee.remark)) {
                return number;
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(invitation.invitee.remark).append("(").append(number).append(")");
                return builder.toString();
            }
        }
    }
}
