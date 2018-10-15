package com.smartisanos.sara.bubble.revone.entity;

import org.json.JSONObject;

public abstract class SearchItem {
    public abstract JSONObject toJSON();

    public abstract void fromJSON(JSONObject json);
}