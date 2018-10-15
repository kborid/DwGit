package com.smartisanos.ideapills.util;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.SmtPCUtils;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.DisplayInfo;
import android.view.WindowManager;
import android.widget.Toast;
import com.smartisanos.ideapills.ExtDisplayProxyActivity;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.ProxyActivity;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.common.util.ShareUtils;
import com.smartisanos.ideapills.view.BubbleAttachmentView;
import com.smartisanos.ideapills.entity.BubbleItem;
import android.service.onestep.GlobalBubble;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import smartisanos.api.ToastSmt;
import smartisanos.api.IntentSmt;
import smartisanos.util.UIHandler;


public class GlobalBubbleUtils {
    private static final LOG log = LOG.getInstance(GlobalBubbleUtils.class);

    public static final String PREVIEW_IMAGE_ATTACHMENT_APP_PACKAGE_NAME = "com.android.gallery3d";
    public static final String PREVIEW_IMAGE_ATTACHMENT_APP_ACTIVITY
            = PREVIEW_IMAGE_ATTACHMENT_APP_PACKAGE_NAME + ".app.Gallery";
    public static final String IMAGE_ATTACHMENT_URI_KEY = "ImageItemList";
    public static final String IMAGE_ATTACHMENT_VIEW_EDIT_KEY = "action_view_edit";
    public static final String PREVIEW_IMAGE_ATTACHMENT_DIALOG_TITLE = "delete_dialog_title_res";
    public static final String PREVIEW_IMAGE_ATTACHMENT_DIALOG_CONFIRM = "delete_dialog_confirm_res";
    public static final String PREVIEW_IMAGE_ATTACHMENT_PACKAGE_NAME = "package_name";

    private static final int DRAG_BUBBLE_MAX_DATA_SIZE = 150 * 1024;


    private static Intent chooserIntent(Intent srcIntent) {

        String shareType = srcIntent.getType();

        Intent targetedShare = new Intent(srcIntent.getAction());
        targetedShare.setType(shareType);

        String shareTextContent = srcIntent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(shareTextContent)) {
            targetedShare.putExtra(Intent.EXTRA_TEXT, shareTextContent);
        }

        if (srcIntent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            ArrayList<Uri> multiUrls = srcIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (multiUrls != null && !multiUrls.isEmpty()) {
                targetedShare.putParcelableArrayListExtra(Intent.EXTRA_STREAM, multiUrls);
            }
        } else {
            Bundle bundle = srcIntent.getExtras();
            if (bundle != null) {
                Uri shareExtraStream = bundle.getParcelable(Intent.EXTRA_STREAM);
                if (shareExtraStream != null) {
                    targetedShare.putExtra(Intent.EXTRA_STREAM, shareExtraStream);
                }
            }
        }

        targetedShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return targetedShare;
    }

    public static void addUriPermission(Context context, ArrayList<Uri> imageUris, String packageName) {
        if (imageUris == null || imageUris.size() == 0 || TextUtils.isEmpty(packageName)) {
            return;
        }
        for (int i = 0; i < imageUris.size(); i++) {
            Uri uri = imageUris.get(i);
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private static Bundle bubble2Bundle(Context context, BubbleItem bubble) {
        Bundle bundle = new Bundle();
        Uri uri = bubble.getUri();
        String path = uri.getPath();
        bundle.putString("path", uri.toString());
        long createTime = BubbleSpeechPlayer.getInstance(context).getVoiceCreateTime(bubble);
        String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
        if (createTime > 0) {
            Date date = new Date(createTime);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String suffix = "_" + dateFormat.format(date);
            name = name + suffix;
        }
        bundle.putString("name", name);
        bundle.putLong("createTime", createTime);
        bundle.putLong("duration", bubble.getVoiceDuration());
        bundle.putInt("samplingRate", bubble.getSamplineRate());
        return bundle;
    }

    public static Intent createShareIntent(Context context, List<BubbleItem> bubbles) {
        return createShareIntent(context, bubbles, null);
    }

    public static Intent createShareIntent(Context context, List<BubbleItem> bubbles, String packageName) {
        ArrayList<BubbleItem> bubbleList = new ArrayList<BubbleItem>();
        StringBuilder sb = new StringBuilder();
        ArrayList<GlobalBubble> toShare = new ArrayList<GlobalBubble>();
        for (BubbleItem bubble : bubbles) {
            if (bubble.isVoiceBubble() && bubble.getUri() != null
                    && Utils.checkIsVoiceAvailable(context, bubble.getUri())) {
                bubbleList.add(bubble);
            }
            sb.append(bubble.getText()).append("\n\n");
            toShare.add(bubble.getBubble());
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        Intent targetIntent = new Intent(Intent.ACTION_SEND);
        targetIntent.setType("text/plain");
        targetIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        targetIntent.putExtra("calling_package", context.getPackageName());
        targetIntent.putParcelableArrayListExtra("action_key_bubble_list", toShare);
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        Intent resultIntent = null;
        if (packageName == null) {
            targetedShareIntents.add(targetIntent);
            Intent copyIntent = new Intent("com.smartisanos.ideapills.intent.action.COPY_TEXT");
            List<ResolveInfo> copyInfo = context.getPackageManager().queryIntentActivities(
                    copyIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (!copyInfo.isEmpty()) {
                copyIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                ResolveInfo info = (ResolveInfo) copyInfo.get(0);
                copyIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                copyIntent.setPackage(info.activityInfo.packageName);
                targetedShareIntents.add(new LabeledIntent(copyIntent, info.activityInfo.packageName, info
                        .loadLabel(context.getPackageManager()), info.icon));
            }
            if (bubbleList.size() == bubbles.size() && bubbleList.size() > 0) {
                final String key = "voicebubble";
                Intent targetIntent2 = new Intent();
                ArrayList<Bundle> bundles = new ArrayList<Bundle>();
                for (BubbleItem bubble : bubbleList) {
                    bundles.add(bubble2Bundle(context, bubble));
                }
                if (bundles.size() == 1) {
                    targetIntent2.setAction("smartisan.action.VIEW_VOICE_INPUT_RECORD");
                    targetIntent2.putExtra(key, bundles.get(0));
                } else {
                    targetIntent2.setAction("smartisan.action.VIEW_MULTI_VOICE_INPUT_RECORD");
                    targetIntent2.putParcelableArrayListExtra(key, bundles);
                }
                targetIntent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                targetIntent2.setPackage(Constants.RECORDER);
                List<ResolveInfo> resInfo2 = context.getPackageManager().queryIntentActivities(
                        targetIntent2, PackageManager.MATCH_DEFAULT_ONLY);
                if (!resInfo2.isEmpty()) {
                    ResolveInfo info = (ResolveInfo) resInfo2.get(0);
                    targetIntent2.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    targetIntent2.setPackage(info.activityInfo.packageName);
                    targetedShareIntents.add(new LabeledIntent(targetIntent2, info.activityInfo.packageName, info
                            .loadLabel(context.getPackageManager()), info.icon));
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                IntentSender intentSender = ChooseResultHelper.getChooseIntentSender(context, bubbles);
                resultIntent = Intent.createChooser(targetedShareIntents.remove(0), null, intentSender);
            } else {
                resultIntent = Intent.createChooser(targetedShareIntents.remove(0), null);
            }
            LabeledIntent[] li = targetedShareIntents.toArray(new LabeledIntent[targetedShareIntents.size()]);
            resultIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, li);
        } else {
            targetIntent.setPackage(packageName);
            try {
                List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
                if (list == null || list.isEmpty()) {
                    return null;
                }
            } catch (Exception e) {
                //ignore
            }
            resultIntent = targetIntent;
            resultIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
        }
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        resultIntent.putExtra("window_type", BubbleController.BUBBLE_WINDOW_TYPE);
        return resultIntent;
    }

    private static Intent generateProxyIntent(Context context) {
        Intent intent;
        if (SmtPCUtils.isValidExtDisplayId(context)) {
            intent = new Intent(context, ExtDisplayProxyActivity.class);
            IntentSmt.putSmtExtra(intent, "window-type", "window_without_caption_view");
        } else {
            intent = new Intent(context, ProxyActivity.class);
        }
        return intent;
    }

    public static void shareToApp(Context context, BubbleItem item, String packageName) {
        ArrayList<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
        bubbleItems.add(item);
        Intent target = createShareIntent(context, bubbleItems, packageName);
        Intent i = generateProxyIntent(context);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_SHARE_TO_APP);
        i.putExtra(ProxyActivity.EXTRA_TARGET_INTENT, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

    public static void shareToApp(Context context, BubbleItem item, ComponentName componentName) {
        ArrayList<BubbleItem> bubbleList = new ArrayList<BubbleItem>();
        bubbleList.add(item);
        Intent targetIntent = new Intent(Intent.ACTION_SEND);
        targetIntent.setType("text/plain");
        targetIntent.putExtra(Intent.EXTRA_TEXT, item.getText());
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        targetIntent.setComponent(componentName);
        Intent resultIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            IntentSender intentSender = ChooseResultHelper.getChooseIntentSender(context, bubbleList);
            resultIntent = Intent.createChooser(targetIntent, null, intentSender);
        } else {
            resultIntent = Intent.createChooser(targetIntent, null);
        }
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        resultIntent.putExtra("window_type", BubbleController.BUBBLE_WINDOW_TYPE);
        Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(context,
                smartisanos.R.anim.pop_up_in, 0).toBundle();
        int[] anims = {0, smartisanos.R.anim.slide_down_out};
        resultIntent.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        context.startActivity(resultIntent, bundleOption);
    }

    public static void shareTextToApps(Context context, String text) {
        if (!TextUtils.isEmpty(text)) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.removeExtra(android.content.Intent.EXTRA_TEXT);
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
            sharingIntent.putExtra("window_type", BubbleController.BUBBLE_WINDOW_TYPE);
            Intent i = generateProxyIntent(context);
            i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_SHARE_TO_APPS);
            i.putExtra(ProxyActivity.EXTRA_TARGET_INTENT, sharingIntent);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityMayInExtDisplay(context, i, true);
        }
    }

    public static Intent createImagePreviewIntent(ArrayList<Uri> imageUris, String contentType, Uri extraUri,
                                                        String packageName, boolean editable) {
        ArrayList<Uri> uris = imageUris;
        Uri uri = extraUri;
        Intent i = null;
        if (uri != null && contentType != null && uris != null) {
            i = new Intent(Intent.ACTION_VIEW);
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ComponentName cn = new ComponentName(PREVIEW_IMAGE_ATTACHMENT_APP_PACKAGE_NAME, PREVIEW_IMAGE_ATTACHMENT_APP_ACTIVITY);
            i.setComponent(cn);
            i.putParcelableArrayListExtra(IMAGE_ATTACHMENT_URI_KEY, uris);
            i.putExtra(IMAGE_ATTACHMENT_VIEW_EDIT_KEY, editable);
            i.putExtra(PREVIEW_IMAGE_ATTACHMENT_DIALOG_TITLE,
                    R.string.preview_image_attachment_dialog_title);
            i.putExtra(PREVIEW_IMAGE_ATTACHMENT_DIALOG_CONFIRM,
                    R.string.preview_image_attachment_dialog_confirm);
            i.putExtra(PREVIEW_IMAGE_ATTACHMENT_PACKAGE_NAME, packageName);
            Utils.setIntentDataAndTypeAndNormalize(
                    i, uri, contentType);
        }
        return i;
    }

    private static Toast sToast = null;
    public static void showSystemToast(int textRes, int duration) {
        Context context = BubbleController.getInstance().getContext();
        if (textRes > 0) {
            String text = context.getResources().getString(textRes);
            showSystemToast(context, text, duration);
        }
    }
    public static void showSystemToast(Context context, int textRes, int duration) {
        if (textRes > 0) {
            String text = context.getResources().getString(textRes);
            showSystemToast(context, text, duration);
        }
    }
    public static void showSystemToast(final Context context,final String text,final int duration) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (SmtPCUtils.isValidExtDisplayId(context)) {
                    Context extContext = context;
                    BubbleController extInstance = BubbleController.getExtInstance();
                    if (extInstance != null && extInstance.getContext() != null) {
                        extContext = extInstance.getContext();
                    }
                    Toast.makeText(extContext, text, duration).show();

                } else {
                    if (sToast == null) {
                        sToast = ToastSmt.getInstance().makeText(context.getApplicationContext(), text, duration, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
                    } else {
                        sToast.setText(text);
                        sToast.setDuration(duration);
                    }
                    sToast.show();
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            UIHandler.post(r);
        }
    }

    public static void startActivityMayInExtDisplay(Context context, Intent intent) {
        startActivityMayInExtDisplay(context, intent, false);
    }

    public static void startActivityMayInExtDisplay(Context context, Intent intent, boolean launchFullScreen) {
        ActivityOptions options = null;
        if (android.os.Build.VERSION.SDK_INT >= 26
                && SmtPCUtils.isValidExtDisplayId(context)) {
            options = ActivityOptions.makeBasic();
            MultiSdkUtils.setLaunchDisplayId(context,options);
            if (launchFullScreen) {
                decorateFullScreenLaunch(context, intent, options);
            }
        }
        startActivityMayInExtDisplay(context, intent, options);
    }

    public static void startActivityMayInExtDisplay(Context context, Intent intent, ActivityOptions options) {
        if (options != null) {
            context.startActivity(intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    public static void decorateFullScreenLaunch(Context context, Intent intent, ActivityOptions options) {
        DisplayInfo displayInfo = new DisplayInfo();
        SmtPCUtils.getExtDisplay(context).getDisplayInfo(displayInfo);
        int statusBarHeight = SmtPCUtils.getSmtStatusBarPixelHeight(context);
        int navBarHeight = SmtPCUtils.getSmtNavigationBarPixelHeight(context);
        IntentSmt.putSmtExtra(intent, "window-type", "window_without_caption_view");
        options.setLaunchBounds(new Rect(0, statusBarHeight, displayInfo.largestNominalAppWidth,
                displayInfo.smallestNominalAppWidth));
    }

    public static void shareToApps(Context context, List<BubbleItem> bubbles) {
        Intent target = createShareIntent(context, bubbles);
        Intent i = generateProxyIntent(context);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_SHARE_TO_APPS);
        i.putExtra(ProxyActivity.EXTRA_TARGET_INTENT, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityMayInExtDisplay(context, i, true);
    }

    public static void shareAttachToApps(Context context, Uri uri, String type) {
        if (type == null) {
            showSystemToast(context, R.string.bubble_share_attachment_unknown_type,
                    Toast.LENGTH_SHORT);
            return;
        }
        IntentSender intentSender = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            intentSender = ChooseResultHelper.getAttachChooseIntentSender(context);
        }
        Intent target = ShareUtils.createShareAttachIntent(uri, type, intentSender);
        Intent i = generateProxyIntent(context);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_SHARE_TO_APPS);
        i.putExtra(ProxyActivity.EXTRA_TARGET_INTENT, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityMayInExtDisplay(context, i, true);
    }

    public static void pickAttachment(Context context, int id) {
        Intent i = generateProxyIntent(context);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_ATTACHCHOOSE);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(ProxyActivity.EXTRA_BUBBLE_ID, id);
        startActivityMayInExtDisplay(context, i, true);
    }

    public static void previewImagesNew(Context context, BubbleAttachmentView attachmentView, ArrayList<String> files, int pos) {
        Intent target = new Intent("com.smartisan.action.VIEW_FILE_FLOATING");
        target.putExtra("extra_files", files);
        target.putExtra("extra_current_num", pos);

        Rect bound = new Rect();
        attachmentView.getBoundsOnScreen(bound);
        target.putExtra("source_bound", bound);
//        int[] anims = {0, smartisanos.R.anim.slide_down_out};
//        target.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        Bundle bundleOption = ActivityOptionsCompat.makeScaleUpAnimation(attachmentView, 0, 0,
                attachmentView.getWidth(), attachmentView.getHeight()).toBundle();

        Intent i = new Intent(context, ProxyActivity.class);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_FILE_PREVIEW);
        i.putExtra(ProxyActivity.EXTRA_TARGET_INTENT, target);
        i.putExtra(ProxyActivity.EXTRA_TARGET_OPTION_BUNDLE, bundleOption);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityMayInExtDisplay(context, i);
    }

    public static void previewImages(Context context, ArrayList<Uri> uris, AttachMentItem attachMentItem) {
        if (SmtPCUtils.isValidExtDisplayId(context)) {
            previewImagesDirectly(context, uris, attachMentItem);
            return;
        }
        Intent i = new Intent(context, ProxyActivity.class);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_IMAGE_PREVIEW_NO_EDIT);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(ProxyActivity.EXTRA_URI, attachMentItem.getUri());
        i.putExtra(ProxyActivity.EXTRA_CONTENTTYPE, attachMentItem.getContentType());
        i.putParcelableArrayListExtra(ProxyActivity.EXTRA_IMAGE_URIS, uris);
        startActivityMayInExtDisplay(context, i);
    }

    public static void previewImagesDirectly(Context context, ArrayList<Uri> uris, AttachMentItem attachMentItem) {
        Intent intent = createImagePreviewIntent(uris, attachMentItem.getContentType(), attachMentItem.getUri(), context.getPackageName(), false);
        if (intent != null) {
            Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(context,
                    smartisanos.R.anim.pop_up_in, 0).toBundle();
            int[] anims = {0, smartisanos.R.anim.slide_down_out, smartisanos.R.anim.pop_up_in, 0};
            intent.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityMayInExtDisplay(context, intent, false);
        }
    }


    public static void openFile(Context context, AttachMentItem attachMentItem) {
        // open compressed file to preview
        if (attachMentItem.getStatus() == AttachMentItem.STATUS_SUCCESS) {
            // open other files
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = attachMentItem.guessContentType();
            if (mimeType == null) {
                showSystemToast(context, R.string.bubble_open_attachment_unknown_type,
                        Toast.LENGTH_SHORT);
                return;
            }
            try {
                intent.setDataAndType(attachMentItem.getUri(), mimeType);
                //grant other Apps the permission to access DocumentUri in usb storage
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (!SmtPCUtils.isValidExtDisplayId(context)) {
                    Intent i = new Intent(context, ProxyActivity.class);
                    i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_FILE_PREVIEW_OLD);
                    i.putExtra(ProxyActivity.EXTRA_TARGET_INTENT, intent);
                    intent = i;
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityMayInExtDisplay(context, intent);
            } catch (ActivityNotFoundException e) {
                log.error("Failed to start " + intent + ": " + e);
                showSystemToast(context, R.string.download_no_application_title,
                        Toast.LENGTH_SHORT);
            }
        }
    }

    public static void trackBubbleChange(BubbleItem item) {
        if (item != null) {
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
            int flag = item.getModificationFlag();
            dataMap.put("ifmod", (flag & BubbleItem.MF_COLOR) != 0 ? 1 : 0);
            dataMap.put("ifadd", item.getAttachmentCount() > 0 ? 1 : 0);
            dataMap.put("ifset", (item.getRemindTime() > 0 || item.getDueDate() > 0) ? 1 : 0);
            dataMap.put("ifcontent", (flag & BubbleItem.MF_TEXT) != 0 ? 1 : 0);
            dataMap.put("ifcheck", item.getUsedTime() > 0 ? 1 : 0);
            dataMap.put("atch_num", item.getAttachmentCount());
            dataMap.put("source", 9001);
            LOG.d("trackBubbleChange =" + dataMap);
            Tracker.onEvent("A420021", dataMap);
        }
    }

    public static void checkCloudSync(Context context) {
        Intent i = generateProxyIntent(context);
        i.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_CHECK_CLOUD_SYNC);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

    public static Bundle getBubbleBundle(List<BubbleItem> bubbleItemList) {
        List<GlobalBubble> globalBubbles = new ArrayList<GlobalBubble>();
        for (BubbleItem item : bubbleItemList) {
            globalBubbles.add(item.getBubble());
        }
        return GlobalBubble.toBundle(globalBubbles);
    }

    public static boolean isBubbleListTooLarge(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        bundle.writeToParcel(parcel, 0);
        int dataSize = parcel.dataSize();
        parcel.recycle();
        LOG.d("isBubbleListTooLarge dataSize =" + dataSize);
        return dataSize > DRAG_BUBBLE_MAX_DATA_SIZE;
    }

    public static boolean removeEmptyBubbleIfNeed(BubbleItem bubbleItem) {
        if (bubbleItem != null && bubbleItem.isEmptyBubble()) {
            bubbleItem.dele();
            GlobalBubbleManager.getInstance().removeBubbleItem(bubbleItem);
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(bubbleItem, BubbleItem.MSG_BUBBLE_DELETE);
            return true;
        }
        return false;
    }
}
