package com.smartisanos.ideapills.sync.entity;

import android.content.ContentValues;
import android.text.TextUtils;

import com.smartisanos.ideapills.util.Utils;

public class SyncShareUser implements IContentValuesChangeAble {
    public long id;
    public String nickname;
    public String avatar;
    public String remark;
    public String email;
    public String phone;

    @Override
    public void fromContentValues(ContentValues contentValues) {
        if (contentValues.containsKey("isInviter")) {
            if (Utils.getBoolean(contentValues, "isInviter", false)) {
                id = Utils.getLong(contentValues, "inviter_id", -1);
                nickname = contentValues.getAsString("inviter_nickname");
                avatar = contentValues.getAsString("inviter_avatar");
                remark = contentValues.getAsString("inviter_remark");
                email = contentValues.getAsString("inviter_email");
                phone = contentValues.getAsString("inviter_phone");
            } else {
                id = Utils.getLong(contentValues, "invitee_id", -1);
                nickname = contentValues.getAsString("invitee_nickname");
                avatar = contentValues.getAsString("invitee_avatar");
                remark = contentValues.getAsString("invitee_remark");
                email = contentValues.getAsString("invitee_email");
                phone = contentValues.getAsString("invitee_phone");
            }
        } else {
            id = Utils.getLong(contentValues, "id", -1);
            nickname = contentValues.getAsString("nickname");
            avatar = contentValues.getAsString("avatar");
        }
    }

    public String getShowName() {
        if (!TextUtils.isEmpty(nickname)) {
            return nickname;
        }
        if (!TextUtils.isEmpty(phone)) {
            return phone;
        }
        if (!TextUtils.isEmpty(email)) {
            return email;
        }
        return String.valueOf(id);
    }

    @Override
    public ContentValues toContentValues() {
        return null;
    }
}
