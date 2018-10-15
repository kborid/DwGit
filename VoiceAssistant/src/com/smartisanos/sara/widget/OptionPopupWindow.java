package com.smartisanos.sara.widget;

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
import com.smartisanos.sara.widget.WebSearchLayout.onCheckChangedListener;
import java.util.Map;

public class OptionPopupWindow {

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

    public OptionPopupWindow(Context context, int textRes, int iconRes, SparseIntArray contentMap, int checkedId, int index,
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
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable());

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.search_option_popup, null);
        mPopupWindow.setContentView(mContentView);
        mListView = (ListView) mContentView.findViewById(R.id.option_listview);
        mListView.setAdapter(new OptionAdapter());

        mIndex = index;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        android.view.Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        mPopupVerticalOffset = dm.heightPixels - parentY;
        mArrowHorrizontalOffset = res.getDimensionPixelOffset(R.dimen.popup_arrow_horrizontal_offset);
    }

    public void show(View view) {
        if (!mPopupWindow.isShowing()) {
            mPopupWindow.showAtLocation(view, Gravity.BOTTOM
                    | Gravity.CENTER_HORIZONTAL, mIndex * mArrowHorrizontalOffset * 2 - view.getWidth() / 2, mPopupVerticalOffset);
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
                mItems[i] = mInflater.inflate(R.layout.search_option_listitem, null, false);
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
            final TextView text = (TextView) convertView.findViewById(R.id.item_text);
            text.setText(mTextRes[position]);
            final ImageView check = (ImageView) convertView.findViewById(R.id.item_check);
            if (position == mCheckedId) {
                check.setSelected(true);
                mChecked = check;
            } else {
                check.setSelected(false);
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position != mCheckedId) {
                        mChecked.setSelected(false);
                        check.setSelected(true);
                        (new Handler()).post(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < mContentMap.size(); i++) {
                                    if (mContentMap.valueAt(i) == position) {
                                        mListener.onCheckChanged(mContentMap.keyAt(i));
                                        break;
                                    }
                                }
                                mPopupWindow.dismiss();
                            }
                        });
                    } else {
                        mPopupWindow.dismiss();
                    }
                }
            });
            return convertView;
        }
    }
}
