package com.smartisanos.ideapills;

import android.app.Activity;
import android.app.SmtPCUtils;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.UriUtils;
import com.smartisanos.ideapills.interfaces.LocalInterface;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;

import java.util.ArrayList;
import java.util.List;

import smartisanos.util.IdeaPillsUtils;

public class ShareActivity extends Activity {
    LOG log = LOG.getInstance(ShareActivity.class);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //1.check data
        Intent intent = getIntent();
        String action = (intent == null) ? null : intent.getAction();
        if (intent == null || action == null || (!intent.getAction().equalsIgnoreCase(Intent.ACTION_SEND) &&
                !intent.getAction().equalsIgnoreCase(Intent.ACTION_SEND_MULTIPLE))) {
            log.error("Unexpected error! action is null");
            finish();
            return;
        }

        if (UriUtils.isIllegal(intent.getClipData())) {
            log.error("illegal uri!");
            finish();
            return;
        }

        //2. get share data
        if (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE)) {

            grantReadPermissionToUri(intent.getClipData());

            if (action.equals(Intent.ACTION_SEND)) {
                 String type = intent.getType();
                 Uri stream = null;
                if(intent.getParcelableExtra(Intent.EXTRA_STREAM) != null){
                    stream = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                }
                final Context context = ShareActivity.this;
                CharSequence extra_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (stream != null && type != null) {
                    List<Uri> uris = new ArrayList<>();
                    uris.add(stream);
                    LOG.d("Get ACTION_SEND intent: Uri = " + stream + "; mimetype = " + type);
                    // share single file Data
                    handleShareFileData(uris, intent, type, context);
                    finish();
                    return;
                } else if (extra_text != null) {
                    LOG.d("Get ACTION_SEND intent with Extra_text = " + extra_text.toString() + "; mimetype = " + type);
                    //share text  Data
                    handleShareTextData(intent);
                    finish();
                    return;
                } else {
                    log.warn("Error trying to do set text...File not created!");
                    finish();
                    return;
                }
            } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                final String mimeType = intent.getType();
                final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                final Context context = ShareActivity.this;
                if (mimeType != null && uris != null) {
                    LOG.d("Get ACTION_SHARE_MULTIPLE intent: uris " + uris + "\n Type= "
                                + mimeType);
                    // share multi file Data
                    handleShareFileData(uris,intent, mimeType, context);
                    finish();
                    return;
                } else {
                    log.error("type is null; or sending files URIs are null");
                    finish();
                    return;
                }
            }

        } else {
            log.warn("Unsupported action: " + action);
            GlobalBubbleUtils.showSystemToast(this, R.string.share_unsupport_file_toast_tips, Toast.LENGTH_SHORT);
            finish();
            return;
        }
    }


    private void handleShareFileData(List<Uri> uris, Intent intent, String mimeType, Context context) {
        if (!Constants.IS_IDEA_PILLS_ENABLE) {
            startShareDialogActivity(getText(R.string.bubble_notice), getText(R.string.enable_idea_pills_for_share),
                    getText(R.string.enable_idea_pills_goto));
            return;
        }
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            luncherIdeaPillsShare(mimeType, context, clipData);
        } else if (uris != null && uris.size() > 0) {
            //use uris build bundle convertUriToClipData()
            clipData = convertUrisToClipData(uris, mimeType, clipData);

            if (clipData != null) {
                luncherIdeaPillsShare(mimeType, context, clipData);
            }
        }
    }

    private ClipData convertUrisToClipData(List<Uri> uris, String mimeType, ClipData clipData) {
        for (int index = 0; index < uris.size(); index++) {
            Uri streamUri = uris.get(index);
            if (clipData == null) {
                clipData = new ClipData(null, new String[]{mimeType}, new ClipData.Item(streamUri));
            } else {
                ClipData.Item item = new ClipData.Item(streamUri);
                clipData.addItem(item);
            }
        }
        return clipData;
    }

    private void luncherIdeaPillsShare(String mimeType, Context context, ClipData clipData) {
        Bundle bundle = new Bundle();
        ClipDescription clipDescription = new ClipDescription(null, new String[]{mimeType});
        if (bundle != null) {
            clipDescription.setExtras(new PersistableBundle(bundle));
        }
        bundle.putParcelable("clip_data", clipData);
        bundle.putParcelable("clip_description", clipDescription);
        boolean extDisplay = SmtPCUtils.isValidExtDisplayId(context);
        bundle.putBoolean("callFromExtDisplay", extDisplay);
        LocalInterface.call(context, "METHOD_HANDLE_DRAG_EVENT", bundle);
    }

    private void handleShareTextData(Intent intent) {
        final ArrayList<GlobalBubble> bubbleArrayList = intent.getParcelableArrayListExtra("bubbles");
        if (bubbleArrayList != null) {
            for (GlobalBubble bubble : bubbleArrayList) {
                bubble.setId(0);
            }
        }

        final CharSequence charSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(charSequence) || bubbleArrayList != null) {
            if (charSequence.toString().trim().length() > 0) {
                if (!Constants.IS_IDEA_PILLS_ENABLE) {
                    startShareDialogActivity(getText(R.string.bubble_notice), getText(R.string.enable_idea_pills_for_share),
                            getText(R.string.enable_idea_pills_goto));
                } else {
                    List<BubbleItem> bubbleItems = null;
                    if (bubbleArrayList != null) {
                        bubbleItems = GlobalBubbleManager.getInstance().addGlobalBubbles(bubbleArrayList);
                    } else {
                        List<GlobalBubble> list = new ArrayList<GlobalBubble>();
                        GlobalBubble item = new GlobalBubble();
                        item.setType(GlobalBubble.TYPE_TEXT);
                        item.setText(charSequence.toString());
                        list.add(item);
                        bubbleItems = GlobalBubbleManager.getInstance().addGlobalBubbles(list);
                    }
                    if (bubbleItems != null && bubbleItems.size() > 0) {
                        GlobalBubbleUtils.showSystemToast(this, R.string.share_to_idea_succeed, Toast.LENGTH_SHORT);
                    }
                }
            } else {
                log.info("trim text is empty");
            }
        }
    }


    /*
   *  Grant permission to access a specific Uri.
   */
    private void grantReadPermissionToUri(ClipData clipData) {
        if (clipData == null) {
            log.info("ClipData is null ");
            return;
        }
        try {
            String packageName = getPackageName();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                Uri uri = item.getUri();
                if (uri != null) {
                    String scheme = uri.getScheme();
                    if (scheme != null && scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
            }
        } catch (Exception e) {
           log.error("GrantUriPermission :" + e.toString());
        }
    }

    private void startShareDialogActivity(CharSequence title, CharSequence message, CharSequence posiText) {
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra("title", title);
        dialogIntent.putExtra("message", message);
        dialogIntent.putExtra("posiText", posiText);
        dialogIntent.putExtra("dialogType", Utils.DIALOG_TYPE.OPEN_IDEA.name());
        startActivity(dialogIntent);
    }
}
