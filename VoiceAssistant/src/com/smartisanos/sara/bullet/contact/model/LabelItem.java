package com.smartisanos.sara.bullet.contact.model;

import android.os.Parcel;

public class LabelItem extends AbsContactItem {
    private final String text;
    private final int labRes;
    private String groupId;

    public LabelItem(String text) {
        this(text, -1);
    }

    public LabelItem(String text , int labelRes) {
        this.text = text;
        this.labRes = labelRes;
    }

    public LabelItem(Parcel in) {
        text = in.readString();
        labRes = in.readInt();
        groupId = in.readString();
    }

    @Override
    public int getItemType() {
        return ItemType.LABEL;
    }

    @Override
    public String belongsGroup() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public final String getText() {
        return text;
    }

    public final int getLabRes() {
        return labRes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(text);
        out.writeInt(labRes);
        out.writeString(groupId);
    }

    public static final Creator<LabelItem> CREATOR = new Creator<LabelItem>() {
        @Override
        public LabelItem createFromParcel(Parcel in) {
            return new LabelItem(in);
        }

        @Override
        public LabelItem[] newArray(int size) {
            return new LabelItem[size];
        }
    };
}
