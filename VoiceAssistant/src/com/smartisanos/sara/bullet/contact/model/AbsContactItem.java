package com.smartisanos.sara.bullet.contact.model;

import android.os.Parcelable;

public abstract class AbsContactItem implements Parcelable {

    public interface ItemType {
        int LABEL = 0;
        int CONTACT = 1;
        int SEARCH = 2;
    }

    protected boolean isChecked = false;
    public void setChecked(boolean checked) {
        isChecked = checked;
    }
    public boolean isChecked() {
        return isChecked;
    }

    public String getContactId() {
        return "";
    }
    /**
     * 所属的类型
     */
    public abstract int getItemType();
    /**
     * 所属的分组
     */
    public abstract String belongsGroup();

    @Override
    public String toString() {
        return getItemType() + ", "
                + belongsGroup() + ", "
                + isChecked;
    }
}
