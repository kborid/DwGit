package com.smartisanos.sara.voicecommand;

import android.content.Context;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static android.util.Patterns.WEB_URL;

public class WebUrlCmd extends VoiceCommand implements SmartWordCallback {
    public static final String TAG = "WebUrlCmd";

    Context mContext;
    CharSequence mWebUrlCmd;

    public WebUrlCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mWebUrlCmd = cxt.getResources().getString(R.string.quick_command_web);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        VoiceCommandEnvironment env = VoiceCommandEnvironment.getInstance();
        if (VoiceCommandUtils.matchCommand(mWebUrlCmd, cmd) && env != null) {
            List<String> urls = extractUrls(env.getCurrentFocusText());
            LogUtils.d(TAG, "onProcess: urls=" + urls);
            if (urls.size() > 1) {
                IntelligentWords.ChooseListAdapter adapter = new IntelligentWords.ChooseListAdapter(mContext, urls);
                IntelligentWords.showMenu(mContext, adapter, mContext.getString(R.string.choose_url_title), this);
                return FINISH_HANDLED;
            } else if (urls.size() > 0) {
                VoiceCommandUtils.goWeb(mContext, urls.get(0));
                return FINISH_HANDLED;
            } else {
                return FINISH_NOT_HANDLED;
            }
        }
        return FORWARD;
    }

    private List<String> extractUrls(CharSequence text) {
        List<String> result = new ArrayList<>();
        if (text != null) {
            Matcher matcher = WEB_URL.matcher(text);
            while (matcher.find()) {
                result.add(matcher.group());
            }
        }
        return result;
    }

    @Override
    public void sendTextToClient(CharSequence text) {
        VoiceCommandUtils.goWeb(mContext, text.toString());
    }
}
