package com.smartisanos.sara.bubble.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SmtPCUtils;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.sara.setting.SendInvitationActivity;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.BubbleSpeechPlayer;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import smartisanos.api.ToastSmt;
import smartisanos.os.RemoteCallback;
import com.smartisanos.sara.R;

public class BubbleManager {

    public static final String BUBBLE_ID = "BUBBLE_ID";
    public static final String BUBBLE_TEXT = "BUBBLE_TEXT";
    public static final String BUBBLE_URI = "BUBBLE_URI";
    public static final String BUBBLE_TYPE = "BUBBLE_TYPE";
    public static final String BUBBLE_COLOR = "BUBBLE_COLOR";
    public static final String BUBBLE_TODO = "BUBBLE_TODO";
    public static final String BUBBLE_TIMESTAMP = "BUBBLE_TIMESTAMP";

    private static final String ACTION_BOOM_TEXT = "smartisanos.intent.action.BOOM_TEXT";

    private static boolean sAddBubble2List = true;

    public static void markAddBubble2List(boolean isAddBubble2List) {
        sAddBubble2List = isAddBubble2List;
    }

    public static boolean isAddBubble2List() {
        return sAddBubble2List;
    }

    public static PointF addBubble2SideBar(final Context context, GlobalBubble mGlobalBubble, List<GlobalBubbleAttach> globalBubbleAttach,
                                           boolean offLine, boolean anim) {
        if (mGlobalBubble == null || !sAddBubble2List) {
            return null;
        }
        sAddBubble2List = false;
        BubbleSpeechPlayer.getInstance(context).stop();
        PointF pointF = null;
        List<GlobalBubble> globalBubbles = new ArrayList<GlobalBubble>();
        if (mGlobalBubble.getType() != GlobalBubble.TYPE_TEXT){
            int duration = BubbleSpeechPlayer.getInstance(context).getDuration(mGlobalBubble);
            mGlobalBubble.setVoiceDuration(duration);
            if (mGlobalBubble.getColor() == GlobalBubble.COLOR_SHARE) {
                mGlobalBubble.setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
            }
        }
        globalBubbles.add(mGlobalBubble);
        Bundle paramBunble = new Bundle();
        paramBunble.putLong(SaraConstant.KEY_DELAY, AnimManager.FLY_BUBBLE_TO_GLOBLE);
        paramBunble.putString(SaraConstant.KEY_PKG, context.getPackageName());
        paramBunble.putBoolean(SaraConstant.KEY_ANIM, anim);
        Bundle bundle = BubbleDataRepository.addGlobalBubbles(context, globalBubbles, globalBubbleAttach, paramBunble);
        if (bundle != null) {
            pointF = bundle.getParcelable(SaraConstant.KEY_LOC);
        }
        Uri uri = mGlobalBubble.getUri();
        if (uri != null) {
            long filesize = FileUtils.getFileSizeByUri(context, uri);
            int time = (int) (filesize * SaraConstant.MSEC_PER_SEC / SaraConstant.MP3_BAUD_RATE);
            com.smartisanos.sanbox.utils.SaraTracker.onEvent("A420029", "time", time);
        }
        BubbleCleaner.INSTANCE.removePendingClearBubble(mGlobalBubble);
        BubbleCleaner.INSTANCE.deleteAttachFile(globalBubbleAttach);
        return pointF;
    }

    public static void addBubbles2SideBar(Context context, ArrayList<GlobalBubble> globalBubbles) {
        if (globalBubbles != null) {
            for (GlobalBubble globalBubble : globalBubbles) {
                if (globalBubble.getColor() == GlobalBubble.COLOR_SHARE) {
                    globalBubble.setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
                }
            }
            BubbleDataRepository.addGlobalBubbles(context, globalBubbles, null, null);
        }
    }

    public static void addOutText2BubbleList(final Context context, final String str) {
        if (!TextUtils.isEmpty(str)) {
            MutiTaskHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GlobalBubble bubble = SaraUtils.toGlobalBubbleText(context, GlobalBubble.COLOR_BLUE);
                    bubble.setText(str);
                    ArrayList<GlobalBubble> globalBubbles = new ArrayList<GlobalBubble>();
                    globalBubbles.add(bubble);

                    addBubbles2SideBar(context, globalBubbles);
                }
            }, 1000);
        }
    }

    public static boolean changeBubbleColor2ShareJumpIfNeed(final Activity activity, GlobalBubble globalBubble, SharedDialogCallback callback) {
        Dialog dialog = null;
        Bundle canShareBundle = BubbleDataRepository.getBubbleCanShare(activity, false);
        if (canShareBundle != null && canShareBundle.getBoolean(SyncUtil.RESULT_KEY_CAN_SHARE)) {
            globalBubble.setColor(GlobalBubble.COLOR_SHARE);
            globalBubble.setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
            if (null != callback) {
                callback.sharedDialogHandler(null);
            }
            return true;
        } else {
            if (canShareBundle != null) {
                final boolean isShowLoginDialog = canShareBundle.getBoolean(SyncUtil.SHOW_LOGIN_DIALOG_FLAG);
                final boolean isShowLoginSyncDialog = canShareBundle.getBoolean(SyncUtil.SHOW_LOGIN_SYNC_DIALOG_FLAG);
                final boolean isShowInvitationDialog = canShareBundle.getBoolean(SyncUtil.SHOW_INVITATION_DIALOG_FLAG);
                if (isShowLoginDialog || isShowLoginSyncDialog) {
                    dialog = SyncUtil.buildLoginDialog(activity, isShowLoginDialog, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = SyncUtil.getCloudAccountLoginIntent(activity.getString(R.string.app_name));
                            SaraUtils.startActivity(activity, intent);
                        }
                    });
                    dialog.show();
                } else if (isShowInvitationDialog) {
                    if (!SaraUtils.isKeyguardLocked()) {
                        dialog = SendInvitationActivity.launchAsDialog(activity);
                        dialog.show();
                    } else {
                        SendInvitationActivity.launchAsActivity(activity);
                    }
                }
            }
            if (null != callback) {
                callback.sharedDialogHandler(dialog);
            }
            return false;
        }
    }

    public static StringBuffer updateBubbleFromIntent(Intent intent, GlobalBubble globalBubble) {
        if (intent == null || intent.getExtras() == null) {
            return null;
        }
        Bundle bundle = intent.getExtras();
        int bubbleId = bundle.getInt(BUBBLE_ID);
        StringBuffer newBubbleText = null;
        if (globalBubble != null && bubbleId == globalBubble.getId()) {
            String bubbleText = bundle.getString(BUBBLE_TEXT);
            Uri bubbleUri = bundle.getParcelable(BUBBLE_URI);
            if (!TextUtils.isEmpty(bubbleText)) {
                newBubbleText = new StringBuffer();
                newBubbleText.append(bubbleText);
                globalBubble.setText(bubbleText);
                globalBubble.setUri(bubbleUri);
            }
            globalBubble.setType(1);
        }
        return newBubbleText;
    }

    public static void shareBubble(Context context, GlobalBubble globalBubble, List<GlobalBubbleAttach> bubbleAttaches) {
        BubbleCleaner.INSTANCE.markPendingClearBubbleDelay(globalBubble);
        if (bubbleAttaches != null && bubbleAttaches.size() > 0) {
            String showToast = context.getString(R.string.bubble_share_tip);
            ToastUtil.showToast(context, showToast);
        }
        if (!TextUtils.isEmpty(globalBubble.getText())) {
            SaraUtils.shareGlobal(context, globalBubble, null);
        }
    }

    public static void shareBubble2Weixin(Context context, GlobalBubble globalBubble) {
        BubbleCleaner.INSTANCE.markPendingClearBubbleDelay(globalBubble);
        SaraUtils.shareGlobal(context, globalBubble,
                new ComponentName(SaraConstant.PACKAGE_NAME_WEIXIN, SaraConstant.CLASS_NAME_WEIXIN));

    }

    public static boolean jump2TextBoom(final Activity activity, final GlobalBubble globalBubble, final View boomView,
                                        final ITextBoomFinishedListener textBoomFinishedListener) {
        if (activity.getPackageManager().queryIntentActivities(
                new Intent(ACTION_BOOM_TEXT), 0).isEmpty()) {
            new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setTitle(R.string.bubble_notice)
                    .setMessage(R.string.install_bigbang_message)
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse("smartisan://smartisan.com/details?id=com.smartisanos.textboom"));
                            activity.startActivity(i);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return false;
        } else {
            if (globalBubble == null) {
                return false;
            }
            Intent intent = new Intent(ACTION_BOOM_TEXT);
            final String oldText = globalBubble.getText();
            intent.putExtra(Intent.EXTRA_TEXT, oldText);
            int[] pos = new int[2];
            boomView.getLocationOnScreen(pos);
            intent.putExtra("boom_startx", pos[0]);
            intent.putExtra("boom_starty", pos[1]);
            intent.putExtra("caller_pkg", activity.getPackageName());
            intent.putExtra("show_all_text", true);
            intent.putExtra("force_callback", true);
            intent.putExtra("editable_resource", true);
            intent.putExtra("enter_edit_mode", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (SmtPCUtils.isValidExtDisplayId(activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                intent.setComponent(new ComponentName("com.smartisanos.textboom", "com.smartisanos.textboom.BoomActivity"));
            }
            RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() {
                @Override
                public void onResult(Bundle result) {
                    final String newText = result.getString(Intent.EXTRA_RETURN_RESULT);
                    handleTextBoomResult(activity, globalBubble, newText, textBoomFinishedListener);
                }
            }, new Handler());
            intent.putExtra("smartisanos.textboom.REMOTE_CALLBACK", callback);
            intent.putExtra("ideapills_content", true);
            SaraUtils.startActivity(activity, intent, true);
            return true;
        }
    }

    public static void jump2SetRemind(Activity activity, GlobalBubble globalBubble, int position) {
        Intent intent = new Intent(SaraConstant.ACTION_TO_REMIND);
        intent.putExtra(SaraConstant.REMIND_TIME_KEY, globalBubble.getRemindTime());
        intent.putExtra(SaraConstant.DUE_DATE_KEY, globalBubble.getDueDate());
        if (SmtPCUtils.isValidExtDisplayId(activity) && position >= 0) {
            intent.setPackage("com.smartisanos.ideapills");
            intent.putExtra("window_position", position);
        }
        SaraUtils.startActivityForResult(activity, intent, SaraConstant.RESULT_PICK_REMIND);
    }

    public static void trackBubbleChange(Context context, GlobalBubble globalBubble, List<GlobalBubbleAttach> bubbleAttaches,
                                         boolean isTextChange) {
        if (globalBubble == null) {
            return;
        }
        LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("ifmod", SaraUtils.getDefaultBubbleColor(context) == globalBubble.getColor() ? 0 : 1);
        if (bubbleAttaches != null && bubbleAttaches.size() > 0) {
            dataMap.put("ifadd", 1);
            dataMap.put("atch_num", bubbleAttaches.size());
        } else {
            dataMap.put("ifadd", 0);
            dataMap.put("atch_num", 0);
        }
        dataMap.put("ifset", (globalBubble.getRemindTime() > 0 || globalBubble.getDueDate() > 0) ? 1 : 0);
        dataMap.put("ifcontent", isTextChange ? 1 : 0);
        dataMap.put("ifcheck", globalBubble.getUsedTime() > 0 ? 1 : 0);
        dataMap.put("source", 9000);
        com.smartisanos.sanbox.utils.SaraTracker.onEvent("A420021", dataMap);
    }

    public interface ITextBoomFinishedListener {
        void onTextBoomFinished(String newText, String oldText, boolean isTextChanged);
    }

    public static void handleTextBoomResult(Activity activity, GlobalBubble globalBubble, String newText, ITextBoomFinishedListener textBoomFinishedListener) {
        String oldText = globalBubble.getText();
        boolean isTextChanged = false;
        if (!TextUtils.isEmpty(newText)) {
            if (!newText.trim().equals(oldText)) {
                isTextChanged = true;
                if (CommonUtils.getStringLength(newText.trim()) <= SaraConstant.BUBBLE_TEXT_MAX) {
                    globalBubble.setText(newText.trim());
                } else {
                    globalBubble.setText(oldText.trim());
                    ToastSmt.getInstance().makeText(activity, activity.getResources().getString(
                            R.string.bubble_add_string_limit), Toast.LENGTH_SHORT,
                            WindowManager.LayoutParams.TYPE_TOAST).show();
                }
                if (globalBubble.getType() == GlobalBubble.TYPE_VOICE_OFFLINE) {
                    globalBubble.setType(GlobalBubble.TYPE_VOICE);
                }
            }
        } else {
            globalBubble.setText("");
            isTextChanged = true;
        }
        if (textBoomFinishedListener != null) {
            textBoomFinishedListener.onTextBoomFinished(newText, oldText, isTextChanged);
        }
    }

    public interface SharedDialogCallback {
        void sharedDialogHandler(Dialog dialog);
        void dismissDialog();
    }
}
