package com.smartisanos.ideapills.sync.share;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.smartisanos.ideapills.BaseActivity;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.SyncBundleRepository.RequestListener;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.R;

import com.smartisanos.ideapills.common.util.TaskHandler;

import java.util.Map;
import java.util.Comparator;
import java.util.TreeMap;

import smartisanos.widget.Title;

public class SyncShareAddActivity extends BaseActivity implements View.OnClickListener {
    LOG log = LOG.getInstance(SyncShareAddActivity.class);
    private static final int REQUEST_PICK_CONTACT = 1000;
    private static final int CHOOSE_COUNTRY_RESULT = 1;
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_INTERNATIONAL = 1;
    private static final int MIN_PHONE_NUM_LENGTH = 7;
    private int mCurrentStatus = STATUS_NORMAL;
    private EditText mTvPhoneEmail;
    private EditText mTvPhone;
    private View mChineseLogin;
    private View mInternationalLogin;
    private View mCountryLayout;
    private TextView mTvCountryName;
    private TextView mTvCountryCode;
    private TextView mTvTip;
    private View mInvitationView;
    private TextView mTvLoginType;
    private ProgressDialog mProgressDialog;
    private Map<String, String> mCountryMap;
    private String mCountryCode = Constants.CHINA_CODE;
    private volatile String mUserAlias = "";
    private volatile String mUserPhone = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_activity_share_add);
        mChineseLogin = findViewById(R.id.contact_content);
        mCountryLayout = findViewById(R.id.country_layout);
        mInternationalLogin = findViewById(R.id.country_contact);
        mTvLoginType = (TextView) findViewById(R.id.login_type);
        mCountryLayout.setOnClickListener(this);
        mTvLoginType.setOnClickListener(this);
        mTvTip = (TextView) findViewById(R.id.invite_tip);
        mTvCountryName = (TextView) findViewById(R.id.country_txt);
        mTvCountryCode = (TextView) findViewById(R.id.country_code);
        mTvPhoneEmail = (EditText) findViewById(R.id.phone_email);
        mTvPhoneEmail.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mTvPhone = (EditText) findViewById(R.id.country_phone);
        mTvPhone.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        findViewById(R.id.contact).setOnClickListener(this);
        mInvitationView = findViewById(R.id.invitation);
        mInvitationView.setOnClickListener(this);
        setLoginButtonEnable(false);
        mTvPhoneEmail.addTextChangedListener(mTextWatcher);
        initTitleBar();
        mProgressDialog = new ProgressDialog(this);
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.sync_add_share_account);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        title.setBackButtonText(smartisanos.R.string.title_button_text_back);

        Intent fromIntent = getIntent();
        if (!fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT) && !fromIntent.hasExtra("from_search")) {
            title.getBackButton().setVisibility(View.VISIBLE);
        } else {
            title.getBackButton().setVisibility(View.GONE);
        }
        title.setShadowVisible(true);
        setTitleByIntent(title);
    }

    private void setLoginButtonEnable(boolean enable) {
        if (mInvitationView.isEnabled() != enable) {
            mInvitationView.setEnabled(enable);
            mInvitationView.setAlpha(enable ? Constants.ENABLE_ALPHA : Constants.DISABLE_ALPHA);
        }
    }

    private void changeLoginStatus() {
        if (mCurrentStatus == STATUS_NORMAL) {
            mCurrentStatus = STATUS_INTERNATIONAL;
            mInternationalLogin.setVisibility(View.VISIBLE);
            mCountryLayout.setVisibility(View.VISIBLE);
            mChineseLogin.setVisibility(View.GONE);
            mTvPhone.addTextChangedListener(mTextWatcher);
            mTvPhoneEmail.removeTextChangedListener(mTextWatcher);
            mTvPhoneEmail.setText("");
            mTvTip.setText(R.string.sync_send_invitation_country_summary);
            mTvLoginType.setText(R.string.chinese_phone_email);
            if (mCountryMap == null) {
                initCountryCode();
            }
            setCountryCode(mCountryCode);
        } else {
            mCurrentStatus = STATUS_NORMAL;
            mCountryCode = Constants.CHINA_CODE;
            mInternationalLogin.setVisibility(View.GONE);
            mCountryLayout.setVisibility(View.GONE);
            mChineseLogin.setVisibility(View.VISIBLE);
            mTvPhoneEmail.addTextChangedListener(mTextWatcher);
            mTvPhone.removeTextChangedListener(mTextWatcher);
            mTvPhone.setText("");
            mTvTip.setText(R.string.sync_send_invitation_summary);
            mTvLoginType.setText(R.string.international_phone);
        }
        setLoginButtonEnable(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contact:
                pickerPhoneAccount();
                break;
            case R.id.invitation:
                if (mCurrentStatus == STATUS_NORMAL) {
                    final String addr = mTvPhoneEmail.getText().toString();
                    sendInvitation(addr);
                } else {
                    final String addr = mTvPhone.getText().toString();
                    sendInvitation(addr);
                }
                break;
            case R.id.country_layout:
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.smartisanos.handinhand", "com.smartisanos.handinhand.choosecountry.ChooseCountryActivity"));
                startActivityForResult(intent, CHOOSE_COUNTRY_RESULT);
                overridePendingTransition(smartisanos.R.anim.slide_in_from_right, smartisanos.R.anim.slide_out_to_left);
                break;
            case R.id.login_type:
                changeLoginStatus();
                break;
        }
    }

    private void initCountryCode() {
        String[] countryInfos = this.getResources().getStringArray(R.array.countries_name_code);
        mCountryMap = new TreeMap<String, String>(new Comparator<String>() {

            @Override
            public int compare(String lhs, String rhs) {
                return rhs == null ? 1 : rhs.compareTo(lhs);
            }
        });
        for (String src : countryInfos) {
            String[] countryInfo = src.split(",");
            mCountryMap.put(countryInfo[1], countryInfo[0]);
        }
    }

    private void setCountryCode(String countryCode) {
        if (TextUtils.isEmpty(countryCode)) {
            return;
        }
        if (countryCode.startsWith("+")) {
            mCountryCode = countryCode;
        } else {
            mCountryCode = "+" + countryCode;
        }
        mTvCountryName.setText(mCountryMap.get(mCountryCode));
        mTvCountryCode.setText(mCountryCode);
    }

    private void updateButtonState(String number) {
        if (Constants.CHINA_CODE.equals(mCountryCode)) {
            setLoginButtonEnable(isLegitimacy(number));
        } else {
            setLoginButtonEnable(number.length() >= MIN_PHONE_NUM_LENGTH);
        }
    }

    private boolean isLegitimacy(String addr) {
        if (TextUtils.isEmpty(addr)) {
            return false;
        }
        if (!SyncUtil.isPhoneNumber(addr) && !SyncUtil.isEmail(addr)) {
            return false;
        }
        return true;
    }

    private void sendInvitation(String addr) {
        SyncShareRepository.startInvite(addr, addr.equals(mUserPhone) ? mUserAlias : "", mCountryCode + addr, listener);
        mProgressDialog.showProgressDelay();
    }

    @Override
    public void onDestroy() {
        mProgressDialog.onDestroy();
        listener.cancel(true);
        super.onDestroy();
    }

    private void onInviteSend() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GlobalBubbleUtils.showSystemToast(SyncShareAddActivity.this, R.string.sync_send_invitation_start_title, Toast.LENGTH_SHORT);
                mProgressDialog.hideProgressDialog();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void handleError(final int status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                final String number = mCurrentStatus == STATUS_NORMAL ? mTvPhoneEmail.getText().toString() : mTvPhone.getText().toString();
                GlobalInvitationAction.getInstance().handleError(status, number);
            }
        });
    }

    private void showDialogIfPossible(Dialog dialog) {
        if (!isFinishing()) {
            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_CONTACT:
                if (resultCode == RESULT_OK) {
                    final Uri contactData = data.getData();
                    if (contactData == null) {
                        return;
                    }
                    parserPicker(contactData);
                }
                break;
            case CHOOSE_COUNTRY_RESULT:
                String countryCode = data.getExtras().getString("countryCode");
                setCountryCode(countryCode);
                updateButtonState(mTvPhone.getText().toString());
                break;
            default:
                break;
        }
    }

    public void updateContactInfo(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvPhoneEmail.setText(info);
                if (!TextUtils.isEmpty(info)) {
                    mTvPhoneEmail.setSelection(info.length());
                }
            }
        });
    }

    private void pickerPhoneAccount() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.contacts");
        intent.setType("vnd.android.cursor.dir/phone_v2");
        startActivityForResult(intent, REQUEST_PICK_CONTACT);
        overridePendingTransition(smartisanos.R.anim.pop_up_in, smartisanos.R.anim.fake_anim);
    }

    private void pickerEmailAccount() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.contacts");
        intent.setType("vnd.android.cursor.dir/email_v2");
        startActivityForResult(intent, REQUEST_PICK_CONTACT);
        overridePendingTransition(smartisanos.R.anim.pop_up_in, smartisanos.R.anim.fake_anim);
    }

    private void parserPicker(final Uri uri) {
        TaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            int mimeTypeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);
                            final String mimeType = cursor.getString(mimeTypeIndex);
                            int contactId = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                            mUserAlias = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String number = "";
                            String email = "";
                            if (TextUtils.equals(mimeType, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                                number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                if (!TextUtils.isEmpty(number)) {
                                    number = number.replace("-", "");
                                    number = number.replace(" ", "");
                                }
                            } else if (TextUtils.equals(mimeType, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                                email = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS));
                            }
                            if (contactId > 0 && (!TextUtils.isEmpty(number) || !TextUtils.isEmpty(email))) {
                                if (number.startsWith("+")) {
                                    number = number.substring(Constants.CHINA_CODE.length(), number.length());
                                }
                                mUserPhone = number;
                                updateContactInfo(number);
                                return;
                            }
                        } while (cursor.moveToNext());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    private RequestListener listener = new RequestListener<SyncShareInvitation>() {
        public void onRequestStart() {

        }

        public void onResponse(SyncShareInvitation response) {
            onInviteSend();
        }

        public void onError(SyncBundleRepository.DataException e) {
            if (e != null) {
                final int status = e.status;
                handleError(status);
            }
        }
    };

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            final String number = editable.toString();
            updateButtonState(number);
        }
    };
}
