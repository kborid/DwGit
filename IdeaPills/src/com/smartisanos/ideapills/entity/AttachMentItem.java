package com.smartisanos.ideapills.entity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.data.ATTACHMENT;
import com.smartisanos.ideapills.data.BubbleDB;
import com.smartisanos.ideapills.common.util.BubbleMimeUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AttachMentItem implements Comparable<AttachMentItem>, BubbleDB.ContentValueItem {
    LOG log = LOG.getInstance(AttachMentItem.class);
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_FILE  = 2;

    public static final int STATUS_UNKNOWN = -1;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 0;

    public static final int DOWNLOAD_STATUS_NOT_DOWNLOAD = 0;
    public static final int DOWNLOAD_STATUS_DOWNLOADING  = 1;
    public static final int DOWNLOAD_STATUS_DOWNLOAD_SUCCESS = 2;
    public static final int DOWNLOAD_STATUS_DOWNLOAD_FAIL = 3;

    public static final int UPLOAD_STATUS_NOT_UPLOAD = 0;
    public static final int UPLOAD_STATUS_UPLOADING  = 1;
    public static final int UPLOAD_STATUS_UPLOAD_SUCCESS = 2;
    public static final int UPLOAD_STATUS_UPLOAD_FAIL = 3;

    private int mId;
    private int mType;
    private Uri mUri;
    private Uri mOriUri;
    private String mFilename;
    private long mTimestamp;
    private int mStatus;
    private String mSyncId;
    private int mBubbleId;
    private String mContentType;
    private long mCreateAt;

    private String mBubbleSyncId;
    private long mSize;
    private int mVersion;
    private int mDownloadStatus = DOWNLOAD_STATUS_DOWNLOAD_SUCCESS;
    private int mUploadStatus = UPLOAD_STATUS_NOT_UPLOAD;
    private Uri mConflictUri;

    private String mSyncEncryptKey;
    private long mUserId = -1;

    public AttachMentItem() {
        mCreateAt = System.currentTimeMillis();
    }

    public int getId() {
        return mId;
    }

    public void clearToNotSync() {
        mUserId = -1;
        mSyncEncryptKey = null;
        mSyncId = null;
        mBubbleSyncId = null;
        mVersion = 0;
    }

    public static AttachMentItem fromGlobalBubbleAttach(GlobalBubbleAttach globalBubbleAttach) {
        AttachMentItem attachMentItem = new AttachMentItem();
        attachMentItem.setId(globalBubbleAttach.getId());
        attachMentItem.setType(globalBubbleAttach.getType());
        attachMentItem.setOriginalUri(globalBubbleAttach.getUri());
        String fileName = globalBubbleAttach.getFilename();
        if (fileName != null && fileName.length() > 120) {
            fileName = fileName.substring(fileName.length() - 120);
        }
        attachMentItem.setFilename(fileName);
        attachMentItem.setTimestamp(System.currentTimeMillis());
        attachMentItem.setStatus(globalBubbleAttach.getStatus());
        attachMentItem.setBubbleId(globalBubbleAttach.getBubbleId());
        attachMentItem.setContentType(globalBubbleAttach.getContentType());
        if (attachMentItem.getTimestamp() > 0) {
            attachMentItem.setCreateAt(attachMentItem.getTimestamp());
        }
        return attachMentItem;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues result = new ContentValues();
        if (getId() > 0) {
            result.put(ATTACHMENT.ID, (Integer) getId());
        }
        result.put(ATTACHMENT.MINETYPE, getContentType());
        Uri uri = getUri();
        result.put(ATTACHMENT.URI, uri == null ? "" : uri.toString());
        result.put(ATTACHMENT.TIME_STAMP, (Long) getTimestamp());
        result.put(ATTACHMENT.FILENAME, getFilename());
        result.put(ATTACHMENT.STATUS, (Integer) getStatus());
        result.put(ATTACHMENT.SYNC_ID, getSyncId());
        result.put(ATTACHMENT.BUBBLE_ID, (Integer) getBubbleId());
        result.put(ATTACHMENT.ORIGINALURI, mOriUri == null ? "" : mOriUri.toString());
        result.put(ATTACHMENT.CREATE_AT, mCreateAt);
        result.put(ATTACHMENT.SIZE, mSize);
        result.put(ATTACHMENT.DOWNLOAD_STATUS, mDownloadStatus);
        result.put(ATTACHMENT.UPLOAD_STATUS, mUploadStatus);
        result.put(ATTACHMENT.VERSION, mVersion);
        result.put(ATTACHMENT.SYNC_ENCRYPT_KEY, mSyncEncryptKey);
        result.put(ATTACHMENT.USER_ID, mUserId);
        result.put(ATTACHMENT.BUBBLE_SYNC_ID, mBubbleSyncId);
        return result;
    }

    public void setOriginalUri(Uri uri) {
        mOriUri = uri;
    }

    public Uri getOriginalUri() {
        return mOriUri;
    }

    @Override
    public void setId(int Id) {
        this.mId = Id;
    }

    public int getType() {
        return mType;
    }

    public void setType(int Type) {
        this.mType = Type;
    }

    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri Uri) {
        this.mUri = Uri;
    }

    private String getUniqueFileName() {
        return String.valueOf(mCreateAt) + mFilename;
    }

    public String getFilename() {
        return mFilename;
    }

    public void setFilename(String Filename) {
        this.mFilename = Filename;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long Timestamp) {
        this.mTimestamp = Timestamp;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int Status) {
        this.mStatus = Status;
    }

    public String getSyncId() {
        return mSyncId;
    }

    public void setSyncId(String SyncId) {
        this.mSyncId = SyncId;
    }

    @Override
    public String getTableName() {
        return ATTACHMENT.NAME;
    }

    public int getBubbleId() {
        return mBubbleId;
    }

    public void setBubbleId(int BubbleId) {
        this.mBubbleId = BubbleId;
    }

    public void setCreateAt(long createAt) {
        this.mCreateAt = createAt;
    }

    public long getCreateAt() {
        return mCreateAt;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        this.mSize = size;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.mDownloadStatus = downloadStatus;
    }

    public int getDownloadStatus() {
        return mDownloadStatus;
    }

    public void setUploadStatus(int uploadStatus) {
        this.mUploadStatus = uploadStatus;
    }

    public int getUploadStatus() {
        return mUploadStatus;
    }

    public void setSyncEncryptKey(String syncEncryptKey) {
        this.mSyncEncryptKey = syncEncryptKey;
    }

    public String getSyncEncryptKey() {
        return mSyncEncryptKey;
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        this.mUserId = userId;
    }

    public void setBubbleSyncId(String bubbleSyncId) {
        this.mBubbleSyncId = bubbleSyncId;
    }

    public String getBubbleSyncId() {
        return mBubbleSyncId;
    }

    public String getContentType() {
        return mContentType;
    }
    public void setContentType(String contentType) {
        mContentType = contentType;
        if (mContentType != null && mContentType.startsWith("image")) {
            setType(TYPE_IMAGE);
        } else {
            setType(TYPE_FILE);
        }
    }

    public String guessContentType() {
        if (mFilename != null && mFilename.endsWith("eml")) {
            return "message/rfc822";
        }
        if (mContentType != null) {
            return mContentType;
        }
        return null;
    }

    public Uri getFinalUri(Context context) {
        return (Uri.fromFile(AttachmentUtil.getSaveTargetFile(context, getFilename())));
    }

    public void changeToConflict(int bubbleId, long timestamp) {
        setId(0);
        setBubbleId(bubbleId);
        setSyncId(null);
        setCreateAt(timestamp);
        setUserId(-1);
        setVersion(0);
        setSyncEncryptKey(null);
        if (mUri != null && mDownloadStatus == DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
            mConflictUri = mUri;
            setUri(getFinalUri(IdeaPillsApp.getInstance()));
        } else {
            mConflictUri = null;
            setUri(null);
        }
        setUploadStatus(AttachMentItem.UPLOAD_STATUS_NOT_UPLOAD);
    }

    public void copyCauseConflict(Context context) {
        if (mConflictUri == null) {
            return;
        }
        InputStream in = null;
        OutputStream out = null;
        File toFile = AttachmentUtil.getSaveTargetFile(context, getFilename());
        try {
            in = context.getContentResolver().openInputStream(mConflictUri);
            out = new FileOutputStream(toFile);
            FileUtils.copyFile(in, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.quietClose(in);
            Utils.quietClose(out);
        }
    }

    @Override
    public boolean bindCursor(Cursor cursor) {
        //ID, TYPE, URI, FILENAME, TIME_STAMP, STATUS, MARK_DELETE, SYNC_ID, BUBBLE_ID, ORIGINALURI
        try {
            int idIdx              = cursor.getColumnIndex(ATTACHMENT.ID);
            int mimeTypeIdx        = cursor.getColumnIndex(ATTACHMENT.MINETYPE);
            int uriIdx             = cursor.getColumnIndex(ATTACHMENT.URI);
            int fnameIdx           = cursor.getColumnIndex(ATTACHMENT.FILENAME);
            int timeIdx            = cursor.getColumnIndex(ATTACHMENT.TIME_STAMP);
            int statusIdx          = cursor.getColumnIndex(ATTACHMENT.STATUS);
            int syncIdIdx          = cursor.getColumnIndex(ATTACHMENT.SYNC_ID);
            int bubbleIdx          = cursor.getColumnIndex(ATTACHMENT.BUBBLE_ID);
            int oriUriIdx          = cursor.getColumnIndex(ATTACHMENT.ORIGINALURI);
            int createAtIdx        = cursor.getColumnIndex(ATTACHMENT.CREATE_AT);
            int sizeIdx            = cursor.getColumnIndex(ATTACHMENT.SIZE);
            int downStatusIdx      = cursor.getColumnIndex(ATTACHMENT.DOWNLOAD_STATUS);
            int upStatusIdx        = cursor.getColumnIndex(ATTACHMENT.UPLOAD_STATUS);
            int versionIdx         = cursor.getColumnIndex(ATTACHMENT.VERSION);
            int syncEncryptKeyIdx  = cursor.getColumnIndex(ATTACHMENT.SYNC_ENCRYPT_KEY);
            int userIdx = cursor.getColumnIndex(ATTACHMENT.USER_ID);
            int bubbleSyncIdx = cursor.getColumnIndex(ATTACHMENT.BUBBLE_SYNC_ID);

            setId(cursor.getInt(idIdx));
            setContentType(cursor.getString(mimeTypeIdx));
            String uri = cursor.getString(uriIdx);
            if (!TextUtils.isEmpty(uri)) {
                setUri(Uri.parse(uri));
            }
            setFilename(cursor.getString(fnameIdx));
            setTimestamp(cursor.getLong(timeIdx));
            setStatus(cursor.getInt(statusIdx));
            setSyncId(cursor.getString(syncIdIdx));
            setBubbleId(cursor.getInt(bubbleIdx));
            String oriuri = cursor.getString(oriUriIdx);
            if (!TextUtils.isEmpty(oriuri)) {
                setOriginalUri(Uri.parse(oriuri));
            }
            setCreateAt(cursor.getLong(createAtIdx));
            setSize(cursor.getLong(sizeIdx));
            setDownloadStatus(cursor.getInt(downStatusIdx));
            setUploadStatus(cursor.getInt(upStatusIdx));
            setVersion(cursor.getInt(versionIdx));
            setSyncEncryptKey(cursor.getString(syncEncryptKeyIdx));
            setUserId(cursor.getLong(userIdx));
            setBubbleSyncId(cursor.getString(bubbleSyncIdx));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean deleteLocalFile() {
        if (mUri != null) {
            File file = new File(mUri.getPath());
            try {
                return file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public Bitmap getThumbIcon(Context context) {
        if (mUri != null) {
            File file = new File(mUri.getPath());
            return BubbleMimeUtils.getFileThumbIcon(context, getContentType(), file);
        } else {
            String fileName = getFilename();
            String suffix = "";
            if (!TextUtils.isEmpty(fileName)) {
                String[] sp = fileName.split("\\.");
                if (sp.length > 1) {
                    suffix = sp[sp.length - 1];
                }
            }
            return BubbleMimeUtils.getFileThumbIcon(context, getContentType(), suffix);
        }
    }

    public GlobalBubbleAttach toGlobalBubbleAttach() {
        GlobalBubbleAttach attach = new GlobalBubbleAttach();
        attach.setId(mId);
        attach.setType(mType);
        attach.setUri(mUri);
        attach.setFilename(mFilename);
        attach.setStatus(mStatus);
        attach.setBubbleId(mBubbleId);
        attach.setContentType(mContentType);
        return attach;
    }

    @Override
    public int compareTo(AttachMentItem o) {
        if (getCreateAt() > o.getCreateAt()) {
            return 1;
        } else if (getCreateAt() < o.getCreateAt()) {
            return -1;
        } else {
            return 0;
        }
    }
}
