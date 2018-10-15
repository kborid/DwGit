
package com.smartisanos.sara.lock.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.lock.util.LockPasswordUtil;
import com.smartisanos.sara.lock.util.SmartisanApi;

import java.util.Stack;

import smartisanos.util.LogTag;

public class LockEasyPasswordLayout extends LockPasswordLayout implements
        View.OnClickListener {
    private static final String TAG = "LockEasyPasswordLayout";

    private static final int PASSWORD_BTN_COUNT = 12;
    private static final int BACK_BTN_POS = 9;
    private static final int BTN_0_POS = 10;
    private static final int BTN_DEL_POS = 11;

    private static final int DOT_STATE_EMPTY = 0;
    private static final int DOT_STATE_FILL = 1;
    private static final int DOT_STATE_ERROR = 2;

    private int PASSWORD_LINGTH = 4;

    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private LinearLayout mDotContainer;
    private GridView mKeyboardGrid;
    private ImageView[] mDots = new ImageView[6];
    private TextView mInfoView;
    private Stack<Integer> mPassword = new Stack<Integer>();
    private TextView mTipText;


    public LockEasyPasswordLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mDotContainer = (LinearLayout)findViewById(R.id.password_container);
        mInfoView = (TextView) findViewById(R.id.info);
        mTipText = (TextView)findViewById(R.id.tip_text);
        mKeyboardGrid = (GridView) findViewById(R.id.keyboard);
        mKeyboardGrid.setAdapter(new EasyPasswordAdapter());

        mDots[0] = (ImageView) findViewById(R.id.easy_password_no_1);
        mDots[1] = (ImageView) findViewById(R.id.easy_password_no_2);
        mDots[2] = (ImageView) findViewById(R.id.easy_password_no_3);
        mDots[3] = (ImageView) findViewById(R.id.easy_password_no_4);
        mDots[4] = (ImageView) findViewById(R.id.easy_password_no_5);
        mDots[5] = (ImageView) findViewById(R.id.easy_password_no_6);
        updateDotsLayout();

        SmartisanApi.setMaxTextSize(mInfoView, mContext.getResources().getDimension(
                R.dimen.lock_tip_max_text_size));

        updateFaceTipString(true);
    }

    private void updateFaceTipString(boolean isShow) {
        if (isShow) {
            if (TextUtils.isEmpty(getFaceTipString()) || !isKeyStoreUnlocked()) {
                mTipText.setVisibility(GONE);
            } else {
                mTipText.setVisibility(VISIBLE);
                mTipText.setText(getFaceTipString());
            }
        } else {
            mTipText.setVisibility(GONE);
        }
    }

    public int getInfoTipString() {
        int resourceTip;
        if (!isKeyStoreUnlocked()) {
            resourceTip = R.string.input_password_reboot;
            return resourceTip;
        } else {
            if (mSecureLockManager.isFingerprintIsEnable(mContext)) {
                resourceTip = R.string.input_password_or_fingerprint;
            } else {
                resourceTip = R.string.input_password;
            }
        }
        return resourceTip;
    }


    @Override
    public void updateConfiguration() {
        if (getSecondsRemaining() == 0) {
            if (isFingerprintLockout()) {
                mInfoView.setTextColor(getTipTextColor(true));
                mInfoView.setText(R.string.fingerprint_password_too_many_times);
                updateFaceTipString(false);
            } else {
                setTipNormalText(mInfoView, getInfoTipString(), getTipTextColor(false));
            }
        }
        TextView keyBoardCancel = (TextView)mKeyboardGrid.getChildAt(BACK_BTN_POS);
        if (keyBoardCancel != null) {
            keyBoardCancel.setText(R.string.accessibility_back);
        }
    }

    @Override
    public void onClick(View v) {
        if (mPassword.size() >= PASSWORD_LINGTH) {
            return;
        }

        int position = (Integer) v.getTag();
        if (position == BACK_BTN_POS) {
            if (mCallback != null) {
                mCallback.onBack();
            }
            return;
        } else if (position == BTN_DEL_POS) {
            if (mPassword.size() > 0) {
                mDots[mPassword.size() - 1].getDrawable().setLevel(DOT_STATE_EMPTY);
                mPassword.pop();
            }
        } else if (position == BTN_0_POS) {
            mPassword.push(0);
            mDots[mPassword.size() - 1].getDrawable().setLevel(DOT_STATE_FILL);
        } else {
            mPassword.push(position + 1);
            mDots[mPassword.size() - 1].getDrawable().setLevel(DOT_STATE_FILL);
        }

        if (mPassword.size() >= PASSWORD_LINGTH) {
            StringBuilder password = new StringBuilder(PASSWORD_LINGTH);
            for (int i : mPassword) {
                password.append(i);
            }
            mSecureLockManager.verifyKeyguardSecurely(this, password.toString());
        }
    }

    public void onKeyguardSecurelyVerifyResult(final boolean success, final int timeoutMs) {
        post(new Runnable() {
            @Override
            public void run() {
                removeCallbacks(mCancelRunnable);
                if (success) {
                    onUnlockSuccess();
                } else {
                    enabledkeyboard(false);
                    mInfoView.setTextColor(getTipTextColor(true));
                    boolean reachMaxFaildCount = onUnlockFailed(timeoutMs);
                    unlockFailedVibrate();
                    if (reachMaxFaildCount) {
                        updateDot(DOT_STATE_EMPTY);
                        playShakeAnimation(mDotContainer);
                    } else {
                        updateDot(DOT_STATE_ERROR);
                        playShakeAnimation(mDotContainer);
                        final int totalFailedAttempts = getTotalFailedAttempts();
                        mInfoView.setText(R.string.password_error_warning);
                        postDelayed(mCancelRunnable, CLEAR_PASSWORD_TIMEOUT_MS);
                    }
                    mPassword.clear();
                }
            }
        });
    }

    @Override
    protected void onFingerprintAuthenticationInfo(final CharSequence info) {
        post(new Runnable() {
            @Override
            public void run() {
                mInfoView.setTextColor(getTipTextColor(true));
                mInfoView.setText(info);
                removeCallbacks(mCancelRunnable);
                postDelayed(mCancelRunnable, CLEAR_PASSWORD_TIMEOUT_MS);
                updateDot(DOT_STATE_ERROR);
                playShakeAnimation(mDotContainer);
            }
        });

    }

    @Override
    protected void onFingerprintAuthenticationLockout(final boolean animat) {
        post(new Runnable() {
            @Override
            public void run() {
                if (!isPasswordTimeout() && !animat) {
                    mInfoView.setTextColor(getTipTextColor(true));
                    mInfoView.setText(R.string.fingerprint_password_too_many_times);
                    updateFaceTipString(false);
                }
                if (animat) {
                    playShakeAnimation(mInfoView);
                }
            }
        });
    }

    @Override
    protected void onFingerprintAuthenticationSucceeded() {
        updateDot(DOT_STATE_FILL);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onUnlockResult(true);
                }
            }
        }, 200);
    }

    @Override
    protected void onFaceIdAuthenticationSucceeded() {
        updateDot(DOT_STATE_FILL);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onUnlockResult(true);
                }
            }
        }, 200);
    }

    private void updateDot(int level) {
        for (ImageView v : mDots) {
            if (v.getVisibility() != View.GONE) {
                v.getDrawable().setLevel(level);
            }
        }
    }

    private void enabledkeyboard(boolean enabled) {
        for (int index = 0; index < mKeyboardGrid.getChildCount(); index++) {
            if (index != BACK_BTN_POS) {
                View v = mKeyboardGrid.getChildAt(index);
                v.setEnabled(enabled);
            }
        }
    }

    @Override
    protected void onFailLockTick(long millisUntilFinished) {
        final int secondsRemaining = (int) (millisUntilFinished / 1000);
        if (secondsRemaining > 0) {
            mInfoView.setText(getResources().getQuantityString(R.plurals.unlock_error_try_later,
                    secondsRemaining, secondsRemaining));
            mInfoView.setTextColor(getTipTextColor(true));
            updateFaceTipString(false);

        }
    }

    @Override
    protected void onFailLockFinish() {
        reset();
        resetTips();
    }

    @Override
    public void clearPassword() {
        mPassword.clear();
        updateDot(DOT_STATE_EMPTY);
    }

    @Override
    public void resetTips() {
        if (isPasswordTimeout()) {
            return;
        }
        if (isFingerprintLockout()) {
            mInfoView.setTextColor(getTipTextColor(true));
            mInfoView.setText(R.string.fingerprint_password_too_many_times);
            updateFaceTipString(false);

        } else {
            setTipNormalText(mInfoView, getInfoTipString(), getTipTextColor(false));
        }
    }

    @Override
    public void reset() {
        mPassword.clear();
        updateDot(DOT_STATE_EMPTY);
        enabledkeyboard(true);
    }

    private class EasyPasswordAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return PASSWORD_BTN_COUNT;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = (TextView) mLayoutInflater.inflate(R.layout.lock_easy_password_item,
                        null);
                textView.setOnClickListener(LockEasyPasswordLayout.this);
            } else {
                textView = (TextView) convertView;
            }

            setTextBackground(textView, position);
            textView.setTag(position);
            return textView;
        }

        private void setTextBackground(final TextView textView, final int position) {
            final int resId;
            if (position == BACK_BTN_POS) {
                textView.setText(R.string.accessibility_back);
                resId = R.drawable.lock_selector_easy_password_keyboard_blank;
            } else if (position == BTN_DEL_POS) {
                resId = R.drawable.lock_selector_easy_password_keyboard_back;
            } else if (position == BTN_0_POS) {
                resId = R.drawable.lock_selector_easy_password_keyboard_0;
            } else {
                String resName = "lock_selector_easy_password_keyboard_" + (position + 1);
                resId = getContext().getResources().getIdentifier(resName, "drawable",
                        getContext().getPackageName());
            }

            textView.setWidth(getContext().getResources().getDimensionPixelSize(R.dimen.keyguard_number_width));
            textView.setHeight(getContext().getResources().getDimensionPixelSize(R.dimen.keyguard_number_height));
            new AsyncTask<Void, Void, Drawable>() {
                @Override
                protected Drawable doInBackground(Void... voids) {
                    return getContext().getResources().getDrawable(resId);
                }

                @Override
                protected void onPostExecute(Drawable d) {
                    textView.setBackgroundDrawable(d);
                }
            }.execute();
        }
    }

    @Override
    public void disableOnTouch() {
        enabledkeyboard(false);
    }

    @Override
    public void updatePasswordLayout() {
        updateDotsLayout();
    }

    private void updateDotsLayout() {
        PASSWORD_LINGTH = LockPasswordUtil.getEasyPasswordLength(mContext);
        LogTag.d(TAG, "update dots count: " + PASSWORD_LINGTH);
        if (mDots == null) return;
        if (PASSWORD_LINGTH == 4) {
            mDots[4].setVisibility(View.GONE);
            mDots[5].setVisibility(View.GONE);
        } else {
            mDots[4].setVisibility(View.VISIBLE);
            mDots[5].setVisibility(View.VISIBLE);
        }
    }
}
