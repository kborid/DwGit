package com.smartisanos.ideapills.sync.share;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;

public class InvitationAdapter extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private List<SyncShareInvitation> mInvitationList = new ArrayList<>();
    private long mUserId;
    private DisplayImageOptions mDisplayOption;
    public InvitationAdapter(Context context, long uid) {
        mLayoutInflater = LayoutInflater.from(context);
        mUserId = uid;
        mDisplayOption = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(false)
                .imageScaleType(ImageScaleType.EXACTLY)
                .showImageOnLoading(R.drawable.sync_default_avatar)
                .showImageForEmptyUri(R.drawable.sync_default_avatar)
                .build();
    }

    public synchronized void setInvitationList(List<SyncShareInvitation> list) {
        mInvitationList.clear();
        if(list != null) {
            mInvitationList.addAll(list);
        }
    }

    public void setUserId(long id) {
        mUserId = id;
    }

    @Override
    public int getCount() {
        return mInvitationList != null ? mInvitationList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public SyncShareInvitation getItem(int position) {
        return mInvitationList.get(position);
    }

    public List<SyncShareInvitation> getData() {
        return mInvitationList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewholder = null;
        if (convertView == null || convertView.getTag() == null) {
            convertView = mLayoutInflater.inflate(R.layout.sync_invitation_item, null);
            viewholder = new ViewHolder();
            viewholder.rootView = convertView;
            viewholder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewholder.name = (TextView) convertView.findViewById(R.id.name);
            viewholder.status = (TextView) convertView.findViewById(R.id.status);
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }
        SyncShareInvitation item = getItem(position);
        viewholder.icon.setImageResource(R.drawable.sync_default_avatar);
        ImageLoader.getInstance().displayImage(mUserId == item.inviter.id ? item.invitee.avatar
                : item.inviter.avatar, viewholder.icon, mDisplayOption);
        viewholder.name.setText(getInvitationShowName(mUserId,item));
        int resId = getStatusString(item);
        if (resId > 0) {
            viewholder.status.setText(resId);
        }
        viewholder.rootView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_single);
        return convertView;
    }

    private String getInvitationShowName(long userId, SyncShareInvitation invitation) {
        if (invitation.inviter.id == userId) {
            if (TextUtils.isEmpty(invitation.inviter.remark)) {
                return invitation.invitee.getShowName();
            } else {
                return invitation.inviter.remark;
            }
        } else {
            if (TextUtils.isEmpty(invitation.invitee.remark)) {
                return invitation.inviter.getShowName();
            } else {
                return invitation.invitee.remark;
            }
        }
    }

    private int getStatusString(SyncShareInvitation invitation) {
        int resId = 0;
        switch (invitation.inviteStatus) {
            case SyncShareInvitation.INVITE_START:
                resId = mUserId == invitation.inviter.id ? R.string.sync_share_status_confirm : R.string.sync_share_status_confirm_invitee;
                break;
            case SyncShareInvitation.INVITE_ACCEPT:
                resId = R.string.sync_share_status_accept;
                break;
            case SyncShareInvitation.INVITE_DECLINE:
                resId = mUserId == invitation.inviter.id ? R.string.sync_share_status_other_refuse : R.string.sync_share_status_refuse;
                break;
            case SyncShareInvitation.INVITE_CANCEL:
                resId = R.string.sync_share_status_canceled;
                break;
        }
        return resId;
    }

    private static class ViewHolder {
        public View rootView;
        public ImageView icon;
        public TextView name;
        public TextView status;
    }
}
