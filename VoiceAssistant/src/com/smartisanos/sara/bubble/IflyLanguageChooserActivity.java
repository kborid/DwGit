
package com.smartisanos.sara.bubble;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.smartisanos.sara.R;
import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import smartisanos.api.SettingsSmt;
import smartisanos.widget.ListContentItemCheck;
import smartisanos.widget.Title;

public class IflyLanguageChooserActivity extends BaseActivity implements
        AdapterView.OnItemClickListener {

    private ListView mOptionsListView;
    private BaseAdapter mAdapter;
    private LayoutInflater mInflater;

    public ArrayList<String> mOptionsList = new ArrayList<String>();
    public ArrayList<String> mValuesList = new ArrayList<String>();

    private int mCurrentIndex;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options_list_layout);
        mOptionsListView = (ListView) findViewById(R.id.options_list);

        initLanguageSetting();

        mInflater = LayoutInflater.from(this);
        mOptionsListView.addHeaderView(SaraUtils.inflateListTransparentHeader(this));
        mOptionsListView.addFooterView(SaraUtils.inflateListTransparentHeader(this));
        mOptionsListView.setOnItemClickListener(this);
        mAdapter = new LanguageAdapter();
        mOptionsListView.setAdapter(mAdapter);
        initTitleBar();
        setCaptionTitleToPillInExtDisplay();
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.recognition_language);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent fromIntent = getIntent();
        title.setBackButtonTextByIntent(fromIntent);
        if (fromIntent.hasExtra("from_search")) {
            title.setBackBtnArrowVisible(false);
        } else {
            title.setBackBtnArrowVisible(true);
        }
        setTitleByIntent(title);

    }

    private void initLanguageSetting() {

        String[] mOptions = getResources().getStringArray(R.array.ilfy_language_entry);
        String[] mValues = getResources().getStringArray(R.array.ilfy_language_value);

        mOptionsList.addAll(Arrays.asList(mOptions));
        mValuesList.addAll(Arrays.asList(mValues));
        mCurrentIndex = getLangSubtitleIndex();
    }

    private int getLangSubtitleIndex() {
        String select = SaraUtils.getSelectedLanguage(this);
        for (int i = 0; i < mValuesList.size(); i++) {
            String value = mValuesList.get(i);
            if (select.equals(value)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        i -= 1;
        if (i >= 0 && i < mValuesList.size()) {
            String value = mValuesList.get(i);
            Settings.Global.putString(getContentResolver(), SettingsSmt.Global.VOICE_LANGUAGE, value);
            mCurrentIndex = i;
            mAdapter.notifyDataSetChanged();
            finish();
        }
    }

    private class LanguageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mOptionsList.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.setting_item_layout, null);
            }

            if (getCount() == 1) {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_single);
            } else if (i == 0) {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_top);
            } else if (i == getCount() - 1) {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_bottom);
            } else {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_middle);
            }

            ListContentItemCheck itemCheck = (ListContentItemCheck) convertView.findViewById(R.id.item_check);;
            itemCheck.setTitle(mOptionsList.get(i));
            itemCheck.setChecked(i == mCurrentIndex);

            return convertView;
        }
    }
}
