package com.smartisanos.sara.bullet.contact.model;

import android.text.TextUtils;

import java.util.List;
import smartisanos.app.voiceassistant.ContactStruct;

public class VoiceSearchResult {

    String       resultString;
    private List<ContactStruct> contactStruct;

    public void setContactStruct(List<ContactStruct> contactStruct) {
        this.contactStruct = contactStruct;
    }

    public List<ContactStruct> getContactStruct() {
        return contactStruct;
    }

    public String getResultString() {
        return resultString;
    }

    public void setResultString(String resultString) {
        this.resultString = resultString;
    }

    public boolean isResultEmpty() {
        return TextUtils.isEmpty(resultString) && (contactStruct == null || contactStruct.size() == 0);
    }
}
