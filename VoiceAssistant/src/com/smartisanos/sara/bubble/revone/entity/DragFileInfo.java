package com.smartisanos.sara.bubble.revone.entity;

import android.graphics.Bitmap;

public class DragFileInfo {
    private String mMimeType;
    private String mFirstFileName;
    private String mText;
    private long mSize;
    private Bitmap mFirstFileIcon;
    private int mFirstFileIconRes;
    private int mFileCount;

    public void setMimeType(String type) {
        mMimeType = type;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setFirstFileName(String name) {
        mFirstFileName = name;
    }

    public String getFirstFileName() {
        return mFirstFileName;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public long getSize() {
        return mSize;
    }

    public void setFirstFileIcon(Bitmap bmp) {
        mFirstFileIcon = bmp;
    }

    public Bitmap getFirstFileIcon() {
        return mFirstFileIcon;
    }

    public void setFirstFileIconRes(int resId) {
        mFirstFileIconRes = resId;
    }

    public int getFirstFileIconRes() {
        return mFirstFileIconRes;
    }

    public void setFileCount(int count) {
        mFileCount = count;
    }

    public int getFileCount() {
        return mFileCount;
    }
}
