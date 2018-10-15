package com.smartisanos.sara.bubble.revone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.smartisanos.sara.bubble.revone.widget.AvatarImageView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.entity.GlobalContact;
import com.smartisanos.sara.bubble.revone.widget.ImContactLayout;

import java.util.HashMap;
import java.util.List;

public class ContactAdapter extends BaseAdapter {
    private Context mContext;
    private List<GlobalContact> mContactsList;
    protected final HashMap<String, Integer> indexes = new HashMap<>();
    private int mLayoutId;

    public ContactAdapter(Context context) {
        mContext = context;
    }

    public ContactAdapter(Context context, int resId) {
        mContext = context;
        mLayoutId = resId;
    }

    public ContactAdapter(Context context, List<GlobalContact> contacts, int resId) {
        mContext = context;
        mLayoutId = resId;
        mContactsList = contacts;
    }

    public void updateContactsList(List<GlobalContact> contacts) {
        mContactsList = contacts;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            int resId = mLayoutId > 0 ? mLayoutId : R.layout.revone_contacts_item;
            convertView = LayoutInflater.from(mContext).inflate(resId, parent, false);
        }
        GlobalContact contact = mContactsList.get(position);
        ImContactLayout contactLayout = (ImContactLayout) convertView;
        contactLayout.setContact(contact);
        AvatarImageView avatar = (AvatarImageView) contactLayout.findViewById(R.id.avatar);
        avatar.setDefaultAvatarId(contact.getMessageType() == GlobalContact.MESSAGE_TYPE_P2P
                ? R.drawable.revone_flash_im_default_avatar
                : R.drawable.revone_contact_team_icon);
        ImageLoader.getInstance().displayImage(contact.getAvatarUri(), avatar);
        TextView name = (TextView) contactLayout.findViewById(R.id.contact);
        name.setText(contact.getContactName());
        return convertView;
    }

    @Override
    public int getCount() {
        return mContactsList != null ? mContactsList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public GlobalContact getItem(int pos) {
        if (mContactsList != null && pos < mContactsList.size()) {
            return mContactsList.get(pos);
        }
        return null;
    }

    public List<GlobalContact> getAllContacts() {
        return mContactsList;
    }

    public HashMap<String, Integer> getIndexes() {
        return indexes;
    }
}