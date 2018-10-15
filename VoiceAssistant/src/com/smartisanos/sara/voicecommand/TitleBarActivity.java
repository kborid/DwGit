package com.smartisanos.sara.voicecommand;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.smartisanos.sara.R;

import smartisanos.widget.Title;

public class TitleBarActivity extends Activity implements View.OnClickListener {

    private Title mTitlebar;
    private ViewGroup mContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_command_titlebar);

        mTitlebar = (Title) findViewById(R.id.titlebar);
        mTitlebar.getBackButton().setOnClickListener(this);

        mContainer = (ViewGroup) findViewById(R.id.body);
    }

    public Title getTitleBar() {
        return mTitlebar;
    }

    public ViewGroup getContainer() {
        return mContainer;
    }

    @Override
    public void onClick(View v) {
        if (v == mTitlebar.getBackButton()) {
            onBackPressed();
        }
    }
}