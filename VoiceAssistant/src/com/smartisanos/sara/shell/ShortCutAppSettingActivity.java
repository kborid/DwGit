package com.smartisanos.sara.shell;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.content.Intent;

import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.R;
import com.smartisanos.sara.widget.ShortcutAppSettingView;

import smartisanos.widget.Title;


public class ShortCutAppSettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcut_app_setting_layout);

        initTitleBar();
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.app_drawer_string);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        title.setBackButtonText(R.string.btn_back);
        Intent fromIntent = getIntent();
        //if from launcher, hidden the back button, otherwise set the background
        if (!fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT) && !fromIntent.hasExtra("from_search")) {
            title.getBackButton().setVisibility(View.VISIBLE);
        } else {
            title.getBackButton().setVisibility(View.GONE);
        }
        setTitleByIntent(title);
    }

}
