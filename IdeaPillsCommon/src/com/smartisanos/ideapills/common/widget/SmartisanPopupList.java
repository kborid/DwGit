package com.smartisanos.ideapills.common.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.smartisanos.ideapills.common.R;

public class SmartisanPopupList {
    private final int mArrowMargin;
    private Context mContext;
    private View mAnchorView;

    private ViewGroup mContentContainer;
    private LinearLayout mMainPanel;
    private LinearLayout.LayoutParams mWrapContent;
    private ImageView mIndicatorView;

    private int mScreenHeight;
    private int mScreenWidth;
    private int mPopupWindowWidth;
    private int mPopupWindowHeight;
    private int mIndicatorWidth;
    private int mIndicatorHeight;

    private PopupItemListener mPopupItemListener;
    private PopupWindow mPopupWindow;
    private final View.OnClickListener mItemButtonOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getTag() instanceof Integer) {
                        if (mPopupItemListener != null) {
                            mPopupItemListener.onPopupItemClick((Integer) v.getTag());
                        }
                    }
                    hidePopupListWindow();
                }
            };
    private List<String> mPopupItemList;
    private boolean mDisabled;

    public SmartisanPopupList(Context context) {
        mContext = context;
        mArrowMargin = mContext.getResources().getDimensionPixelSize(
                R.dimen.smartisan_floating_popup_menu_arrow_vertical_margin);
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
    }

    public void disable(boolean disabled) {
        mDisabled = disabled;
    }

    public void bind(View anchorView, List<String> popupItemList, PopupItemListener popupItemListener) {
        this.mAnchorView = anchorView;
        this.mPopupItemList = popupItemList;
        this.mPopupItemListener = popupItemListener;
        this.mPopupWindow = null;

        if (mAnchorView instanceof AbsListView) {
            ((AbsListView) mAnchorView).setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (!mDisabled) {
                        showPopupWindow();
                    }
                    return true;
                }
            });
        } else {
            mAnchorView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    if (!mDisabled) {
                        showPopupWindow();
                    }
                    return true;
                }
            });
        }
    }

    private void showPopupWindow() {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            return;
        }

        if (mPopupWindow == null) {
            updateResources();
            mMainPanel = createMainPanel();
            mContentContainer = createContentContainer(mMainPanel);
            layoutItems(mPopupItemList, mPopupItemListener);
            mPopupWindowWidth = getViewWidth(mContentContainer);
            if (mIndicatorView != null && mIndicatorWidth == 0) {
                if (mIndicatorView.getLayoutParams().width > 0) {
                    mIndicatorWidth = mIndicatorView.getLayoutParams().width;
                } else {
                    mIndicatorWidth = getViewWidth(mIndicatorView);
                }
            }
            if (mIndicatorView != null && mIndicatorHeight == 0) {
                if (mIndicatorView.getLayoutParams().height > 0) {
                    mIndicatorHeight = mIndicatorView.getLayoutParams().height;
                } else {
                    mIndicatorHeight = getViewHeight(mIndicatorView);
                }
            }
            if (mPopupWindowHeight == 0) {
                mPopupWindowHeight = getViewHeight(mContentContainer);
            }
            mPopupWindow = createPopupWindow(mContentContainer);
        }

        int location[] = new int[2];
        mAnchorView.getLocationInWindow(location);
        final int centerX = location[0] + mAnchorView.getWidth() / 2;
        if (mIndicatorView != null) {
            float marginLeftScreenEdge = centerX;
            float marginRightScreenEdge = mScreenWidth - centerX;
            if (marginLeftScreenEdge < mPopupWindowWidth / 2f) {
                // in case of the draw of indicator out of corner's bounds
                if (marginLeftScreenEdge < mIndicatorWidth / 2f) {
                    mIndicatorView.setTranslationX(mIndicatorWidth / 2f - mPopupWindowWidth / 2f);
                } else {
                    mIndicatorView.setTranslationX(marginLeftScreenEdge - mPopupWindowWidth / 2f);
                }
            } else if (marginRightScreenEdge < mPopupWindowWidth / 2f) {
                if (marginRightScreenEdge < mIndicatorWidth / 2f) {
                    mIndicatorView.setTranslationX(mPopupWindowWidth / 2f - mIndicatorWidth / 2f);
                } else {
                    mIndicatorView.setTranslationX(mPopupWindowWidth / 2f - marginRightScreenEdge);
                }
            } else {
                mIndicatorView.setTranslationX(0);
            }
        }
        if (!mPopupWindow.isShowing()) {
            int xOff = centerX - mPopupWindowWidth / 2 - (int) mIndicatorView.getTranslationX();
            int yOff = location[1] - mPopupWindowHeight;
            // there has too large space under arrow indicator, reduce it.
            yOff += mContext.getResources().getDimensionPixelSize(
                    R.dimen.smartisan_floating_popup_menu_reduce);
            mPopupWindow.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, xOff, yOff);
        }
    }

    public void hidePopupListWindow() {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            return;
        }
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    private PopupWindow createPopupWindow(ViewGroup content) {
        mPopupWindow = new PopupWindow(content, mPopupWindowWidth, mPopupWindowHeight, true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        return mPopupWindow;
    }

    public void layoutItems(
            List<String> popupItems,
            PopupItemListener popupItemClickListener) {
        mPopupItemListener = popupItemClickListener;
        layoutMainPanelItems(popupItems);
    }

    private LinearLayout createMainPanel() {
        ViewGroup.LayoutParams wrapContent = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT);
        LinearLayout panel = new LinearLayout(mContext);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setLayoutParams(wrapContent);
        panel.setBackgroundResource(R.drawable.smartisan_floating_popup_window);

        mWrapContent = new LinearLayout.LayoutParams(wrapContent);
        mWrapContent.gravity = Gravity.CENTER;
        return panel;
    }

    private ViewGroup createContentContainer(View panel) {
        ViewGroup container;
        LinearLayout horizontalLayout = new LinearLayout(mContext);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.addView(panel);
        horizontalLayout.setHorizontalScrollBarEnabled(false);
        container = horizontalLayout;

        ViewGroup.LayoutParams wrapContent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(wrapContent);
        container.setFitsSystemWindows(false);

        FrameLayout frameLayout = new FrameLayout(mContext);
        frameLayout.addView(container);
        Drawable bottomArrowDrawable = mContext.getDrawable(R.drawable.smartisan_floating_popup_menu_bottom_arrow);
        mIndicatorView = addArrowViewIntoContainer(frameLayout, bottomArrowDrawable, false);
        container = frameLayout;
        return container;
    }

    private ImageView addArrowViewIntoContainer(FrameLayout container, Drawable arrowDrawable, boolean top) {
        ImageView arrowView = new ImageView(mContext);
        arrowView.setImageDrawable(arrowDrawable);
        arrowView.setScaleType(ImageView.ScaleType.CENTER);
        FrameLayout.LayoutParams arrowParams = new
                FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (top ? Gravity.TOP : Gravity.BOTTOM) | Gravity.CENTER_HORIZONTAL);
        if (top) {
            arrowParams.topMargin = mArrowMargin;
        } else {
            arrowParams.bottomMargin = mArrowMargin;
        }
        container.addView(arrowView, arrowParams);
        return arrowView;
    }

    public void layoutMainPanelItems(List<String> popupItems) {
        if (mMainPanel == null) {
            return;
        }
        final LinkedList<String> remainingItems = new LinkedList<String>(popupItems);

        mMainPanel.removeAllViews();

        boolean isFirstItem = true;
        int i = 0;
        while (!remainingItems.isEmpty()) {
            final String popupItem = remainingItems.peek();
            View menuItemButton = createMenuItemTextView(mContext, popupItem, i);

            if (isFirstItem) {
                isFirstItem = false;
            } else {
                mMainPanel.addView(createMenuItemDivider(mContext));
            }
            mMainPanel.addView(menuItemButton);
            remainingItems.pop();
            i++;
        }
    }

    private View createMenuItemTextView(Context context, String title, int position) {
        TextView textView = (TextView) LayoutInflater.from(context)
                .inflate(R.layout.smartisan_floating_popup_menu_textview, null);
        textView.setLayoutParams(mWrapContent);
        textView.setText(title);
        textView.setTag(position);
        textView.setOnClickListener(mItemButtonOnClickListener);
        return textView;
    }

    private View createMenuItemDivider(Context context) {
        ImageView v = new ImageView(context);
        v.setImageResource(R.drawable.smartisan_floating_popup_menu_divider);
        v.setScaleType(ImageView.ScaleType.CENTER);
        v.setLayoutParams(mWrapContent);
        return v;
    }

    private int getViewWidth(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredWidth();
    }

    private int getViewHeight(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredHeight();
    }

    public interface PopupItemListener {
        void onPopupItemClick(int position);
    }

    public boolean isShowing() {
        if (mPopupWindow != null) {
            return mPopupWindow.isShowing();
        }
        return false;
    }

    public void updateWidthAndHeight() {
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
    }

    private void updateResources() {
        if (mPopupItemList == null)
            return;
        mPopupItemList.clear();
        mPopupItemList.add(mContext.getResources().getString(R.string.bubble_remove));
        mPopupItemList.add(mContext.getResources().getString(R.string.bubble_share));
    }

    public void resetPopupWindow() {
        mPopupWindow = null;
    }
}
