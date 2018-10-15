package com.smartisanos.ideapills.sync.share;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.sync.entity.SyncShareUser;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.BubbleItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SyncShareRepository extends SyncBundleRepository {
    static final String CLOUD_INTERFACE_SHARE_START_INVITE = "share_start_invite";
    static final String CLOUD_INTERFACE_SHARE_GET_LIST_INVITES = "share_get_list_invites";
    static final String CLOUD_INTERFACE_SHARE_HANDLE_INVITE = "share_handle_invite";
    static final String CLOUD_INTERFACE_SHARE_SET_MARK = "share_set_mark";
    static final String CLOUD_INTERFACE_SHARE_REMOVE_INVITER = "share_remove_inviter";
    static final String CLOUD_INTERFACE_SHARE_SET_INVITER_SWITCH = "share_set_inviter_switch";
    static final String CLOUD_INTERFACE_SHARE_GET_INVITER_SWITCH = "share_get_inviter_switch";
    static final String CLOUD_INTERFACE_SHARE_ADD_PARTICIPANTS = "share_add_participants";
    static final String CLOUD_INTERFACE_SHARE_REMOVE_PARTICIPANTS = "share_remove_participants";
    static final String CLOUD_INTERFACE_SHARE_GET_PARTICIPANTS_LIST = "share_get_participants_list";

    // 这种参数错误一般是客户端异常，或者非法请求, 正常情况下(或者正常客户端)不会出现
    public static final int ERROR_INVALID_ARGUMENT = 1;
    // 应用版本号错误
    public static final int ERROR_INVALID_APP_VERSION = 2;
    // 应用版本号过低，此时客户端可提示用户升级新版本
    public static final int ERROR_APP_VERSION_TOO_LOW = 3;
    // 协议时间错误
    public static final int ERROR_INVALID_PROTOCOL_TIME = 4;
    // 设备 registration_token 错误
    public static final int ERROR_INVALID_REGISTRATION_TOKEN = 5;
    // 同步在进行中, 意味着别的端正在上传数据
    // 碰上这种异常，客户端应该采取 `exponential backoff` 的方式重试, 并且最小的等待时间是 1 秒。
    public static final int ERROR_SYNC_IN_PROGRESS = 21;
    // 同步版本号冲突，当前客户端下载完成，上传之前，别的端修改了数据，这种情况，客户端需要先从服务器下载更新
    public static final int ERROR_SYNC_VERSION_CONFLICT = 22;
    // 存储空间已满
    public static final int ERROR_STORAGE_FULL = 23;
    // 清空云端数据进行中
    public static final int ERROR_WIPE_IN_PROGRESS = 24;
    // 资源不存在
    public static final int ERROR_NOT_FOUND = 41;
    // 请求过于频繁
    public static final int ERROR_TOO_MANY_REQUESTS = 42;
    // 无权限
    public static final int ERROR_PERMISSION_DENIED = 43;
    // 未认证
    public static final int ERROR_UNAUTHENTICATED = 44;
    // 服务器错误
    // 这种情况是一些没有处理的到异常发生，或者一些 catch 到了，但是也不能做什么事情得异常(比如：数据库连接失败，网络闪断等等)
    public static final int ERROR_SERVER_ERROR = 60;
    // 共享邀请，输入的别名不存在
    public static final int ERROR_ALIAS_NOT_FOUND = 71;
    // 共享邀请，不能邀请自己
    public static final int ERROR_UNABLE_INVITE_YOURSELF = 72;
    // 共享邀请被禁用
    public static final int ERROR_SHARE_INVITE_DISABLED = 73;
    // 共享邀请人数超出限制
    public static final int ERROR_SHARE_INVITE_QUOTA_EXCEEDED = 74;
    // 共享邀请关系不存在
    public static final int ERROR_SHARE_INVITE_NOT_EXISTS = 75;
    // 共享邀请关系不适用
    public static final int ERROR_SHARE_INVITE_STATUS_NOT_APPLICABLE = 76;
    // 共享关系取消进行中
    public static final int ERROR_SHARE_CANCEL_INVITE_IN_PROGRESS = 77;
    // 共享关系不存在
    public static final int ERROR_SHARE_NOT_EXISTS = 91;
    // 共享状态不适用
    // 比如：共享已经取消，但是却执行了修改共享胶囊操作
    public static final int ERROR_SHARE_STATUS_NOT_APPLICABLE = 92;
    // 添加共享人时，共享人不在可共享列表中
    // 可共享列表包括 owner 的邀请人，以及邀请了 owner 的人并且邀请状态是“接受”
    public static final int ERROR_SHARE_PARTICIPANT_NOT_ALLOWED = 93;

    public static final int ERROR_NO_LOGIN = -80;
    public static final int ERROR_NO_SHARE_INVITATION = -81;
    public static final int ERROR_NET_WORK_ERROR = 400;

    private static final int CANCEL_STATUS_INIT = 0;
    private static final int CANCEL_STATUS_DONE = 1;

    public static void startInvite(String inviteName, RequestListener<SyncShareInvitation> requestListener) {
        startInvite(inviteName, -1, "", "", requestListener);
    }

    public static void startInvite(String inviteName, String remark, String alias, RequestListener<SyncShareInvitation> requestListener) {
        startInvite(inviteName, -1, remark, alias, requestListener);
    }

    public static void startInvite(long inviteeId, RequestListener<SyncShareInvitation> requestListener) {
        startInvite("", inviteeId, "", "", requestListener);
    }

    private static void startInvite(String inviteName, long inviteeId, String remark, String alias, RequestListener<SyncShareInvitation> requestListener) {
        Bundle params = new Bundle();
        if (inviteeId > 0) {
            params.putLong("invitee_id", inviteeId);
        } else {
            params.putString("inviteName", inviteName);
        }
        params.putString("inviter_remark", remark);
        params.putString("user_alias", alias);
        requestAsync(CLOUD_INTERFACE_SHARE_START_INVITE, params, new WrapRequestListener<SyncShareInvitation>(requestListener) {
            @Override
            public SyncShareInvitation handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ContentValues cv = bundle.getParcelable("data");
                    SyncShareInvitation syncShareInvitation = new SyncShareInvitation();
                    syncShareInvitation.fromContentValues(cv);
                    SyncShareManager.INSTANCE.addInvitation(syncShareInvitation);
                    return syncShareInvitation;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void sendRegisterInvite(String inviteName, final RequestListener<Integer> requestListener) {
        new AsyncTask<String, Object, Integer>() {
            @Override
            protected void onPreExecute() {
                requestListener.onRequestStart();
            }

            @Override
            protected Integer doInBackground(String... params) {
                final Uri FEED_BACK_URI = Uri.parse("content://com.smartisanos.cloudsync.accountcenter");
                try {
                    ContentResolver contentResolver = IdeaPillsApp.getInstance().getContentResolver();
                    Bundle extra = new Bundle();
                    extra.putString("inviteName", params[0]);
                    Bundle bundle = contentResolver.call(FEED_BACK_URI, "request_account_invite", null, extra);
                    // 903  请求太频繁
                    // 1203 邀请对象已经注册
                    // 1002 请求参数错误
                    // 1106 uid 不存在
                    // 0 发送成功
                    if (bundle != null) {
                        return bundle.getInt("result", -1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return -1;
            }

            @Override
            protected void onPostExecute(Integer o) {
                if (requestListener != null) {
                    if (o == 0 || o == 1203) {
                        requestListener.onResponse(o);
                    } else {
                        requestListener.onError(new DataException(-1));
                    }
                }
            }
        }.execute(inviteName);
    }

    public static void getInviteList(RequestListener<List<SyncShareInvitation>> requestListener) {
        Bundle params = new Bundle();
        requestAsync(CLOUD_INTERFACE_SHARE_GET_LIST_INVITES, params, new WrapRequestListener<List<SyncShareInvitation>>(requestListener) {
            @Override
            public List<SyncShareInvitation> handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ArrayList<ContentValues> cvList = bundle.getParcelableArrayList("data");
                    ArrayList<SyncShareInvitation> invitationArrayList = new ArrayList<SyncShareInvitation>();
                    for (ContentValues cv : cvList) {
                        SyncShareInvitation syncShareInvitation = new SyncShareInvitation();
                        syncShareInvitation.fromContentValues(cv);
                        invitationArrayList.add(syncShareInvitation);
                    }
                    SyncShareManager.INSTANCE.setInvitationList(invitationArrayList);
                    return invitationArrayList;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }

            @Override
            public void onError(DataException e) {
                SyncShareManager.INSTANCE.markInvitationListRequested();
                super.onError(e);
            }
        });
    }

    public static void handleInvite(final long inviterId, final int inviteStatus, RequestListener<Integer> requestListener) {
        Bundle params = new Bundle();
        params.putLong("inviter_id", inviterId);
        params.putInt("invite_status", inviteStatus);
        requestAsync(CLOUD_INTERFACE_SHARE_HANDLE_INVITE, params, new WrapRequestListener<Integer>(requestListener) {
            @Override
            public Integer handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    SyncShareManager.INSTANCE.modifyInvitation(inviterId, inviteStatus);
                    return result;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void setRemark(long inviterId, long inviteeId, String inviterRemark,
                                 String inviteeRemark, RequestListener<SyncShareInvitation> requestListener) {
        Bundle params = new Bundle();
        params.putLong("inviter_id", inviterId);
        params.putLong("invitee_id", inviteeId);
        if (inviterRemark != null) {
            params.putString("inviter_remark", inviterRemark);
        }
        if (inviteeRemark != null) {
            params.putString("invitee_remark", inviteeRemark);
        }
        requestAsync(CLOUD_INTERFACE_SHARE_SET_MARK, params, new WrapRequestListener<SyncShareInvitation>(requestListener) {
            @Override
            public SyncShareInvitation handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ContentValues cv = bundle.getParcelable("data");
                    SyncShareInvitation syncShareInvitation = new SyncShareInvitation();
                    syncShareInvitation.fromContentValues(cv);
                    return syncShareInvitation;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void removeInviterWithRetry(final long inviterId, final long inviteeId, RequestListener<Integer> requestListener) {
        final Bundle params = new Bundle();
        params.putLong("inviter_id", inviterId);
        params.putLong("invitee_id", inviteeId);

        final WrapRequestRetryListener<Integer> retryListener = new WrapRequestRetryListener<Integer>(requestListener, 3, 4000) {
            @Override
            public Integer handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0 && result != ERROR_SHARE_INVITE_NOT_EXISTS) {
                        throw new DataException(result);
                    }
                    int removeStatus = bundle.getInt("remove_status");
                    if (removeStatus == CANCEL_STATUS_DONE || reachRetryLimit() || result == ERROR_SHARE_INVITE_NOT_EXISTS) {
                        long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
                        GlobalBubbleManager.getInstance().handleCancelShare(userId);
                        SyncShareManager.INSTANCE.removeInvitation(inviterId, inviteeId);
                    } else if (!reachRetryLimit()) {
                        setResponseNeedRetry(true);
                    }
                    return result;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }

            @Override
            public void handleRetry() {
                requestAsync(CLOUD_INTERFACE_SHARE_REMOVE_INVITER, params, this);
            }
        };
        requestAsync(CLOUD_INTERFACE_SHARE_REMOVE_INVITER, params, retryListener);
    }

    public static void removeInviter(final long inviterId, final long inviteeId, RequestListener<Integer> requestListener) {
        Bundle params = new Bundle();
        params.putLong("inviter_id", inviterId);
        params.putLong("invitee_id", inviteeId);
        requestAsync(CLOUD_INTERFACE_SHARE_REMOVE_INVITER, params, new WrapRequestListener<Integer>(requestListener) {
            @Override
            public Integer handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    int removeStatus = bundle.getInt("remove_status");
                    if (removeStatus != 0 && removeStatus != 1){
                        throw new DataException(-1);
                    }
                    long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
                    GlobalBubbleManager.getInstance().handleCancelShare(userId);
                    SyncShareManager.INSTANCE.removeInvitation(inviterId, inviteeId);
                    return result;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void initInviterSwitch() {
        Bundle params = new Bundle();
        requestAsync(CLOUD_INTERFACE_SHARE_GET_INVITER_SWITCH, params, new WrapRequestListener<Boolean>(null) {
            @Override
            public Boolean handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ContentValues cv = bundle.getParcelable("data");
                    // 0 初始邀请开关, 1 允许接收共享邀请, 2 不允许接收共享邀请
                    int inviteSwitch = Utils.getInt(cv, "invite_switch", -1);
                    if (inviteSwitch == 0) {
                        setInviterSwitch(true, null);
                    }
                    return inviteSwitch == 1;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void setInviterSwitch(final boolean isInviterSwitchOn, RequestListener<Integer> requestListener) {
        Bundle params = new Bundle();
        // 0 初始邀请开关, 1 允许接收共享邀请, 2 不允许接收共享邀请
        params.putInt("invite_switch", isInviterSwitchOn ? 1 : 2);
        requestAsync(CLOUD_INTERFACE_SHARE_SET_INVITER_SWITCH, params, new WrapRequestListener<Integer>(requestListener) {
            @Override
            public Integer handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    SyncShareManager.INSTANCE.setInviteSwitch(isInviterSwitchOn);
                    return result;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void getInviterSwitch(RequestListener<Boolean> requestListener) {
        Bundle params = new Bundle();
        requestAsync(CLOUD_INTERFACE_SHARE_GET_INVITER_SWITCH, params, new WrapRequestListener<Boolean>(requestListener) {
            @Override
            public Boolean handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ContentValues cv = bundle.getParcelable("data");
                    boolean inviteSwitch = Utils.getInt(cv, "invite_switch", -1) == 1;
                    SyncShareManager.INSTANCE.setInviteSwitch(inviteSwitch);
                    return inviteSwitch;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void addParticipants(long userId, Collection<BubbleItem> pendingAddParticipantItems,
                                       RequestListener<List<Long>> requestListener) {
        Bundle params = new Bundle();
        ArrayList<ContentValues> addSharePills = new ArrayList<>();
        for (BubbleItem pendingAddItem : pendingAddParticipantItems) {
            ContentValues cv = new ContentValues();
            cv.put("owner_id", userId);
            cv.put("pill_id", pendingAddItem.getSyncId());
            cv.put("participant_ids", pendingAddItem.getSharePendingParticipantsString());
            addSharePills.add(cv);
        }
        params.putParcelableArrayList("add_sharelist", addSharePills);
        requestAsync(CLOUD_INTERFACE_SHARE_ADD_PARTICIPANTS, params, new WrapRequestListener<List<Long>>(requestListener) {
            @Override
            public List<Long> handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ArrayList<ContentValues> successShareList = bundle.getParcelableArrayList("success_sharelist");
                    if (successShareList == null) {
                        throw new DataException(result);
                    }
                    ArrayList<Long> successIds = new ArrayList<>();
                    for (ContentValues cv : successShareList) {
                        long pillId = Utils.getLong(cv, "pill_id", -1);
                        if (pillId >= 0) {
                            successIds.add(pillId);
                        }
                    }
                    return successIds;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void removeParticipants(long userId, Collection<BubbleItem> pendingAddParticipantItems,
                                          RequestListener<List<Long>> requestListener) {
        Bundle params = new Bundle();
        ArrayList<ContentValues> removeSharePills = new ArrayList<>();
        for (BubbleItem pendingAddItem : pendingAddParticipantItems) {
            ContentValues cv = new ContentValues();
            cv.put("owner_id", userId);
            cv.put("pill_id", pendingAddItem.getSyncId());
            cv.put("participant_ids", pendingAddItem.getSharePendingParticipantsString());
            removeSharePills.add(cv);
        }
        params.putParcelableArrayList("remove_sharelist", removeSharePills);
        requestAsync(CLOUD_INTERFACE_SHARE_REMOVE_PARTICIPANTS, params, new WrapRequestListener<List<Long>>(requestListener) {
            @Override
            public List<Long> handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ArrayList<ContentValues> successShareList = bundle.getParcelableArrayList("success_sharelist");
                    if (successShareList == null) {
                        throw new DataException(result);
                    }
                    ArrayList<Long> successIds = new ArrayList<>();
                    for (ContentValues cv : successShareList) {
                        long pillId = Utils.getLong(cv, "pill_id", -1);
                        if (pillId >= 0) {
                            successIds.add(pillId);
                        }
                    }
                    return successIds;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void removeParticipants(long userId, String syncId, String participantsString,
                                          RequestListener<List<Long>> requestListener) {
        Bundle params = new Bundle();
        ArrayList<ContentValues> removeSharePills = new ArrayList<>();
        ContentValues cv = new ContentValues();
        cv.put("owner_id", userId);
        cv.put("pill_id", syncId);
        cv.put("participant_ids", participantsString);
        removeSharePills.add(cv);
        params.putParcelableArrayList("remove_sharelist", removeSharePills);
        requestAsync(CLOUD_INTERFACE_SHARE_REMOVE_PARTICIPANTS, params, new WrapRequestListener<List<Long>>(requestListener) {
            @Override
            public List<Long> handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ArrayList<ContentValues> successShareList = bundle.getParcelableArrayList("success_sharelist");
                    if (successShareList == null) {
                        throw new DataException(result);
                    }
                    ArrayList<Long> successIds = new ArrayList<>();
                    for (ContentValues cv : successShareList) {
                        long pillId = Utils.getLong(cv, "pill_id", -1);
                        if (pillId >= 0) {
                            successIds.add(pillId);
                        }
                    }
                    return successIds;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }

    public static void getParticipants(long userId, long syncId,
                                          RequestListener<List<SyncShareUser>> requestListener) {
        Bundle params = new Bundle();
        params.putLong("owner_id", userId);
        params.putLong("pill_id", syncId);
        requestAsync(CLOUD_INTERFACE_SHARE_GET_PARTICIPANTS_LIST, params, new WrapRequestListener<List<SyncShareUser>>(requestListener) {
            @Override
            public List<SyncShareUser> handleResponse(Bundle bundle) throws DataException {
                try {
                    int result = bundle.getInt("result", -1);
                    if (result != 0) {
                        throw new DataException(result);
                    }
                    ArrayList<ContentValues> cvList = bundle.getParcelableArrayList("data");
                    ArrayList<SyncShareUser> userList = new ArrayList<SyncShareUser>();
                    for (ContentValues cv : cvList) {
                        SyncShareUser syncShareUser = new SyncShareUser();
                        syncShareUser.fromContentValues(cv);
                        userList.add(syncShareUser);
                    }
                    return userList;
                } catch (Exception e) {
                    throw new DataException(e);
                }
            }
        });
    }
}
