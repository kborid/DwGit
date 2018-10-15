package com.smartisanos.sara.widget;

import java.util.List;

import android.app.SmtPCUtils;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import smartisanos.app.numberassistant.YellowPageResult;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.VoiceSearchResultAdapter;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolderApp;

import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.MediaStruct;

public class LocalSearchLayout extends FrameLayout {
    private String TAG = "VoiceAss.LocalSearchLayout";
    private VoiceSearchResultAdapter mAdapter;
    private ListView mResultList = null;
    private ImageView mResultHeadContact;
    private ImageView mResultHeadApp;
    private ImageView mResultHeadMusic;
    private ImageView mResultHide;
    private TextView mEmptyTip;
    private String[] resultList;
    private DisplayImageOptions mDisplayImageOptions;
    private SaraUtils.BaseViewListener mListener;
    public static final int EXPAND_NONE = 0;
    public static final int EXPAND_MEDIUM = 1;
    public static final int EXPAND_FULL = 2;

    public LocalSearchLayout(Context context) {
        super(context);
    }

    public LocalSearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LocalSearchLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(Context activityContext) {
        initView();
        mAdapter = new VoiceSearchResultAdapter(activityContext);
        mResultList.setAdapter(mAdapter);
        SaraApplication.getInstance().getAppImageLoader().clearCache();
    }

    public void setViewListener(SaraUtils.BaseViewListener listener){
        mListener = listener;
    }

    public void destroy() {
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
    }

    public int getExpandType() {
        int viewcount = mResultList.getLastVisiblePosition() - mResultList.getFirstVisiblePosition();
        int totalcount = mAdapter.getCount();
        if (totalcount > viewcount) {
            if (totalcount > viewcount * 2) {
                return EXPAND_FULL;
            }
            return EXPAND_MEDIUM;
        }
        return EXPAND_NONE;
    }

    public void switchExtDisplay(boolean isExtDisplay) {
        mEmptyTip.setText("");
        if (isExtDisplay) {
            RoundedRectLinearLayout roundBg = (RoundedRectLinearLayout) findViewById(R.id.round_bg);
            if (roundBg != null) {
                roundBg.setRadiusMargin(0, 0, 0, 0);
                roundBg.setPadding(0, 0, 0, 0);
                roundBg.setBackgroundResource(R.drawable.revone_rounded_rectangle_bg);
                MarginLayoutParams bgLp = (MarginLayoutParams) roundBg.getLayoutParams();
                bgLp.topMargin = 0;
                bgLp.leftMargin = 0;
                bgLp.rightMargin = 0;
                bgLp.bottomMargin = 0;
                MarginLayoutParams listLp = (MarginLayoutParams) mResultList.getLayoutParams();
                listLp.leftMargin = 0;
                listLp.rightMargin = 0;
                MarginLayoutParams titleLp = (MarginLayoutParams) findViewById(R.id.title_layout).getLayoutParams();
                titleLp.leftMargin = 0;
                titleLp.rightMargin = 0;
                titleLp.height = getCaptionViewHeight();
                MarginLayoutParams bottomLp = (MarginLayoutParams) findViewById(R.id.bottom_mask).getLayoutParams();
                bottomLp.leftMargin = 0;
                bottomLp.rightMargin = 0;
                bottomLp.bottomMargin = 0;
            }
        }
    }

    private int getCaptionViewHeight() {
        Display display = mContext.getDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int captionViewHeight = SmtPCUtils.getSmtCaptionBarPixelHeight(mContext);
        int calibration = mContext.getResources().getDimensionPixelOffset(R.dimen.caption_view_height_calibration);
        int density = SystemProperties.getInt("persist.sys.search-window-density", 120);
        if (metrics.densityDpi >= 240) {
            density = 192;
        }
        return (int) (captionViewHeight * density / (float) metrics.densityDpi) + calibration;
    }

    public void initView() {
        mResultHeadContact = (ImageView) findViewById(R.id.result_header_contact);
        mResultHeadApp = (ImageView) findViewById(R.id.result_header_app);
        mResultHeadMusic = (ImageView) findViewById(R.id.result_header_music);
        mResultHide = (ImageView) findViewById(R.id.local_result_hide);
        mResultList = (ListView) findViewById(R.id.result_list_view);
        mResultList.setVerticalScrollBarEnabled(false);
        mResultList.setSmoothScrollbarEnabled(true);
        mEmptyTip = (TextView) findViewById(R.id.result_list_empty);
        mResultList.setEmptyView(mEmptyTip);
        View v = new View(mContext);
        int footerHeight = mContext.getResources().getDimensionPixelOffset(R.dimen.bubble_result_footer_height);
        v.setLayoutParams(new AbsListView.LayoutParams(1, footerHeight));
        mResultList.addFooterView(v);
        mResultHeadContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int position = mAdapter
                        .getCurrentPosition(VoiceSearchResultAdapter.TYPE_CONTACT);
                if (position != -1) {
                    mResultList.setSelection(position);
                }
            }
        });
        mResultHeadApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int position = mAdapter
                        .getCurrentPosition(VoiceSearchResultAdapter.TYPE_APP);
                if (position != -1) {
                    mResultList.setSelection(position);
                }
            }
        });
        mResultHeadMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int position = mAdapter
                        .getCurrentPosition(VoiceSearchResultAdapter.TYPE_MUSIC);
                if (position != -1) {
                    mResultList.setSelection(position);
                }
            }
        });
        mResultHide.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mResultHide.setVisibility(View.GONE);
                if (mListener != null) {
                    mListener.hideView(1, null, false, false);
                }
            }
        });
    }

    public void notifyDataSetChanged() {
        if (mAdapter == null) {
            return;
        }
        mAdapter.notifyDataSetChanged();
        updateHeadView();
    }

    public VoiceSearchResultAdapter getAdapter() {
        return mAdapter;
    }

    public void setData(List<ApplicationStruct> app,
            List<ContactStruct> contact, List<YellowPageResult> yellow,
            List<MediaStruct> music) {
        if (mAdapter != null) {
            mAdapter.setData(app, contact, yellow, music);
            updateHeadView();
        }
    }

    public boolean hasData() {
        if (mAdapter != null) {
            return mAdapter.getCount() > 0;
        }
        return false;
    }

    public void showHideView() {
        mResultHide.setVisibility(View.VISIBLE);
    }

    public void hideHideView() {
        mResultHide.setVisibility(View.GONE);
    }

    private void updateHeadView() {
        if (mAdapter.isViewVisibleByType(VoiceSearchResultAdapter.TYPE_APP)) {
            mResultHeadApp.setVisibility(View.VISIBLE);
        } else {
            mResultHeadApp.setVisibility(View.GONE);
        }
        if (mAdapter.isViewVisibleByType(VoiceSearchResultAdapter.TYPE_CONTACT)) {
            mResultHeadContact.setVisibility(View.VISIBLE);
        } else {
            mResultHeadContact.setVisibility(View.GONE);
        }
        if (mAdapter.isViewVisibleByType(VoiceSearchResultAdapter.TYPE_MUSIC)) {
            mResultHeadMusic.setVisibility(View.VISIBLE);
        } else {
            mResultHeadMusic.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        BubbleManager.markAddBubble2List(false);
        return super.onInterceptTouchEvent(ev);
    }

    public void setAppStartListener(ViewHolderApp.IAppStartListener listener) {
        if (mAdapter != null) {
            mAdapter.setAppStartListener(listener);
        }
    }

    public void reset() {
        if (mResultList != null && mAdapter != null) {
            mResultList.setAdapter(mAdapter);
            mResultList.setSelectionAfterHeaderView();
        }
    }
}
