package com.smartisanos.sara.voicecommand;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.MapUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import smartisanos.widget.Title;

public class HomeInfoActivity extends TitleBarActivity implements TextWatcher, Inputtips.InputtipsListener, AdapterView.OnItemClickListener {

    public static final String TAG = "HomeInfoActivity";
    public static final String EXTRA_FINISH_WITH_NAVIGATE = "finish_with_navigate";

    private TextView mBtnSave;
    private EditText mEtAddr;
    private View mSearchResult;

    private AddressInfo mAddressInfo;

    private SearchAdapter mAdapter;
    private Object mHightLightSpan = new ForegroundColorSpan(Color.parseColor("#E66157"));

    private String mLastQuery;
    private Pattern wordPattern = Pattern.compile("[a-zA-Z\u4e00-\u9fa5]");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initCity();
    }

    public void initView() {
        Title titlebar = getTitleBar();
        if (getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_FINISH_WITH_NAVIGATE, false)) {
            titlebar.setBackButtonText(R.string.btn_back);
        } else {
            titlebar.setBackButtonText(R.string.quick_command_title);
        }
        titlebar.setTitle(R.string.quick_command_nav_home);
        titlebar.setOkButtonText(R.string.bubble_save);

        mBtnSave = (TextView) titlebar.getOkButton();
        mBtnSave.setContentDescription(getString(R.string.bubble_save));
        mBtnSave.setVisibility(View.VISIBLE);
        mBtnSave.setEnabled(false);
        mBtnSave.setOnClickListener(this);

        ViewGroup container = getContainer();
        getLayoutInflater().inflate(R.layout.quick_command_home_info, container, true);

        mAddressInfo = AddressInfo.fromString(SharePrefUtil.getHomeAddr(this));
        Log.d(TAG, "initView: mAddressInfo=" + mAddressInfo);

        mEtAddr = (EditText) container.findViewById(R.id.home_info_edit_text);
        mEtAddr.setText(mAddressInfo.name);
        mEtAddr.setSelection(mEtAddr.getText().length());
        mEtAddr.addTextChangedListener(this);
        mEtAddr.setFilters(new InputFilter[]{
                new LoginFilter.UsernameFilterGeneric() {
                    @Override
                    public boolean isAllowed(char c) {
                        return !Character.isWhitespace(c);  // disallow whitespace character.
                    }
                }
        });

        mSearchResult = container.findViewById(R.id.search_result);
        mSearchResult.setVisibility(View.INVISIBLE);

        ListView listView = (ListView) container.findViewById(R.id.list);
        listView.setEmptyView(findViewById(R.id.empty));
        mAdapter = new SearchAdapter();
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
    }

    private String mCity;
    private void initCity() {
        MapUtils.getLocation(this, MapUtils.LOCATION_MODE_BATTERY_SAVING,
                new MapUtils.OnLocationResult() {
                    @Override
                    public void onLocationResult(AMapLocation aMapLocation) {
                        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                            mCity = aMapLocation.getCity();
                            LogUtils.d(TAG, "get city = " + mCity);
                        }
                    }
                });
    }

    private void query(String keyWords) {
        if (TextUtils.equals(mLastQuery, keyWords)) {
            return;
        }

        mLastQuery = keyWords;
        LogUtils.d(TAG, "query = " + keyWords);
        InputtipsQuery query = new InputtipsQuery(keyWords, mCity);
        Inputtips inputTips = new Inputtips(this, query);
        inputTips.setInputtipsListener(this);
        inputTips.requestInputtipsAsyn();
    }

    @Override
    public void onGetInputtips(List<Tip> inputTips, int resultID) {
        if (resultID == 1000) { //good result
            List<AddressInfo> addresses = new ArrayList<>();
            if (inputTips != null) {
                for (Tip tip : inputTips) {
                    if (!TextUtils.isEmpty(tip.getAddress())
                            && tip.getPoint() != null) {
                        addresses.add(new AddressInfo(tip.getName(), tip.getAddress(), tip.getPoint()));
                    }
                }
            }

            if (mAdapter != null) {
                mAdapter.updateAddress(addresses);
            }
        } else {
            LogUtils.e(TAG, "onGetInputtips error = " + resultID);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (v == mBtnSave) {
            saveAddrAndFinish();
        }
    }

    public void saveAddrAndFinish() {
        SharePrefUtil.setHomeAddr(this, mAddressInfo.toRegularString());
        if (getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_FINISH_WITH_NAVIGATE, false)) {
            VoiceCommandUtils.goNavigateHome(this, mAddressInfo);
        }
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (mBtnSave.isEnabled()) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_NEGATIVE:
                            HomeInfoActivity.super.onBackPressed();
                            break;
                        case DialogInterface.BUTTON_POSITIVE:
                            saveAddrAndFinish();
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.save_address_title)
                    .setNegativeButton(R.string.save_address_no, listener)
                    .setPositiveButton(R.string.save_address_yes, listener)
                    .setMessage(R.string.save_address_msg)
                    .setCancelable(false);
            builder.create().show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        mBtnSave.setEnabled(!TextUtils.isEmpty(s) && wordPattern.matcher(s).find());

        if (!TextUtils.equals(mAddressInfo.name, s.toString())) {
            mAddressInfo.reset();
            mAddressInfo.name = s.toString();
            setAddress(mAddressInfo);
        }

        if (!TextUtils.isEmpty(s) && !mAddressInfo.isAccuracy()) {
            query(s.toString());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setAddress(mAdapter.getItem(position));
    }

    private void setAddress(AddressInfo addr) {
        mAddressInfo = addr;

        mEtAddr.setText(addr.name);
        mEtAddr.setSelection(mEtAddr.getText().length());

        if (addr.isAccuracy()) {
            mSearchResult.setVisibility(View.INVISIBLE);
            hiddenInputMethod();
        } else {
            mSearchResult.setVisibility(View.VISIBLE);
        }
    }

    public void hiddenInputMethod() {
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }, 100);
    }

    class SearchAdapter extends BaseAdapter {
        private List<AddressInfo> mAddrs;

        public void updateAddress(List<AddressInfo> addresses) {
            LogUtils.e(TAG, "updateAddress = " + addresses);
            mAddrs = addresses;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mAddrs != null ? mAddrs.size() : 0;
        }

        @Override
        public AddressInfo getItem(int position) {
            return mAddrs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.home_info_list_item, null);
                convertView.setTag(new ViewHolderAddr(convertView));
            }

            ViewHolderAddr holder = (ViewHolderAddr) convertView.getTag();
            AddressInfo addr = getItem(position);

            int index = addr.name.indexOf(mAddressInfo.name);
            if (index >= 0) {
                SpannableString ss = new SpannableString(addr.name);
                ss.setSpan(mHightLightSpan, index, index + mAddressInfo.name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.name.setText(ss);
            } else {
                holder.name.setText(addr.name);
            }

            holder.addr.setText(addr.addr);
            return convertView;
        }
    }

    class ViewHolderAddr {
        TextView name;
        TextView addr;

        public ViewHolderAddr(View v) {
            name = (TextView) v.findViewById(R.id.name);
            addr = (TextView) v.findViewById(R.id.address);
        }
    }
}