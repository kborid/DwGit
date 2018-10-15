package com.smartisanos.sara.bullet.contact.holder;

import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bullet.contact.adapter.PickContactAdapter;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.contact.model.VoiceSearchResult;
import com.smartisanos.sara.bullet.widget.AvatarImageView;
import com.smartisanos.sara.bullet.widget.CircleImageView;
import com.smartisanos.sara.util.LogUtils;

import smartisanos.app.voiceassistant.ContactStruct;

import java.util.List;

public class ViewHolderContact extends ViewHolderBase {

    private static final float FONT_SIZE_IN_PICK_VIEW = 20.6F;
    private CircleImageView headImageView;
    private AvatarImageView avatarImageView;

    private TextView nameTextView;
    private DisplayImageOptions mDisplayImageOptions;

    public ViewHolderContact(View itemView) {
        super(itemView);
        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY)
                .showImageOnLoading(R.drawable.photo_default_in_phone).build();
    }

    protected void inflate(View view) {
        headImageView = (CircleImageView) view.findViewById(R.id.imageViewHeader);
        avatarImageView = (AvatarImageView) view.findViewById(R.id.imageViewHeader_phone);
        avatarImageView.setNameTextSize(FONT_SIZE_IN_PICK_VIEW);
        avatarImageView.setVisibility(View.GONE);
        nameTextView = (TextView) view.findViewById(R.id.textViewName);
    }

    @Override
    public void refreshView(PickContactAdapter adapter, AbsContactItem item) {
        if (item.getItemType() != AbsContactItem.ItemType.SEARCH) {
            int bgRes = item.isChecked() && adapter != null && adapter.hasCheckedEffect() ?
                    R.drawable.select_contact_bg_pressed : R.drawable.select_contact_bg_normal;
            itemView.setBackgroundResource(bgRes);
        }
        if (item instanceof ContactItem) {
            ContactItem contactItem = (ContactItem) item;
            String matchStr = getMatcherString(adapter, contactItem);
            matchStr = TextUtils.isEmpty(matchStr) ? "" : matchStr;
            LogUtils.d("ViewHolderContact refreshView() matchString = " + matchStr);
            nameTextView.setText(getSpannableString(contactItem.getContactName(), matchStr));

            if (ContactItem.MessageType.TEAM == contactItem.getMessageType()) {
                headImageView.setImageDrawable(headImageView.getContext().getDrawable(R.drawable.photo_default_in_team));
            } else if (ContactItem.MessageType.P2P == contactItem.getMessageType()) {
                headImageView.setImageDrawable(headImageView.getContext().getDrawable(R.drawable.photo_default_in_p2p));
            } else {
                headImageView.setImageDrawable(headImageView.getContext().getDrawable(R.drawable.photo_default_in_phone));
            }

            if (ContactItem.MessageType.PHONECONTACT == contactItem.getMessageType()) {
                return;
            }
            String uri = contactItem.getAvatarUri();
            if (!TextUtils.isEmpty(uri)) {
                if (!uri.startsWith("content://") && !uri.startsWith("http")) {
                    uri = "file://" + uri;
                }
                ImageLoader.getInstance().displayImage(uri, headImageView, mDisplayImageOptions);
            }
        }
    }

    private String getMatcherString(PickContactAdapter adapter, ContactItem contactItem) {
        if (null != adapter && null != adapter.getSearchText()) {
            Object searchText = adapter.getSearchText();
            if (null != searchText) {
                if (searchText instanceof String) {
                    return (String) searchText;
                } else if (searchText instanceof VoiceSearchResult) {
                    List<ContactStruct> contactStructList = ((VoiceSearchResult) searchText).getContactStruct();
                    if (null != contactStructList) {
                        for (ContactStruct contactStruct : contactStructList) {
                            if (!TextUtils.isEmpty(contactItem.getContactName()) && contactItem.getContactName().equals(contactStruct.getDisplayName())) {
                                return contactStruct.getMatchName();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private CharSequence getSpannableString(String originStr, String searchResult) {
        if (TextUtils.isEmpty(originStr) || TextUtils.isEmpty(searchResult)) {
            return originStr;
        }
        Spannable span = new SpannableString(originStr);
        originStr = originStr.toLowerCase();
        searchResult = searchResult.toLowerCase();
        int index = originStr.indexOf(searchResult);
        if (!TextUtils.isEmpty(searchResult) && index != -1) {
            span.setSpan(new ForegroundColorSpan(SaraApplication.getInstance().getResources().getColor(R.color.high_light_red)),
                    index, index + searchResult.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return span;
    }
}
