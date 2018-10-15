package com.smartisanos.sara.bubble.revone.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.adapter.ContactAdapter;
import com.smartisanos.sara.bubble.revone.entity.GlobalContact;
import com.smartisanos.sara.bubble.revone.utils.Cubic;

import org.json.JSONObject;

import java.util.ArrayList;

public class ImBaseViewManager extends ViewManager {
    public static final int DEFAULT_COLUMN_FAVORITE = 2;
    public static final int DEFAULT_COLUMN_RECENT = 2;
    public static final int DRAG_MODE_COLUMN_FAVORITE = 3;
    public static final int DRAG_MODE_COLUMN_RECENT = 3;
    public static final String AUTHORITY = "content://com.bullet.messenger";
    public static final String METHOD_CONTENT_URI_ALL = "FLASHIM_ALL_CONTACT";
    public static final String METHOD_CONTENT_URI_RECENT = "FLASHIM_RECENT_CONTACT";
    public static final String METHOD_CONTENT_URI_STAR = "FLASHIM_STAR_CONTACT";
    public static final String KEY_CONTENT_ALL = "KEY_CONTENT_ALL";
    public static final String KEY_CONTENT_RECENT = "KEY_CONTENT_RECENT";
    public static final String KEY_CONTENT_STAR = "KEY_CONTENT_STAR";
    private static final int FALL_DOWN_ANIM_Y = -500;
    public static final int TYPE_CONTENT_ALL = 0;
    public static final int TYPE_CONTENT_RECENT = 1;
    public static final int TYPE_CONTENT_STAR = 2;
    protected AbsListView mListView;
    protected ContactAdapter mContactAdapter;
    protected OnItemClickListener mItemClickListener;
    protected int mTopPadding;
    protected int mBottomPadding;
    protected int mDividerBottomMargin;
    protected int mContactType = TYPE_CONTENT_ALL;
    protected boolean mDragMode;

    public ImBaseViewManager(Context context, View view, boolean mode) {
        super(context, view);
        mDragMode = mode;
        Resources res = context.getResources();
        mTopPadding = res.getDimensionPixelSize(mDragMode ? R.dimen.flash_im_drag_contact_content_margin_top : R.dimen.flash_im_contact_content_margin_top);
        mBottomPadding = res.getDimensionPixelSize(R.dimen.global_search_margin_bottom);
        mDividerBottomMargin = res.getDimensionPixelSize(R.dimen.flash_im_divider_bottom_margin);
        if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
            mTopPadding += ExtScreenConstant.STATUS_BAR_HEIGHT;
            mBottomPadding += ExtScreenConstant.NAVIGATION_BAR_HEIGHT;
        }
        setAnimationEnabled(false);
    }

    @Override
    public void show() {
        if (mView == null) {
            mView = getView();
        }
        mView.setVisibility(View.VISIBLE);
    }

    @Override
    protected View getView() {
        return null;
    }

    protected void loadData(int type) {
        mContactType = type;
        switch (type) {
            case TYPE_CONTENT_ALL:
                new SearchTask().execute(METHOD_CONTENT_URI_ALL, KEY_CONTENT_ALL);
                break;
            case TYPE_CONTENT_RECENT:
                new SearchTask().execute(METHOD_CONTENT_URI_RECENT, KEY_CONTENT_RECENT);
                break;
            case TYPE_CONTENT_STAR:
                new SearchTask().execute(METHOD_CONTENT_URI_STAR, KEY_CONTENT_STAR);
                break;
        }
    }

    private void showFallDownAnimtion() {
        final View content = mView.findViewById(R.id.contact_content);
        AnimatorSet animSet = new AnimatorSet();
        int delay = getAnimDelay(mContactType);
        TimeInterpolator interpolator = Cubic.easeOut;
        ObjectAnimator transY = ObjectAnimator.ofFloat(content, "translationY", FALL_DOWN_ANIM_Y, 0);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mView, "alpha", 0f, 1f);
        alpha.setDuration(300);
        transY.setDuration(300);
        alpha.setInterpolator(interpolator);
        transY.setInterpolator(interpolator);
        alpha.setStartDelay(delay);
        transY.setStartDelay(delay);
        alpha.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animSet.playTogether(alpha, transY);
        animSet.start();
    }

    protected void onContactDataChange(ArrayList<GlobalContact> contacts) {
        mContactAdapter.updateContactsList(contacts);
        showFallDownAnimtion();
    }

    public View findFocusChild(int x, int y) {
        if (mListView != null) {
            Rect rect = new Rect();
            mListView.getGlobalVisibleRect(rect);
            if (rect.contains(x, y)) {
                int position = mListView.pointToPosition(x - rect.left, y - rect.top);
                int index = position - mListView.getFirstVisiblePosition();
                if (position >= 0 && mListView.getChildCount() > index) {
                    return mListView.getChildAt(index);
                }
            }
        }
        return null;
    }

    public void setItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    private class SearchTask extends AsyncTask<String, Void, ArrayList<GlobalContact>> {

        @Override
        protected ArrayList<GlobalContact> doInBackground(String... params) {
            String method = params[0];
            String key = params[1];
            if (params == null || params.length <= 1)
                return null;
            return loadContanctFromFlashIm(mContext, method, key);
        }

        @Override
        protected void onPostExecute(ArrayList<GlobalContact> result) {
            super.onPostExecute(result);
            onContactDataChange(result);
        }
    }

    private static int getAnimDelay(int type) {
        int delay = 0;
        switch (type) {
            case TYPE_CONTENT_ALL:
                delay = 10;
                break;
            case TYPE_CONTENT_STAR:
                delay = 5;
                break;
        }
        return delay;
    }

    public static ArrayList<GlobalContact> loadContanctFromFlashIm(Context context, String method, String key) {

        if (context == null) {
            return null;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            Bundle bundle = resolver.call(Uri.parse(AUTHORITY), method, null, null);
            if (bundle != null) {
                ArrayList<String> stringBubbles = bundle.getStringArrayList(key);
                ArrayList<GlobalContact> globalContacts = new ArrayList<>();
                for (String bubble : stringBubbles) {
                    JSONObject jsonObject = new JSONObject(bubble);
                    globalContacts.add(GlobalContact.toGlobalContact(jsonObject));
                }

                return globalContacts;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(view, mContactAdapter.getItem(position));
            }
        }
    };

    public interface OnItemClickListener {
        void onItemClick(View v, GlobalContact contact);
    }
}
