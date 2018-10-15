package com.smartisanos.sara.bubble.search.viewholder;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.entity.IModel;
import com.smartisanos.sara.entity.YellowPageModel;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraUtils;

public class ViewHolderYellowPage extends ViewHolder
        implements View.OnClickListener, OnLongClickListener {

    TextView yellowTitle;
    TextView yellowName;
    TextView yellowNumber;
    private YellowPageModel model;

    public ViewHolderYellowPage(Context context, View v) {
        super(context, v);

        yellowTitle = (TextView) v.findViewById(R.id.yellow_title);
        yellowName = (TextView) v.findViewById(R.id.yellow_name);
        yellowNumber = (TextView) v.findViewById(R.id.yellow_number);
        View yellowButton = v.findViewById(R.id.yellow_call);
        View itemLayout = v.findViewById(R.id.yellow_number_result_layout);

        yellowButton.setOnClickListener(this);
        itemLayout.setOnClickListener(this);
        itemLayout.setOnLongClickListener(this);
    }

    @Override
    public void bindView(IModel m, boolean showTitle) {
        model = (YellowPageModel) m;
        yellowTitle.setVisibility(true == showTitle ? View.VISIBLE : View.GONE);
        yellowName.setText(model.yellowPageResult.name);
        yellowNumber.setText(model.yellowPageResult.number);
        updateViewFontSize();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.yellow_call:
                boolean isCall = !SaraUtils.isKeyguardSecureLocked();
                SaraUtils.dial(mContext, model.yellowPageResult.number, true, isCall);
                break;
            case R.id.yellow_number_result_layout:
                isCall = !SaraUtils.isKeyguardSecureLocked();
                SaraUtils.dial(mContext, model.yellowPageResult.number, false, isCall);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    private void updateViewFontSize() {
       final float MAX_SCALE = 1.4f;
       final int NAME_MAX_SIZE = 16;
       final int NUMBER_MAX_SIZE = 12;

       try {
           float fontScale = yellowName.getResources().getConfiguration().fontScale;
           if (fontScale >= MAX_SCALE) {
               yellowName.setTextSize(TypedValue.COMPLEX_UNIT_SP, NAME_MAX_SIZE);
               yellowNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, NUMBER_MAX_SIZE);
           }
       } catch (Exception e) {
           LogUtils.d("Fail to update view font size");
       }
    }
}
