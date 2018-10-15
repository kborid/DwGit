package com.smartisanos.ideapills.sync.entity;

import android.content.ContentValues;

import com.smartisanos.ideapills.util.Utils;

public class SyncPushItem implements IContentValuesChangeAble{
    // 未定义值
    public static final int SHARE_ACTION_NOT_DEFINED = 0;
    // 共享：发起邀请
    public static final int SHARE_ACTION_START_INVITE = 1;
    // 共享：处理邀请
    public static final int SHARE_ACTION_HANDLE_INVITE = 2;
    // 共享：创建共享
    public static final int SHARE_ACTION_START_SHARE = 3;
    // 共享：取消共享
    public static final int SHARE_ACTION_CANCEL_SHARE = 4;
    // 共享：编辑共享胶囊
    public static final int SHARE_ACTION_EDIT_SHARED_PILL = 5;
    // 共享：取消邀请
    public static final int SHARE_ACTION_REMOVE_INVITE = 6;

    // 普通的编辑操作
    public static final int OP_EDIT = 0;
    // 标记完成
    public static final int OP_MARK_FINISHED = 1;
    // 删除
    public static final int OP_DELETE = 2;

    //表示 “动作”, 在共享事件中参考 ShareAction proto 定义
    public int action;
    //事件产生人 id
    public long fid;
    //事件产生人 name
    public String fName;
    //事件接收人 id
    public long tid;
    //invite_status, 处理共享邀请事件中，表示邀请状态
    public int inviteStatus;
    //编辑共享胶囊事件中，表示操作
    public int op;
    //胶囊sync_id
    public long pid;

    @Override
    public void fromContentValues(ContentValues contentValues) {
        fid = Utils.getLong(contentValues, "fid", 0);
        tid = Utils.getLong(contentValues, "tid", 0);
        action = Utils.getInt(contentValues, "a", SHARE_ACTION_NOT_DEFINED);
        inviteStatus = Utils.getInt(contentValues, "is", SyncShareInvitation.INVITE_START);
        op = Utils.getInt(contentValues, "op", OP_EDIT);
        fName = contentValues.getAsString("fn");
        pid = Utils.getLong(contentValues, "pid", 0);
    }

    @Override
    public ContentValues toContentValues() {
        return null;
    }
}
