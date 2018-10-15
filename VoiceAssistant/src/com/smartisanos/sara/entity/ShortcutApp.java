package com.smartisanos.sara.entity;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;


public class ShortcutApp {

    private String key;
    private String title;
//    private int imageResId;

    private ComponentName mComponentName;
    private boolean isSelected;
    private String mDispalyName;
    private String mPackageName;
    private String mActivityName;
    private Drawable mDrawable;

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public void setComponentName(ComponentName componentName) {
        this.mComponentName = componentName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public String getActivityName() {
        return mActivityName;
    }

    public void setActivityName(String activityName) {
        this.mActivityName = activityName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getDispalyName() {
        return mDispalyName;
    }

    public void setDispalyName(String dispalyName) {
        this.mDispalyName = dispalyName;
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        ShortcutApp other = (ShortcutApp) obj;
        if (getActivityName().equals(other.getActivityName()) &&
                getPackageName().equals(other.getPackageName())) {
            return true;
        }
        return false;
    }
}
