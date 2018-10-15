package com.smartisanos.ideapills.sync.entity;

import android.content.ContentValues;

import com.smartisanos.ideapills.util.Utils;

public class SyncShareInvitation implements IContentValuesChangeAble {

    // 初始化状态
    public static final int INVITE_START = 0;
    // 已接受状态
    public static final int INVITE_ACCEPT = 1;
    // 拒绝邀请
    public static final int INVITE_DECLINE = 2;
    // 取消共享
    public static final int INVITE_CANCEL = 3;

    public long id;
    public SyncShareUser inviter;
    public SyncShareUser invitee;
    public int inviteStatus;
    public long createdAt;
    public long updatedAt;

    @Override
    public void fromContentValues(ContentValues contentValues) {
        id = Utils.getLong(contentValues, "id", -1);
        inviter = new SyncShareUser();
        contentValues.put("isInviter", true);
        inviter.fromContentValues(contentValues);
        invitee = new SyncShareUser();
        contentValues.put("isInviter", false);
        invitee.fromContentValues(contentValues);
        inviteStatus = Utils.getInt(contentValues, "invite_status", INVITE_START);
        createdAt = Utils.getLong(contentValues, "created_at", 0);
        updatedAt = Utils.getLong(contentValues, "updated_at", 0);
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", id);
        contentValues.put("inviter_id", inviter.id);
        contentValues.put("inviter_nickname", inviter.nickname);
        contentValues.put("inviter_avatar", inviter.avatar);
        contentValues.put("inviter_remark", inviter.remark);
        contentValues.put("inviter_email", inviter.email);
        contentValues.put("inviter_phone", inviter.phone);
        contentValues.put("invitee_id", invitee.id);
        contentValues.put("invitee_nickname", invitee.nickname);
        contentValues.put("invitee_avatar", invitee.avatar);
        contentValues.put("invitee_remark", invitee.remark);
        contentValues.put("invitee_email", invitee.email);
        contentValues.put("invitee_phone", invitee.phone);
        contentValues.put("invite_status", inviteStatus);
        contentValues.put("created_at", createdAt);
        contentValues.put("updated_at", updatedAt);
        return contentValues;
    }
}