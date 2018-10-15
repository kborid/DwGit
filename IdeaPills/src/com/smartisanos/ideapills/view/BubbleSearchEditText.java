package com.smartisanos.ideapills.view;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import android.widget.TextView;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.util.LOG;

public class BubbleSearchEditText extends EditText{
    LOG log = LOG.getInstance(BubbleSearchEditText.class);
    private BubbleListView mBubbleListView;

    public BubbleSearchEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleSearchEditText(Context context, AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (mBubbleListView != null) {
                    mBubbleListView.filtBubblesByWords(text);
                }
            }
        });
        setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (mBubbleListView != null) {
                        mBubbleListView.filtBubblesByWords(v.getText().toString());
                    }
                    Utils.hideInputMethod(BubbleSearchEditText.this);
                    return true;
                }
                return false;
            }
        });
    }

    public void setBubbleListView(BubbleListView listView) {
        mBubbleListView = listView;
    }

    public boolean dispatchDragEvent(DragEvent event) {
        return false;
    }

    @Override
    public View focusSearch(int direction) {
        return null;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setHint(getContext().getResources().getText(R.string.search_hint));
    }
}
