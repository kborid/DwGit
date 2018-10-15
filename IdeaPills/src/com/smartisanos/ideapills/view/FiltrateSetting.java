package com.smartisanos.ideapills.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.service.onestep.GlobalBubble;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.sync.SyncProcessor;

import static android.os.Build.VERSION.SDK_INT;


public class FiltrateSetting {
    public static final String FILTRATE_COLOR = "filtrate_color";
    public static final int FILTRATE_ALL = 0;
    private Context mContext;
    private PopupWindow mPopupWindow;
    private View mContentView;
    private ImageView mIvArrow;
    private int mPopupWidth;
    private int mPopupHeight;
    private OnFiltrateChangeListener mOnFiltrateChangeListener;
    private int mColorFilter = -1;
    private int mWindowMarginY = 0;

    public FiltrateSetting(Context context) {
        mContext = context;
        mWindowMarginY = context.getResources().getDimensionPixelSize(R.dimen.bubble_filtrate_window_margin_y);
    }

    public void showSettingWindow(View anchor, int endX) {
        if (anchor == null) {
            return;
        }
        if (mContext == null) {
            mContext = anchor.getContext();
        }
        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow(mContext);
            mContentView = LayoutInflater.from(mContext).inflate(R.layout.bubble_head_filter, null);
            mIvArrow = (ImageView) mContentView.findViewById(R.id.iv_arrow);
            mContentView.findViewById(R.id.filtrate_all).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_blue).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_red).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_orange).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_green).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_purple).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_share).setOnClickListener(mOnClickListener);
            mContentView.findViewById(R.id.filtrate_ppt).setOnClickListener(mOnClickListener);
            mPopupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setContentView(mContentView);
            mPopupWindow.setAnimationStyle(R.style.RemindPopupAnim);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(SDK_INT >= 23 ? null : new ColorDrawable(Color.TRANSPARENT));
            mPopupWindow.setFocusable(true);
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mContentView.measure(measureSpec, measureSpec);
            mPopupWidth = mContentView.getMeasuredWidth();
            mPopupHeight = mContentView.getMeasuredHeight();
        }
        int loc[] = new int[2];
        anchor.getLocationInWindow(loc);
        int backgroundRes = R.drawable.popup_filtrate_up_bg;
        int arrowRes = R.drawable.popup_filtrate_arrow_up;
        int positionY = loc[1] - mPopupHeight - mWindowMarginY;
        int arrowGravity = Gravity.BOTTOM;
        if (mPopupHeight > loc[1]) {
            arrowRes = R.drawable.popup_filtrate_arrow_down;
            positionY = loc[1] + anchor.getHeight() + mWindowMarginY;
            backgroundRes = R.drawable.popup_filtrate_down_bg;
            arrowGravity = Gravity.TOP;
        }
        mContentView.setBackgroundResource(backgroundRes);
        Drawable arrow = mContext.getDrawable(arrowRes);
        mIvArrow.setImageDrawable(arrow);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mIvArrow.getLayoutParams();
        lp.gravity = arrowGravity;
        lp.leftMargin = mPopupWidth - (endX - loc[0] - anchor.getWidth() / 2) - arrow.getIntrinsicWidth() / 2;
        mIvArrow.setLayoutParams(lp);
        mPopupWindow.showAtLocation(anchor, Gravity.TOP | Gravity.LEFT, endX - mPopupWidth, positionY);
    }

    public void hideSettingWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    public void setFiltrateChangeListener(OnFiltrateChangeListener listener) {
        mOnFiltrateChangeListener = listener;
    }

    public static int getFiltrateDrawableRes(int color) {
        int resId = R.drawable.filtrate_all;
        switch (color) {
            case GlobalBubble.COLOR_RED:
                resId = R.drawable.filtrate_red;
                break;
            case GlobalBubble.COLOR_ORANGE:
                resId = R.drawable.filtrate_orange;
                break;
            case GlobalBubble.COLOR_GREEN:
                resId = R.drawable.filtrate_green;
                break;
            case GlobalBubble.COLOR_BLUE:
                resId = R.drawable.filtrate_blue;
                break;
            case GlobalBubble.COLOR_PURPLE:
                resId = R.drawable.filtrate_purple;
                break;
            case GlobalBubble.COLOR_SHARE:
                resId = R.drawable.filtrate_share;
                break;
            case GlobalBubble.COLOR_NAVY_BLUE:
                resId = R.drawable.filtrate_ppt;
                break;
        }
        return resId;
    }

    public int getFiltrateColor() {
        if (mColorFilter == -1) {
            mColorFilter = getFiltrateColor(mContext);
        }
        return mColorFilter;
    }

    public static int getFiltrateColor(Context context) {
        int color = FILTRATE_ALL;
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
            color = sp.getInt(FILTRATE_COLOR, FILTRATE_ALL);
        }
        return color;
    }

    public void restoreFiltrateColor() {
        mColorFilter = FILTRATE_ALL;
        if (mContext != null) {
            SharedPreferences sp = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(FILTRATE_COLOR, FILTRATE_ALL);
            editor.commit();
        }
    }

    private void saveFiltrateColor(Context context, int color) {
        mColorFilter = color;
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(FILTRATE_COLOR, color);
            editor.commit();
        }
        if (mOnFiltrateChangeListener != null) {
            mOnFiltrateChangeListener.onFiltrateChange(color);
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            int color = FILTRATE_ALL;
            mPopupWindow.dismiss();
            switch (id) {
                case R.id.filtrate_blue:
                    color = GlobalBubble.COLOR_BLUE;
                    break;
                case R.id.filtrate_red:
                    color = GlobalBubble.COLOR_RED;
                    break;
                case R.id.filtrate_orange:
                    color = GlobalBubble.COLOR_ORANGE;
                    break;
                case R.id.filtrate_green:
                    color = GlobalBubble.COLOR_GREEN;
                    break;
                case R.id.filtrate_purple:
                    color = GlobalBubble.COLOR_PURPLE;
                    break;
                case R.id.filtrate_share:
                    if (SyncProcessor.canShare(true)) {
                        saveFiltrateColor(mContext, GlobalBubble.COLOR_SHARE);
                    }
                    return;
                case R.id.filtrate_ppt:
                    color = GlobalBubble.COLOR_NAVY_BLUE;
                    break;
            }
            saveFiltrateColor(mContext, color);
        }
    };

    public interface OnFiltrateChangeListener {
        void onFiltrateChange(int color);
    }
}