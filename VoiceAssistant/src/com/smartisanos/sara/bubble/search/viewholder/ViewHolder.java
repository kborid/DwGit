package com.smartisanos.sara.bubble.search.viewholder;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.entity.IModel;

public abstract class ViewHolder {

    protected TextView title;
    protected Context mContext;
    protected Resources mResources;

    public ViewHolder(Context context, View v) {
        mContext = context;
        mResources = context.getResources();
    }

    public abstract void bindView(IModel m, boolean showTitle);
}
