package com.smartisanos.voice.util;

import android.text.TextUtils;
import android.util.Log;

import com.smartisanos.voice.VoiceApplication;
import com.smartisanos.voice.engine.GrammarManager;

import java.util.List;

import smartisanos.app.voiceassistant.ContactStruct;

public class AliasUtils {

    public static final String TAG = "AliasUtils";

    /**
     * 根据词典中的扩展关键词返回对应的真实值
     */
    public static String getValue(String lexicon, String word) {
        if (GrammarManager.LEXICON_CONTACT.equals(lexicon)) {
            List<ContactStruct> contacts = DataLoadUtil.loadContacts(VoiceApplication.getInstance(), new String[]{word});
            String realName = null;
            if (contacts != null && contacts.size() > 0) {
                for (ContactStruct contact : contacts) {
                    if (TextUtils.equals(contact.displayName, word)) {
                        return word;
                    }
                    // use the first name as the real name.
                    if (realName == null && !TextUtils.isEmpty(contact.displayName)) {
                        realName = contact.displayName;
                    }
                }
            }

            if (realName != null) {
                Log.d(TAG, "getValue: " + word + " -> " + realName);
                return realName;
            }
        }

        return word;
    }
}
