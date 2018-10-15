package com.smartisanos.ideapills.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ResultReceiver;
import android.text.TextUtils;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

public class SyncManager {
    private static final LOG log = LOG.getInstance(SyncManager.class);

    private static final String CLOUD_INTERFACE = "content://com.smartisanos.cloudsync";

    private static final String CLOUD_INTERFACE_REQUEST_SYNC = "method_ideapill_request_sync";
    private static final String CLOUD_INTERFACE_RESTORE_FINISH = "method_ideapill_restore_finish";
    private static final String CLOUD_INTERFACE_RESTORE_ATTACHMENT_FINISH = "method_ideapill_restore_attachment_finish";
    private static final String CLOUD_INTERFACE_DOWNLOAD_ATTACHMENT = "method_ideapill_download_attachment";
    private static final String CLOUD_INTERFACE_SYNC_IDEAPILL_ENABLE = "method_ideapill_sync_enable";
    private static final String CLOUD_INTERFACE_ACCOUNT_INFO = "method_ideapill_account_info";
    private static final String CLOUD_INTERFACE_SHARE_RESTORE_FINISH = "method_share_ideapill_restore_finish";
    private static final String CLOUD_INTERFACE_SHARE_RESTORE_ATTACHMENT_FINISH = "method_share_ideapill_restore_attachment_finish";
    private static final String CLOUD_INTERFACE_DOWNLOAD_SHARE_ATTACHMENT = "method_ideapill_download_share_attachment";

    private static String PARAM_IS_SHARE_FROM_OTHERS = "is_share_from_others";

    private static final HandlerThread sWorkerThread = new HandlerThread("SyncManager");
    private static ThreadPoolExecutor sDownloadThreadPoolExecutor = new ThreadPoolExecutor(
            1, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(128));

    static {
        sWorkerThread.start();
    }

    private static final int REQUEST_SYNC_DELAY = 1500;

    private static final int TASK_NUM_BASE = 30000;
    private static final int TASK_REQUEST_SYNC = TASK_NUM_BASE + 1;
    private static final int TASK_DOWNLOAD_ATTACHMENT = TASK_NUM_BASE + 2;
    private static final int TASK_NOTICE_RESTORE_FINISH = TASK_NUM_BASE + 3;
    private static final int TASK_NOTICE_RESTORE_ATTACHMENT_FINISH = TASK_NUM_BASE + 4;
    private static final int TASK_NOTICE_SHARE_RESTORE_FINISH = TASK_NUM_BASE + 5;
    private static final int TASK_NOTICE_SHARE_RESTORE_ATTACHMENT_FINISH = TASK_NUM_BASE + 6;

    private static void handleTask(int task) {
        handleTask(task, null, true, 0);
    }

    private static void handleTask(int task, long delay) {
        handleTask(task, null, true, delay);
    }

    private static void handleTask(int task, Object params, long delay) {
        handleTask(task, params, true, delay);
    }

    private static void handleTask(int task, Object params, boolean removeMessagesBefore, long delay) {
        if (removeMessagesBefore) {
            mWorker.removeMessages(task);
        }
        Message msg = mWorker.obtainMessage();
        msg.what = task;
        msg.obj = params;
        if (delay > 0) {
            mWorker.sendMessageDelayed(msg,  delay);
        } else {
            mWorker.sendMessage(msg);
        }
    }

    public static boolean post(Runnable r) {
        return mWorker.post(r);
    }

    public static boolean postDelayed(Runnable r, long delayMillis) {
        return mWorker.postDelayed(r, delayMillis);
    }

    public static void removeCallbacks(Runnable r) {
        mWorker.removeCallbacks(r);
    }

    private static final Handler mWorker = new Handler(sWorkerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int action = msg.what;
            switch (action) {
                case TASK_REQUEST_SYNC: {
                    requestSyncInternal();
                    break;
                }
                case TASK_DOWNLOAD_ATTACHMENT: {
                    if (msg.obj instanceof Bundle) {
                        downloadAttachmentInternal((Bundle) msg.obj);
                    }
                    break;
                }
                case TASK_NOTICE_RESTORE_FINISH: {
                    if (msg.obj instanceof Runnable) {
                        noticeRestoreFinishInternal((Runnable) msg.obj);
                    } else {
                        noticeRestoreFinishInternal(null);
                    }
                    break;
                }
                case TASK_NOTICE_RESTORE_ATTACHMENT_FINISH: {
                    if (msg.obj instanceof Runnable) {
                        noticeRestoreAttachmentFinishInternal((Runnable) msg.obj);
                    } else {
                        noticeRestoreAttachmentFinishInternal(null);
                    }
                    break;
                }
                case TASK_NOTICE_SHARE_RESTORE_FINISH: {
                    if (msg.obj instanceof Runnable) {
                        noticeShareRestoreFinishInternal((Runnable) msg.obj);
                    } else {
                        noticeShareRestoreFinishInternal(null);
                    }
                    break;
                }
                case TASK_NOTICE_SHARE_RESTORE_ATTACHMENT_FINISH: {
                    if (msg.obj instanceof Runnable) {
                        noticeShareRestoreAttachmentFinishInternal((Runnable) msg.obj);
                    } else {
                        noticeShareRestoreAttachmentFinishInternal(null);
                    }
                    break;
                }
            }
        }
    };

    public static void requestSync() {
        if (!SyncProcessor.isInSyncing()) {
            handleTask(SyncManager.TASK_REQUEST_SYNC, null, true, REQUEST_SYNC_DELAY);
        }
    }

    private static void requestSyncInternal() {
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app != null) {
            //notify data changed
            callCloudSync(app, CLOUD_INTERFACE_REQUEST_SYNC, null, null);
        }
    }

    public static void noticeRestoreFinish(Runnable preRunnable) {
        handleTask(SyncManager.TASK_NOTICE_RESTORE_FINISH, preRunnable, 0);
    }

    private static void noticeRestoreFinishInternal(Runnable preRunnable) {
        log.warn("noticeRestoreFinish");
        if (preRunnable != null) {
            preRunnable.run();
        }
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app != null) {
            callCloudSync(app, CLOUD_INTERFACE_RESTORE_FINISH, null, null);
        }
    }

    public static void noticeRestoreAttachmentFinish(Runnable preRunnable) {
        handleTask(SyncManager.TASK_NOTICE_RESTORE_ATTACHMENT_FINISH, preRunnable, 0);
    }

    private static void noticeRestoreAttachmentFinishInternal(Runnable preRunnable) {
        log.warn("noticeRestoreAttachmentFinish");
        if (preRunnable != null) {
            preRunnable.run();
        }
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app != null) {
            callCloudSync(app, CLOUD_INTERFACE_RESTORE_ATTACHMENT_FINISH, null, null);
        }
    }

    public static void noticeShareRestoreFinish(Runnable preRunnable) {
        handleTask(SyncManager.TASK_NOTICE_SHARE_RESTORE_FINISH, preRunnable, 0);
    }

    private static void noticeShareRestoreFinishInternal(Runnable preRunnable) {
        log.warn("noticeShareRestoreFinish");
        if (preRunnable != null) {
            preRunnable.run();
        }
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app != null) {
            callCloudSync(app, CLOUD_INTERFACE_SHARE_RESTORE_FINISH, null, null);
        }
    }

    public static void noticeShareRestoreAttachmentFinish(Runnable preRunnable) {
        handleTask(SyncManager.TASK_NOTICE_SHARE_RESTORE_ATTACHMENT_FINISH, preRunnable, 0);
    }

    private static void noticeShareRestoreAttachmentFinishInternal(Runnable preRunnable) {
        log.warn("noticeShareRestoreAttachmentFinish");
        if (preRunnable != null) {
            preRunnable.run();
        }
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app != null) {
            callCloudSync(app, CLOUD_INTERFACE_SHARE_RESTORE_ATTACHMENT_FINISH, null, null);
        }
    }

    public static void downloadAttachment(Context context, AttachMentItem attachMentItem, ResultReceiver loadCompleteReceiver) {
        if (TextUtils.isEmpty(attachMentItem.getSyncId())) {
            return;
        }
        if (attachMentItem.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOADING
                || attachMentItem.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
            return;
        }
        BubbleItem relateBubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(attachMentItem.getBubbleId());
        if (relateBubbleItem == null
                || TextUtils.isEmpty(relateBubbleItem.getSyncId())) {
            return;
        }
        long syncId;
        long pillSyncId;
        try {
            syncId = Long.parseLong(attachMentItem.getSyncId());
            pillSyncId = Long.parseLong(relateBubbleItem.getSyncId());
        } catch (Exception e) {
            syncId = -1;
            pillSyncId = -1;
        }
        if (syncId < 0 || pillSyncId < 0 || relateBubbleItem.getUserId() < 0) {
            return;
        }

        String filePath = AttachmentUtil.getSaveTargetFile(context, attachMentItem.getFilename()).getAbsolutePath();
        Bundle params = new Bundle();
        params.putString("file_dir", filePath);
        params.putLong("pill_id", pillSyncId);
        params.putLong("user_id", relateBubbleItem.getUserId());
        params.putLong("file_sync_id", syncId);
        params.putParcelable("load_result_receiver", loadCompleteReceiver);
        if (!TextUtils.isEmpty(attachMentItem.getSyncEncryptKey())) {
            byte[] encryptKeyBytes = SyncDataConverter.hexStrToByteArray(attachMentItem.getSyncEncryptKey());
            if (encryptKeyBytes != null) {
                params.putByteArray("ecrypt_key", encryptKeyBytes);
            }
        }
        params.putBoolean(PARAM_IS_SHARE_FROM_OTHERS, relateBubbleItem.isShareFromOthers());
        attachMentItem.setDownloadStatus(AttachMentItem.DOWNLOAD_STATUS_DOWNLOADING);
        handleTask(SyncManager.TASK_DOWNLOAD_ATTACHMENT, params, false, 0);
    }

    private static void downloadAttachmentInternal(final Bundle params) {
        final IdeaPillsApp app = IdeaPillsApp.getInstance();
        if (app != null) {
            sDownloadThreadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    long syncId = params.getLong("file_sync_id");
                    String filePath = params.getString("file_dir");
                    ResultReceiver resultReceiver = ((ResultReceiver)params.getParcelable("load_result_receiver"));
                    params.remove("load_result_receiver");
                    Bundle downResult;
                    if (params.getBoolean(PARAM_IS_SHARE_FROM_OTHERS)) {
                        downResult = callCloudSync(app, CLOUD_INTERFACE_DOWNLOAD_SHARE_ATTACHMENT, null, params);
                    } else {
                        downResult = callCloudSync(app, CLOUD_INTERFACE_DOWNLOAD_ATTACHMENT, null, params);
                    }
                    if (downResult == null) {
                        downResult = new Bundle();
                        downResult.putInt("result", -1);
                    }
                    downResult.putString("file_sync_id", String.valueOf(syncId));
                    downResult.putString("file_dir", filePath);
                    downResult.putParcelable("load_result_receiver", resultReceiver);
                    SyncProcessor.downloadAttachmentResult(downResult);
                }
            });
        }
    }

    public static boolean syncEnable(Context context) {
        Bundle bundle = callCloudSync(context, CLOUD_INTERFACE_SYNC_IDEAPILL_ENABLE, null, null);
        if (bundle != null) {
            return bundle.getBoolean("ideapill_enable");
        }
        return false;
    }

    public static long getCloudAccountUserId(Context context) {
        Bundle bundle = callCloudSync(context, CLOUD_INTERFACE_ACCOUNT_INFO, null, null);
        if (bundle != null) {
            int result = bundle.getInt("result", -1);
            if (result == 0) {
                return bundle.getLong("accountid", -1);
            }
        }
        return -1;
    }

    public static Bundle callCloudSync(Context context, String method, String arg, Bundle params) {
        Bundle bundle = null;
        try {
            Uri uri = Uri.parse(CLOUD_INTERFACE);
            ContentResolver resolver = context.getContentResolver();
            bundle = resolver.call(uri, method, arg, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bundle;
    }

}