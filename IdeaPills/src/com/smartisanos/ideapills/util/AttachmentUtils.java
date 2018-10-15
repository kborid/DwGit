package com.smartisanos.ideapills.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.AttachmentUtil;
import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.data.DataHandler;

import com.smartisanos.ideapills.common.util.UriUtils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;

import java.util.ArrayList;
import java.util.List;


public class AttachmentUtils {
    private static final LOG log = LOG.getInstance(AttachmentUtils.class);

    public static final int ATTACHMENT_QUANTITY_LIMIT = 14;
    public static final int CHILD_COUNT_PER_ROW = 7;
    public static final long ATTACHMENT_SIZE_LIMIT = 30 * 1024 * 1024;

    public static boolean removeFromListOverSize(List<AttachMentItem> list, int limit) {
        if (limit < 0) {
            limit = 0;
        }
        final int sizeorg = list.size();
        for (int i = list.size() - 1; i >= limit; i--) {
            list.remove(i);
        }
        return list.size() != sizeorg;
    }

    public static boolean handleAttachmentPickResult(Intent data, final int bubbleId, final Context context) {
        if (data == null) {
            LOG.DEF.error("handleAttachmentPickResult return by data is null");
            return false;
        }
        return handleAttachmentPickResult(data.getClipData(), data.getData(), bubbleId, context);
    }

    public static boolean handleAttachmentPickResult(ClipData clipData, Uri dataUri, final int bubbleId, final Context context) {
        if (clipData == null && dataUri == null) {
            LOG.DEF.error("handleAttachmentPickResult return by data is null");
            return false;
        }
        final List<AttachMentItem> list = new ArrayList<AttachMentItem>();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                AttachMentItem item = generateAttachmentFromUri(context, uri);
                if (null != item) {
                    list.add(item);
                }
            }
        } else {
            AttachMentItem item = generateAttachmentFromUri(context, dataUri);
            if (null != item) {
                list.add(item);
            }
        }
        BubbleItem bubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(bubbleId);
        if (bubbleItem == null) {
            LOG.DEF.error("handleAttachmentPickResult return by bubbleItem is null");
            return false;
        }
        long limitSize = AttachmentUtils.ATTACHMENT_SIZE_LIMIT;
        List<AttachMentItem> listAttachBefore = bubbleItem.getAttachments();
        if (listAttachBefore != null && listAttachBefore.size() > 0) {
            for (AttachMentItem item : listAttachBefore) {
                item.setSize(AttachmentUtil.queryFileSize(context.getApplicationContext(), item.getUri()));
                limitSize -= item.getSize();
            }
        }
        int restSize = AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT - bubbleItem.getAttachments().size();

        boolean isFileTooLarge = false;
        if (list.size() > 0) {
            long totalSize = 0;
            for (AttachMentItem item : list) {
                item.setSize(AttachmentUtil.queryFileSize(context.getApplicationContext(), item.getOriginalUri()));
                totalSize += item.getSize();
                if (totalSize > limitSize) {
                    isFileTooLarge = true;
                    break;
                }
            }

            boolean isFileTooMuch = list.size() > restSize;

            if (isFileTooMuch  && isFileTooLarge) {
                String showToast = context.getString(R.string.bubble_attachment_size_count_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                GlobalBubbleUtils.showSystemToast(context, showToast, Toast.LENGTH_SHORT);
                return false;
            } else if (isFileTooMuch) {
                String showToast = context.getString(R.string.bubble_attachment_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                GlobalBubbleUtils.showSystemToast(context, showToast, Toast.LENGTH_SHORT);
                return false;
            } else if (isFileTooLarge) {
                GlobalBubbleUtils.showSystemToast(context, R.string.bubble_attachment_size_limit, Toast.LENGTH_SHORT);
                return false;
            }

            if (list.size() > 0) {
                bubbleItem.setIsAddingAttachmentLock(true);
                final String bubbleSyncId = bubbleItem.getSyncId();
                TaskHandler.post(new Runnable() {
                    public void run() {
                        Context applicationContext = context.getApplicationContext();
                        for (AttachMentItem item : list) {
                            item.setBubbleId(bubbleId);
                            item.setBubbleSyncId(bubbleSyncId);
                            item.setFilename(AttachmentUtil.queryFileName(applicationContext, item.getOriginalUri()));
                            item.setContentType(AttachmentUtil.queryFileType(applicationContext, item.getOriginalUri(), item.getFilename()));
                            Uri saveUri = AttachmentUtil.copyFileToInnerDir(applicationContext, item.getOriginalUri(), item.getFilename());
                            if (saveUri != null) {
                                item.setUri(saveUri);
                                item.setStatus(AttachMentItem.STATUS_SUCCESS);
                            } else {
                                item.setStatus(AttachMentItem.STATUS_FAIL);
                            }
                        }
                        List params = new ArrayList();
                        params.add(list);
                        params.add(bubbleId);
                        DataHandler.handleTask(DataHandler.TASK_ADD_ATTACHMENTS, params);
                    }
                });
                return true;
            }
        }
        return false;
    }

    public static AttachMentItem generateAttachmentFromUri(Context context, Uri uri) {
        if (UriUtils.isIllegal(uri)) {
            log.error("uri is illegal");
            return null;
        }
        if (!UriUtils.isValid(context, uri)) {
            log.error("uri is invalid");
            return null;
        }
        AttachMentItem item = new AttachMentItem();
        item.setOriginalUri(uri);
        return item;
    }

    public static void restoreAttachmentsLocallyIfNeeded(Context context, List<AttachMentItem> attachments) {
        if (null == context || null == attachments) {
            return;
        }
        for (int i = 0; i < attachments.size(); i++) {
            restoreAttachmentLocallyIfNeeded(context, attachments.get(i));
        }
    }

    public static boolean restoreAttachmentLocallyIfNeeded(Context context, AttachMentItem attachment) {
        if (null == context || null == attachment) {
            return false;
        }
        if (AttachMentItem.STATUS_UNKNOWN == attachment.getStatus()) {
            //do nothing
            log.error("can't restore attachment file locally!!");
            return false;
        }
        if (AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS == attachment.getDownloadStatus()) {
            boolean fileExist = FileUtils.isFileExist(attachment.getUri());
            if (fileExist) {
                //everything is ok
                return false;
            }
            boolean needRestoreLocally = null == attachment.getSyncId();
            if (!needRestoreLocally) {
                //try download from cloud first
                attachment.setDownloadStatus(AttachMentItem.DOWNLOAD_STATUS_NOT_DOWNLOAD);
                return false;
            }
            String originalFileName = FileUtils.getFileNameByUri(context, attachment.getOriginalUri());
            if (TextUtils.isEmpty(originalFileName)) {
                //original attachment file is also missing, can't restore locally
                attachment.setStatus(AttachMentItem.STATUS_UNKNOWN);
                return false;
            }
            return AttachmentUtil.copyFileToSpecifiedInnerDir(context, attachment.getOriginalUri(), attachment.getUri());
        }
        return false;
    }
}
