package com.smartisanos.sara.bubble.search.viewholder;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import smartisanos.app.voiceassistant.ContactStruct;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.entity.ContactModel;
import com.smartisanos.sara.entity.IModel;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.PackageUtil;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;

import java.util.ArrayList;

import smartisanos.app.MenuDialog;
import smartisanos.app.MenuDialogListAdapter;

public class ViewHolderContact extends ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private static String TAG = "VoiceAss.ViewHolderContact";
    TextView name;
    TextView number;
    TextView label;
    TextView type;

    View itemLayout;
    View btnMimeType;
    View btnSms;
    View btnCall;

    private ContactModel model;

    public ViewHolderContact(Context context, View v) {
        super(context, v);

        title = (TextView) v.findViewById(R.id.title);
        name = (TextView) v.findViewById(R.id.name);
        number = (TextView) v.findViewById(R.id.number);
        label = (TextView) v.findViewById(R.id.label);
        type = (TextView) v.findViewById(R.id.type);
        itemLayout= v.findViewById(R.id.item_layout);
        btnMimeType = v.findViewById(R.id.btn_mimeType);
        btnSms = v.findViewById(R.id.btn_sms);
        btnCall = v.findViewById(R.id.btn_call);

        itemLayout.setOnClickListener(this);
        btnSms.setOnClickListener(this);
        btnCall.setOnClickListener(this);
        btnMimeType.setOnClickListener(this);

        itemLayout.setOnLongClickListener(this);
    }

    @Override
    public void bindView(IModel m, boolean showTitle) {
        model = (ContactModel) m;
        ContactStruct struct = model.struct;

        title.setVisibility(true == showTitle ? View.VISIBLE : View.GONE);
        if (model.type == ContactModel.TYPE_MULTI_OTHER) {
            name.setVisibility(View.GONE);
        } else {
            name.setText(struct.displayName);
            name.setVisibility(View.VISIBLE);
        }
        number.setText(struct.phoneNumber);

        if (model.type != ContactModel.TYPE_SINGLE) {
            if (!TextUtils.isEmpty(struct.numberLocationInfo)) {
                type.setText(struct.numberLocationInfo);
                type.setVisibility(View.VISIBLE);
            } else {
                type.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(struct.phoneLabel)) {
                label.setText(struct.phoneLabel);
                label.setVisibility(View.VISIBLE);
            } else {
                label.setVisibility(View.GONE);
            }
        } else {
            type.setVisibility(View.GONE);
            label.setVisibility(View.GONE);
        }

        if (SaraConstant.WEIXIN_MIMETYPE.equals(struct.mimeType)) {
            btnMimeType.setVisibility(View.VISIBLE);
        } else {
            btnMimeType.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(struct.phoneNumber)) {
            btnSms.setVisibility(View.VISIBLE);
            btnCall.setVisibility(View.VISIBLE);
        } else {
            btnSms.setVisibility(View.GONE);
            btnCall.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sms:
               smsContact(model.struct.phoneNumber);
                break;
            case R.id.btn_call:
                boolean isCall = !SaraUtils.isKeyguardSecureLocked();
                SaraUtils.dial(mContext, model.struct.phoneNumber, true, isCall);
                break;
            case R.id.item_layout:
                isCall = !SaraUtils.isKeyguardSecureLocked();
                SaraUtils.dial(mContext, model.struct.phoneNumber, false, isCall);
                break;
            case R.id.btn_mimeType:
                enterTencentMmChatting(model.struct.dataId,SaraConstant.WEIXIN_MIMETYPE);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.item_layout:
                shareContact() ;
                return true;
        }
        return false;
    }
    public void shareContact() {
        final ContactStruct contact = model.struct;
        if (contact == null) {
            return;
        }

        String number = contact.phoneNumber;
        if (TextUtils.isEmpty(number))
            return;
        number = number.replaceAll(" ", "");
        number = number.replaceAll("-", "");
        final String content = number;
        ArrayList<String> menuItems = new ArrayList<String>();
        ArrayList<View.OnClickListener> listeners = new ArrayList<View.OnClickListener>();
        menuItems.add(mResources.getString(R.string.view_contact_detail));
        menuItems.add(mResources.getString(R.string.copy_text));
        menuItems.add(mResources.getString(R.string.btn_share_contact_text));
        menuItems.add(mResources.getString(R.string.btn_share_contact_vcf));
        listeners.add(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterContactDetail(contact.contactId);
            }
        });
        listeners.add(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item dataItem = new ClipData.Item(content);
                ClipData data = new ClipData(content,
                        new String[] { ClipDescription.MIMETYPE_TEXT_URILIST },
                        dataItem);
                cm.setPrimaryClip(data);
                Toast.makeText(mContext, R.string.toast_text_copied,
                        Toast.LENGTH_SHORT).show();
            }
        });
        listeners.add(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                setShareTextContact();
            }
        });
        listeners.add(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShareContactVcf(contact);
            }
        });
        final MenuDialog dialog = new MenuDialog(mContext);
        dialog.setTitle(R.string.more_action);
        dialog.setAdapter(new MenuDialogListAdapter(mContext, menuItems, listeners));
        dialog.show();
    }

    public void setShareTextContact() {
        try {
            String name = model.struct.displayName;
            if (TextUtils.isEmpty(name)) {
                name = mContext.getString(R.string.missing_name);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(mContext.getString(R.string.full_name)).append("] ").append(name).append(";");
            for (ContactStruct struct : model.structs) {
                if (struct.contactId == model.struct.contactId) {
                    sb.append("\n[").append(struct.phoneLabel).append("] ").append(struct.phoneNumber).append(";");
                }
            }

            if (TextUtils.isEmpty(sb.toString())) {
                return;
            }
            String subject = mResources.getString(R.string.share_contact_subject_context, name);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            BubbleManager.markAddBubble2List(false);
            Intent chooseIntent = Intent.createChooser(intent, mResources.getText(R.string.menu_share), SaraUtils.getChooseIntentSender(mContext, null, false));
            chooseIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
            mContext.startActivity(chooseIntent);
        } catch (Exception e) {
            LogUtils.e(TAG, "Exception shareUri:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setShareContactVcf(ContactStruct contact) {
        if (contact == null)
            return;
        try {
            Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, SaraUtils.getLookupKey(mContext, contact.contactId));
            String displayName = TextUtils.isEmpty(contact.displayName) ? mResources.getString(R.string.missing_name) : contact.displayName;
            String subject = mResources.getString(R.string.share_contact_subject_context, displayName);
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(Contacts.CONTENT_VCARD_TYPE);
            intent.putExtra(Intent.EXTRA_STREAM, shareUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            final CharSequence chooseTitle = mResources.getText(R.string.menu_share);
            final Intent chooseIntent = Intent.createChooser(intent, chooseTitle, SaraUtils.getChooseIntentSender(mContext, null, false));
            chooseIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
            BubbleManager.markAddBubble2List(false);
            mContext.startActivity(chooseIntent);
        } catch (Exception ex) {
            Toast.makeText(mContext, R.string.share_error, Toast.LENGTH_SHORT).show();
            LogUtils.e(TAG, "Exception shareUri:" + ex.getMessage());
        }
    }

    public void enterContactDetail(long contactId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, contactId));
        SaraUtils.startActivity(mContext, intent);
    }

    public void smsContact(String number) {
        Uri uri = Uri.parse("sms:" + number.trim());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(uri);
        SaraUtils.startActivity(mContext, intent);
    }

    public void enterTencentMmChatting(String id, String mimeType) {
        Uri uri = Uri.parse(SaraConstant.URI_CONTACT_DATA + id);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.setComponent(new ComponentName(SaraConstant.WEIXIN_PACKAGE_NAME,
                                              PackageUtil.getCorrectClassName(mContext, SaraConstant.WEIXIN_CLASS_NAME)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SaraUtils.startActivity(mContext, intent);
    }

}
