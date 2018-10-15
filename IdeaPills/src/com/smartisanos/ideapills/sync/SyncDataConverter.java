package com.smartisanos.ideapills.sync;

import android.content.ContentValues;
import android.net.Uri;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;

import com.smartisanos.ideapills.data.ATTACHMENT;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;

import java.util.List;

class SyncDataConverter {
    public static final String BUBBLE_ID = BUBBLE.ID;
    public static final String BUBBLE_COLOR = BUBBLE.COLOR;
    public static final String BUBBLE_TODO_TYPE = BUBBLE.TODO_TYPE;
    public static final String BUBBLE_TEXT = BUBBLE.TEXT;
    public static final String BUBBLE_TIME_STAMP = BUBBLE.TIME_STAMP;
    public static final String BUBBLE_WEIGHT = BUBBLE.WEIGHT;
    public static final String BUBBLE_SYNC_ID = BUBBLE.SYNC_ID;
    public static final String BUBBLE_REMIND_TIME = BUBBLE.REMIND_TIME;
    public static final String BUBBLE_DUE_DATE = BUBBLE.DUE_DATE;
    public static final String BUBBLE_CREATE_AT = BUBBLE.CREATE_AT;
    public static final String BUBBLE_VERSION = BUBBLE.VERSION;
    public static final String BUBBLE_DELETE_STATUS = "delete_status";
    public static final String BUBBLE_REMOVED_DATE = "removed_date";
    public static final String BUBBLE_USED_DATE = "used_date";
    public static final String BUBBLE_CONFLICT_SYNC_ID = BUBBLE.CONFLICT_SYNC_ID;
    public static final String BUBBLE_USER_ID = BUBBLE.USER_ID;
    public static final String BUBBLE_SHARE_STATUS = BUBBLE.SHARE_STATUS;

    public static final String ATTACHMENT_ID = ATTACHMENT.ID;
    public static final String ATTACHMENT_TYPE = "type";
    public static final String ATTACHMENT_URI = "uri";
    public static final String ATTACHMENT_FILENAME = ATTACHMENT.FILENAME;
    public static final String ATTACHMENT_SYNC_ID = ATTACHMENT.SYNC_ID;
    public static final String ATTACHMENT_PILL_ID = "pill_id";
    public static final String ATTACHMENT_PILL_LOCAL_ID = "pill_local_id";
    public static final String ATTACHMENT_CREATE_AT = ATTACHMENT.CREATE_AT;
    public static final String ATTACHMENT_TIME_STAMP = ATTACHMENT.TIME_STAMP;
    public static final String ATTACHMENT_SIZE = ATTACHMENT.SIZE;
    public static final String ATTACHMENT_DOWNLOAD_STATUS = ATTACHMENT.DOWNLOAD_STATUS;
    public static final String ATTACHMENT_UPLOAD_STATUS = ATTACHMENT.UPLOAD_STATUS;
    public static final String ATTACHMENT_VERSION = ATTACHMENT.VERSION;
    public static final String ATTACHMENT_DELETE_STATUS = "delete_status";
    public static final String ATTACHMENT_ENCRYPT_KEY = "encrypt_key";
    public static final String ATTACHMENT_USER_ID = ATTACHMENT.USER_ID;

    public static final String VOICE_CONTENT_FILE_PATH = "content://com.smartisanos.sara/wav/";

    public static final int BUBBLE_ITEM_NONE_CHANGED = 0;
    public static final int BUBBLE_ITEM_CHANGED_OTHER = 0x1 << 1;
    public static final int BUBBLE_ITEM_CHANGED_TODO = 0x1 << 2;
    public static final int BUBBLE_ITEM_CHANGED_CONFLICT_TEXT = 0x1 << 3;
    public static final int BUBBLE_ITEM_CHANGED_TEXT = 0x1 << 4;
    public static final int BUBBLE_ITEM_CHANGED_DUE_DATE = 0x1 << 5;
    public static final int BUBBLE_ITEM_CHANGED_TO_SHOW = 0x1 << 6;
    public static final int BUBBLE_ITEM_CHANGED_TO_HIDE = 0x1 << 7;
    public static final int BUBBLE_ITEM_CHANGED_SHARE_STATUS_TO_NO = 0x1 << 8;
    public static final int BUBBLE_ITEM_CHANGED_SHARE_STATUS_TO_YES = 0x1 << 9;

    // 未定义, 代表请求或者响应中没有设置该值
    public static final int DELETE_STATUS_NOT_DEFINED = 0;
    // 正常状态
    public static final int DELETE_STATUS_NORMAL = 1;
    // 标记删除，此时界面可以隐藏，或者放入回收站
    public static final int DELETE_STATUS_MARK_DELETED = 2;
    // 彻底删除
    public static final int DELETE_STATUS_PERMANENT_DELETED = 3;

    static BubbleItem newBubbleItem(ContentValues value) {
        if (value == null) {
            return null;
        }
        BubbleItem item = new BubbleItem();
        if (value.containsKey(BUBBLE_COLOR)) {
            int color = value.getAsInteger(BUBBLE_COLOR);
            item.setColorSilent(color);
        }
        if (value.containsKey(BUBBLE_TODO_TYPE)) {
            int todo = value.getAsInteger(BUBBLE_TODO_TYPE);
            // bubble always can todo check now
            todo = (todo == 0 ? GlobalBubble.TODO : todo);
            item.setToDoSilent(todo);
        }
        if (value.containsKey(BUBBLE_TEXT)) {
            String text = value.getAsString(BUBBLE_TEXT);
            item.setTextSilent(text);
            item.setLastCloudText(text);
        }
        if (value.containsKey(BUBBLE_TIME_STAMP)) {
            long timestamp = value.getAsLong(BUBBLE_TIME_STAMP);
            item.setTimeStampSilent(timestamp);
        }
        if (value.containsKey(BUBBLE_WEIGHT)) {
            int weight = value.getAsInteger(BUBBLE_WEIGHT);
            item.setWeightSilent(weight);
        }
        if (value.containsKey(BUBBLE_SYNC_ID)) {
            String syncId = value.getAsString(BUBBLE_SYNC_ID);
            item.setSyncId(syncId);
            item.setVoiceBubbleSyncId(syncId);
        }
        if (value.containsKey(BUBBLE_REMIND_TIME)) {
            item.setRemindTimeSilent(value.getAsLong(BUBBLE_REMIND_TIME));
        }
        if (value.containsKey(BUBBLE_DUE_DATE)) {
            item.setDueDateSilent(value.getAsLong(BUBBLE_DUE_DATE));
        }
        if (value.containsKey(BUBBLE_CREATE_AT)) {
            item.setCreateAtSilent(value.getAsLong(BUBBLE_CREATE_AT));
        }
        if (value.containsKey(BUBBLE_REMOVED_DATE)) {
            item.setRemovedTimeSilent(value.getAsLong(BUBBLE_REMOVED_DATE));
        }
        if (value.containsKey(BUBBLE_USED_DATE)) {
            item.setUsedTimeSilent(value.getAsLong(BUBBLE_USED_DATE));
        }
        if (value.containsKey(BUBBLE_CONFLICT_SYNC_ID)) {
            String serverConflictId = value.getAsString(BUBBLE_CONFLICT_SYNC_ID);
            if (BubbleItem.CONFLICT_HANDLED_TAG.equals(serverConflictId)) {
                item.setConflictSyncIdSilent(null);
            } else {
                item.setConflictSyncIdSilent(serverConflictId);
            }
        } else {
            item.setConflictSyncIdSilent(null);
        }
        if (value.containsKey(BUBBLE_VERSION)) {
            int version = value.getAsInteger(BUBBLE_VERSION);
            item.setVersion(version);
        }
        if (value.containsKey(BUBBLE_SHARE_STATUS)) {
            int shareStatus = value.getAsInteger(BUBBLE_SHARE_STATUS);
            item.setShareStatusSilent(shareStatus);
        }
        item.setModificationFlag(0);
        item.setSecondaryModificationFlag(0);
        return item;
    }

    static int replaceBubbleItem(BubbleItem item, ContentValues value, List<BubbleItem> containerForConflict) {
        int changedFlag = BUBBLE_ITEM_NONE_CHANGED;
        if (item == null || value == null) {
            return changedFlag;
        }
        boolean changedConflict = false;
        String lastItemText = null;
        if (!item.getModificationFlag(BubbleItem.MF_TEXT)) {
            //text remote changed
            if (value.containsKey(BUBBLE_TEXT)) {
                String text = value.getAsString(BUBBLE_TEXT);
                if (text == null) {
                    text = "";
                }
                if (!text.equals(item.getText())) {
                    item.setTextSilent(text);
                    changedFlag |= BUBBLE_ITEM_CHANGED_TEXT;
                }
                item.setLastCloudText(text);
            }
        } else {
            if (containerForConflict != null) {
                if (value.containsKey(BUBBLE_TEXT)) {
                    String text = value.getAsString(BUBBLE_TEXT);
                    String lastCloudText = item.getLastCloudText();
                    if (text != null && lastCloudText != null && !text.equals(lastCloudText)
                            && !text.equals(item.getText())) {
                        lastItemText = item.getText();
                        item.setTextSilent(text);
                        item.setLastCloudText(text);
                        changedFlag |= BUBBLE_ITEM_CHANGED_CONFLICT_TEXT;
                        changedConflict = true;
                    }
                }
            }
        }
        if (changedConflict && lastItemText != null) {
            item.setModificationFlag(0);
            item.setSecondaryModificationFlag(0);
            BubbleItem conflictItem = item.cloneForConflict();
            conflictItem.setTextSilent(lastItemText);
            containerForConflict.add(conflictItem);
        }

        if (!item.getModificationFlag(BubbleItem.MF_COLOR) || changedConflict) {
            if (value.containsKey(BUBBLE_COLOR)) {
                int color = value.getAsInteger(BUBBLE_COLOR);
                if (item.getColor() != color) {
                    item.setColorSilent(color);
                    changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_TIME_STAMP) || changedConflict) {
            if (value.containsKey(BUBBLE_TIME_STAMP)) {
                long timestamp = value.getAsLong(BUBBLE_TIME_STAMP);
                if (item.getTimeStamp() != timestamp) {
                    item.setTimeStampSilent(timestamp);
                    changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_REMIND_TIME) || changedConflict) {
            if (value.containsKey(BUBBLE_REMIND_TIME)) {
                long remindTime = value.getAsLong(BUBBLE_REMIND_TIME);
                if (item.getRemindTime() != remindTime) {
                    item.setRemindTimeSilent(remindTime);
                    changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_DUE_DATE) || changedConflict) {
            if (value.containsKey(BUBBLE_DUE_DATE)) {
                long dueDate = value.getAsLong(BUBBLE_DUE_DATE);
                if (item.getDueDate() != dueDate) {
                    item.setDueDateSilent(dueDate);
                    changedFlag |= BUBBLE_ITEM_CHANGED_DUE_DATE;
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_CREATE_DATE) || changedConflict) {
            if (value.containsKey(BUBBLE_CREATE_AT)) {
                long createDate = value.getAsLong(BUBBLE_CREATE_AT);
                if (item.getCreateAt() != createDate) {
                    item.setCreateAtSilent(createDate);
                    changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                }
            }
        }
        if (value.containsKey(BUBBLE_VERSION)) {
            int version = value.getAsInteger(BUBBLE_VERSION);
            if (item.getVersion() != version) {
                item.setVersion(version);
                changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
            }
        }
        if (value.containsKey(BUBBLE_SYNC_ID)) {
            String syncId = value.getAsString(BUBBLE_SYNC_ID);
            if (syncId != null && !syncId.equals(item.getSyncId())) {
                item.setSyncId(syncId);
                item.setVoiceBubbleSyncId(syncId);
                changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_CONFLICT_SYNC_ID) || changedConflict) {
            if (value.containsKey(BUBBLE_CONFLICT_SYNC_ID)) {
                String serverConflictId = value.getAsString(BUBBLE_CONFLICT_SYNC_ID);
                if (BubbleItem.CONFLICT_HANDLED_TAG.equals(serverConflictId)) {
                    item.setConflictSyncIdSilent(null);
                } else {
                    item.setConflictSyncIdSilent(serverConflictId);
                }
            } else {
                item.setConflictSyncIdSilent(null);
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_TODO) || changedConflict) {
            if (value.containsKey(BUBBLE_TODO_TYPE)) {
                int todo = value.getAsInteger(BUBBLE_TODO_TYPE);
                // bubble always can todo check now
                todo = (todo == 0 ? GlobalBubble.TODO : todo);
                if (item.getToDo() != todo) {
                    item.setToDoSilent(todo);
                    if (item.getToDo() == GlobalBubble.TODO_OVER) {
                        changedFlag |= BUBBLE_ITEM_CHANGED_TODO;
                    } else {
                        changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                    }
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_REMOVED_TIME) || changedConflict) {
            if (value.containsKey(BUBBLE_REMOVED_DATE)) {
                long removedDate = value.getAsLong(BUBBLE_REMOVED_DATE);
                if (item.getRemovedTime() != removedDate) {
                    long oldRemoveTime = item.getRemovedTime();
                    item.setRemovedTimeSilent(removedDate);
                    if (item.getRemovedTime() == 0) {
                        changedFlag |= BUBBLE_ITEM_CHANGED_TO_SHOW;
                    } else if (oldRemoveTime == 0) {
                        changedFlag |= BUBBLE_ITEM_CHANGED_TO_HIDE;
                    } else {
                        changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                    }
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_USED_TIME) || changedConflict) {
            if (value.containsKey(BUBBLE_USED_DATE)) {
                long usedDate = value.getAsLong(BUBBLE_USED_DATE);
                if (item.getUsedTime() != usedDate) {
                    item.setUsedTimeSilent(usedDate);
                    changedFlag |= BUBBLE_ITEM_CHANGED_OTHER;
                }
            }
        }
        if (!item.getModificationFlag(BubbleItem.MF_SHARE_STATUS) || changedConflict) {
            if (value.containsKey(BUBBLE_SHARE_STATUS)) {
                int shareStatus = value.getAsInteger(BUBBLE_SHARE_STATUS);
                if (item.getShareStatus() != shareStatus) {
                    item.setShareStatusSilent(shareStatus);
                    if (shareStatus == GlobalBubble.SHARE_STATUS_ONE_TO_ONE
                            || shareStatus == GlobalBubble.SHARE_STATUS_MANY_TO_MANY) {
                        changedFlag |= BUBBLE_ITEM_CHANGED_SHARE_STATUS_TO_YES;
                    } else {
                        changedFlag |= BUBBLE_ITEM_CHANGED_SHARE_STATUS_TO_NO;
                    }
                }
            }
        }
        return changedFlag;
    }

    static ContentValues convertToItemValues(ContentValues value) {
        if (value == null) {
            return null;
        }
        ContentValues result = new ContentValues();
        if (value.containsKey(BUBBLE_ID)) {
            int id = value.getAsInteger(BUBBLE_ID);
            result.put(BUBBLE.ID, id);
        }
        if (value.containsKey(BUBBLE_COLOR)) {
            int color = value.getAsInteger(BUBBLE_COLOR);
            result.put(BUBBLE.COLOR, color);
        }
        if (value.containsKey(BUBBLE_TODO_TYPE)) {
            int todo = value.getAsInteger(BUBBLE_TODO_TYPE);
            result.put(BUBBLE.TODO_TYPE, todo);
        }
        if (value.containsKey(BUBBLE_TEXT)) {
            String text = value.getAsString(BUBBLE_TEXT);
            result.put(BUBBLE.TEXT, text);
        }
        if (value.containsKey(BUBBLE_TIME_STAMP)) {
            long timestamp = value.getAsLong(BUBBLE_TIME_STAMP);
            result.put(BUBBLE.TIME_STAMP, timestamp);
        }
        if (value.containsKey(BUBBLE_WEIGHT)) {
            int weight = value.getAsInteger(BUBBLE_WEIGHT);
            result.put(BUBBLE.WEIGHT, weight);
        }
        if (value.containsKey(BUBBLE_SYNC_ID)) {
            String syncId = value.getAsString(BUBBLE_SYNC_ID);
            result.put(BUBBLE.SYNC_ID, syncId);
            result.put(BUBBLE.VOICE_BUBBLE_SYNC_ID, syncId);
        }
        if (value.containsKey(BUBBLE_REMIND_TIME)) {
            result.put(BUBBLE.REMIND_TIME, value.getAsLong(BUBBLE_REMIND_TIME));
        }
        if (value.containsKey(BUBBLE_DUE_DATE)) {
            result.put(BUBBLE.DUE_DATE, value.getAsLong(BUBBLE_DUE_DATE));
        }
        if (value.containsKey(BUBBLE_CREATE_AT)) {
            result.put(BUBBLE.CREATE_AT, value.getAsLong(BUBBLE_CREATE_AT));
        }
        if (value.containsKey(BUBBLE_REMOVED_DATE)) {
            result.put(BUBBLE.REMOVED_TIME, value.getAsLong(BUBBLE_REMOVED_DATE));
        }
        if (value.containsKey(BUBBLE_USED_DATE)) {
            result.put(BUBBLE.USED_TIME, value.getAsLong(BUBBLE_USED_DATE));
        }
        if (value.containsKey(BUBBLE_VERSION)) {
            int version = value.getAsInteger(BUBBLE_VERSION);
            result.put(BUBBLE.VERSION, version);
        }
        if (value.containsKey(BUBBLE_CONFLICT_SYNC_ID)) {
            String syncId =  value.getAsString(BUBBLE_CONFLICT_SYNC_ID);
            result.put(BUBBLE.CONFLICT_SYNC_ID, syncId);
        }
        if (value.containsKey(BUBBLE_SHARE_STATUS)) {
            int shareStatus =  value.getAsInteger(BUBBLE_SHARE_STATUS);
            result.put(BUBBLE.SHARE_STATUS, shareStatus);
        }
        return result;
    }

    static ContentValues bubbleToContentValues(BubbleItem bubbleItem, long userId) {
        ContentValues result = new ContentValues();
        if (bubbleItem.getId() > 0) {
            result.put(BUBBLE_ID, bubbleItem.getId());
        }
        result.put(BUBBLE_COLOR, bubbleItem.getColor());
        result.put(BUBBLE_TODO_TYPE, bubbleItem.getToDo());
        result.put(BUBBLE_TEXT, bubbleItem.getText());
        result.put(BUBBLE_TIME_STAMP, bubbleItem.getTimeStamp());
        result.put(BUBBLE_WEIGHT, bubbleItem.getWeight());
        result.put(BUBBLE_REMIND_TIME, bubbleItem.getRemindTime());
        result.put(BUBBLE_DUE_DATE, bubbleItem.getDueDate());
        result.put(BUBBLE_REMOVED_DATE, bubbleItem.getRemovedTime());
        result.put(BUBBLE_USED_DATE, bubbleItem.getUsedTime());
        result.put(BUBBLE_CREATE_AT, bubbleItem.getCreateAt());
        result.put(BUBBLE_SHARE_STATUS, bubbleItem.getShareStatus());
        if (!TextUtils.isEmpty(bubbleItem.getSyncId())) {
            result.put(BUBBLE_SYNC_ID, bubbleItem.getSyncId());
        }
        result.put(BUBBLE_VERSION, bubbleItem.getVersion());
        if (!TextUtils.isEmpty(bubbleItem.getConflictSyncId())) {
            result.put(BUBBLE_CONFLICT_SYNC_ID, bubbleItem.getConflictSyncId());
        }
        if (bubbleItem.getRemovedTime() > 0) {
            result.put(BUBBLE_DELETE_STATUS, DELETE_STATUS_MARK_DELETED);
        } else {
            result.put(BUBBLE_DELETE_STATUS, DELETE_STATUS_NORMAL);
        }
        if (userId > 0) {
            result.put(BUBBLE_USER_ID, userId);
        }
        return result;
    }

    static ContentValues incrementBubbleToContentValues(BubbleItem bubbleItem) {
        ContentValues result = new ContentValues();

        result.put(BUBBLE_ID, bubbleItem.getId());
        if (bubbleItem.getModificationFlag(BubbleItem.MF_COLOR)) {
            result.put(BUBBLE_COLOR, bubbleItem.getColor());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_TODO)) {
            result.put(BUBBLE_TODO_TYPE, bubbleItem.getToDo());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_TEXT)) {
            result.put(BUBBLE_TEXT, bubbleItem.getText());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_TIME_STAMP)) {
            result.put(BUBBLE_TIME_STAMP, bubbleItem.getTimeStamp());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_WEIGHT)) {
            result.put(BUBBLE_WEIGHT, bubbleItem.getWeight());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_REMIND_TIME)) {
            result.put(BUBBLE_REMIND_TIME, bubbleItem.getRemindTime());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_DUE_DATE)) {
            result.put(BUBBLE_DUE_DATE, bubbleItem.getDueDate());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_REMOVED_TIME)) {
            result.put(BUBBLE_REMOVED_DATE, bubbleItem.getRemovedTime());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_USED_TIME)) {
            result.put(BUBBLE_USED_DATE, bubbleItem.getUsedTime());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_CREATE_DATE)) {
            result.put(BUBBLE_CREATE_AT, bubbleItem.getCreateAt());
        }
        if (bubbleItem.getModificationFlag(BubbleItem.MF_CONFLICT_SYNC_ID)) {
            if (!TextUtils.isEmpty(bubbleItem.getConflictSyncId())) {
                result.put(BUBBLE_CONFLICT_SYNC_ID, bubbleItem.getConflictSyncId());
            } else {
                result.put(BUBBLE_CONFLICT_SYNC_ID, BubbleItem.CONFLICT_HANDLED_TAG);
            }
        }
        if (bubbleItem.getUserId() > 0) {
            result.put(BUBBLE_USER_ID, bubbleItem.getUserId());
        }
        result.put(BUBBLE_SHARE_STATUS, bubbleItem.getShareStatus());
        result.put(BUBBLE_SYNC_ID, bubbleItem.getSyncId());
        result.put(BUBBLE_VERSION, bubbleItem.getVersion());
        if (bubbleItem.getRemovedTime() > 0) {
            result.put(BUBBLE_DELETE_STATUS, DELETE_STATUS_MARK_DELETED);
        } else {
            result.put(BUBBLE_DELETE_STATUS, DELETE_STATUS_NORMAL);
        }
        return result;
    }

    static ContentValues triggerWeightBubbleToContentValues(BubbleItem bubbleItem) {
        ContentValues result = new ContentValues();
        result.put(BUBBLE_ID, bubbleItem.getId());
        result.put(BUBBLE_WEIGHT, bubbleItem.getWeight());
        if (bubbleItem.getUserId() > 0) {
            result.put(BUBBLE_USER_ID, bubbleItem.getUserId());
        }
        result.put(BUBBLE_SYNC_ID, bubbleItem.getSyncId());
        result.put(BUBBLE_VERSION, bubbleItem.getVersion());
        if (bubbleItem.getRemovedTime() > 0) {
            result.put(BUBBLE_DELETE_STATUS, DELETE_STATUS_MARK_DELETED);
        } else {
            result.put(BUBBLE_DELETE_STATUS, DELETE_STATUS_NORMAL);
        }
        return result;
    }

    static AttachMentItem newAttachmentItem(ContentValues value, int bubbleId) {
        if (value == null) {
            return null;
        }
        AttachMentItem item = new AttachMentItem();
        replaceAttachmentItem(item, value, bubbleId, true);
        return item;
    }

    static boolean replaceAttachmentItem(AttachMentItem item, ContentValues value, int bubbleId) {
        return replaceAttachmentItem(item, value, bubbleId, false);
    }

    static boolean replaceAttachmentItem(AttachMentItem item, ContentValues value, int bubbleId,
                                         boolean isNewAttachment) {
        if (item == null || value == null) {
            return false;
        }
        boolean changed = false;
        if (value.containsKey(ATTACHMENT_FILENAME) && isNewAttachment) {
            String fileName = value.getAsString(ATTACHMENT_FILENAME);
            if (fileName == null) {
                fileName = "";
            }
            if (!fileName.equals(item.getFilename())) {
                item.setFilename(fileName);
                changed = true;
            }
        }
        if (value.containsKey(ATTACHMENT_TYPE)) {
            int attachmentType = value.getAsInteger(ATTACHMENT_TYPE);
            String mimeType = SyncMediaTypeHelper.attachmentTypeToMimeType(attachmentType, item.getFilename());
            if (!mimeType.equals(item.getContentType())) {
                item.setContentType(mimeType);
                changed = true;
            }
        }
        if (value.containsKey(ATTACHMENT_SYNC_ID)) {
            String syncId = value.getAsString(ATTACHMENT_SYNC_ID);
            if (syncId != null && !syncId.equals(item.getSyncId())) {
                item.setSyncId(syncId);
                changed = true;
            }
        }
        if (value.containsKey(ATTACHMENT_CREATE_AT) && isNewAttachment) {
            long createAt = value.getAsLong(ATTACHMENT_CREATE_AT);
            if (createAt > 0 && createAt != item.getCreateAt()) {
                item.setCreateAt(createAt);
                changed = true;
            }
        }
        if (value.containsKey(ATTACHMENT_TIME_STAMP)) {
            long timestamp = value.getAsLong(ATTACHMENT_TIME_STAMP);
            if (timestamp != item.getTimestamp()) {
                item.setTimestamp(timestamp);
                changed = true;
            }
        }
        if (value.containsKey(ATTACHMENT_SIZE)) {
            long size = value.getAsLong(ATTACHMENT_SIZE);
            if (size != item.getSize()) {
                item.setSize(size);
                changed = true;
            }
        }
        if (item.getBubbleId() != bubbleId && bubbleId > 0) {
            item.setBubbleId(bubbleId);
        }
        if (value.containsKey(ATTACHMENT_DOWNLOAD_STATUS)) {
            item.setDownloadStatus(value.getAsInteger(ATTACHMENT_DOWNLOAD_STATUS));
        } else {
            item.setDownloadStatus(AttachMentItem.DOWNLOAD_STATUS_NOT_DOWNLOAD);
        }
        if (value.containsKey(ATTACHMENT_UPLOAD_STATUS)) {
            item.setUploadStatus(value.getAsInteger(ATTACHMENT_UPLOAD_STATUS));
        } else {
            item.setUploadStatus(AttachMentItem.UPLOAD_STATUS_NOT_UPLOAD);
        }
        if (value.containsKey(ATTACHMENT_ENCRYPT_KEY)) {
            item.setSyncEncryptKey(byteArrayToHexStr(value.getAsByteArray(ATTACHMENT_ENCRYPT_KEY)));
        }
        if (value.containsKey(ATTACHMENT_VERSION)) {
            int version = value.getAsInteger(ATTACHMENT_VERSION);
            if (version != item.getVersion()) {
                item.setVersion(version);
                changed = true;
            }
        }
        return changed;
    }

    static void replaceAttachmentInBubble(BubbleItem item, ContentValues value) {
        if (value.containsKey(ATTACHMENT_FILENAME)) {
            String fileName = value.getAsString(ATTACHMENT_FILENAME);
            String uri = VOICE_CONTENT_FILE_PATH + fileName;
            item.setUri(Uri.parse(uri));
            item.setType(BubbleItem.TYPE_VOICE);
        }
        if (value.containsKey(ATTACHMENT_SYNC_ID)) {
            String syncId = value.getAsString(ATTACHMENT_SYNC_ID);
            item.setVoiceSyncId(syncId);
        }
        if (value.containsKey(ATTACHMENT_ENCRYPT_KEY)) {
            String encryptKey = byteArrayToHexStr(value.getAsByteArray(ATTACHMENT_ENCRYPT_KEY));
            item.setVoiceEncryptKey(encryptKey);
        }
        if (value.containsKey(ATTACHMENT_VERSION)) {
            int version = value.getAsInteger(ATTACHMENT_VERSION);
            item.setVoiceVersion(version);
        }
    }

//    static void replaceAttachmentInBubbleWave(BubbleItem item, ContentValues value) {
//        if (value.containsKey(ATTACHMENT_FILENAME)) {
//            String fileName = value.getAsString(ATTACHMENT_FILENAME);
//            String uri = VOICE_CONTENT_FILE_PATH + fileName;
//            final byte[] wave;
//            IdeaPillsApp app = IdeaPillsApp.getInstance();
//            wave = Utils.getWaveDataByWaveUri(app, Uri.parse(uri));
//            item.setWaveData(wave);
//        }
//        if (value.containsKey(ATTACHMENT_SYNC_ID)) {
//            String syncId = value.getAsString(ATTACHMENT_SYNC_ID);
//            item.setVoiceWaveSyncId(syncId);
//        }
//        if (value.containsKey(ATTACHMENT_VERSION)) {
//            int version = value.getAsInteger(ATTACHMENT_VERSION);
//            item.setVoiceWaveVersion(version);
//        }
//    }

    static ContentValues attachmentToContentValues(AttachMentItem attachMentItem,
                                                   String bubbleSyncId, int bubbleLocalId, long userId) {
        ContentValues result = new ContentValues();
        if (attachMentItem.getId() > 0) {
            result.put(ATTACHMENT_ID, attachMentItem.getId());
        }
        result.put(ATTACHMENT_FILENAME, attachMentItem.getFilename());
        result.put(ATTACHMENT_TYPE, SyncMediaTypeHelper.mimeTypeToAttachmentType(
                attachMentItem.getContentType(), attachMentItem.getFilename()));
        if (attachMentItem.getUri() != null) {
            result.put(ATTACHMENT_URI, attachMentItem.getUri().getPath());
        }
        if (!TextUtils.isEmpty(attachMentItem.getSyncId())) {
            result.put(ATTACHMENT_SYNC_ID, attachMentItem.getSyncId());
        }
        if (!TextUtils.isEmpty(bubbleSyncId)) {
            result.put(ATTACHMENT_PILL_ID, bubbleSyncId);
        }
        if (bubbleLocalId > 0) {
            result.put(ATTACHMENT_PILL_LOCAL_ID, bubbleLocalId);
        }
        result.put(ATTACHMENT_CREATE_AT, attachMentItem.getCreateAt());
        result.put(ATTACHMENT_TIME_STAMP, attachMentItem.getTimestamp());
        if (attachMentItem.getSize() > 0) {
            result.put(ATTACHMENT_SIZE, attachMentItem.getSize());
        }
        result.put(ATTACHMENT_VERSION, attachMentItem.getVersion());
        if (!TextUtils.isEmpty(attachMentItem.getSyncEncryptKey())) {
            byte[] encryptKeyBytes = hexStrToByteArray(attachMentItem.getSyncEncryptKey());
            if (encryptKeyBytes != null) {
                result.put(ATTACHMENT_ENCRYPT_KEY, encryptKeyBytes);
            }
        }
        result.put(ATTACHMENT_DELETE_STATUS, DELETE_STATUS_NORMAL);
        if (userId > 0) {
            result.put(ATTACHMENT_USER_ID, userId);
        }
        return result;
    }

    static ContentValues voiceAttachmentToContentValues(BubbleItem bubbleItem) {
        ContentValues result = new ContentValues();
        // for attachment we pass bubble id in ATTACHMENT_ID
        result.put(ATTACHMENT_ID, bubbleItem.getId());
        result.put(ATTACHMENT_TYPE, SyncMediaTypeHelper.ATTACHMENT_TYPE_VOICE_RECORD);
        if (bubbleItem.getUri() != null) {
            String uriString = bubbleItem.getUri().toString();
            result.put(ATTACHMENT_URI, uriString);
            int index = uriString.lastIndexOf("/");
            if (index >= 0 && index < uriString.length()) {
                result.put(ATTACHMENT_FILENAME, uriString.substring(index + 1));
            }
        }
        if (!TextUtils.isEmpty(bubbleItem.getVoiceSyncId())) {
            result.put(ATTACHMENT_SYNC_ID, bubbleItem.getVoiceSyncId());
        }
        if (!TextUtils.isEmpty(bubbleItem.getVoiceEncryptKey())) {
            byte[] encryptKeyBytes = hexStrToByteArray(bubbleItem.getVoiceEncryptKey());
            if (encryptKeyBytes != null) {
                result.put(ATTACHMENT_ENCRYPT_KEY, bubbleItem.getVoiceEncryptKey());
            }
        }
        result.put(ATTACHMENT_CREATE_AT, bubbleItem.getCreateAt());
        result.put(ATTACHMENT_TIME_STAMP, bubbleItem.getTimeStamp());
        if (!TextUtils.isEmpty(bubbleItem.getSyncId())) {
            result.put(ATTACHMENT_PILL_ID, bubbleItem.getSyncId());
        } else if (!TextUtils.isEmpty(bubbleItem.getVoiceBubbleSyncId())) {
            result.put(ATTACHMENT_PILL_ID, bubbleItem.getVoiceBubbleSyncId());
        }
        if (bubbleItem.getId() > 0) {
            result.put(ATTACHMENT_PILL_LOCAL_ID, bubbleItem.getId());
        }
        result.put(ATTACHMENT_DELETE_STATUS, DELETE_STATUS_NORMAL);
        result.put(ATTACHMENT_VERSION, bubbleItem.getVoiceVersion());
        return result;
    }

//    static ContentValues voiceWaveAttachmentToContentValues(BubbleItem bubbleItem) {
//        ContentValues result = new ContentValues();
//        // for attachment we pass bubble id in ATTACHMENT_ID
//        result.put(ATTACHMENT_ID, bubbleItem.getId());
//        result.put(ATTACHMENT_TYPE, ATTACHMENT_TYPE_VOICE_RECORD);
//        if (bubbleItem.getUri() != null) {
//            String uriString = bubbleItem.getUri().toString();
//            result.put(ATTACHMENT_URI, uriString + BubbleItem.WAVE_SUFFIX);
//            int index = uriString.lastIndexOf("/");
//            if (index >= 0 && index < uriString.length()) {
//                result.put(ATTACHMENT_FILENAME, uriString.substring(index + 1) + BubbleItem.WAVE_SUFFIX);
//            }
//        }
//        if (!TextUtils.isEmpty(bubbleItem.getVoiceWaveSyncId())) {
//            result.put(ATTACHMENT_SYNC_ID, bubbleItem.getVoiceWaveSyncId());
//        }
//        result.put(ATTACHMENT_PILL_ID, bubbleItem.getSyncId() == null ? "" : bubbleItem.getSyncId());
//        result.put(ATTACHMENT_PILL_LOCAL_ID, bubbleItem.getId());
//        result.put(ATTACHMENT_CREATE_AT, bubbleItem.getCreateAt());
//        result.put(ATTACHMENT_TIME_STAMP, bubbleItem.getTimeStamp());
//        result.put(ATTACHMENT_SIZE, attachMentItem.getSize());
//        result.put(ATTACHMENT_VERSION, bubbleItem.getVoiceWaveVersion());
//        result.put(ATTACHMENT_DELETE_STATUS, DELETE_STATUS_NORMAL);
//        return result;
//    }

    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStrToByteArray(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        try {
            byte[] byteArray = new byte[str.length() / 2];
            for (int i = 0; i < byteArray.length; i++) {
                String subStr = str.substring(2 * i, 2 * i + 2);
                byteArray[i] = ((byte) Integer.parseInt(subStr, 16));
            }
            return byteArray;
        } catch (Exception e) {
            return null;
        }
    }
}
