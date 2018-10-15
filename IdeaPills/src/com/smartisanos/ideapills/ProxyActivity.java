package com.smartisanos.ideapills;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SmtPCUtils;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.common.util.TaskUtils;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.util.AttachmentUtils;
import com.smartisanos.ideapills.util.ChooseResultHelper;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import java.util.ArrayList;
import java.util.List;

import smartisanos.api.IntentSmt;
import smartisanos.app.share.ShareItem;
import smartisanos.cloudsync.util.SmilingAccountsUtils;
import smartisanos.util.BulletSmsUtils;

public class ProxyActivity extends Activity {
    public static final String EXTRA_TASK = "EXTRA_TASK";
    public static final int TASK_ATTACHCHOOSE = 1;
    public static final int TASK_IMAGE_PREVIEW = 2;
    public static final int TASK_FILE_PREVIEW = 3;
    public static final int TASK_SHARE_TO_APPS = 4;
    public static final int TASK_IMAGE_PREVIEW_NO_EDIT = 5;
    public static final int TASK_FILE_PREVIEW_OLD = 6;
    public static final int TASK_ACCOUNT_LOGIN = 7;
    public static final int TASK_OPEN_IDEAPILLS = 8;
    public static final int TASK_CHECK_CLOUD_SYNC = 9;
    public static final int TASK_SHARE_TO_APP = 10;

    public static final String EXTRA_BUBBLE_ID = "EXTRA_BUBBLE_ID";
    public static final String EXTRA_IMAGE_URIS = "EXTRA_IMAGE_URIS";
    public static final String EXTRA_CONTENTTYPE = "EXTRA_CONTENTTYPE";
    public static final String EXTRA_URI = "EXTRA_URI";
    public static final String EXTRA_TARGET_INTENT = "EXTRA_TARGET_INTENT";
    public static final String EXTRA_TARGET_OPTION_BUNDLE = "EXTRA_TARGET_OPTION_BUNDLE";
    public static final String EXTRA_BUBBLE_SYNC_ID = "EXTRA_BUBBLE_SYNC_ID";
    public static final String PACKAGE_NAME = "com.smartisanos.ideapills";
    public static final String CLASS_NAME = "com.smartisanos.ideapills.ProxyActivity";

    protected static final int RESULT_PICK_ATTACHMENT = 1;
    private static final int RESULT_IMAGE_PREVIEW = 2;
    private static final int RESULT_FILE_PREVIEW = 3;
    private static final int RESULT_SHARE_TO_APPS = 4;
    private static final int RESULT_IMAGE_PREVIEW_NO_EDIT = 5;
    private static final int RESULT_FILE_PREVIEW_OLD = 6;
    private static final int RESULT_ACCOUNT_LOGIN = 7;
    private static final int RESULT_SHARE_TO_APP = 8;

    protected boolean mRequestBeforeVisible = true;
    LOG log = LOG.getInstance(ProxyActivity.class);
    protected int mBubbleId;
    protected ChooseResultHelper mChooseResultHelper;

    private Handler mTaskHandler = new Handler();
    private Runnable mTaskRunnable = new Runnable() {
        @Override
        public void run() {
            handleTask();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChooseResultHelper = new ChooseResultHelper(this);
        mTaskHandler.postDelayed(mTaskRunnable, 300);
    }

    private void handleTask() {
        Intent intent = getIntent();
        int task = intent.getIntExtra(EXTRA_TASK, 0);
        switch (task) {
            case TASK_ATTACHCHOOSE:
                startAttachementChoose(intent);
                break;
            case TASK_IMAGE_PREVIEW:
                startImagePreview(intent);
                break;
            case TASK_IMAGE_PREVIEW_NO_EDIT:
                startImagePreviewNoEdit(intent);
                break;
            case TASK_FILE_PREVIEW:
                startFilePreview(intent);
                break;
            case TASK_FILE_PREVIEW_OLD:
                startFilePreviewOld(intent);
                break;
            case TASK_SHARE_TO_APPS:
                startShareToApps(intent);
                break;
            case TASK_ACCOUNT_LOGIN:
                startAccountLogin();
                break;
            case TASK_OPEN_IDEAPILLS:
                showIdeapillsList(intent);
                break;
            case TASK_CHECK_CLOUD_SYNC:
                try {
                    SmilingAccountsUtils.checkCloudSyncFirstLaunchWithWindowType(this, 5);
                } catch (Exception e) {
                    LOG.e(e);
                }
                break;
            case TASK_SHARE_TO_APP:
                startShareToApp(intent);
                break;
            default:
                log.error("no available task");
                finish();
                break;
        }
    }

    private void startAccountLogin() {
        Intent intent = SyncUtil.getCloudAccountLoginIntent(getString(R.string.app_name));
        startActivityForResult(intent, RESULT_ACCOUNT_LOGIN);
        mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
        BubbleController.getInstance().hideBubbleListWithAnim();
    }

    protected void startAttachementChoose(Intent intent) {
        mBubbleId = intent.getIntExtra(EXTRA_BUBBLE_ID, 0);
        if (mBubbleId > 0) {
            try {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                i.setType("*/*");
//              i.putExtra(MediaStore.EXTRA_SIZE_LIMIT, leftSizeInByte);
                Intent resultIntent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    resultIntent = Intent.createChooser(i, getText(R.string.select_attachment_type),
                            ChooseResultHelper.getAttachPickIntentSender(this));
                } else  {
                    resultIntent = Intent.createChooser(i, getText(R.string.select_attachment_type));
                }
                resultIntent.putExtra("window_type", BubbleController.BUBBLE_WINDOW_TYPE);
                int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
                resultIntent.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
                mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
                startActivityForResult(resultIntent,
                        RESULT_PICK_ATTACHMENT);
            } catch (ActivityNotFoundException e) {
                log.error("Can not start activity " + e);
                finish();
            }
        } else {
            finish();
        }
    }

    protected void startShareToApps(Intent intent) {
        Intent target = intent.getExtras().getParcelable(EXTRA_TARGET_INTENT);
        target.setFlags(target.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
        int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
        target.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        if (!BulletSmsUtils.isBulletSmsInstalled(this)) {
            ArrayList<ShareItem> items = new ArrayList<ShareItem>();
            items.add(ShareItem.createForCustom(getPackageName(),
                    R.drawable.bullet_sms_share_icon, R.string.share_to_bullet_sms, ChooseResultHelper.CHOOSE_BULLET_MESSENGER));
            target.putParcelableArrayListExtra(ShareItem.EXTRA_SHARE_ITEM_DATAS, items);
        }
        Parcelable targetParcelable = target.getParcelableExtra(Intent.EXTRA_INTENT);
        if (targetParcelable != null && targetParcelable instanceof Intent) {
            Intent extraIntent = (Intent) targetParcelable;
            extraIntent.putExtra("disable_suggest_bullet_sms", true);
        }
        mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
        try {
            startActivityForResult(target, RESULT_SHARE_TO_APPS);
        } catch (ActivityNotFoundException e) {
            finish();
        }
    }

    protected void startShareToApp(Intent intent) {
        Intent target = intent.getExtras().getParcelable(EXTRA_TARGET_INTENT);
        target.setFlags(target.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
        int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
        target.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
        try {
            mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
            BubbleController.getInstance().hideBubbleListWithAnim();
            startActivityForResult(target, RESULT_SHARE_TO_APP);
        } catch (ActivityNotFoundException e) {
            finish();
        }
    }

    protected void startImagePreview(Intent intent) {
        mBubbleId = intent.getIntExtra(EXTRA_BUBBLE_ID, 0);
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(EXTRA_IMAGE_URIS);
        String contentType = intent.getStringExtra(EXTRA_CONTENTTYPE);
        Uri uri = (Uri) intent.getParcelableExtra(EXTRA_URI);
        Intent i = GlobalBubbleUtils.createImagePreviewIntent(uris, contentType, uri, getPackageName(), false);
        if (mBubbleId > 0 && i != null) {
            Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(this,
                    smartisanos.R.anim.pop_up_in, 0).toBundle();
            int[] anims = {0, smartisanos.R.anim.slide_down_out, smartisanos.R.anim.pop_up_in, 0};
            i.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
            startActivityForResult(i, RESULT_IMAGE_PREVIEW, bundleOption);
            mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
            BubbleController.getInstance().hideBubbleListWithAnim();
        } else {
            finish();
        }
    }

    protected void startImagePreviewNoEdit(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(EXTRA_IMAGE_URIS);
        String contentType = intent.getStringExtra(EXTRA_CONTENTTYPE);
        Uri uri = (Uri) intent.getParcelableExtra(EXTRA_URI);
        Intent i = GlobalBubbleUtils.createImagePreviewIntent(uris, contentType, uri, getPackageName(), false);
        if (i != null) {
            Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(this,
                    smartisanos.R.anim.pop_up_in, 0).toBundle();
            int[] anims = {0, smartisanos.R.anim.slide_down_out, smartisanos.R.anim.pop_up_in, 0};
            i.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
            startActivityForResult(i, RESULT_IMAGE_PREVIEW_NO_EDIT, bundleOption);
            mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
            BubbleController.getInstance().hideBubbleListWithAnim();
        } else {
            finish();
        }
    }

    protected void startFilePreview(Intent intent) {
        Intent target = intent.getExtras().getParcelable(EXTRA_TARGET_INTENT);
        //Bundle option = intent.getExtras().getParcelable(EXTRA_TARGET_OPTION_BUNDLE);
        Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(this,
                smartisanos.R.anim.pop_up_in, 0).toBundle();
        int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
        target.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
        mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
        try {
            startActivityForResult(target, RESULT_FILE_PREVIEW, bundleOption);
        } catch (ActivityNotFoundException e) {
            finish();
        }
    }

    protected void startFilePreviewOld(Intent intent) {
        Intent target = intent.getExtras().getParcelable(EXTRA_TARGET_INTENT);
        //Bundle option = intent.getExtras().getParcelable(EXTRA_TARGET_OPTION_BUNDLE);
        Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(this,
                smartisanos.R.anim.pop_up_in, 0).toBundle();
        int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
        target.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
        try {
            startActivityForResult(target, RESULT_FILE_PREVIEW_OLD, bundleOption);
            mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
            BubbleController.getInstance().hideBubbleListWithAnim();
        } catch (ActivityNotFoundException e) {
            finish();
        }
    }

    private void showIdeapillsList(Intent intent){
        long syncId = intent.getLongExtra(EXTRA_BUBBLE_SYNC_ID, 0);
        BubbleController.getInstance().showBubbleList(syncId);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BubbleController.getInstance().clearForceHideWindow();
        if (mRequestBeforeVisible && !mChooseResultHelper.isShareToBlueTooth() && !mChooseResultHelper.isChooseCustomBullet()) {
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(null, BubbleItem.MSG_SHARE_OVER);
        }
        if (mChooseResultHelper != null) {
            mChooseResultHelper.resetChooseState();
        }
        switch (requestCode) {
            case RESULT_PICK_ATTACHMENT:
                BubbleItem bubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(mBubbleId);
                if (bubbleItem != null) {
                    boolean isAddingAttachment = bubbleItem.isAddingAttachment();
                    bubbleItem.setAddingAttachment(false);
                    if (resultCode == RESULT_OK && AttachmentUtils.handleAttachmentPickResult(data, mBubbleId, this)) {
                        GlobalBubbleUtils.trackBubbleChange(bubbleItem);
                    } else if (isAddingAttachment) {
                        GlobalBubbleUtils.removeEmptyBubbleIfNeed(bubbleItem);
                    }
                }
                break;
            case RESULT_IMAGE_PREVIEW:
                if (resultCode == RESULT_OK) {
                    handleImagePreviewResult(data);
                }
                break;
            case RESULT_FILE_PREVIEW:
                break;
            default:
                log.error("unknown requestCode");
                break;
        }
        finishAndRemoveTask();
    }

    void handleImagePreviewResult(Intent data) {
        ClipData cd = data.getClipData();
        if (cd == null) {
            return;
        }
        ArrayList<Uri> remainUris = new ArrayList<Uri>();
        for (int i = 0; i < cd.getItemCount(); i++) {
            Uri uri = cd.getItemAt(i).getUri();
            if (uri != null) {
                remainUris.add(uri);
            }
        }
        GlobalBubbleManager.getInstance().removeAttachmentImgNoIn(mBubbleId, remainUris);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskHandler.removeCallbacks(mTaskRunnable);
        if(mChooseResultHelper != null){
            mChooseResultHelper.onActivityDestroy();
        }
        BubbleController.getInstance().clearForceHideWindow();
        BubbleItem bubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(mBubbleId);
        if (bubbleItem != null && bubbleItem.isAddingAttachment()) {
            bubbleItem.setAddingAttachment(false);
            if (bubbleItem.isEmptyBubble()) {
                bubbleItem.dele();
                GlobalBubbleManager.getInstance().removeBubbleItem(bubbleItem);
                IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(bubbleItem, BubbleItem.MSG_BUBBLE_DELETE);
            }
        }
        clearAddingAttachmentStatus();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        if (options == null) {
            options = ActivityOptions.makeBasic().toBundle();
        }
        if (!options.containsKey("android.activity.launchDisplayId")) {
            options.putInt("android.activity.launchDisplayId", Display.DEFAULT_DISPLAY);
        }
        super.startActivityForResult(intent, requestCode, options);
    }

    protected void clearAddingAttachmentStatus() {
        StatusManager.setStatus(StatusManager.ADDING_ATTACHMENT, false);
    }
}
