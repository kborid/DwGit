package com.smartisanos.sara.voicecommand;

import android.graphics.Rect;

interface IVoiceCommandEnvironment {
        CharSequence getCurrentPackage();
        CharSequence getCurrentWindowTitle();
        Rect getCurrentFocusRect();
        CharSequence getCurrentFocusText();
        oneway void clickVoiceButton(String text);
}
