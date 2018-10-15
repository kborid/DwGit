package com.smartisanos.sara.bubble.revone.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class WebSearchItem extends SearchItem {
    private static String JSON_IS_SELECTED = "selected";
    private static String JSON_TYPE = "type";
    private static String JSON_ENGINE = "engine";
    public boolean mChecked;
    public int mType;
    public String mEngineName;
    public String mUrl;
    //public Drawable mEngineIcon;

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public String getEnginName() {
        return mEngineName;
    }

    public void setEnginName(String name) {
        mEngineName = name;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
    /*public Drawable getEngineIcon() {
        return mEngineIcon;
    }

    public void setEngineIcon(Drawable icon) {
        this.mEngineIcon = icon;
    }*/

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_IS_SELECTED, mChecked);
            json.put(JSON_TYPE, mType);
            json.put(JSON_ENGINE, mEngineName);
        } catch (JSONException e) {
        }
        return json;
    }

    @Override
    public void fromJSON(JSONObject json) {
        try {
            mChecked = json.getBoolean(JSON_IS_SELECTED);
            mType = json.getInt(JSON_TYPE);
            mEngineName = json.getString(JSON_ENGINE);
        } catch (JSONException e) {
        }
    }
}
