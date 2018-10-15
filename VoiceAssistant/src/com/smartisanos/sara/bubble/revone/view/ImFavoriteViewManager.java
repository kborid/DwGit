package com.smartisanos.sara.bubble.revone.view;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewStub;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.adapter.ContactAdapter;

public class ImFavoriteViewManager extends ImBaseViewManager {
    public ImFavoriteViewManager(Context context, View view, boolean mode) {
        super(context, view, mode);
    }

    @Override
    protected View getView() {
        View view = null;
        if (mRootView != null) {
            ViewStub contentStup = (ViewStub) mRootView.findViewById(R.id.favorite_contact_container_stub);
            if (contentStup != null && contentStup.getParent() != null) {
                view = contentStup.inflate();
            } else {
                view = mRootView.findViewById(R.id.favorite_contact_container);
            }
            CommonUtils.setAlwaysCanAcceptDragForAll(view, true);
            View divider = view.findViewById(R.id.divider);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) divider.getLayoutParams();
            lp.topMargin = mTopPadding;
            lp.bottomMargin = mBottomPadding + mDividerBottomMargin;
            divider.setLayoutParams(lp);
            View contactContent = view.findViewById(R.id.contact_content);
            contactContent.setPadding(0, mTopPadding, 0, mBottomPadding);
            GridView gridView = (GridView) view.findViewById(R.id.list_view);
            gridView.setNumColumns(mDragMode ? ImBaseViewManager.DRAG_MODE_COLUMN_FAVORITE : ImBaseViewManager.DEFAULT_COLUMN_FAVORITE);
            Resources res = mContext.getResources();
            int hSpacingRes = mDragMode ? R.dimen.flash_im_contact_drag_item_divider_x : R.dimen.flash_im_contact_item_divider_x;
            gridView.setHorizontalSpacing(res.getDimensionPixelSize(hSpacingRes));
            gridView.setVerticalSpacing(res.getDimensionPixelSize(R.dimen.flash_im_contact_item_divider_y));
            mContactAdapter = new ContactAdapter(mContext);
            gridView.setOnItemClickListener(mOnItemClickListener);
            gridView.setAdapter(mContactAdapter);
            loadData(ImBaseViewManager.TYPE_CONTENT_STAR);
            mListView = gridView;
        }
        return view;
    }
}
