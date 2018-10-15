package com.smartisanos.sara.bubble.revone.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalSearchItem extends SearchItem {
    private static String JSON_IS_SELECTED = "selected";
    private static String JSON_PACKAGE = "package_name";
    private static String JSON_ACTIVITY = "activity_name";
    private static String JSON_ACTION = "action_name";
    private static String JSON_DISPLAY_NAME = "display_name";
    public boolean mChecked;
    public String mPackageName;
    public String mClassName;
    public String mActionName;
    public String mDisplayName;

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String name) {
        mPackageName = name;
    }

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String name) {
        mClassName = name;
    }

    public String getActionName() {
        return mActionName;
    }

    public void setActionName(String actionName) {
        this.mActionName = actionName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String name) {
        mDisplayName = name;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_IS_SELECTED, mChecked);
            json.put(JSON_PACKAGE, mPackageName == null ? "" : mPackageName);
            json.put(JSON_ACTIVITY, mClassName == null ? "" : mClassName);
            json.put(JSON_DISPLAY_NAME, mDisplayName == null ? "" : mDisplayName);
            json.put(JSON_ACTION, mActionName == null ? "" : mActionName);
        } catch (JSONException e) {
        }
        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        mChecked = json.optBoolean(JSON_IS_SELECTED);
        mPackageName = json.optString(JSON_PACKAGE);
        mClassName = json.optString(JSON_ACTIVITY);
        mDisplayName = json.optString(JSON_DISPLAY_NAME);
        mActionName = json.optString(JSON_ACTION);
    }
}
