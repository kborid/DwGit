package com.smartisanos.ideapills.interfaces;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.widget.Toast;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.BubbleDisplayManager;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.data.BubbleDB;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.sync.SyncProcessor;
import com.smartisanos.ideapills.util.AttachmentUtils;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class LocalInterface {
    private static final LOG log = LOG.getInstance(LocalInterface.class);

    private static final String METHOD_REQUEST_SHOW_BUBBLE     = "METHOD_REQUEST_SHOW_BUBBLE";
    private static final String METHOD_REQUEST_ADD_BUBBLE      = "METHOD_REQUEST_ADD_BUBBLE";
    private static final String METHOD_HIDE_BUBBLE_LIST        = "METHOD_HIDE_BUBBLE_LIST";
    private static final String METHOD_SHOW_BUBBLE_LIST        = "METHOD_SHOW_BUBBLE_LIST";
    private static final String METHOD_BUBBLES_GONE            = "METHOD_BUBBLES_GONE";
    private static final String METHOD_BUBBLES_VISIBLE         = "METHOD_BUBBLES_VISIBLE";
    private static final String METHOD_HANDLE_DRAG_EVENT       = "METHOD_HANDLE_DRAG_EVENT";
    private static final String METHOD_RECEIVE_DRAG_DROP       = "METHOD_RECEIVE_DRAG_DROP";
    private static final String METHOD_RESTORE_BUBBLES         = "METHOD_RESTORE_BUBBLES";
    private static final String METHOD_RESTORE_DELETE_BUBBLES  = "METHOD_RESTORE_DELETE_BUBBLES";
    private static final String METHOD_DESTROY_BUBBLES         = "METHOD_DESTROY_BUBBLES";
    private static final String METHOD_VISIBLE_BUBBLE_COUNT    = "METHOD_VISIBLE_BUBBLE_COUNT";
    private static final String METHOD_UPDATE_VOICE_BUBBLE_URI = "METHOD_UPDATE_VOICE_BUBBLE_URI";
    private static final String METHOD_LIST_BUBBLES            = "METHOD_LIST_BUBBLES";
    private static final String METHOD_LIST_BUBBLE_ATTACHMENTS = "METHOD_LIST_BUBBLE_ATTACHMENTS";
    private static final String METHOD_DESTROY_ALL_BUBBLES     = "METHOD_DESTROY_ALL_BUBBLES";
    private static final String METHOD_CALL_BUBBLE             = "METHOD_CALL_BUBBLE";
    private static final String METHOD_MERGE_VOICE_WAVE_DATA   = "METHOD_MERGE_VOICE_WAVE_DATA";
    private static final String METHOD_RESTORE_LEGACY_BUBBLES  = "METHOD_RESTORE_LEGACY_BUBBLES";
    private static final String METHOD_CAN_BUBBLE_SHARE        = "METHOD_CAN_BUBBLE_SHARE";
    private static final String METHOD_SEND_SHARE_INVITE       = "METHOD_SEND_SHARE_INVITE";

    // 欢喜云相关接口
    private static final String METHOD_SYNC_BUBBLE_COUNT       = "METHOD_BUBBLE_COUNT";
    private static final String METHOD_SYNC_RESTORE            = "METHOD_SYNC_RESTORE";
    private static final String METHOD_SYNC_PREPARE_DATA       = "METHOD_SYNC_PREPARE_DATA";
    private static final String METHOD_SYNC_RESULT             = "METHOD_SYNC_RESULT";
    private static final String METHOD_SYNC_RESTORE_ATTACHMENT = "METHOD_SYNC_RESTORE_ATTACHMENT";
    private static final String METHOD_SYNC_PREPARE_ATTACHMENT_DATA = "METHOD_SYNC_PREPARE_ATTACHMENT_DATA";
    private static final String METHOD_SYNC_ATTACHMENT_RESULT = "METHOD_SYNC_ATTACHMENT_RESULT";
    private static final String METHOD_SYNC_FINISH = "METHOD_SYNC_FINISH";
    private static final String METHOD_SYNC_DOWNLOAD_ATTACHMENT_RESULT = "METHOD_DOWNLOAD_ATTACHMENT_RESULT";
    private static final String METHOD_SYNC_LOG_OUT = "METHOD_SYNC_LOGOUT";
    private static final String METHOD_SHARE_SYNC_RESTORE = "METHOD_SHARE_SYNC_RESTORE";
    private static final String METHOD_SHARE_SYNC_PREPARE_DATA = "METHOD_SHARE_SYNC_PREPARE_DATA";
    private static final String METHOD_SHARE_SYNC_RESULT = "METHOD_SHARE_SYNC_RESULT";
    private static final String METHOD_SHARE_SYNC_RESTORE_ATTACHMENT = "METHOD_SHARE_SYNC_RESTORE_ATTACHMENT";
    private static final String METHOD_SHARE_SYNC_PREPARE_ATTACHMENT_DATA = "METHOD_SHARE_SYNC_PREPARE_ATTACHMENT_DATA";
    private static final String METHOD_SHARE_SYNC_ATTACHMENT_RESULT = "METHOD_SHARE_SYNC_ATTACHMENT_RESULT";
    private static final String METHOD_SYNC_PUSH = "METHOD_PUSH_IDEAPILL_DATA";
    private static final String METHOD_LIST_BUBBLES_ATTACHMENTS = "METHOD_LIST_BUBBLES_ATTACHMENTS";

    // 大屏相关接口
    private static final String METHOD_REQUEST_SHOW_BUBBLE_EXT_DISPLAY = "METHOD_REQUEST_SHOW_BUBBLE_EXT_DISPLAY";
    private static final String METHOD_REQUEST_MARGIN_RIGHT_EXT_DISPLAY = "METHOD_REQUEST_MARGIN_RIGHT_EXT_DISPLAY";
    private static final String METHOD_REQUEST_PPT_MODE_ATTACHMENT_DATA = "METHOD_REQUEST_PPT_MODE_ATTACHMENT_DATA";
    private static final String METHOD_IS_BUBBLE_SHOWN_EXT_DISPLAY = "METHOD_IS_BUBBLE_SHOWN_EXT_DISPLAY";

    public static final int HANDLE_ATTCHMENT_OK = 0;
    public static final int HANDLE_ATTCHMENT_INVALID = 1;
    public static final int HANDLE_ATTCHMENT_OUTOF_LENGTH = 2;
    public static final int HANDLE_ATTCHMENT_OUTOF_SIZE = 3;
    public static final int HANDLE_ATTCHMENT_OUTOF_SIZE_AND_LENGTH = 4;

    public static Bundle call(final Context context, String method, Bundle extras) {
        log.error("call ["+method+"]");
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app == null) {
            log.error("IdeaPillsApp instance is null, abandon method ["+method+"]");
            if (isSyncCallMethod(method)) {
                return new Bundle();
            } else {
                return null;
            }
        }
        if (METHOD_SYNC_LOG_OUT.equals(method)) {
            // sync log out must be handled
            SyncProcessor.syncLogout(Constants.DATA_INIT_READY);
            return new Bundle();
        }

        if (!Utils.isIdeaPillsEnable(app)) {
            if (isSyncCallMethod(method)) {
                return new Bundle();
            } else if (METHOD_REQUEST_SHOW_BUBBLE_EXT_DISPLAY.equals(method)) {
                Context extContext = BubbleDisplayManager.INSTANCE.getExtContext(context);
                if (extContext != null) {
                    GlobalBubbleUtils.showSystemToast(extContext,
                            R.string.ideapills_not_opened, Toast.LENGTH_SHORT);
                }
                return null;
            } else {
                return null;
            }
        }
        if (METHOD_LIST_BUBBLES.equals(method)) {
            if (extras == null) {
                return null;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("bubbles", VoiceAssistantInterface.listBubble(extras));
            return bundle;
        } else if (METHOD_LIST_BUBBLE_ATTACHMENTS.equals(method)) {
            if (extras == null) {
                return null;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("bubble_attachments", VoiceAssistantInterface.listAttachments(extras));
            return bundle;
        } else if (METHOD_UPDATE_VOICE_BUBBLE_URI.equals(method)) {
            ArrayList<GlobalBubble> list = extras.getParcelableArrayList("bubbles");
            boolean offlineToOnline = extras.getBoolean("BUBBLE_OFF_TO_ONLINE");
            VoiceAssistantInterface.updateVoiceBubbleUri(list, offlineToOnline);
            return null;
        } else if (METHOD_MERGE_VOICE_WAVE_DATA.equals(method)) {
            ArrayList<GlobalBubble> list = extras.getParcelableArrayList("bubbles");
            VoiceAssistantInterface.mergeVoiceBubbleWave(list);
            return null;
        } else if (METHOD_VISIBLE_BUBBLE_COUNT.equals(method)) {
            int count = VoiceAssistantInterface.visibleBubbleCount();
            Bundle bundle = new Bundle();
            bundle.putInt("count", count);
            return bundle;
        } else if (METHOD_DESTROY_BUBBLES.equals(method)) {
            int[] ids = extras.getIntArray("bubble_ids");
            String destroyType = extras.getString("destroy_type");
            VoiceAssistantInterface.destroyBubble(ids, destroyType);
            return null;
        } else if (METHOD_LIST_BUBBLES_ATTACHMENTS.equals(method)) {
            if (extras == null) {
                return null;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("bubbles_attachments", VoiceAssistantInterface.listAttachmentsCount(extras));
            return bundle;
        }
        if (!Constants.DATA_INIT_READY) {
            log.error("Constants.DATA_INIT_READY false ! abandon call ["+method+"]");
            if (isSyncCallMethod(method)) {
                return new Bundle();
            } else {
                return null;
            }
        }
        if (METHOD_CAN_BUBBLE_SHARE.equals(method)) {
            switchDisplayByExtraFlag(extras);
            return SyncProcessor.canShareFromCrossProcess(extras);
        } else if (METHOD_SEND_SHARE_INVITE.equals(method)) {
            SyncProcessor.sendShareInvite(extras);
            return new Bundle();
        } else if (METHOD_SYNC_PUSH.equals(method)) {
            return SyncProcessor.handleSyncPush(extras);
        } else if (METHOD_SYNC_RESTORE.equals(method)) {
            return SyncProcessor.syncRestore(extras);
        } else if (METHOD_SYNC_PREPARE_DATA.equals(method)) {
            return SyncProcessor.getPreparedData();
        } else if (METHOD_SYNC_RESULT.equals(method)) {
            return SyncProcessor.handleSyncResult(extras);
        } else if (METHOD_SYNC_RESTORE_ATTACHMENT.equals(method)) {
            return SyncProcessor.syncRestoreAttachment(extras);
        } else if (METHOD_SYNC_PREPARE_ATTACHMENT_DATA.equals(method)) {
            return SyncProcessor.getPreparedAttachmentData();
        } else if (METHOD_SYNC_ATTACHMENT_RESULT.equals(method)) {
            SyncProcessor.handleSyncAttachmentResult(extras);
            return new Bundle();
        } else if (METHOD_SYNC_FINISH.equals(method)) {
            SyncProcessor.syncFinish();
            return new Bundle();
        } else if (METHOD_SYNC_DOWNLOAD_ATTACHMENT_RESULT.equals(method)) {
            SyncProcessor.downloadAttachmentResult(extras);
            return new Bundle();
        } else if (METHOD_SHARE_SYNC_RESTORE.equals(method)) {
            return SyncProcessor.shareSyncRestore(extras);
        } else if (METHOD_SHARE_SYNC_PREPARE_DATA.equals(method)) {
            return SyncProcessor.getSharePreparedData();
        } else if (METHOD_SHARE_SYNC_RESULT.equals(method)) {
            return SyncProcessor.handleShareSyncResult(extras);
        } else if (METHOD_SHARE_SYNC_RESTORE_ATTACHMENT.equals(method)) {
            return SyncProcessor.shareSyncRestoreAttachment(extras);
        } else if (METHOD_SHARE_SYNC_PREPARE_ATTACHMENT_DATA.equals(method)) {
            return SyncProcessor.getSharePreparedAttachmentData();
        } else if (METHOD_SHARE_SYNC_ATTACHMENT_RESULT.equals(method)) {
            SyncProcessor.handleShareSyncAttachmentResult(extras);
            return new Bundle();
        } else if (METHOD_SYNC_BUBBLE_COUNT.equals(method)) {
            return SyncProcessor.getRealBubbleCountData();
        }
        if (!Constants.WINDOW_READY) {
            log.error("Constants.WINDOW_READY false ! abandon call ["+method+"]");
            return null;
        }
        if (METHOD_BUBBLES_GONE.equals(method)) {
            //systemui call this
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    BubbleController.getInstance().setBubbleWindowEnable(false);
                }
            });
        } else if(METHOD_BUBBLES_VISIBLE.equals(method)){
            //systemui call this
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    BubbleController.getInstance().updateVisibility();
                }
            });
        } else if (METHOD_HIDE_BUBBLE_LIST.equals(method)) {
            //textboom sara call this
            UIHandler.post(new Runnable() {
                public void run() {
                    BubbleController.getInstance().playHideAnimation(false);
                }
            });
            Bundle result = new Bundle();
            result.putBoolean(METHOD_HIDE_BUBBLE_LIST, true);
            return result;
        } else if (METHOD_SHOW_BUBBLE_LIST.equals(method)) {
            UIHandler.post(new Runnable() {
                public void run() {
                    if (!Utils.isKeyguardLocked() && !BubbleController.getInstance().isAlreadyShow()) {
                        BubbleController.getInstance().showOrHide();
                    }
                }
            });
        } else if (METHOD_REQUEST_ADD_BUBBLE.equals(method)) {
            switchDisplayByExtraFlag(extras);
            //for public interface IdeaPillsUtils.addGlobalBubbles
            List<GlobalBubble> list = extras.getParcelableArrayList("list");
            List<GlobalBubbleAttach> attachList = extras.getParcelableArrayList("listAttach");
            Bundle bundle = extras.getBundle("extra");
            return BubbleController.getInstance().addGlobalBubbles(list, attachList, bundle);
        } else if (METHOD_REQUEST_SHOW_BUBBLE.equals(method)) {
            BubbleDisplayManager.INSTANCE.switchDisplayIfNeeded(false);
            //for public interface IdeaPillsUtils.requestShowBubble
            float speed = extras != null ? extras.getFloat("currentXVelocity", 0.0f) : 0.0f;
            BubbleController.getInstance().requestShowBubble(context, speed);
        } else if (METHOD_HANDLE_DRAG_EVENT.equals(method)) {
            boolean isSwitchInstance = switchDisplayByExtraFlag(extras);
            //sidebar call this
            return handleDragEvent(isSwitchInstance ? BubbleController.getInstance().getContext() : context, extras);
        } else if (METHOD_RECEIVE_DRAG_DROP.equals(method)) {
            //sidebar call this
            BubbleController.getInstance().setBubbleHandleBySidebar(true);
        } else if (METHOD_RESTORE_BUBBLES.equals(method)
                || METHOD_RESTORE_LEGACY_BUBBLES.equals(method)) {
            int[] ids = extras.getIntArray("bubble_ids");
            VoiceAssistantInterface.restoreBubble(context, ids);
        } else if (METHOD_DESTROY_ALL_BUBBLES.equals(method)) {
            BubbleDB.deleteAll();
            GlobalBubbleManager.getInstance().removeAll();
        } else if (METHOD_CALL_BUBBLE.equals(method)) {
            UIHandler.post(new Runnable() {
                public void run() {
                    showOrHideBubble(false);
                }
            });
        } else if (METHOD_RESTORE_DELETE_BUBBLES.equals(method)) {
            int[] ids = extras.getIntArray("bubble_ids");
            VoiceAssistantInterface.restoreDeleteBubble(context, ids);
        } else if (METHOD_REQUEST_SHOW_BUBBLE_EXT_DISPLAY.equals(method)) {
            UIHandler.post(new Runnable() {
                public void run() {
                    showOrHideBubble(true);
                }
            });
        } else if (METHOD_REQUEST_MARGIN_RIGHT_EXT_DISPLAY.equals(method)) {
            int rightMargin = extras.getInt("width");
            String curPkg = extras.getString("cur_pkg", null);
            if (BubbleController.getExtInstance() != null) {
                BubbleController.getExtInstance().setIdeaPillsRightTrans(rightMargin, curPkg);
            }
        } else if (METHOD_REQUEST_PPT_MODE_ATTACHMENT_DATA.equals(method)) {
            if (BubbleController.getInstance().getCurPptAddBubbleId() >= 0) {
                ClipData clipData = (ClipData) extras.getParcelable("clip_data");
                AttachmentUtils.handleAttachmentPickResult(clipData, null, BubbleController.getInstance().getCurPptAddBubbleId()
                        , BubbleController.getInstance().getContext());
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BubbleController.getInstance().setPptBubbleId(-1, -1);
                    }
                });
            }
        } else if (METHOD_IS_BUBBLE_SHOWN_EXT_DISPLAY.equals(method)) {
            return bubbleShownInExtDisplay();
        }
        return null;
    }

    private static boolean switchDisplayByExtraFlag(Bundle extras) {
        if (extras != null && extras.get("callFromExtDisplay") != null) {
            return BubbleDisplayManager.INSTANCE.switchDisplayIfNeeded(extras.getBoolean("callFromExtDisplay", false));
        }
        return false;
    }

    private static void showOrHideBubble(boolean fromExtDisplay) {
        if (!Utils.isKeyguardLocked() || fromExtDisplay) {
            BubbleDisplayManager.INSTANCE.switchDisplayIfNeeded(fromExtDisplay);
            if (BubbleController.getInstance().isAlreadyShow()) {
                BubbleController.getInstance().showOrHide();
            } else {
                if (fromExtDisplay || !IdeaPillsApp.isShieldShowIdeapillsByTopAppInfo(IdeaPillsApp.getInstance())) {
                    BubbleController.getInstance().showOrHide();
                }
            }
        }
    }

    private static boolean isSyncCallMethod(String method) {
        return METHOD_SYNC_BUBBLE_COUNT.equals(method)
                || METHOD_SYNC_RESTORE.equals(method)
                || METHOD_SYNC_RESTORE_ATTACHMENT.equals(method)
                || METHOD_SYNC_RESULT.equals(method)
                || METHOD_SYNC_ATTACHMENT_RESULT.equals(method)
                || METHOD_SYNC_PREPARE_DATA.equals(method)
                || METHOD_SYNC_PREPARE_ATTACHMENT_DATA.equals(method)
                || METHOD_SYNC_FINISH.equals(method)
                || METHOD_SYNC_DOWNLOAD_ATTACHMENT_RESULT.equals(method)
                || METHOD_SYNC_LOG_OUT.equals(method)
                || METHOD_SHARE_SYNC_RESTORE.equals(method)
                || METHOD_SHARE_SYNC_PREPARE_DATA.equals(method)
                || METHOD_SHARE_SYNC_RESULT.equals(method)
                || METHOD_SHARE_SYNC_RESTORE_ATTACHMENT.equals(method)
                || METHOD_SHARE_SYNC_PREPARE_ATTACHMENT_DATA.equals(method)
                || METHOD_SHARE_SYNC_ATTACHMENT_RESULT.equals(method);
    }

    public static int handleAttachments(final BubbleItem bubbleItem, final ClipDescription clipDescription,
                                            ClipData clipData, final Context context, final Runnable callBack) {
        int result = HANDLE_ATTCHMENT_INVALID;
        if (clipDescription != null && !ClipDescription.MIMETYPE_TEXT_PLAIN.equals(clipDescription.getMimeType(0))) {
            final List<AttachMentItem> attachMentItemList = new ArrayList<AttachMentItem>();
            if (clipData != null) {
                int count = clipData.getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    AttachMentItem item = AttachmentUtils.generateAttachmentFromUri(context, uri);
                    if (item == null) {
                        result = HANDLE_ATTCHMENT_INVALID;
                        continue;
                    }
                    attachMentItemList.add(item);
                }

                boolean isFileToLarge = false;
                long limitSize = AttachmentUtils.ATTACHMENT_SIZE_LIMIT;
                for (AttachMentItem item : attachMentItemList) {
                    item.setSize(AttachmentUtil.queryFileSize(context, item.getOriginalUri()));
                    limitSize -= item.getSize();
                    if (limitSize < 0) {
                        isFileToLarge = true;
                        break;
                    }
                }

                boolean isFileToMuch = attachMentItemList.size() > AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT;

                if (isFileToMuch  && isFileToLarge) {
                    String showToast = context.getString(R.string.bubble_attachment_size_count_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                    GlobalBubbleUtils.showSystemToast(context, showToast, Toast.LENGTH_SHORT);
                    result = HANDLE_ATTCHMENT_OUTOF_SIZE_AND_LENGTH;
                    return result;
                } else if (isFileToMuch) {
                    String showToast = context.getString(R.string.bubble_attachment_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                    GlobalBubbleUtils.showSystemToast(context, showToast, Toast.LENGTH_SHORT);
                    result = HANDLE_ATTCHMENT_OUTOF_LENGTH;
                    return result;
                } else if (isFileToLarge) {
                    GlobalBubbleUtils.showSystemToast(context, R.string.bubble_attachment_size_limit, Toast.LENGTH_SHORT);
                    result = HANDLE_ATTCHMENT_OUTOF_SIZE;
                    return result;
                }

                if (attachMentItemList.size() > 0) {
                    result = HANDLE_ATTCHMENT_OK;
                    Collections.sort(attachMentItemList);
                    bubbleItem.setAttachments(attachMentItemList);
                    bubbleItem.setWeight(GlobalBubbleManager.getInstance().getMaxWeight() + 1);
                    TaskHandler.post(new Runnable() {
                        public void run() {
                            for (AttachMentItem item : attachMentItemList) {
                                item.setFilename(AttachmentUtil.queryFileName(context, item.getOriginalUri()));
                                item.setContentType(AttachmentUtil.queryFileType(context, item.getOriginalUri(), item.getFilename()));
                                if (TextUtils.isEmpty(item.getContentType()) && clipDescription.getMimeTypeCount() > 0) {
                                    item.setContentType(clipDescription.getMimeType(0));
                                }
                                item.setBubbleSyncId(bubbleItem.getSyncId());
                            }
                            bubbleItem.setText("");
                            bubbleItem.setType(BubbleItem.TYPE_TEXT);
                            bubbleItem.setTimeStamp(System.currentTimeMillis());
                            List params = new ArrayList();
                            List<BubbleItem> bubbleItemList = new ArrayList<BubbleItem>();
                            bubbleItemList.add(bubbleItem);
                            params.add(bubbleItemList);
                            if (callBack != null) {
                                params.add(callBack);
                            }
                            DataHandler.handleTask(DataHandler.TASK_ADD_BUBBLE, params);
                        }
                    });
                }
            }
        }

        return result;
    }

    private static Bundle handleDragEvent(final Context context, Bundle extras) {
        ClipData clipData = (ClipData) extras.getParcelable("clip_data");
        ClipDescription clipDescription = (ClipDescription) extras.getParcelable("clip_description");
        boolean flag = false;
        try {
            boolean handleBySelf = false;
            if (clipDescription != null) {
                List<GlobalBubble> list = GlobalBubble.listFromClipDescription(clipDescription);
                if (list != null && list.size() > 0) {
                    HashSet<Integer> ids = new HashSet<Integer>();
                    for (GlobalBubble gb : list) {
                        ids.add(gb.getId());
                    }
                    handleBySelf = true;
                    BubbleController.getInstance().handleBubbleBySelf(ids);
                }
            }
            if (!handleBySelf) {
                final List<GlobalBubble> items = Utils.convertToBubbleItems(clipData);
                if (items != null) {
                    UIHandler.post(new Runnable() {
                        public void run() {
                            int result = GlobalBubbleManager.getInstance().addGlobalBubblesByDragEvent(items);
                            if (result == 1) {
                                GlobalBubbleUtils.showSystemToast(context, R.string.drag_save_succeed, Toast.LENGTH_SHORT);
                            }
                        }
                    });
                    flag = true;
                } else {
                    final BubbleItem bubbleItem = new BubbleItem();
                    bubbleItem.setColor(Constants.getNewBubbleColor());
                    Runnable callback = new Runnable() {
                        public void run() {
                            final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
                            bubbleItems.add(bubbleItem);
                            UIHandler.post(new Runnable() {
                                public void run() {
                                    GlobalBubbleManager.getInstance().notifyBubbleAdded(bubbleItems);
                                }
                            });
                        }
                    };
                    if (GlobalBubble.COLOR_SHARE == Constants.getDefaultBubbleColor()) {
                        List<BubbleItem> bubbleItems = new ArrayList<>();
                        bubbleItems.add(bubbleItem);
                        GlobalBubbleManager.getInstance().handleShareItems(bubbleItems);
                    }
                    int result = handleAttachments(bubbleItem, clipDescription, clipData, context, callback);
                    if (result == HANDLE_ATTCHMENT_OK) {
                        GlobalBubbleUtils.showSystemToast(context, R.string.drag_save_succeed, Toast.LENGTH_SHORT);
                        flag = true;
                    } else if (result == HANDLE_ATTCHMENT_INVALID) {
                        GlobalBubbleUtils.showSystemToast(context, R.string.drag_empty_tip, Toast.LENGTH_SHORT);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bundle result = new Bundle();
        result.putBoolean("result", flag);
        return result;
    }

    private static Bundle bubbleShownInExtDisplay() {
        Bundle bundle = new Bundle();
        boolean showing = false;
        if (BubbleController.getInstance().isExtDisplay() && BubbleController.getInstance().isBubbleListVisible()) {
            showing = true;
        }
        bundle.putBoolean("showing", showing);
        return bundle;
    }
}