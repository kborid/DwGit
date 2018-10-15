
package com.smartisanos.sara.setting.recycle;

public interface QuickBubbleListener {

    public void hideQuickBubble();

    public void setQuickBubbleCacheId(long id);

    public long getQuickBubbleCacheId();

    public void setHasAddedTime(boolean hasAddedTime);

    public boolean hasAddedTime();

    public void setIsShowQuickBubble(boolean isShowQuickBubble);

    public boolean isShowQuickBubble();

}
