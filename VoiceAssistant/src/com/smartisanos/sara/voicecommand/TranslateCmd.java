package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateCmd extends VoiceCommand implements SmartWordCallback {
    public static final String TAG = "TranslateCmd";

    Context mContext;
    CharSequence mTranslateCmd;

    Pattern mParagraphPattern = Pattern.compile("[A-Za-z]+[\\x00-\\x7f]+");

    public TranslateCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mTranslateCmd = cxt.getResources().getString(R.string.quick_command_translate);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        VoiceCommandEnvironment env = VoiceCommandEnvironment.getInstance();
        if (VoiceCommandUtils.matchCommand(mTranslateCmd, cmd) && env != null) {
            List<String> paragraphs = extractParagraphs(env.getCurrentFocusText());
            LogUtils.d(TAG, "onProcess: paragraphs=" + paragraphs);

            if (TextUtils.equals(env.getCurrentWindowTitle(),
                    VoiceCommandUtils.getActivityLabel(mContext, new Intent(VoiceCommandUtils.ACTION_TRANSLATE)))) {
                ToastUtil.showToast(R.string.voice_command_result_has_shown);
                return FINISH_HANDLED;
            }

            if (paragraphs.size() > 1) {
                IntelligentWords.ChooseListAdapter adapter = new IntelligentWords.ChooseListAdapter(mContext, paragraphs);
                IntelligentWords.showMenu(mContext, adapter, mContext.getString(R.string.choose_paras_title), this);
                return FINISH_HANDLED;
            } else if (paragraphs.size() > 0) {
                VoiceCommandUtils.goTranslate(mContext, paragraphs.get(0));
                return FINISH_HANDLED;
            } else {
                return FINISH_NOT_HANDLED;
            }
        }
        return FORWARD;
    }

    private List<String> extractParagraphs(CharSequence text) {
        List<String> result = new ArrayList<>();
        if (text != null) {
            Matcher matcher = mParagraphPattern.matcher(text);
            while (matcher.find()) {
                result.add(matcher.group());
            }
        }
        return result;
    }

    @Override
    public void sendTextToClient(CharSequence text) {
        VoiceCommandUtils.goTranslate(mContext, text.toString());
    }
}
