package com.smartisanos.sara.bullet.contact.model;

import android.os.Parcel;
import android.text.TextUtils;

import com.smartisanos.sara.bullet.util.PinYinUtils;
import com.smartisanos.sara.bullet.util.TextComparatorUtils;

import org.json.JSONObject;

import java.util.ArrayList;

public class ContactItem extends AbsContactItem {
    public static interface MessageType {
        int P2P = 1;
        int TEAM = 2;
        int UNKNOWN = 0x3;
        int PHONECONTACT = 0x4;
    }


    private String mContactName;
    private String mAvatarUri;
    private String mContactId;
    private int mMessageType;
    private ArrayList<String> mPinyinWords;
    private String mPinyin;
    private String mInitialSet;
    private String mFirstLetter;
    private boolean isChecked;
    private String mGroup;

    private boolean mIsPinyinLoaded;
    private boolean mIsGroupLoaded;

    private int itemType = ItemType.CONTACT;

    public ContactItem() {
    }

    public ContactItem(Parcel in) {
        mContactName = in.readString();
        mAvatarUri = in.readString();
        mContactId = in.readString();
        mMessageType = in.readInt();
        mPinyin = in.readString();
        mFirstLetter = in.readString();
        isChecked = in.readByte() != 0;
    }

    public int getOrderByMessageType() {
        if (getMessageType() == MessageType.TEAM) {
            return 1;
        } else {
            return 0;
        }
    }

    public String getContactName() {
        return mContactName;
    }

    public void setContactName(String contactName) {
        this.mContactName = contactName;
    }

    public String getAvatarUri() {
        return mAvatarUri;
    }

    public void setAvatarUri(String portrait) {
        this.mAvatarUri = portrait;
    }

    public String getContactId() {
        return mContactId;
    }

    public void setItemType (int itemType) {
        this.itemType = itemType;
    }
    @Override
    public int getItemType() {
        return itemType;
    }

    @Override
    public String belongsGroup() {
        if (!mIsGroupLoaded) {
            if (!TextUtils.isEmpty(mContactName)) {
                String group = TextComparatorUtils.getLeadingUp(mContactName);
                mGroup = !TextUtils.isEmpty(group) ? group : "#";
            } else {
                mGroup = "#";
            }
            mIsGroupLoaded = true;
        }
        return mGroup;
    }

    public void setContactId(String contactId) {
        this.mContactId = contactId;
    }

    public int getMessageType() {
        return mMessageType;
    }

    public void setMessageType(int messageType) {
        this.mMessageType = messageType;
    }

    public String getPinyin() {
        if (mIsPinyinLoaded || !TextUtils.isEmpty(mPinyin)) {
            return mPinyin;
        }
        if (!TextUtils.isEmpty(mContactName)) {
            PinYinUtils.PinYinStructInfo pinYinStructInfo = PinYinUtils.getPinYinAndInitialSet(mContactName);
            if (pinYinStructInfo == null) {
                mPinyin = null;
                mInitialSet = null;
                mPinyinWords = null;
            } else {
                mPinyin = pinYinStructInfo.getPinyin();
                mInitialSet = pinYinStructInfo.getInitialSet();
                mPinyinWords = pinYinStructInfo.getPinYinList();
            }
        }
        mIsPinyinLoaded = true;
        return mPinyin;
    }

    public String getInitialSet() {
        return mInitialSet;
    }

    public boolean containsLetters(String keyLetters) {
        if (keyLetters == null || keyLetters.isEmpty()) {
            return false;
        }
        keyLetters = keyLetters.toLowerCase();
        getPinyin();
        if (keyLetters.length() == 1) {
            return mInitialSet != null && mInitialSet.contains(keyLetters);
        } else {
            return (mInitialSet != null && mInitialSet.contains(keyLetters)) ||
                    matcherPinYinEveryWord(keyLetters);
        }
    }

    private boolean matcherPinYinEveryWord(String keyLetters) {
        if (TextUtils.isEmpty(mPinyin) || TextUtils.isEmpty(keyLetters) || null == mPinyinWords || mPinyinWords.size() <= 0) {
            return false;
        }
        StringBuilder ignoreStr = new StringBuilder();
        for (String pinyin : mPinyinWords) {
            if (!TextUtils.isEmpty(pinyin)) {
                String needMatchStr = mPinyin.substring(ignoreStr.length(), mPinyin.length());
                if (needMatchStr.startsWith(keyLetters)) {
                    return true;
                }
                ignoreStr.append(pinyin);
            }
        }
        return false;
    }

    public boolean containsLetters(String keyLetters, String keyInitialSet) {
        if (keyLetters == null || keyLetters.isEmpty()) {
            return false;
        }
        if (keyInitialSet == null || keyInitialSet.isEmpty()) {
            return containsLetters(keyLetters);
        }
        getPinyin();
        return (mInitialSet != null && mInitialSet.contains(keyInitialSet)) ||
                matcherPinYinEveryWord(keyLetters);
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

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    @Override
    protected ContactItem clone() throws CloneNotSupportedException {
        ContactItem contact = new ContactItem();
        contact.setContactName(mContactName);
        contact.setAvatarUri(mAvatarUri);
        contact.setContactId(mContactId);
        contact.setPinyin(mPinyin);
        contact.setFirstLetter(mFirstLetter);
        contact.setChecked(isChecked);
        return contact;
    }

    @Override
    public String toString() {
        return "ContactItem{" +
                "mContactName='" + mContactName + '\'' +
                ", mAvatarUri='" + mAvatarUri + '\'' +
                ", mContactId='" + mContactId + '\'' +
                ", mMessageType='" + mMessageType + '\'' +
                ", mPinyin='" + mPinyin + '\'' +
                ", mFirstLetter='" + mFirstLetter + '\'' +
                ", isChecked='" + isChecked + '\'' +
                ", " + super.toString() +
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
        out.writeByte((byte)(isChecked ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ContactItem> CREATOR = new Creator<ContactItem>() {
        @Override
        public ContactItem createFromParcel(Parcel in) {
            return new ContactItem(in);
        }

        @Override
        public ContactItem[] newArray(int size) {
            return new ContactItem[size];
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
            object.put("isChecked", isChecked);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ContactItem toBulletContactItem(JSONObject object) {
        if (object == null) {
            return null;
        }
        try {
            ContactItem contact = new ContactItem();
            contact.setContactName(object.optString("contactName"));
            contact.setContactId(object.optString("contactId"));
            contact.setAvatarUri(object.optString("avatarUri"));
            contact.setMessageType(object.optInt("messageType"));
            contact.setPinyin(object.optString("pinyin"));
            contact.setFirstLetter(object.optString("firstletter"));
            contact.setChecked(object.optBoolean("isChecked"));
            return contact;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
