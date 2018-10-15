package com.smartisanos.sara.widget;

import android.view.View;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.widget.WebSearchLayout.onCheckChangedListener;
import java.util.Map;

public class SearchPopupWindow {

    private final String[] mTextRes;
    private final int[] mIconRes;
    private final int mCheckedId;
    private final SparseIntArray mContentMap;
    private final onCheckChangedListener mListener;
    private final int mIndex;
    private final int mPopupVerticalOffset;
    private final int mArrowHorrizontalOffset;

    private final PopupWindow mPopupWindow;
    private final LayoutInflater mInflater;
    private final ListView mListView;
    private final View mContentView;

    private ImageView mChecked;

    public SearchPopupWindow(Context context, int textRes, int iconRes, SparseIntArray contentMap, int checkedId, int index,
                             onCheckChangedListener listener, int parentY) {
        final Resources res = context.getResources();
        mTextRes = res.getStringArray(textRes);
        TypedArray array = res.obtainTypedArray(iconRes);
        mIconRes = new int[mTextRes.length];
        for (int i = 0; i < mIconRes.length; ++i) {
            mIconRes[i] = array.getResourceId(i, 0);
        }
        array.recycle();
        mContentMap = contentMap;
        mCheckedId = checkedId;
        mListener = listener;

        mPopupWindow = new PopupWindow(context);
        mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopupWindow.setFocusable(false);
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setOutsideTouchable(false);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable());

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.search_view_option_popup, null);
        mPopupWindow.setContentView(mContentView);
        mListView = (ListView) mContentView.findViewById(R.id.option_listview);
        mListView.setAdapter(new OptionAdapter());

        mIndex = index;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        android.view.Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        int size = mIconRes.length;
        mPopupVerticalOffset = (parentY +
                (1 - size)*(context.getResources().getDimensionPixelSize(R.dimen.search_margin_bottom) + context.getResources().getDimensionPixelSize(R.dimen.search_item_space))) / 2;
        mArrowHorrizontalOffset = getScreenWidth(context) - context.getResources().getDimensionPixelOffset(R.dimen.web_search_margin) * 11;
    }

    public void show(View view) {
        if (!mPopupWindow.isShowing()) {
            mPopupWindow.setAnimationStyle(R.style.popup_window);
            mPopupWindow.showAtLocation(view, Gravity.BOTTOM, mArrowHorrizontalOffset, mPopupVerticalOffset);
        }
    }

    public void hide() {
        mPopupWindow.dismiss();
    }

    private class OptionAdapter extends BaseAdapter {
        private View[] mItems;

        public OptionAdapter() {
            mItems = new View[mIconRes.length];
            for (int i = 0; i < mIconRes.length; ++i) {
                mItems[i] = mInflater.inflate(R.layout.search_view_option_item, null, false);
            }
        }

        @Override
        public int getCount() {
            return mIconRes.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            convertView = mItems[position];
            final ImageView icon = (ImageView) convertView.findViewById(R.id.item_icon);
            icon.setBackgroundResource(mIconRes[position]);
            final View space = (View) convertView.findViewById(R.id.space_top);

            if (position == mItems.length - 1) {
                space.setVisibility(View.GONE);
            } else {
                space.setVisibility(View.VISIBLE);
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    (new Handler()).post(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < mContentMap.size(); i++) {
                                if (mContentMap.valueAt(i) == position) {
                                    mListener.onCheckChanged(mContentMap.keyAt(i));
                                    break;
                                }
                            }
//                                mPopupWindow.dismiss();
                        }
                    });
                }
            });
            return convertView;
        }
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
