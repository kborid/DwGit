package com.smartisanos.sara.voicecommand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.media.MediaPlayer.OnCompletionListener;

import smartisanos.widget.SettingItemSwitch;
import smartisanos.api.SettingsSmt;
import smartisanos.widget.SettingItemText;
import smartisanos.widget.Title;
import smartisanos.widget.TipsView;
import smartisanos.util.DeviceType;
import smartisanos.util.LogTag;

import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;

import android.provider.Settings;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class VoiceCommondSettingActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener,
        OnCompletionListener{
    private static final String TAG = "VoiceCommondSettingActivity";

    private boolean isOdin = DeviceType.is(DeviceType.ODIN);

    private SettingItemSwitch mVoiceCommondSwitch;
    private SettingItemText mShortcutItem;
    private ImageButton mPlayButton;
    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private Handler mHandler = null;
    private int mTotalTimeData = 0;
    private int mCurrentTimeData = 0;
    private TipsView mVoiceTipsView;
    private TipsView mShortCutTipsView;


    private MediaPlayer mMediaPlayer=null;
    private boolean isStop = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_FINISH_SETTINGS_ACTIVITY.equals(action)) {
                finish();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason) && SaraUtils.isKillSelf(context)) {
                    SaraUtils.killSelf(context);
                }
            }
        }
    };

    private Runnable mRunable = new Runnable() {
        @Override
        public void run() {
            if (!isStop) {
                setCurrentTime();
                mHandler.postDelayed(mRunable, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogTag.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_commond_main_layout);

        initMediaPlayer();
        initView();
        initTitleBar();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SaraConstant.ACTION_FINISH_SETTINGS_ACTIVITY);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);
    }

    private void initView() {
        mVoiceCommondSwitch = (SettingItemSwitch) findViewById(R.id.voice_commond);
        mShortcutItem = (SettingItemText) findViewById(R.id.voice_commond_shortcut);
        mPlayButton = (ImageButton) findViewById(R.id.voice_intro_video_btn);
        mSeekBar = (SeekBar) findViewById(R.id.voice_intro_seekbar);
        mSeekBar.setProgress(0);
        mCurrentTime = (TextView) findViewById(R.id.voice_current_time);
        mTotalTime  =(TextView) findViewById(R.id.voice_end_time);
        mPlayButton.setAccessibilityTraversalAfter(R.id.voice_commond_intro_txt);
        mVoiceTipsView = (TipsView) findViewById(R.id.voice_commond_tips);
        mShortCutTipsView = (TipsView) findViewById(R.id.voice_commond_shortcut_tips);
        mSeekBar.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setContentDescription(getString(R.string.voice_command_seekbar_alert3, mCurrentTimeData / 60 , mCurrentTimeData % 60,
                        mTotalTimeData / 60, mTotalTimeData % 60));
                info.setClassName(null);
            }
        });

        mVoiceCommondSwitch.setFocusable(true);
        mShortcutItem.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnCompletionListener(this);
        }

        if (isOdin) {
            mVoiceTipsView.setText(getText(R.string.voice_commond_title_tip_home_key));
            mShortCutTipsView.setText(getText(R.string.voice_commond_shortcut_home_key));
        } else {
            mVoiceTipsView.setText(getText(R.string.voice_commond_title_tip_ideapill_key));
            mShortCutTipsView.setText(getText(R.string.voice_commond_shortcut_tip_ideapill_key));
        }
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.voice_commond_title);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent fromIntent = getIntent();
        //if from launcher, hidden the back button, otherwise set the background
        if (!fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT) && !fromIntent.hasExtra("from_search")) {
            title.getBackButton().setVisibility(View.GONE);
        } else {
            title.setBackButtonTextByIntent(getIntent());
            if (fromIntent.hasExtra("from_search")) {
                title.setBackBtnArrowVisible(false);
            } else {
                title.setBackBtnArrowVisible(true);
            }
            title.setBackButtonTextGravity(Gravity.CENTER);
            title.getBackButton().setVisibility(View.VISIBLE);
        }
        setTitleByIntent(title);

    }

    private void initMediaPlayer() {
        mMediaPlayer = MediaPlayer.create(this, R.raw.voicecommand);
    }

    private void updateView() {
        mVoiceCommondSwitch.setChecked(SaraUtils.isBlindModeOpen());
        if(mMediaPlayer == null) {
            initMediaPlayer();
        }
        mTotalTimeData = Math.round(mMediaPlayer.getDuration() / 1000);
        String totalStr = formatNumber(mTotalTimeData / 60) + ":" + formatNumber(mTotalTimeData % 60);
        mTotalTime.setText(totalStr);
        mTotalTime.setContentDescription(getString(R.string.voice_command_seekbar_alert2, mTotalTimeData / 60 , mTotalTimeData % 60));
        mCurrentTime.setContentDescription(getString(R.string.voice_command_seekbar_alert1, 0 , 0));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mVoiceCommondSwitch.getSwitch()) {
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.VOICE_COMMAND_STATE, isChecked ? 1 : 0);
        }
    }

    @Override
    protected void onStart() {
        LogTag.d(TAG, "onStart()");
        super.onStart();
        isStop = false;
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            };
        }
    }

    @Override
    protected void onResume() {
        LogTag.d(TAG, "onResume()");
        super.onResume();
        mVoiceCommondSwitch.setOnCheckedChangeListener(this);
        updateView();
    }


    @Override
    protected void onPause() {
        LogTag.d(TAG, "onPause()");
        super.onPause();
        mVoiceCommondSwitch.setOnCheckedChangeListener(null);
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mPlayButton.callOnClick();
        }
    }

    @Override
    protected void onStop() {
        LogTag.d(TAG, "onStop()");
        super.onStop();
        isStop = true;
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunable);
            mHandler = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mSeekBar != null) {
            mSeekBar.setProgress(0);
        }
        if (mPlayButton != null) {
            mPlayButton.setBackground(getDrawable(R.drawable.voice_commond_pause_bg));
            mPlayButton.setContentDescription(getString(R.string.read_play));
        }

        if (mCurrentTime != null) {
            mCurrentTime.setText(getText(R.string.voice_commond_time_default));
            mCurrentTime.setContentDescription(getString(R.string.voice_command_seekbar_alert1, 0 , 0));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voice_commond_shortcut:
                Intent intentShortcut = new Intent(this, QuickCommandActivity.class);
                startActivity(intentShortcut);
                break;
            case R.id.voice_intro_video_btn:
                if (mMediaPlayer == null) {
                    break;
                }

                if (!mMediaPlayer.isPlaying()) {
                    mPlayButton.setBackground(getDrawable(R.drawable.voice_commond_play_bg));
                    mPlayButton.clearAccessibilityFocus();
                    mPlayButton.setContentDescription(getString(R.string.read_pause));
                    mMediaPlayer.start();
                    mHandler.postDelayed(mRunable, 1000);
                    mSeekBar.setMax(mMediaPlayer.getDuration());
                } else {
                    mPlayButton.setBackground(getDrawable(R.drawable.voice_commond_pause_bg));
                    mPlayButton.clearAccessibilityFocus();
                    mPlayButton.announceForAccessibility(getString(R.string.already_read_pause));
                    mPlayButton.setContentDescription(getString(R.string.read_play));
                    mMediaPlayer.pause();
                    if (!isStop) {
                        setCurrentTime();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void finish() {
        super.finish();
        if (SaraUtils.isKillSelf(this)) {
            SaraUtils.killSelf(this);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (mMediaPlayer != null && fromUser) {
            mMediaPlayer.seekTo(seekBar.getProgress());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mPlayButton.setBackground(getDrawable(R.drawable.voice_commond_pause_bg));
        mPlayButton.setContentDescription(getString(R.string.read_play));
    }

    private String formatNumber(int number) {
        NumberFormat formatter = new DecimalFormat("00");
        return formatter.format(number);
    }

    private void setCurrentTime() {
        if(null == mMediaPlayer) {
            return;
        }
        int currentPosition = mMediaPlayer.getCurrentPosition();
        int currentProgress;
        if (currentPosition == mMediaPlayer.getDuration()) {
            mCurrentTimeData = 0;
            currentProgress = 0;
        } else {
            mCurrentTimeData = Math.round(currentPosition / 1000);
            currentProgress = currentPosition;
        }
        String currentStr = formatNumber(mCurrentTimeData / 60) + ":" + formatNumber(mCurrentTimeData % 60);
        mCurrentTime.setText(currentStr);
        mSeekBar.setProgress(currentProgress);
        mCurrentTime.setContentDescription(getString(R.string.voice_command_seekbar_alert1, mCurrentTimeData / 60 , mCurrentTimeData % 60));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogTag.d(TAG, "onNewIntent()");
        super.onNewIntent(intent);
        setIntent(intent);
        initTitleBar();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        LogTag.d(TAG, "onWindowFocusChanged() -- hasFocus = " + hasFocus);
        if (!hasFocus) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mPlayButton.callOnClick();
            }
        }
    }
}
