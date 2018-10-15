package com.smartisanos.ideapills.common.model;


import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import com.smartisanos.ideapills.common.util.PackageUtils;

public class ShareItem {

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

    public Drawable getDrawable(Context context) {
        if (mDrawable == null) {
            mDrawable = PackageUtils.getAppIcon(context, mComponentName);
        }
        return mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }
}
