package com.smartisanos.sara.bubble.revone.view;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.FlashImActivity;
import com.smartisanos.sara.bubble.revone.adapter.ContactAdapter;
import com.smartisanos.sara.bubble.revone.entity.GlobalContact;
import com.smartisanos.sara.bullet.util.PinYinUtils;
import com.smartisanos.sara.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ImVoiceViewManager extends ViewManager {
    private StringBuilder mContact;
    private View mVoiceButton;
    private View mWaveView;
    private View mEmptyView;
    private ListView mListView;
    private ContactAdapter mContactAdapter;
    private ImBaseViewManager.OnItemClickListener mItemClickListener;
    private View mPopWindowContent;
    private int mWindowWidth;
    private int mScreenHeight;

    public ImVoiceViewManager(Context context, View view) {
        super(context, view);
        mWindowWidth = mContext.getResources().getDimensionPixelSize(R.dimen.popup_window_width);
        Point point = new Point();
        context.getDisplay().getSize(point);
        mScreenHeight = point.y;
    }

    @Override
    protected View getView() {
        View view = null;
        if (mRootView != null) {
            ViewStub containerStub = (ViewStub) mRootView.findViewById(R.id.im_voice_container_stub);
            if (containerStub != null && containerStub.getParent() != null) {
                view = containerStub.inflate();
            } else {
                view = mRootView.findViewById(R.id.im_voice_container);
            }
            CommonUtils.setAlwaysCanAcceptDragForAll(view, true);
            if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                lp.bottomMargin += ExtScreenConstant.NAVIGATION_BAR_HEIGHT;
            }
            mWaveView = view.findViewById(R.id.wave);
            mVoiceButton = view.findViewById(R.id.voice);
            mVoiceButton.setOnTouchListener(mVoiceTouchListener);
        }
        return view;
    }

    @Override
    public void show() {
        setFadeInAnimDelay(250);
        super.show();
    }

    public void handleTouchEvent(MotionEvent ev) {
        if (mPopWindowContent != null) {
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            Rect rect = new Rect();
            mPopWindowContent.getGlobalVisibleRect(rect);
            if (!rect.contains(x, y)) {
                hidePopupWindow();
            }
        }
    }

    public void hidePopupWindow() {
        if (mPopWindowContent != null) {
            mPopWindowContent.setVisibility(View.GONE);
        }
    }

    private void startRecognize() {
        startRecordWithAnimation(mWaveView);
        mContact = new StringBuilder();
        FlashImActivity activity = (FlashImActivity) mContext;
        activity.startContactRecognize();
    }

    private void stopRecognize() {
        FlashImActivity activity = (FlashImActivity) mContext;
        activity.stopContactRecognize();
    }

    public void setItemClickListener(ImBaseViewManager.OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    private void showFlashImResult() {
        if (mPopWindowContent == null) {
            mPopWindowContent = LayoutInflater.from(mView.getContext()).inflate(R.layout.revone_flash_im_contact_result, null);
            mPopWindowContent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent ev) {
                    return true;
                }
            });
            CommonUtils.setAlwaysCanAcceptDragForAll(mPopWindowContent, true);
            mEmptyView = mPopWindowContent.findViewById(R.id.empty);
            mListView = (ListView) mPopWindowContent.findViewById(R.id.list_view);
            mListView.setOnItemClickListener(mOnItemClickListener);
            mListView.setEmptyView(mEmptyView);
            mContactAdapter = new ContactAdapter(mContext, R.layout.revone_search_contacts_item);
            mListView.setAdapter(mContactAdapter);
            int loc[] = new int[2];
            loc = mVoiceButton.getLocationOnScreen();
            int y = mScreenHeight - loc[1];
            FrameLayout allContactContent = (FrameLayout) mRootView.findViewById(R.id.all_contact_container);
            FrameLayout.LayoutParams lp = new LayoutParams(mWindowWidth, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = y;
            allContactContent.addView(mPopWindowContent, lp);
        }
        FlashImActivity activity = (FlashImActivity) mContext;
        List<GlobalContact> contacts = filterContacts(activity.getFlashImContact());
        mContactAdapter.updateContactsList(contacts);
        if (mPopWindowContent.getVisibility() != View.VISIBLE) {
            mPopWindowContent.setVisibility(View.VISIBLE);
        }
    }

    private List<GlobalContact> filterContacts(List<GlobalContact> allContacts) {
        if (allContacts != null && mContact != null) {
            String filter = PinYinUtils.getPinYin(mContact.toString());
            if (!TextUtils.isEmpty(filter)) {
                List<GlobalContact> filterContacts = new ArrayList<>();
                for (GlobalContact contact : allContacts) {
                    if (contact.getPinyin().contains(filter)) {
                        filterContacts.add(contact);
                    }
                }
                return filterContacts;
            }
        }
        return null;
    }

    public View findFocusChild(int x, int y) {
        if (mPopWindowContent != null && mPopWindowContent.getVisibility() == View.VISIBLE && mListView != null) {
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

    public void onError(int errorCode) {
        stopRecordWithAnimation(mWaveView);
    }

    public void onResultRecived(String resultStr) {
        if (mContact != null) {
            mContact.append(resultStr);
            if (mContact.length() >= 1 && StringUtils.isPunctuation(mContact.charAt(mContact.length() - 1))) {
                mContact.deleteCharAt(mContact.length() - 1);
            }
        }
        showFlashImResult();
    }

    public void onPartialResult(String partialResult) {
        if (mContact != null) {
            mContact.append(partialResult);
        }
    }

    public void onRecordStart() {

    }

    public void onRecordEnd() {
        stopRecordWithAnimation(mWaveView);
    }

    public static void startRecordWithAnimation(final View view) {
        if (view != null) {
            AnimationSet set = new AnimationSet(true);
            float random = (float) (1.0 + 0.06 * Math.random());

            Animation anim = new ScaleAnimation(0.95f, random, 0.95f, random, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(80);

            Animation anim1 = new ScaleAnimation(random, 0.95f, random, 0.95f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim1.setStartOffset(80);
            anim1.setDuration(100);
            anim1.setInterpolator(new LinearInterpolator());

            set.addAnimation(anim);
            set.addAnimation(anim1);
            set.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationEnd(Animation animation) {
                    startRecordWithAnimation(view);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
            view.setVisibility(View.VISIBLE);
            view.startAnimation(set);
        }
    }

    public static void stopRecordWithAnimation(final View view) {
        if (view != null && view.getVisibility() != View.GONE) {
            Animation anim = new ScaleAnimation(1.05f, 1f, 1.05f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }
            });
            view.startAnimation(anim);
        }
    }

    private View.OnTouchListener mVoiceTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent ev) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    view.setPressed(true);
                    startRecognize();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.setPressed(false);
                    stopRecognize();
                    break;
            }
            return true;
        }
    };

    AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(view, mContactAdapter.getItem(position));
            }
        }
    };
}
