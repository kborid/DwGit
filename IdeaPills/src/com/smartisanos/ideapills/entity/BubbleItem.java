package com.smartisanos.ideapills.entity;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.data.BubbleDB;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BubbleItem implements Comparable<BubbleItem> , BubbleDB.ContentValueItem{
    public static final BubbleItem sBubbleItemFake = new BubbleItem(new GlobalBubble());
    static {
        sBubbleItemFake.setIsTemp(true);
        sBubbleItemFake.setSelected(true);
    }
    public static final int TYPE_TEXT = GlobalBubble.TYPE_TEXT;
    public static final int TYPE_VOICE = GlobalBubble.TYPE_VOICE;
    public static final int TYPE_VOICE_OFFLINE = GlobalBubble.TYPE_VOICE_OFFLINE;

    public static final int SHARE_PENDING_NONE = 0;
    public static final int SHARE_PENDING_REMOVE_PARTICIPANTS = 1;
    public static final int SHARE_PENDING_ADD_PARTICIPANTS = 2;
    public static final int SHARE_PENDING_INVITATION = 3;

    public static final String WAVE_SUFFIX = ".wave";
    public static final String CONFLICT_HANDLED_TAG = "0";

    private static final int FLAG_INLARGMODE              = 0x00000001;
    private static final int FLAG_SELECTED                = 0x00000004;
    private static final int FLAG_ISTEMP                  = 0x00000008;
    private static final int FLAG_PLAY_SHOW_ANIM          = 0x00000010;
    private static final int FLAG_NEEDINPUT               = 0x00000020;
    public static final int FLAG_NEEDTRASH               = 0x00000040;
    public static final int FLAG_NEEDDELE                = 0x00000080;
    private static final int FLAG_ADDING_ATTACHMENT       = 0x00000100;
    public static final int FLAG_NEEDREMOVE              = 0x00000200;
    private static final int FLAG_DELEMASK                = FLAG_NEEDDELE|FLAG_NEEDTRASH|FLAG_NEEDREMOVE;

    public static final int MSG_BEFOREEDIT       = 0;
    public static final int MSG_ONEDIT           = 1;
    public static final int MSG_AFTEREDIT        = 2;
    public static final int MSG_SELECTED_CHANGE  = 3;
    public static final int MSG_INPUT_OVER       = 4;
    public static final int MSG_LARGEMODE_CHANGE = 5;
    public static final int MSG_BUBBLE_DELETE    = 6;
    public static final int MSG_SHARE_OVER       = 7;
    public static final int MSG_AFTEREDIT_TIME   = 8;
    public static final int MSG_TODO_OVER   = 9;
    public static final int MSG_TODO_OVER_INPUT   = 10;
    public static final int MSG_TODO_OVER_REVERSE   = 11;

    public static final int MF_TEXT = 0x01 << 0;
    public static final int MF_COLOR = 0x01 << 1;
    public static final int MF_TODO = 0x01 << 2;
    public static final int MF_WEIGHT = 0x01 << 3;
    public static final int MF_TIME_STAMP = 0x01 << 4;
    public static final int MF_REMIND_TIME = 0x01 << 5;
    public static final int MF_DUE_DATE = 0x01 << 6;
    public static final int MF_CREATE_DATE = 0x01 << 7;
    public static final int MF_CONFLICT_SYNC_ID = 0x01 << 8;
    public static final int MF_REMOVED_TIME = 0x01 << 9;
    public static final int MF_USED_TIME = 0x01 << 10;
    public static final int MF_SHARE_STATUS = 0x01 << 11;

    public static final int STATUS_MAIL = 1;

    private final GlobalBubble bubble;
    private int mFlags = 0;
    private int mNormalWidth = -1;
    private int mHeightInLarge = -1;
    private int mWeight;
    private byte[] mWaveData;
    private long mCreateAt;

    //for sync
    private String mSyncId;
    private long mRequestSyncTime;
    private int mModifyFlag;
    private int mSecondaryModifyFlag;
    private int mVersion;
    private String mVoiceSyncId;
    private int mVoiceVersion;
    private String mVoiceEncryptKey;
    private long mUserId = -1;
    private String mLastCloudText;
    private String mConflictSyncId;
    private String mVoiceBubbleSyncId;
    private boolean mIsShareFromOthers;
    private int mSharePendingStatus;
    @NonNull
    private List<Long> mSharePendingParticipants = new ArrayList<>();

    private int mConflictLocalId;
    private boolean mHasChangedBubbleWithoutSync;

    public boolean mWaitingUpdateId = false;

    //for mail bubble
    private int mStatus;
    private List<AttachMentItem> mAttachments = new ArrayList<AttachMentItem>();
    private boolean mRefreshAttachment = false;

    private String mChangeBeforeTxt;
    private boolean mIsSyncLock;
    private boolean mIsAddingAttachmentLock;

    public BubbleItem() {
        this(new GlobalBubble());
    }

    public BubbleItem(GlobalBubble bubble) {
        this(bubble, 0);
    }

    public BubbleItem(GlobalBubble bubble, int weight) {
        this(bubble, weight, Constants.DEFAULT_BUBBLE_COLOR);
    }

    public BubbleItem(GlobalBubble bubble, int weight, int defaultColor) {
        this.bubble = bubble;
        this.mWeight = weight;
        if (mCreateAt == 0) {
            if (bubble.getTimeStamp() > 0) {
                mCreateAt = bubble.getTimeStamp();
            } else {
                mCreateAt = System.currentTimeMillis();
            }
        }
        if (BubbleController.getInstance().isInPptMode()) {
            if (getColor() == 0) {
                setColorSilent(GlobalBubble.COLOR_NAVY_BLUE);
            }
        } else {
            if (getColor() == 0 || getColor() == GlobalBubble.COLOR_SHARE) {
                if (defaultColor == 0 || defaultColor == GlobalBubble.COLOR_SHARE) {
                    defaultColor = GlobalBubble.COLOR_BLUE;
                }
                setColorSilent(defaultColor);
            }
        }
        if (getShareStatus() == GlobalBubble.SHARE_STATUS_NOT_DEFINED) {
            setShareStatusSilent(GlobalBubble.SHARE_STATUS_NOT_SHARED);
        }
        if (getToDo() == 0) {
            setToDoSilent(GlobalBubble.TODO);
        }
    }

    public void setRefreshAttachment(boolean refreshAttachment) {
        mRefreshAttachment = refreshAttachment;
    }

    public boolean needRefreshAttachment() {
        return mRefreshAttachment;
    }

    public int getId() {
        return bubble.getId();
    }

    @Override
    public void setId(int id) {
        bubble.setId(id);
    }

    public Uri getUri() {
        return bubble.getUri();
    }

    public void setUri(Uri uri) {
        bubble.setUri(uri);
    }

    public long getTimeStamp() {
        return bubble.getTimeStamp();
    }

    public void setTimeStamp(long time) {
        if (getTimeStamp() != time) {
            markModified(MF_TIME_STAMP);
        }
        bubble.setTimeStamp(time);
    }

    public void setTimeStampSilent(long time) {
        bubble.setTimeStamp(time);
    }

    public int getType() {
        return bubble.getType();
    }

    public void setType(int type) {
        if (type != bubble.getType()) {
            bubble.setType(type);
            setNormalWidth(-1);
        }
    }

    public int getColor() {
        return bubble.getColor();
    }

    public void setColor(int color) {
        if (color != bubble.getColor()) {
            int oldColor = bubble.getColor();
            markModified(MF_COLOR);
            bubble.setColor(color);
        }
    }

    public void setColorSilent(int color) {
        bubble.setColor(color);
    }

    public int getToDo() {
        return bubble.getToDo();
    }

    public void setToDo(int toDo) {
        if (toDo != bubble.getToDo()) {
            markModified(MF_TODO);
            bubble.setToDo(toDo);
        }
    }

    public void setToDoSilent(int toDo) {
        bubble.setToDo(toDo);
    }

    public int getWeight() {
        return this.mWeight;
    }

    public void setWeight(int weight) {
        if (mWeight != weight) {
            markModified(MF_WEIGHT);
        }
        this.mWeight = weight;
    }

    public void setWeightSilent(int weight) {
        this.mWeight = weight;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        this.mStatus = status;
    }

    public boolean isStatusMail() {
        return mStatus == STATUS_MAIL;
    }

    public long getLastModified() {
        return bubble.getModifiedTime();
    }

    public void setLastModified(long modified) {
        bubble.setModifiedTime(modified);
    }

    public long getUsedTime() {
        return bubble.getUsedTime();
    }

    public void setUsedTimeSilent(long time) {
        bubble.setUsedTime(time);
    }

    public void setUsedTime(long time) {
        if (time != getUsedTime()) {
            bubble.setUsedTime(time);
            markModified(MF_USED_TIME);
        }
    }

    public long getLegacyUsedTime() {
        return bubble.getLegacyUsedTime();
    }

    public void setLegacyUsedTime(long time) {
        if (time != getLegacyUsedTime()) {
            bubble.setLegacyUsedTime(time);
        }
    }

    public boolean isShareColor() {
        return bubble.getShareStatus() == GlobalBubble.SHARE_STATUS_ONE_TO_ONE
                || bubble.getShareStatus() == GlobalBubble.SHARE_STATUS_MANY_TO_MANY;
    }

    public int getShareStatus() {
        return bubble.getShareStatus();
    }

    public void setShareStatus(int shareStatus) {
        if (shareStatus != bubble.getShareStatus()) {
            markModified(MF_SHARE_STATUS);
            bubble.setShareStatus(shareStatus);
            invalidateNormalWidth();
        }
    }

    public void setShareStatusSilent(int shareStatus) {
        bubble.setShareStatus(shareStatus);
    }

    public boolean isShareFromOthers() {
        return mIsShareFromOthers;
    }

    public void setShareFromOthers(boolean isShareFromOthers) {
        this.mIsShareFromOthers = isShareFromOthers;
    }

    public void clearPendingShareStatus() {
        mSharePendingStatus = SHARE_PENDING_NONE;
        mSharePendingParticipants = new ArrayList<>();
    }

    public void modifyPendingParticipants(List<Long> newSharePendingParticipants) {
        mSharePendingParticipants = new ArrayList<>();
        mSharePendingParticipants.addAll(newSharePendingParticipants);
    }

    public void changeShareStatusToWaitInvitation(long addParticipantId) {
        mSharePendingStatus = SHARE_PENDING_INVITATION;
        mSharePendingParticipants = new ArrayList<>();
        if (addParticipantId > 0) {
            mSharePendingParticipants.add(addParticipantId);
        }
        setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
    }

    public void changeShareStatusToAdd(List<Long> addParticipantIds) {
        if (addParticipantIds == null || addParticipantIds.isEmpty()) {
            return;
        }
        if (mSharePendingStatus == SHARE_PENDING_NONE || mSharePendingStatus == SHARE_PENDING_INVITATION) {
            mSharePendingStatus = SHARE_PENDING_ADD_PARTICIPANTS;
            mSharePendingParticipants = addParticipantIds;
            setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
        } else if (mSharePendingStatus == SHARE_PENDING_ADD_PARTICIPANTS) {
            for (Long participantId : addParticipantIds) {
                if (!mSharePendingParticipants.contains(participantId)) {
                    mSharePendingParticipants.add(participantId);
                }
            }
            setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
        } else if (mSharePendingStatus == SHARE_PENDING_REMOVE_PARTICIPANTS) {
            List<Long> newAddParticipants = new ArrayList<>();
            for (Long participantId : addParticipantIds) {
                if (!mSharePendingParticipants.contains(participantId)) {
                    newAddParticipants.add(participantId);
                }
            }
            mSharePendingParticipants = newAddParticipants;
            if (newAddParticipants.isEmpty()) {
                mSharePendingStatus = SHARE_PENDING_NONE;
            } else {
                mSharePendingStatus = SHARE_PENDING_ADD_PARTICIPANTS;
            }
            setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
        }
    }

    public void changeShareStatusToRemove(List<Long> removeParticipantIds, int color) {
        if (color == GlobalBubble.COLOR_SHARE) {
            setColor(GlobalBubble.COLOR_BLUE);
        } else {
            setColor(color);
        }
        if (removeParticipantIds == null || removeParticipantIds.isEmpty()) {
            mSharePendingStatus = SHARE_PENDING_NONE;
            mSharePendingParticipants = new ArrayList<>();
            setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
            return;
        }
        if (mSharePendingStatus == SHARE_PENDING_NONE || mSharePendingStatus == SHARE_PENDING_INVITATION) {
            mSharePendingStatus = SHARE_PENDING_REMOVE_PARTICIPANTS;
            mSharePendingParticipants = removeParticipantIds;
        } else if (mSharePendingStatus == SHARE_PENDING_ADD_PARTICIPANTS) {
            List<Long> newRemoveParticipants = new ArrayList<>();
            for (Long participantId : removeParticipantIds) {
                if (!mSharePendingParticipants.contains(participantId)) {
                    newRemoveParticipants.add(participantId);
                }
            }
            mSharePendingStatus = SHARE_PENDING_REMOVE_PARTICIPANTS;
            mSharePendingParticipants = newRemoveParticipants;
            if (newRemoveParticipants.isEmpty()) {
                mSharePendingStatus = SHARE_PENDING_NONE;
            } else {
                mSharePendingStatus = SHARE_PENDING_REMOVE_PARTICIPANTS;
            }
        } else if (mSharePendingStatus == SHARE_PENDING_REMOVE_PARTICIPANTS) {
            for (Long participantId : removeParticipantIds) {
                if (!mSharePendingParticipants.contains(participantId)) {
                    mSharePendingParticipants.add(participantId);
                }
            }
        }
        setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
    }

    public int getSharePendingStatus() {
        return mSharePendingStatus;
    }

    public List<Long> getSharePendingParticipants() {
        return mSharePendingParticipants;
    }

    public String getSharePendingParticipantsString() {
        return buildSharePendingParticipantsString(mSharePendingParticipants);
    }

    public static String buildSharePendingParticipantsString(List<Long> pendingParticipants) {
        if (!pendingParticipants.isEmpty()) {
            StringBuilder userIdsBuilder = new StringBuilder();
            for (Long userId : pendingParticipants) {
                userIdsBuilder.append(userId).append(",");
            }
            if (userIdsBuilder.length() > 0) {
                userIdsBuilder.deleteCharAt(userIdsBuilder.length() - 1);
            }
            return userIdsBuilder.toString();
        } else {
            return "";
        }
    }

    public String getSendTo() {
        return bubble.getSendTo();
    }

    public void setSendTo(String sendTo) {
        bubble.setSendTo(sendTo);
    }

    public String getText() {
        return bubble.getText();
    }

    public void setText(String text) {
        if (!text.equals(bubble.getText())) {
            bubble.setText(text);
            setNormalWidth(-1);
            setHeightInLarge(-1);
            if (mChangeBeforeTxt == null) {
                markModified(MF_TEXT);
            }
        }
    }

    public void setTextSilent(String text) {
        bubble.setText(text);
        setNormalWidth(-1);
        setHeightInLarge(-1);
    }

    public void judgeTextChangedWhenInput() {
        if (mChangeBeforeTxt != null) {
            if (!mChangeBeforeTxt.equals(bubble.getText())) {
                markModified(MF_TEXT, true);
                setTimeStamp(System.currentTimeMillis());
            }
        }
    }

    public int getSamplineRate() {
        return bubble.getSamplineRate();
    }

    public long getVoiceDuration() {
        return bubble.getVoiceDuration();
    }

    public void setVoiceDuration(int duration) {
        bubble.setVoiceDuration(duration);
    }

    public long getRemovedTime() {
        return bubble.getRemovedTime();
    }

    public void setRemovedTimeSilent(long time) {
        bubble.setRemovedTime(time);
    }

    public void setRemovedTime(long time) {
        if (time != getRemovedTime()) {
            bubble.setRemovedTime(time);
            markModified(MF_REMOVED_TIME);
        }
    }

    public byte[] getWaveData() {
        return mWaveData;
    }

    public void setWaveData(byte[] data) {
        mWaveData = data;
    }

    public String getSyncId() {
        return mSyncId;
    }

    public void setSyncId(String syncId) {
        mSyncId = syncId;
    }

    public void setVoiceBubbleSyncId(String voiceBubbleSyncId) {
        this.mVoiceBubbleSyncId = voiceBubbleSyncId;
    }

    public String getVoiceBubbleSyncId() {
        return mVoiceBubbleSyncId;
    }

    public long getRequestSyncTime() {
        return mRequestSyncTime;
    }

    public void setRequestSyncTime(long requestSyncTime) {
        mRequestSyncTime = requestSyncTime;
    }

    public String getSingleText() {
        String text = bubble.getText();
        if (!TextUtils.isEmpty(text)) {
            return text.trim();
        }
        if (mAttachments != null && mAttachments.size() > 0) {
            final int count = mAttachments.size();
            Resources res = IdeaPillsApp.getInstance().getResources();
            if (allAttachIsImage()) {
                return String.format(res.getQuantityString(
                        R.plurals.bubble_image_count_attach, count),
                        count);
            } else {
                return String.format(res.getQuantityString(
                        R.plurals.bubble_count_attach, count),
                        count);
            }
        }
        return "";
    }

    public boolean isTextAvailable() {
        String text = bubble.getText() == null ? bubble.getText() : bubble.getText().trim();
        return !TextUtils.isEmpty(text);
    }

    public void setInLargeMode(boolean mode) {
        if (mode) {
            mFlags |= FLAG_INLARGMODE;
        } else {
            mFlags &= (~FLAG_INLARGMODE);
        }
    }

    public boolean isInLargeMode() {
        return (mFlags & FLAG_INLARGMODE) == FLAG_INLARGMODE;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            mFlags |= FLAG_SELECTED;
        } else {
            mFlags &= (~FLAG_SELECTED);
        }
    }

    public boolean matches(Pattern pattern) {
        String text = bubble.getText();
        if (!TextUtils.isEmpty(text)) {
            Matcher matcher = pattern.matcher(text);
            return matcher.matches();
        }
        return false;
    }

    public boolean isSelected() {
        return (mFlags & FLAG_SELECTED) == FLAG_SELECTED;
    }

    public void setIsTemp(boolean isTemp) {
        if(isTemp) {
            mFlags |= FLAG_ISTEMP;
        } else {
            mFlags &= (~FLAG_ISTEMP);
        }
    }

    public boolean isTemp() {
        return (mFlags & FLAG_ISTEMP) == FLAG_ISTEMP;
    }

    public void setWillPlayShowAnim(boolean playShowAnim) {
        if(playShowAnim) {
            mFlags |= FLAG_PLAY_SHOW_ANIM;
        } else {
            mFlags &= (~FLAG_PLAY_SHOW_ANIM);
        }
    }

    public boolean willPlayShowAnim() {
        return (mFlags & FLAG_PLAY_SHOW_ANIM) == FLAG_PLAY_SHOW_ANIM;
    }

    public void setNeedInput(boolean needInput) {
        if(needInput) {
            mFlags |= FLAG_NEEDINPUT;
            mChangeBeforeTxt = getText();
        } else {
            mFlags &= (~FLAG_NEEDINPUT);
            mChangeBeforeTxt = null;
        }
    }

    public void trash() {
        mFlags &= (~FLAG_DELEMASK);
        mFlags |= FLAG_NEEDTRASH;
    }

    public void dele() {
        mFlags &= (~FLAG_DELEMASK);
        mFlags |= FLAG_NEEDDELE;
    }

    public void remove() {
        mFlags &= (~FLAG_DELEMASK);
        mFlags |= FLAG_NEEDREMOVE;
    }

    public boolean needTrash() {
        return (mFlags & FLAG_DELEMASK) == FLAG_NEEDTRASH;
    }

    public boolean needDele() {
        return (mFlags & FLAG_DELEMASK) == FLAG_NEEDDELE;
    }

    public boolean needRemove() {
        return (mFlags & FLAG_DELEMASK) == FLAG_NEEDREMOVE;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        mFlags = flags;
    }

    public void addFlag(int flag) {
        mFlags |= flag;
    }

    public void clearFlag(int flag) {
        mFlags &= (~flag);
    }

    public boolean hasFlag(int flag) {
        return (mFlags & flag) > 0;
    }

    public boolean isToDo() {
        return bubble.getToDo() == GlobalBubble.TODO;
    }

    public boolean isToDoOver() {
        return bubble.getToDo() == GlobalBubble.TODO_OVER;
    }

    public boolean isNeedInput() {
        return (mFlags & FLAG_NEEDINPUT) == FLAG_NEEDINPUT;
    }

    public void setAddingAttachment(boolean isAddingAttachment) {
        if(isAddingAttachment) {
            mFlags |= FLAG_ADDING_ATTACHMENT;
        } else {
            mFlags &= (~FLAG_ADDING_ATTACHMENT);
        }
    }

    public boolean isAddingAttachment() {
        return (mFlags & FLAG_ADDING_ATTACHMENT) == FLAG_ADDING_ATTACHMENT;
    }

    public int getNormalWidth() {
        if (mNormalWidth < 0) {
            mNormalWidth =  BubbleController.getInstance().getBubbleItemViewHelper().measureNormalWidth(this);
        }
        return mNormalWidth;
    }

    public void invalidateNormalWidth() {
        mNormalWidth = -1;
    }

    public int getHeightInLarge() {
        if (mHeightInLarge < 0) {
            mHeightInLarge = BubbleController.getInstance().getBubbleItemViewHelper().measureLargeHeightByItem(this);
        }
        return mHeightInLarge;
    }

    public void setNormalWidth(int normalWidth) {
        this.mNormalWidth = normalWidth;
    }

    public void setHeightInLarge(int heightInLarge) {
        mHeightInLarge = heightInLarge;
    }

    public void setEdited() {
        if (getType() == TYPE_VOICE_OFFLINE) {
            setType(TYPE_VOICE);
        }
    }

    public boolean isText() {
        return bubble.getType() == TYPE_TEXT;
    }

    public boolean isVoiceBubble() {
        int type = bubble.getType();
        return (type == TYPE_VOICE || type == TYPE_VOICE_OFFLINE);
    }

    public GlobalBubble getBubble() {
        return bubble;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int mVersion) {
        this.mVersion = mVersion;
    }

    public String getVoiceSyncId() {
        return mVoiceSyncId;
    }

    public void setVoiceSyncId(String voiceSyncId) {
        this.mVoiceSyncId = voiceSyncId;
    }

    public int getVoiceVersion() {
        return mVoiceVersion;
    }

    public void setVoiceVersion(int voiceVersion) {
        this.mVoiceVersion = voiceVersion;
    }

    public String getVoiceEncryptKey() {
        return mVoiceEncryptKey;
    }

    public void setVoiceEncryptKey(String voiceEncryptKey) {
        this.mVoiceEncryptKey = voiceEncryptKey;
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        this.mUserId = userId;
    }

    public String getLastCloudText() {
        return mLastCloudText;
    }

    public void setLastCloudText(String lastCloudText) {
        this.mLastCloudText = lastCloudText;
    }

    public String getConflictSyncId() {
        return mConflictSyncId;
    }

    public void setConflictSyncId(String conflictSyncId) {
        if (TextUtils.isEmpty(conflictSyncId) && TextUtils.isEmpty(mConflictSyncId)) {
            return;
        }
        if (conflictSyncId != null && mConflictSyncId != null
                && conflictSyncId.equals(mConflictSyncId)) {
            return;
        }
        this.mConflictSyncId = conflictSyncId;
        markModified(MF_CONFLICT_SYNC_ID);
    }

    public void setConflictSyncIdSilent(String conflictSyncId) {
        this.mConflictSyncId = conflictSyncId;
    }

    public void setConflictLocalId(int conflictLocalId) {
        this.mConflictLocalId = conflictLocalId;
    }

    public int getConflictLocalId() {
        return mConflictLocalId;
    }

    public void markModified(int flag) {
        markModified(flag, true);
    }

    public void markModified(int flag, boolean changed) {
        if (mRequestSyncTime == 0) {
            updateModifyFlag(flag, changed);
        } else {
            updateSecondaryModifyFlag(flag, changed);
        }
    }

    public void updateModifyFlag(int flag, boolean modified) {
        if (getModificationFlag(flag) == modified) {
            return;
        }
        if (modified) {
            mModifyFlag |= flag;
        } else {
            mModifyFlag &= ~flag;
        }
    }

    public void updateSecondaryModifyFlag(int flag, boolean modified) {
        if (getSecondaryModificationFlag(flag) == modified) {
            return;
        }
        if (modified) {
            mSecondaryModifyFlag |= flag;
        } else {
            mSecondaryModifyFlag &= ~flag;
        }
    }

    public boolean hasModificationFlagChanged() {
        return mModifyFlag > 0 || mSecondaryModifyFlag > 0;
    }

    public boolean isBubbleColorChanged() {
        return (mModifyFlag & MF_COLOR | mSecondaryModifyFlag & mModifyFlag & MF_COLOR) > 0
                || (mModifyFlag & MF_SHARE_STATUS | mSecondaryModifyFlag & mModifyFlag & MF_SHARE_STATUS) > 0;
    }

    public boolean getModificationFlag(final int flag) {
        return (mModifyFlag & flag) == flag;
    }

    public int getModificationFlag() {
        return mModifyFlag;
    }

    public int getSecondaryModifyFlag() {
        return mSecondaryModifyFlag;
    }

    public boolean getSecondaryModificationFlag(int flag) {
        return (mSecondaryModifyFlag & flag) == flag;
    }

    public void setModificationFlag(int modifyFlag) {
        mModifyFlag = modifyFlag;
    }

    public void setSecondaryModificationFlag(int flag) {
        mSecondaryModifyFlag = flag;
    }

    public void resetModifyFlag() {
        if (mModifyFlag != 0) {
            if (mSecondaryModifyFlag != 0) {
                mModifyFlag = mSecondaryModifyFlag;
                mSecondaryModifyFlag = 0;
            } else {
                mModifyFlag = 0;
            }
        }
        setRequestSyncTime(0);
    }

    public long getRemindTime() {
        return bubble.getRemindTime();
    }

    public void setRemindTime(long time) {
        if (getRemindTime() != time) {
            markModified(MF_REMIND_TIME);
            bubble.setRemindTime(time);
        }
    }

    public void setRemindTimeSilent(long time) {
        bubble.setRemindTime(time);
    }

    public long getCreateAt() {
        return mCreateAt;
    }

    public void setCreateAt(long createDate) {
        if (getCreateAt() != createDate) {
            markModified(MF_CREATE_DATE);
        }
        this.mCreateAt = createDate;
    }

    public void setCreateAtSilent(long createDate) {
        this.mCreateAt = createDate;
    }

    public long getDueDate() {
        return bubble.getDueDate();
    }

    public void setDueDate(long dueDate) {
        if (getDueDate() != dueDate) {
            markModified(MF_DUE_DATE);
            bubble.setDueDate(dueDate);
        }
    }

    public void setDueDateSilent(long dueDate) {
        bubble.setDueDate(dueDate);
    }

    @Override
    public int compareTo(BubbleItem o) {
        if (o == null) {
            return -1;
        }
        if (getWeight() > o.getWeight()) {
            return -1;
        } else if (getWeight() < o.getWeight()) {
            return 1;
        } else {
            if (!TextUtils.isEmpty(mConflictSyncId)
                    && TextUtils.isEmpty(o.getConflictSyncId())) {
                return 1;
            } else if (TextUtils.isEmpty(mConflictSyncId)
                    && !TextUtils.isEmpty(o.getConflictSyncId())) {
                return -1;
            } else {
                if (mCreateAt > o.getCreateAt()) {
                    return -1;
                } else if (mCreateAt < o.getCreateAt()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    public int getDragBackground(int num) {
        if (isShareColor()) {
            if (num == 1) {
                return R.drawable.bubble_drag_background_share;
            } else if (num == 2) {
                return R.drawable.bubble_drag_background_two_share;
            } else {
                return R.drawable.bubble_drag_background_three_share;
            }
        }
        switch (getColor()) {
            case GlobalBubble.COLOR_RED: {
                if (num == 1) {
                    return R.drawable.bubble_drag_background_red;
                } else if (num == 2) {
                    return R.drawable.bubble_drag_background_two_red;
                } else {
                    return R.drawable.bubble_drag_background_three_red;
                }
            }
            case GlobalBubble.COLOR_ORANGE: {
                if (num == 1) {
                    return R.drawable.bubble_drag_background_orange;
                } else if (num == 2) {
                    return R.drawable.bubble_drag_background_two_orange;
                } else {
                    return R.drawable.bubble_drag_background_three_orange;
                }
            }
            case GlobalBubble.COLOR_GREEN: {
                if (num == 1) {
                    return R.drawable.bubble_drag_background_green;
                } else if (num == 2) {
                    return R.drawable.bubble_drag_background_two_green;
                } else {
                    return R.drawable.bubble_drag_background_three_green;
                }
            }
            case GlobalBubble.COLOR_PURPLE: {
                if (num == 1) {
                    return R.drawable.bubble_drag_background_purple;
                } else if (num == 2) {
                    return R.drawable.bubble_drag_background_two_purple;
                } else {
                    return R.drawable.bubble_drag_background_three_purple;
                }
            }
            case GlobalBubble.COLOR_NAVY_BLUE: {
                if (num == 1) {
                    return R.drawable.ppt_bubble_drag_background;
                } else if (num == 2) {
                    return R.drawable.ppt_bubble_drag_background_two;
                } else {
                    return R.drawable.ppt_bubble_drag_background_three;
                }
            }
            default: {
                if (num == 1) {
                    return R.drawable.bubble_drag_background;
                } else if (num == 2) {
                    return R.drawable.bubble_drag_background_two;
                } else {
                    return R.drawable.bubble_drag_background_three;
                }
            }
        }
    }

    public int getDragTextColor() {
        return isShareColor() ? R.color.bubble_text_color_share : R.color.bubble_text_color;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues result = new ContentValues();
        if (getId() > 0) {
            result.put(BUBBLE.ID, getId());
        }
        result.put(BUBBLE.TYPE, getType());
        result.put(BUBBLE.COLOR, getColor());
        result.put(BUBBLE.TODO_TYPE, getToDo());
        Uri uri = getUri();
        result.put(BUBBLE.URI, uri == null ? "" : uri.toString());
        result.put(BUBBLE.TEXT, getText());
        result.put(BUBBLE.TIME_STAMP, getTimeStamp());
        result.put(BUBBLE.SAMPLE_RATE, getSamplineRate());
        result.put(BUBBLE.WEIGHT, getWeight());
        result.put(BUBBLE.VOICE_DURATION, getVoiceDuration());
        result.put(BUBBLE.REMOVED_TIME, getRemovedTime());
        result.put(BUBBLE.STATUS, getStatus());
        result.put(BUBBLE.MODIFIED, getLastModified());
        result.put(BUBBLE.USED_TIME, getUsedTime());
        result.put(BUBBLE.RECEIVER, getSendTo());
        result.put(BUBBLE.SYNC_ID, mSyncId);
        result.put(BUBBLE.VOICE_BUBBLE_SYNC_ID, mVoiceBubbleSyncId);
        result.put(BUBBLE.MODIFY_FLAG, mModifyFlag);
        result.put(BUBBLE.REMIND_TIME, getRemindTime());
        result.put(BUBBLE.DUE_DATE, getDueDate());
        result.put(BUBBLE.CREATE_AT, mCreateAt);
        result.put(BUBBLE.SECONDARY_MODIFY_FLAG, mSecondaryModifyFlag);
        result.put(BUBBLE.VERSION, mVersion);
        result.put(BUBBLE.VOICE_SYNC_ID, mVoiceSyncId);
        result.put(BUBBLE.VOICE_VERSION, mVoiceVersion);
        result.put(BUBBLE.VOICE_SYNC_ENCRYPT_KEY, mVoiceEncryptKey);
        result.put(BUBBLE.USER_ID, mUserId);
        result.put(BUBBLE.LAST_CLOUD_TEXT, mLastCloudText);
        result.put(BUBBLE.CONFLICT_SYNC_ID, mConflictSyncId == null ? "" : mConflictSyncId);
        result.put(BUBBLE.LEGACY_USED_TIME, getLegacyUsedTime());
        result.put(BUBBLE.SHARE_STATUS, getShareStatus());
        result.put(BUBBLE.IS_SHARE_FROM_OTHERS, mIsShareFromOthers);
        result.put(BUBBLE.SHARE_PENDING_STATUS, mSharePendingStatus);
        result.put(BUBBLE.SHARE_PENDING_PARTICIPANTS, getSharePendingParticipantsString());
        return result;
    }

    public boolean isVisibleItem() {
        if (getRemovedTime() > 0 || getLegacyUsedTime() > 0) {
            return false;
        }
        return true;
    }

    public void mergeItem(BubbleItem item) {
        if (item == null) {
            return;
        }
        String text = item.getText();
        if (text != null) {
            setTextSilent(text);
        }
        if (item.getLastCloudText() != null) {
            setLastCloudText(item.getLastCloudText());
        }

        if (getColor() != item.getColor()) {
            setColorSilent(item.getColor());
        }
        if (getToDo() != item.getToDo()) {
            setToDoSilent(item.getToDo());
        }
        if (getTimeStamp() != item.getTimeStamp()) {
            setTimeStampSilent(item.getTimeStamp());
        }
        if (getRemindTime() != item.getRemindTime()) {
            setRemindTimeSilent(item.getRemindTime());
        }
        if (getDueDate() != item.getDueDate()) {
            setDueDateSilent(item.getDueDate());
        }
        if (getCreateAt() != item.getCreateAt()) {
            setCreateAtSilent(item.getCreateAt());
        }
        if (getVersion() != item.getVersion()) {
            setVersion(item.getVersion());
        }
        if (getUserId() != item.getUserId()) {
            setUserId(item.getUserId());
        }
        if (getRemovedTime() != item.getRemovedTime()) {
            setRemovedTimeSilent(item.getRemovedTime());
        }
        if (getUsedTime() != item.getUsedTime()) {
            setUsedTimeSilent(item.getUsedTime());
        }
        if (getShareStatus() != item.getShareStatus()) {
            setShareStatusSilent(item.getShareStatus());
        }
        setSyncId(item.getSyncId());
        setVoiceBubbleSyncId(item.getVoiceBubbleSyncId());
        mIsShareFromOthers = item.isShareFromOthers();
        mSharePendingStatus = item.getSharePendingStatus();
        mSharePendingParticipants = item.getSharePendingParticipants();
        setConflictSyncIdSilent(item.getConflictSyncId());
        //don't merge uri. won't be changed
    }

    public void mergePendingShareStatus(BubbleItem item) {
        if (getShareStatus() != item.getShareStatus()) {
            setShareStatusSilent(item.getShareStatus());
        }
        mSharePendingStatus = item.getSharePendingStatus();
        mSharePendingParticipants = item.getSharePendingParticipants();
    }

    @NonNull
    public List<AttachMentItem> getAttachments() {
        return mAttachments;
    }

    public void setAttachments(List<AttachMentItem> list) {
        mAttachments.clear();
        mAttachments.addAll(list);
    }

    public void addAttachments(List<AttachMentItem> list) {
        mAttachments.addAll(list);
    }

    public void addAttachment(AttachMentItem attachMentItem) {
        mAttachments.add(attachMentItem);
    }

    public boolean haveAttachments() {
        return mAttachments.size() > 0;
    }

    public int getAttachmentCount() {
        if (mAttachments != null) {
            return mAttachments.size();
        }
        return 0;
    }

    public boolean allAttachIsImage() {
        if (mAttachments != null && mAttachments.size() > 0) {
            for (AttachMentItem item : mAttachments) {
                if (item.getType() != AttachMentItem.TYPE_IMAGE) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isEmptyBubble() {
        return !isTextAvailable() && !haveAttachments();
    }

    public void setHasChangedBubbleWithoutSync(boolean hasChangedBubbleWithoutSync) {
        this.mHasChangedBubbleWithoutSync = hasChangedBubbleWithoutSync;
    }

    public boolean isHasChangedBubbleWithoutSync() {
        return mHasChangedBubbleWithoutSync;
    }

    public void setIsAddingAttachmentLock(boolean isAddingAttachmentLock) {
        this.mIsAddingAttachmentLock = isAddingAttachmentLock;
    }

    public void setSyncLock(boolean isSyncLock) {
        mIsSyncLock = isSyncLock;
    }

    public boolean isSyncLocked() {
        return mIsSyncLock;
    }

    public boolean isAttachLocked() {
        return mIsAddingAttachmentLock;
    }

    public BubbleItem cloneForConflict() {
        BubbleItem bubbleItem = new BubbleItem();
        bubbleItem.setColorSilent(getColor());
        bubbleItem.setType(getType());
        bubbleItem.setToDoSilent(getToDo());
        bubbleItem.setRemovedTimeSilent(getRemovedTime());
        bubbleItem.setUsedTimeSilent(getUsedTime());
        bubbleItem.setDueDateSilent(getDueDate());
        bubbleItem.setRemindTimeSilent(getRemindTime());
        bubbleItem.setUri(getUri());
        bubbleItem.setWeightSilent(getWeight());
        bubbleItem.setConflictSyncIdSilent(getSyncId());
        bubbleItem.setConflictLocalId(getId());
        return bubbleItem;
    }

    @Override
    public String getTableName() {
        return BUBBLE.NAME;
    }

    @Override
    public boolean bindCursor(Cursor cursor) {
        try {
            int idIdx            = cursor.getColumnIndex(BUBBLE.ID);
            int typeIdx          = cursor.getColumnIndex(BUBBLE.TYPE);
            int colorIdx         = cursor.getColumnIndex(BUBBLE.COLOR);
            int todoTypeIdx      = cursor.getColumnIndex(BUBBLE.TODO_TYPE);
            int uriIdx           = cursor.getColumnIndex(BUBBLE.URI);
            int textIdx          = cursor.getColumnIndex(BUBBLE.TEXT);
            int timeIdx          = cursor.getColumnIndex(BUBBLE.TIME_STAMP);
            int rateIdx          = cursor.getColumnIndex(BUBBLE.SAMPLE_RATE);
            int weightIdx        = cursor.getColumnIndex(BUBBLE.WEIGHT);
            int voiceDurationIdx = cursor.getColumnIndex(BUBBLE.VOICE_DURATION);
            int removedTimeIdx   = cursor.getColumnIndex(BUBBLE.REMOVED_TIME);
            int modifiedIdx      = cursor.getColumnIndex(BUBBLE.MODIFIED);
            int usedTimeIdx      = cursor.getColumnIndex(BUBBLE.USED_TIME);
            int receiverIdx      = cursor.getColumnIndex(BUBBLE.RECEIVER);
            int syncIdIdx        = cursor.getColumnIndex(BUBBLE.SYNC_ID);
            int modifyFlagIdx    = cursor.getColumnIndex(BUBBLE.MODIFY_FLAG);
            int remindTimeIdx    = cursor.getColumnIndex(BUBBLE.REMIND_TIME);
            int dueDateIdx    = cursor.getColumnIndex(BUBBLE.DUE_DATE);
            int createDateIdx    = cursor.getColumnIndex(BUBBLE.CREATE_AT);
            int secModifyFlagIdx    = cursor.getColumnIndex(BUBBLE.SECONDARY_MODIFY_FLAG);
            int versionIdx    = cursor.getColumnIndex(BUBBLE.VERSION);
            int voiceSyncIdx    = cursor.getColumnIndex(BUBBLE.VOICE_SYNC_ID);
            int voiceVersionIdx    = cursor.getColumnIndex(BUBBLE.VOICE_VERSION);
            int voiceEncryptIdx = cursor.getColumnIndex(BUBBLE.VOICE_SYNC_ENCRYPT_KEY);
            int userIdx = cursor.getColumnIndex(BUBBLE.USER_ID);
            int lastCloudTextIdx = cursor.getColumnIndex(BUBBLE.LAST_CLOUD_TEXT);
            int conflictSyncIdx = cursor.getColumnIndex(BUBBLE.CONFLICT_SYNC_ID);
            int voiceBubbleSyncIdx = cursor.getColumnIndex(BUBBLE.VOICE_BUBBLE_SYNC_ID);
            int legacyUsedTimeIdx = cursor.getColumnIndex(BUBBLE.LEGACY_USED_TIME);
            int markDeleteIdx = cursor.getColumnIndex(BUBBLE.MARK_DELETE);
            int shareStatusIdx = cursor.getColumnIndex(BUBBLE.SHARE_STATUS);
            int isShareFromOthersIdx = cursor.getColumnIndex(BUBBLE.IS_SHARE_FROM_OTHERS);
            int sharePendingStatusIdx = cursor.getColumnIndex(BUBBLE.SHARE_PENDING_STATUS);
            int sharePendingParticipantsIds = cursor.getColumnIndex(BUBBLE.SHARE_PENDING_PARTICIPANTS);

            bubble.setId(cursor.getInt(idIdx));
            bubble.setColor(cursor.getInt(colorIdx));
            bubble.setType(cursor.getInt(typeIdx));
            int todoInDb = cursor.getInt(todoTypeIdx);
            // bubble always can todo check now
            todoInDb = (todoInDb == 0 ? GlobalBubble.TODO : todoInDb);
            bubble.setToDo(todoInDb);
            bubble.setText(cursor.getString(textIdx));
            bubble.setTimeStamp(cursor.getLong(timeIdx));
            bubble.setSamplineRate(cursor.getInt(rateIdx));
            bubble.setVoiceDuration(cursor.getLong(voiceDurationIdx));
            bubble.setRemovedTime(cursor.getLong(removedTimeIdx));
            String uri = cursor.getString(uriIdx);
            if (!TextUtils.isEmpty(uri)) {
                bubble.setUri(Uri.parse(uri));
            }
            mWeight = (cursor.getInt(weightIdx));
            setLastModified(cursor.getLong(modifiedIdx));
            setUsedTimeSilent(cursor.getLong(usedTimeIdx));
            setSendTo(cursor.getString(receiverIdx));
            setSyncId(cursor.getString(syncIdIdx));
            setRemindTimeSilent(cursor.getLong(remindTimeIdx));
            setDueDateSilent(cursor.getLong(dueDateIdx));
            setCreateAtSilent(cursor.getLong(createDateIdx));
            if (mCreateAt == 0) {
                setCreateAtSilent(bubble.getTimeStamp());
            }
            setVersion(cursor.getInt(versionIdx));
            setVoiceSyncId(cursor.getString(voiceSyncIdx));
            setVoiceVersion(cursor.getInt(voiceVersionIdx));
            setModificationFlag(cursor.getInt(modifyFlagIdx));
            setSecondaryModificationFlag(cursor.getInt(secModifyFlagIdx));
            setVoiceEncryptKey(cursor.getString(voiceEncryptIdx));
            setUserId(cursor.getLong(userIdx));
            setLastCloudText(cursor.getString(lastCloudTextIdx));
            setConflictSyncIdSilent(cursor.getString(conflictSyncIdx));
            setVoiceBubbleSyncId(cursor.getString(voiceBubbleSyncIdx));
            setLegacyUsedTime(cursor.getLong(legacyUsedTimeIdx));
            setShareStatusSilent(cursor.getInt(shareStatusIdx));
            setShareFromOthers(cursor.getInt(isShareFromOthersIdx) == 1);
            mSharePendingStatus = cursor.getInt(sharePendingStatusIdx);
            mSharePendingParticipants = new ArrayList<>();
            try {
                String shareParticipantsString = cursor.getString(sharePendingParticipantsIds);
                if (!TextUtils.isEmpty(shareParticipantsString)) {
                    String[] participantArray = shareParticipantsString.split(",");
                    for (String participant : participantArray) {
                        try {
                            mSharePendingParticipants.add(Long.parseLong(participant));
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            } catch (Exception e) {
                //ignore
            }
            if (cursor.getInt(markDeleteIdx) == 1) {
                dele();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
