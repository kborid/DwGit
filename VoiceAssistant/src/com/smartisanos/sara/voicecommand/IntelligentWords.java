package com.smartisanos.sara.voicecommand;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.TaskHandler;

import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smartisanos.api.IntentSmt;

public class IntelligentWords {
    private static final boolean DBG_RESULT = true;

    public static final String TYPE_ADDRESS = "1";
    public static final String TYPE_PHONE_NUM = "2";
    public static final String TYPE_FLIGHT_NUM = "3";
    public static final String TYPE_TRAIN_TRIPS = "4";
    public static final String TYPE_MOVIE_NAME = "5";
    public static final String TYPE_RESTAURANT_OR_ENTERTAINMENT = "6";
    public static final String TYPE_DATE = "7";
    public static final String TYPE_TIME = "8";
    public static final String TYPE_STOCK_NAME = "9";
    public static final String TYPE_STOCK_CODE = "10";
    public static final String TYPE_EXPRESS_NUMBER = "11";
    public static final String TYPE_GOODS = "12";

    private static class MatchRule {
        public String pkg;
        public String[] types;

        public MatchRule(String pkg, String[] types) {
            this.pkg = pkg;
            this.types = types;
        }

        public boolean isMatch(String pkg, String type) {
            if (pkg == null || type == null) {
                return false;
            }
            if (this.pkg == null || types == null) {
                return false;
            }
            if (this.pkg.equals(pkg)) {
                for (String category : types) {
                    if (category.equals(type)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean isMatch(String type) {
            if (pkg == null || types == null || type == null) {
                return false;
            }
            for (String category : types) {
                if (category.equals(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final List<MatchRule> mRules = new ArrayList<MatchRule>();
    static void addRules(String pkg, String[] types) {
        mRules.add(new MatchRule(pkg, types));
    }

    static {
        addRules("com.baidu.BaiduMap", new String[] {TYPE_RESTAURANT_OR_ENTERTAINMENT, TYPE_ADDRESS});//百度地图
        addRules("com.autonavi.minimap", new String[] {TYPE_RESTAURANT_OR_ENTERTAINMENT, TYPE_ADDRESS});//高德地图
        addRules("com.Kingdee.Express", new String[] {TYPE_EXPRESS_NUMBER});//快递100
        addRules("com.cainiao.wireless", new String[] {TYPE_EXPRESS_NUMBER});//菜鸟
        addRules("vz.com", new String[] {TYPE_FLIGHT_NUM});//飞常准
        addRules("ctrip.android.view", new String[] {TYPE_FLIGHT_NUM});//携程
        addRules("com.jingdong.app.mall", new String[] {TYPE_GOODS});//京东
        addRules("com.netease.yanxuan", new String[] {TYPE_GOODS});//网易严选
        addRules("com.android.benlailife.activity", new String[] {TYPE_GOODS});//本来生活
        addRules("com.taobao.taobao", new String[] {TYPE_GOODS});//淘宝
        addRules("com.dianping.v1", new String[] {TYPE_GOODS, TYPE_RESTAURANT_OR_ENTERTAINMENT});//大众点评
    }

    private static MatchRule getRule() {
        String pkg = "com.autonavi.minimap";
        if (pkg != null) {
            for (MatchRule rule : mRules) {
                if (pkg.equals(rule.pkg)) {
                    return rule;
                }
            }
        }
        return null;
    }

    //if has matched content
    private static Map<String, List<String>> textAnalysis(Context context, String text) {
        MatchRule rule = getRule();
        if (rule == null) {
            return null;
        }
        text = text.trim();
        final String str;
        if (text.length() > 1024) {
            str = text.substring(0, 1024);
        } else {
            str = text;
        }

        Bundle result = null;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("PARAM_TEXT", str);
            Uri uri = Uri.parse("content://com.smartisanos.intelligenwords");
            result = context.getContentResolver().call(uri, "METHOD_LOCAL_QUERY", null, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == null || result.isEmpty()) {
            return null;
        }
        Map<String, List<String>> matchedContent = parseData(result.getString("result"));
        return matchedContent;
    }

    private static Map<String, List<String>> parseData(String result) {
        if (result == null) {
            return null;
        }
        try {
            JSONArray array = new JSONArray(result);
            int count = array.length();
            Map<String, List<String>> dataMap = new HashMap<String, List<String>>();
            for (int i = 0; i < count; i++) {
                JSONObject object = array.getJSONObject(i);
                if (object == null) {
                    continue;
                }
                double score = object.getDouble("score");
                String intent = object.getString("intent");
                JSONObject contentObj = object.getJSONObject("content");
                if (contentObj == null) {
                    continue;
                }
                Iterator<String> types = contentObj.keys();
                while (types.hasNext()) {
                    String type = types.next();
                    List<String> list = dataMap.get(type);
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    JSONArray contents = contentObj.getJSONArray(type);
                    int length = contents.length();
                    for (int j = 0; j < length; j++) {
                        String content = contents.getString(j);
                        if (!list.contains(content)) {
                            list.add(content);
                        }
                    }
                    dataMap.put(type, list);
                }
            }
            //merge data, remove duplicate item
            if (dataMap != null && dataMap.size() > 0) {
                Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
                List<String> keys = new ArrayList<String>(dataMap.keySet());
                for (String key : keys) {
                    List<String> list = dataMap.get(key);
                    if (list != null) {
                        if (list.size() == 1) {
                            resultMap.put(key, list);
                        } else {
                            List<String> items = new ArrayList<String>();
                            for (String value : list) {
                                if (!hasMatchedItem(value, list)) {
                                    items.add(value);
                                }
                            }
                            resultMap.put(key, items);
                        }
                    }
                }
                return resultMap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean hasMatchedItem(String value, List<String> values) {
        if (values == null || values.size() == 0) {
            return false;
        }
        for (String v : values) {
            if (v != null) {
                if (v.length() > value.length() && v.indexOf(value) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Dialog mMenuDialog;

    static boolean showMenu(final Context context, final ChooseListAdapter adapter, String title, final SmartWordCallback receiver) {
        if (context == null || receiver == null || adapter == null || adapter.getCount() == 0) {
            return false;
        }
        if (mMenuDialog != null) {
            mMenuDialog.dismiss();
            mMenuDialog = null;
        }
        ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light);
        LayoutInflater inflater = LayoutInflater.from(themeWrapper);
        View view = inflater.inflate(R.layout.intelligent_words_choose_list, null);
        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        int height = 0;
        LinearLayout itemView = null;
        if (adapter.isShowFlightNum()) {
            itemView = (LinearLayout) View.inflate(context, R.layout.intelligent_words_choose_flight_num_item, null);
        } else {
            itemView = (LinearLayout) View.inflate(context, R.layout.intelligent_words_choose_text_item, null);
        }
        itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        height = itemView.getMeasuredHeight();
        int count = adapter.getCount();
        int showItemLimited = 5;
        if (count > showItemLimited) {
            count = showItemLimited;
        }
        listView.getLayoutParams().height = height * count;

        if (TextUtils.isEmpty(title)) {
            title = context.getString(R.string.intelligen_words_menu_title);
        }
        mMenuDialog = new AlertDialog.Builder(themeWrapper)
                .setTitle(title)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mMenuDialog = null;
                        mFlightNumAdapter = null;
                    }
                })
                .create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Object object = adapter.getItem(position);
                    String content = null;
                    if (object instanceof String) {
                        content = (String) object;
                    } else {
                        FlightNum flightNum = (FlightNum) object;
                        content = flightNum.getFlightNo();
                    }
                        receiver.sendTextToClient(content);
                } catch (Exception e) {}
                mMenuDialog.dismiss();
            }
        });
        mMenuDialog.getWindow().getAttributes().type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mMenuDialog.show();
        return true;
    }

    public static void postRequest(final Context context, final String text, final SmartWordCallback info, final ComponentName name) {
        if (mMenuDialog != null) {
            mMenuDialog.dismiss();
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> contentMap = textAnalysis(context, text);
                final List<String> all = new ArrayList<String>();
                //merge result to list
                MatchRule rule = getRule();
                if (contentMap != null && rule != null && rule.types != null) {
                    String[] types = rule.types;
                    for (int i = 0; i < types.length; i++) {
                        String type = types[i];
                        List<String> contents = contentMap.get(type);
                        if (contents != null) {
                            for (String value : contents) {
                                if (!all.contains(value)) {
                                    all.add(value);
                                }
                            }
                        }
                    }
                }
                if (all.size() > 1) {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ChooseListAdapter adapter = new ChooseListAdapter(context, all);
                            adapter.appendFullText(text);
                            IntelligentWords.showMenu(context, adapter,
                                    context.getString(R.string.choose_address_title), info);
                        }
                    });
                    return;
                }
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        info.sendTextToClient(all.isEmpty() ? "" : all.get(0));
                    }
                });
            }
        });
    }

    //##############################################################################################################

    private static class ItemData {

        private double mScore;
        private String mIntent;
        private HashMap<String, List<String>> mContentMap;

        public double getScore() {
            return mScore;
        }

        public void setScore(double score) {
            mScore = score;
        }

        public String getIntent() {
            return mIntent;
        }

        public void setIntent(String intent) {
            mIntent = intent;
        }

        public HashMap<String, List<String>> getContentMap() {
            return mContentMap;
        }

        public void setContentMap(HashMap<String, List<String>> contentMap) {
            mContentMap = contentMap;
        }

        public void appendContent(String key, String value) {
            if (key == null || value == null) {
                return;
            }
            if (mContentMap == null) {
                mContentMap = new HashMap<String, List<String>>();
            }
            List<String> list = mContentMap.get(key);
            if (list == null) {
                list = new ArrayList<String>();
            }
            if (!list.contains(value)) {
                list.add(value);
            }
            mContentMap.put(key, list);
        }

        public JSONObject toJSONObject() {
            JSONObject object = null;
            if (mContentMap == null || mContentMap.size() == 0) {
                return object;
            }
            try {
                object = new JSONObject();
                object.put("score", mScore);
                object.put("intent", mIntent);
                JSONObject contentObject = new JSONObject();
                List<String> keys = new ArrayList<String>(mContentMap.keySet());
                for (String key : keys) {
                    List<String> arr = mContentMap.get(key);
                    if (arr == null || arr.size() == 0) {
                        continue;
                    }
                    Set<String> set = new HashSet<String>(arr);
                    JSONArray array = new JSONArray();
                    for (String str : set) {
                        array.put(str);
                    }
                    contentObject.put(key, array);
                }
                object.put("content", contentObject);
            } catch (Exception e) {
                object = null;
                e.printStackTrace();
            }
            return object;
        }

        public static ItemData toItemData(JSONObject object) {
            if (object == null) {
                return null;
            }
            ItemData item = new ItemData();
            try {
                String intent = object.optString("intent");
                try {
                    double score = Double.parseDouble(object.optString("score"));
                    item.setScore(score);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                item.setIntent(intent);
                JSONObject result = object.optJSONObject("result");
                Iterator<String> keys = result.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key == null) {
                        continue;
                    }
                    JSONArray values = result.optJSONArray(key);
                    if (values == null) {
                        continue;
                    }
                    int length = values.length();
                    for (int i = 0; i < length; i++) {
                        String value = values.optString(i);
                        if (value == null) {
                            continue;
                        }
                        item.appendContent(key, value);
                    }
                }
            } catch (Exception e) {
                item = null;
                e.printStackTrace();
            }
            return item;
        }
    }

    public static void init(Context context) {
        String data = readData(context);
        if (data != null) {
            parseToRule(data);
        }
    }

    public static void saveData(Context context, String data) {
        SharedPreferences sp = context.getSharedPreferences("intelligent_words_rule", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("data", data);
        editor.apply();
    }

    private static String readData(Context context) {
        SharedPreferences sp = context.getSharedPreferences("intelligent_words_rule", 0);
        return sp.getString("data", null);
    }

    private static boolean parseToRule(String data) {
        try {
            JSONObject obj = new JSONObject(data);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key == null || key.trim().length() == 0) {
                    continue;
                }
                JSONArray array = obj.getJSONArray(key);
                int length = array.length();
                String[] types = new String[length];
                for (int i = 0; i < length; i++) {
                    String value = array.getString(i);
                    types[i] = value;
                }
                MatchRule rule = getRule();
                if (rule == null) {
                    rule = new MatchRule(key, types);
                    mRules.add(rule);
                } else {
                    rule.types = types;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static class ChooseListAdapter extends BaseAdapter {

        private Context mContext;
        private List mList;
        private List<FlightNum> mFlightNumList;
        private boolean mIsShowFlightNum = false;
        private boolean mIsShowFullText = false;

        public ChooseListAdapter(Context context, List list) {
            mContext = context;
            mList = list;
            if (mList == null) {
                mList = new ArrayList();
            }
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            Object object = getItem(position);
            if (mIsShowFlightNum) {
                FlightNum flightNum = (FlightNum) object;
                if (view == null) {
                    view = View.inflate(mContext, R.layout.intelligent_words_choose_flight_num_item, null);
                }
                TextView flightCompany = (TextView) view.findViewById(R.id.flight_company);
                flightCompany.setText(flightNum.getFlightCompany());
                TextView flightNumText = (TextView) view.findViewById(R.id.flight_num);
                flightNumText.setText(flightNum.getFlightNo());
                TextView fromTo = (TextView) view.findViewById(R.id.from_to);
                String from = flightNum.getFromCity();
                String to = flightNum.getToCity();
                if (from != null && to != null) {
                    fromTo.setText(flightNum.getFromCity() + " - " + flightNum.getToCity());
                } else {
                    fromTo.setText("");
                }
                TextView dateText = (TextView) view.findViewById(R.id.date);
                String[] date = flightNum.departureTime();
                if (date != null) {
                    String departureTime = mContext.getString(R.string.intelligent_words_flight_num_date);
                    departureTime = String.format(departureTime, date[0], date[1], date[2], date[3], date[4]);
                    dateText.setText(departureTime);
                } else {
                    dateText.setText("");
                    fromTo.setPadding(0, 0, 0, 0);
                }
            } else {
                String content = (String) object;
                if (content != null) {
                    content = content.trim();
                }
                if (view == null) {
                    view = View.inflate(mContext, R.layout.intelligent_words_choose_text_item, null);
                }
                boolean isLastItem = mIsShowFullText && (getCount() - 1) == position;
                TextView textItem = (TextView) view.findViewById(R.id.text_content);
                if (isLastItem) {
                    String fullText = content.replaceAll("\r", "").replaceAll("\n", "");
                    textItem.setText(fullText);
                } else {
                    textItem.setText(content);
                }
                String desc = null;
                if (isLastItem) {
                    desc = mContext.getResources().getString(R.string.intelligent_text_item_desc_full_text);
                } else {
                    desc = mContext.getResources().getString(R.string.intelligent_text_item_desc_result) + (position + 1);
                }
                TextView descItem = (TextView) view.findViewById(R.id.desc);
                descItem.setText(desc);
            }
            return view;
        }

        public void appendFlightNum(FlightNum num) {
            mIsShowFlightNum = true;
            if (mFlightNumList == null) {
                mFlightNumList = new ArrayList<FlightNum>();
            }
            mFlightNumList.add(num);
            if (mFlightNumList.size() == mList.size()) {
                mList.clear();
                for (FlightNum fn : mFlightNumList) {
                    if (fn != null) {
                        mList.add(fn);
                    }
                }
                if (mTask != null) {
                    UIHandler.post(mTask);
                }
            }
        }

        private Runnable mTask;

        public void setTask(Runnable task) {
            mTask = task;
        }

        public boolean isShowFlightNum() {
            return mIsShowFlightNum;
        }

        public void appendFullText(String fullText) {
            if (!mList.contains(fullText)) {
                mIsShowFullText = true;
                mList.add(fullText);
            }
        }
    }

    private static ChooseListAdapter mFlightNumAdapter = null;

    public static void receiveQueryResult(List<String> result) {
        if (result == null || result.size() == 0) {
            return;
        }
        if (mFlightNumAdapter == null) {
            return;
        }
        for (String data : result) {
            try {
                JSONArray arr = new JSONArray(data);
                if (mFlightNumAdapter != null) {
                    FlightNum num = new FlightNum(arr);
                    mFlightNumAdapter.appendFlightNum(num);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class FlightNum {
        private String mFrom = null;
        private String mTo = null;
        private String mFlightCompany = null;
        private String mFlightNo;
        private String mFlightDeptimeDate;//fly start time

        private boolean mIsAvailable = false;

        public FlightNum(JSONArray arr) {
            try {
                int length = arr.length();
                JSONObject object = null;
                if (length > 1) {
                    for (int i = 0; i < length; i++) {
                        JSONObject obj = arr.optJSONObject(i);
                        if (obj != null) {
                            String flag = obj.optString("stopFlag", "0");
                            if ("1".equals(flag)) {
                                object = obj;
                                break;
                            }
                        }
                    }
                }
                if (object == null) {
                    object = arr.getJSONObject(0);
                }
                mFlightNo = object.getString("FlightNo");
                if (mFlightNo != null) {
                    mFlightNo = mFlightNo.toUpperCase();
                }
                mFrom = object.optString("FlightDep", null);
                mTo = object.optString("FlightArr", null);
                mFlightCompany = object.optString("FlightCompany");
                mFlightDeptimeDate = object.getString("FlightDeptimePlanDate");
                if (mFlightCompany != null) {
                    mIsAvailable = true;
                }
            } catch (Exception e) {
                // NA
            }
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(mFrom);
            buffer.append("->");
            buffer.append(mTo);
            buffer.append(", ");
            buffer.append(mFlightCompany);
            buffer.append(", ");
            buffer.append(mFlightNo);
            buffer.append(", ");
            buffer.append(mFlightDeptimeDate);
            return buffer.toString();
        }

        public boolean isAvailable() {
            return mIsAvailable;
        }

        public String[] departureTime() {
            if (mIsAvailable) {
                if (mFlightDeptimeDate != null) {
                    mFlightDeptimeDate = mFlightDeptimeDate.trim();
                    try {
                        String[] deptime = mFlightDeptimeDate.split(" ");
                        if (deptime != null && deptime.length == 2) {
                            String[] date = deptime[0].split("-");
                            String[] time = deptime[1].split(":");
                            return new String[] {date[0], date[1], date[2], time[0], time[1]};
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        public String getFromCity() {
            return mFrom;
        }

        public String getToCity() {
            return mTo;
        }

        public String getFlightCompany() {
            return mFlightCompany;
        }

        public String getFlightNo() {
            return mFlightNo;
        }
    }
}
