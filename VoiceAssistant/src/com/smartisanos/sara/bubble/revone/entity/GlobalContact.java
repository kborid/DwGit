package com.smartisanos.sara.bubble.revone.entity;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.Comparator;

public class GlobalContact implements Parcelable {

    public static final int MESSAGE_TYPE_P2P = 1;
    public static final int MESSAGE_TYPE_TEAM = 2;
    private String mContactName;
    private String mAvatarUri;
    private String mContactId;
    private int mMessageType;
    private String mPinyin;
    private String mFirstLetter;

    public String getAvatarUri() {
        return mAvatarUri;
    }

    public void setAvatarUri(String portrait) {
        this.mAvatarUri = portrait;
    }

    public String getContactId() {
        return mContactId;
    }

    public void setContactId(String contactId) {
        this.mContactId = contactId;
    }

    public GlobalContact() {

    }

    public String getContactName() {
        return mContactName;
    }

    public void setContactName(String contactName) {
        this.mContactName = contactName;
    }

    public int getMessageType() {
        return mMessageType;
    }

    public void setMessageType(int messageType) {
        this.mMessageType = messageType;
    }

    public GlobalContact(Parcel in) {
        mContactName = in.readString();
        mAvatarUri = in.readString();
        mContactId = in.readString();
    }

    public String getPinyin() {
        return mPinyin;
    }

    public void setPinyin(String pinyin) {
        mPinyin = pinyin;
    }

    public String getFirstLetter() {
        return mFirstLetter;
    }

    public void setFirstLetter(String letter) {
        mFirstLetter = letter;
    }

    @Override
    protected GlobalContact clone() throws CloneNotSupportedException {
        GlobalContact contact = new GlobalContact();
        contact.setContactName(mContactName);
        contact.setAvatarUri(mAvatarUri);
        contact.setContactId(mContactId);
        contact.setPinyin(mPinyin);
        contact.setFirstLetter(mFirstLetter);
        return contact;
    }

    @Override
    public String toString() {
        return "GlobalContact{" +
                "mContactName='" + mContactName + '\'' +
                ", mAvatarUri='" + mAvatarUri + '\'' +
                ", mContactId='" + mContactId + '\'' +
                ", mMessageType='" + mMessageType + '\'' +
                ", mPinyin='" + mPinyin + '\'' +
                ", mPinyin='" + mFirstLetter + '\'' +
                '}';
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mContactName);
        out.writeString(mAvatarUri);
        out.writeString(mContactId);
        out.writeInt(mMessageType);
        out.writeString(mPinyin);
        out.writeString(mFirstLetter);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GlobalContact> CREATOR = new Creator<GlobalContact>() {
        @Override
        public GlobalContact createFromParcel(Parcel in) {
            return new GlobalContact(in);
        }

        @Override
        public GlobalContact[] newArray(int size) {
            return new GlobalContact[size];
        }
    };


    public JSONObject toJsonObject() {
        try {
            JSONObject object = new JSONObject();
            object.put("contactName", mContactName);
            object.put("contactId", mContactId);
            object.put("avatarUri", mAvatarUri);
            object.put("messageType", mMessageType);
            object.put("pinyin", mPinyin);
            object.put("firstletter", mFirstLetter);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GlobalContact toGlobalContact(JSONObject object) {
        if (object == null) {
            return null;
        }
        try {
            GlobalContact contact = new GlobalContact();
            contact.setContactName(object.optString("contactName"));
            contact.setContactId(object.optString("contactId"));
            contact.setAvatarUri(object.optString("avatarUri"));
            contact.setMessageType(object.optInt("messageType"));
            contact.setPinyin(object.optString("pinyin"));
            contact.setFirstLetter(object.optString("firstletter"));
            return contact;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final Comparator<GlobalContact> CONTACT_COMPARATOR = new Comparator<GlobalContact>() {
        @Override
        public int compare(GlobalContact contact, GlobalContact contact2) {
            if (contact == null || contact2 == null) {
                return -1;
            }
            if (contact.mPinyin == null || contact2.mPinyin == null) {
                return -1;
            }
            return contact.mPinyin.compareTo(contact2.mPinyin);
        }
    };
}
