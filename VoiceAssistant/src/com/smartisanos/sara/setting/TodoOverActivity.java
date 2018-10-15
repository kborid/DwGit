package com.smartisanos.sara.setting;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;

import java.util.ArrayList;
import java.util.List;

import smartisanos.widget.Title;
import smartisanos.widget.TipsView;

public class TodoOverActivity extends BaseActivity {
    private ListView mListView;
    private TipsView mTipsView;
    private List<TodoOverItem> mTodoOverItems = new ArrayList<TodoOverItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_over_setting);
        initTitleBar();
        mTipsView = (TipsView) getLayoutInflater().inflate(R.layout.todo_over_footer, null);
        mListView = (ListView) findViewById(R.id.options_list);
        mListView.addFooterView(mTipsView);
        initAdapter();
        updateTipView();
        setCaptionTitleToPillInExtDisplay();
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.bubble_todo_over_settings_title);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    private void updateTipView() {
        int type = SaraUtils.getTodoOverType(TodoOverActivity.this);
        int tipId = R.string.bubble_todo_over_immediately_tip;
        switch (type) {
            case SaraConstant.VOICE_TODO_OVER_IMMEDIATELY:
                tipId = R.string.bubble_todo_over_immediately_tip;
                break;
            case SaraConstant.VOICE_TODO_OVER_DAYLY:
                tipId = R.string.bubble_todo_over_dayly_tip;
                break;
            case SaraConstant.VOICE_TODO_OVER_WEEKLY:
                tipId = R.string.bubble_todo_over_weekly_tip;
                break;
        }
        mTipsView.setText(tipId);
    }

    private void initAdapter() {
        mTodoOverItems.clear();
        String[] todoOverTitles = getResources().getStringArray(R.array.bubble_todo_over_title);
        int[] todoOverValues = getResources().getIntArray(R.array.bubble_todo_over_value);
        for (int i = 0; i < todoOverTitles.length; i++) {
            TodoOverItem item = new TodoOverItem();
            item.title = todoOverTitles[i];
            item.settingsValue = todoOverValues[i];
            mTodoOverItems.add(item);
        }
        final TodoOverAdapter adapter = new TodoOverAdapter(this, R.layout.setting_item_layout, mTodoOverItems);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                TodoOverItem selectedItem = mTodoOverItems.get(index);
                SaraUtils.setTodoOverType(TodoOverActivity.this, selectedItem.settingsValue);
                adapter.notifyDataSetChanged();
                updateTipView();
            }
        });
    }

    public static class TodoOverItem {
        public String title;
        public int settingsValue;
    }
}
