package com.smartisanos.sara.setting.recycle;
import android.view.View;

public interface RecycleItemListener {
    public boolean isSelected(RecycleItem item);

    public void umbButtonClicked(View itemView, RecycleItem item, int buttonId);

    public void onBubbleDelete(RecycleItem item);

    public void onBubbleRestore(RecycleItem item);

}
