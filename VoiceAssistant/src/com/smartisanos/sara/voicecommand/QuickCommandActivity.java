package com.smartisanos.sara.voicecommand;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.smartisanos.sara.R;
import com.smartisanos.sanbox.utils.SaraTracker;

import smartisanos.widget.SettingItemText;
import smartisanos.widget.Title;

public class QuickCommandActivity extends TitleBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        SaraTracker.onEvent("A130005", "id", "26030902");
    }

    public void initView() {
        Title titlebar = getTitleBar();
        titlebar.setBackButtonText(R.string.voice_commond_title);
        titlebar.setTitle(R.string.quick_command_title);

        ViewGroup container = getContainer();
        getLayoutInflater().inflate(R.layout.quick_command_content, container, true);

        ViewGroup vg = (ViewGroup) container.findViewById(R.id.command_items);
        for (int i=vg.getChildCount()-1; i>=0; --i) {
            View item = vg.getChildAt(i);
            if (item instanceof SettingItemText) {
                if (item.getId() == R.id.nav_home) {
                    item.setOnClickListener(this);
                } else {
                    item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.nav_home:
                startActivity(new Intent(this, HomeInfoActivity.class));
                break;
        }
    }
}