package com.iflytek.speech;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.business.speech.IResourceServiceListener;
import com.iflytek.business.speech.ResourceServiceUtil;
import com.iflytek.business.speech.SpeechError;
import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import smartisanos.api.IntentSmt;
import smartisanos.api.SettingsSmt;
import smartisanos.util.LogTag;
import smartisanos.widget.Title;


public class SpeakersActivity extends BaseActivity {

    private static final String TAG = "SpeakersActivity";
    private static final int GET_LIST_TIMEOUT = 20000;

    private Context mContext;
    private MediaPlayer mAudioMediaPlayer;
    private Title mTitle;
    private ListView mSpeakerListView = null;

    private SpeakerListAdapter mSpeakerAdapter;
    private ArrayList<Integer> mDownloadList = new ArrayList<Integer>();

    public String[] mFileNames;
    private String[] mSpeakerNames;
    private String[] mSpeakerAccents;
    private String[] mContentDesc;

    private boolean mIsResourceServiceConnected = false;
    private boolean mIsGettingList = false;             // get the speaker list
    private boolean mIsDownloadingResource = false;     // download TTS resource
    private boolean mIsSampleAudioDownloading = false;  // download the sample audio.

    private ResourceServiceUtil mResourceServiceUtil = null;
    // data for resource utils.
    private ArrayList<Bundle> mListData = null;

    private int mSelectedIndex = -1;
    private int mTTSPlayIndex = -1;
    private int mDownSpeakerPos = -1;
    private int mDownProgress = 0;

    // view tags
    private static final String LIST_TTS_PLAY_PREFIX = "play_tts_";
    private static final String LIST_TTS_NAME_PREFIX = "tts_name_";
    private static final String LIST_TTS_DOWNLOAD_PREFIX = "download_tts_";
    private static final String LIST_TTS_PAUSE_PREFIX = "pause_tts_";
    private static final String LIST_TTS_PROCESS_PREFIX = "process_tts_";
    private static final String LIST_TTS_SWITCH_PREFIX = "choose_tts_";

    private static final int MSG_DOWNLOAD_PREPARE = 0x0001;     // service connected
    private static final int MSG_DOWNLOAD_START = 0x0002;       // download begin
    private static final int MSG_DOWNLOAD_PROGRESS = 0x0003;    // download progress
    private static final int MSG_DOWNLOAD_SUCCESS = 0x0004;     // download succeed
    private static final int MSG_DOWNLOAD_FALSE = 0x0005;       // download failed
    private static final int MSG_LISTEN_READY = 0x0006;         // the audio is ready to listen
    private static final int MSG_RESOURCE_SERVICE_CONNECTED = 0x0007;  // service connected
    private static final int MSG_TIPS = 0x0008;                 // show toast tips

    private static final float DOWNLOAD_PROCESS_OFFSET = 100.0f / 31;
    private static final int TTS_STATE_IDLE = 0;
    private static final int TTS_STATE_PLAYING = 1;

    private final IResourceServiceListener mResourceServiceListener = new IResourceServiceListener.Stub() {
        @Override
        public void onEvent(int eventType, final int arg1, int arg2, Bundle bundle) throws RemoteException {
            switch (eventType) {
                case ResourceServiceUtil.EVENT_SERVICE_CONNECTED: {
                    LogTag.d(TAG, "EVENT_SERVICE_CONNECTED, errorCode: " + arg1);
                    if (SpeechError.SUCCESS == arg1) {
                        mIsResourceServiceConnected = true;
                    }
                    sendMessage(MSG_RESOURCE_SERVICE_CONNECTED, arg1, 0);
                    break;
                }
                case ResourceServiceUtil.EVENT_SERVICE_DISCONNECTED: {
                    LogTag.w(TAG, "EVENT_SERVICE_DISCONNECTED");
                    mIsResourceServiceConnected = false;
                    mListData = null;
                    break;
                }
                case ResourceServiceUtil.EVENT_GET_LIST_OK: {
                    mListData = bundle.getParcelableArrayList(ResourceServiceUtil.KEY_LIST_DATA);
                    mIsGettingList = false;
                    if (mTTSPlayIndex != -1) {
                        prepareSampleAudio();
                    }
                    sendMessage(MSG_DOWNLOAD_PREPARE);
                    break;
                }
                case ResourceServiceUtil.EVENT_GET_LIST_ERROR: {
                    mIsGettingList = false;
                    if (mDownloadList.size() > 0) {
                        sendMessage(MSG_DOWNLOAD_FALSE);
                    }
                    if (mTTSPlayIndex != -1) {
                        showTips(getResources().getString(R.string.ifly_buffer_fail));
                        mTTSPlayIndex = -1;
                    }
                    break;
                }
                case ResourceServiceUtil.EVENT_DOWNLOAD_START: {
                    // the lib downloading resource now.
                    break;
                }
                case ResourceServiceUtil.EVENT_DOWNLOAD_PROGRESS: {
                    String subResType = bundle.getString(ResourceServiceUtil.KEY_SUB_RES_TYPE);
                    if (ResourceServiceUtil.RESOURCE.equals(subResType)) {
                        mDownProgress = arg1;
                        sendMessage(MSG_DOWNLOAD_PROGRESS, mDownProgress);
                    }
                    break;
                }
                case ResourceServiceUtil.EVENT_DOWNLOAD_ERROR: {
                    String subResType = bundle.getString(ResourceServiceUtil.KEY_SUB_RES_TYPE);
                    if (ResourceServiceUtil.RESOURCE.equals(subResType)) {
                        mIsDownloadingResource = false;
                        sendMessage(MSG_DOWNLOAD_FALSE);
                    } else if (ResourceServiceUtil.LISTEN.equals(subResType)) {
                        mIsSampleAudioDownloading = false;
                        showTips(getResources().getString(R.string.ifly_buffer_fail));
                        mTTSPlayIndex = -1;
                    }
                    break;
                }
                case ResourceServiceUtil.EVENT_DOWNLOAD_FINISH: {
                    String subResType = bundle.getString(ResourceServiceUtil.KEY_SUB_RES_TYPE);
                    ArrayList<String> filePaths = bundle.getStringArrayList(ResourceServiceUtil.KEY_FILE_PATHS);
                    String result = filePaths.isEmpty() ? null : filePaths.get(0);
                    if (ResourceServiceUtil.RESOURCE.equals(subResType)) {
                        mIsDownloadingResource = false;
                        sendMessage(MSG_DOWNLOAD_SUCCESS, result);
                    } else if (ResourceServiceUtil.LISTEN.equals(subResType)) {
                        mIsSampleAudioDownloading = false;
                        sendMessage(MSG_LISTEN_READY, result);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESOURCE_SERVICE_CONNECTED:
                    handleServiceConnected(msg);
                    break;
                case MSG_DOWNLOAD_PREPARE:
                    handlePrepareDownload();
                    break;
                case MSG_DOWNLOAD_START:
                    handleDownloadStart();
                    break;
                case MSG_DOWNLOAD_PROGRESS:
                    handleDownloadProgress((int) msg.obj);
                    break;
                case MSG_DOWNLOAD_SUCCESS:
                    handleDownloadSuccess();
                    handlePrepareDownload();
                    break;
                case MSG_DOWNLOAD_FALSE:
                    handleDownloadError();
                    showTips(getString(R.string.audio_download_failed));
                    handlePrepareDownload();
                    break;
                case MSG_LISTEN_READY:
                    playSampleAudio((String) msg.obj);
                    break;
                 case MSG_TIPS:
                     Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
                     break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ifly_speaker_setting_layout);
        // for accessibility report
        setTitle(R.string.ifly_speaker_setting_title);
        mSpeakerListView = (ListView) findViewById(android.R.id.list);
        mTitle = (Title) findViewById(R.id.title_bar);

        mSpeakerAccents = getResources().getStringArray(R.array.ifly_speakers_accent_value);
        //sSpeakerNames = getResources().getStringArray(R.array.ifly_speakers_value);
        mContentDesc = getResources().getStringArray(R.array.ifly_speakers_gender_value);
        mFileNames = getResources().getStringArray(R.array.ifly_profiles_value);

        mContext = this;
        setupBackBtn();
        Bundle initParams = new Bundle();
        initParams.putString(ResourceServiceUtil.KEY_ACTIVITY_NAME, SpeakersActivity.class.getSimpleName());
        initParams.putString(ResourceServiceUtil.KEY_RES_TYPE, ResourceServiceUtil.TTS);
        mResourceServiceUtil = new ResourceServiceUtil(getApplicationContext(), initParams, mResourceServiceListener);
        mSpeakerAdapter = new SpeakerListAdapter(getApplicationContext());
        mSpeakerListView.addHeaderView(SaraUtils.inflateListTransparentHeader(mContext));
        mSpeakerListView.addFooterView(SaraUtils.inflateListTransparentHeader(mContext));
        mSpeakerListView.setAdapter(mSpeakerAdapter);
    }

    private void setupBackBtn() {
        mTitle.getBackButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        Intent fromIntent = getIntent();
        mTitle.setBackBtnArrowVisible(!fromIntent.hasExtra("from_search"));
        setTitleByIntent(mTitle);
        if(fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT)) {
            mTitle.setBackButtonText(fromIntent.getStringExtra(Title.EXTRA_BACK_BTN_TEXT));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsResourceServiceConnected) {
            String selectedName = getDefaultSpeaker();
            if (null != selectedName) {
                mSelectedIndex = getSelectedIndex(selectedName);
            }
            if (null != mSpeakerAdapter) {
                mSpeakerAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogTag.d(TAG, "onPause: mPlayIndex:" + mTTSPlayIndex + ", selectIndex:" + mSelectedIndex);
        releaseMediaPlayer();
        handleAudioListenEnd();
    }

    private void handleServiceConnected(Message msg) {
        int errorCode = msg.arg1;
        if (SpeechError.SUCCESS != errorCode) {
            showTips(getString(R.string.resource_service_init_failed));
            return;
        }
        String selectedName = getIntent().getStringExtra("selected_role");
        if (null == selectedName) {
            selectedName = getSelectedSpeakerName();
        }
        mSelectedIndex = getSelectedIndex(selectedName);
        if (mSelectedIndex != -1 && !isAudioExist(mFileNames[mSelectedIndex])) {
            mDownloadList.add(mSelectedIndex);
            handlePrepareDownload();
        }
        if(mSpeakerAdapter != null) {
            mSpeakerAdapter.notifyDataSetChanged();
        }
    }

    private String getSelectedSpeakerName() {
        String selectedSpeakerName = null;
        Bundle inParams = new Bundle();
        inParams.putString(ResourceServiceUtil.KEY_KEY, ResourceServiceUtil.SELECTED_SPEAKER_NAME);
        Bundle outParams = new Bundle();
        int errorCode = mResourceServiceUtil.getParam(inParams, outParams);
        if (SpeechError.SUCCESS != errorCode) {
            LogTag.e(TAG, "getSelectedSpeakerName: errorCode:" + errorCode);
        } else {
            selectedSpeakerName = outParams.getString(ResourceServiceUtil.KEY_VALUE);
        }
        return selectedSpeakerName;
    }

    private int getSelectedIndex(String name) {
        int index = 0;
        if (name == null) {
            String defaultName = mFileNames[0];
            if (!isAudioExist(defaultName)) {
                defaultName = null;
                index = -1;
            }
            Utils.switchSpeakerName(mResourceServiceUtil, defaultName);
            return index;
        }
        if (isAudioExist(name)) {
            int length = mFileNames.length;
            for (int i = 0; i < length; i++) {
                if (name.equals(mFileNames[i])) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    private boolean isAudioExist(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        boolean isResourceExist;
        Bundle inParams = new Bundle();
        inParams.putString(ResourceServiceUtil.KEY_KEY, ResourceServiceUtil.IS_RESOURCE_EXIST);
        inParams.putString(ResourceServiceUtil.KEY_NAME, name);
        inParams.putBoolean(ResourceServiceUtil.KEY_IS_CHECK_FILE, true);
        Bundle outParams = new Bundle();
        int errorCode = mResourceServiceUtil.getParam(inParams, outParams);
        if (SpeechError.SUCCESS != errorCode) {
            LogTag.e(TAG, "isAudioExist: errorCode = " + errorCode);
            isResourceExist = false;
        } else {
            isResourceExist = outParams.getBoolean(ResourceServiceUtil.KEY_VALUE, false);
        }
        return isResourceExist;
    }

    private String getDefaultSpeaker() {
        String selectedName = getIntent().getStringExtra("selected_role");
        if (selectedName == null) {
            selectedName = getSelectedSpeakerName();
        }
        return selectedName;
    }

    private class SpeakerListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public SpeakerListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // the catherine does not support yet.
            return mFileNames.length;
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

            ViewHolder holder = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.ifly_speaker_list_item, null);
                holder.allView = convertView;
                holder.playAudioImgView = (ImageView) convertView.findViewById(R.id.id_audio_play);
                holder.speakerNameTv = (TextView) convertView.findViewById(R.id.id_speaker_voice_info);
                holder.speakerAccentTv = (TextView) convertView.findViewById(R.id.id_speaker_accent_info);
                holder.downloadBtn = (Button) convertView.findViewById(R.id.id_audio_download);
                holder.downloadProcessImgView = (ImageView) convertView.findViewById(R.id.id_audio_downloading_img);
                holder.speakerSwitchImgView = (ImageView) convertView.findViewById(R.id.id_audio_selected);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if(getCount() == 1) {
                holder.allView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_single);
            } else if (position == 0) {
                holder.allView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_top);
            } else if (position == getCount() - 1) {
                holder.allView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_bottom);
            } else {
                holder.allView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_middle);
            }
            setViewToHolder(holder, position);
            return convertView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            // prevent click for accessibility mode.
            return false;
        }
    }

    class ViewHolder {
        View allView;
        ImageView playAudioImgView;
        TextView speakerNameTv;
        TextView speakerAccentTv;
        Button downloadBtn;
        ImageView downloadProcessImgView;
        ImageView speakerSwitchImgView;
    }

    private void setViewToHolder(ViewHolder holder, int position) {
        holder.playAudioImgView.setImageLevel(TTS_STATE_IDLE);
        holder.playAudioImgView.setOnClickListener(new AudioPlayClickListener(position));
        holder.playAudioImgView.setTag(LIST_TTS_PLAY_PREFIX + position);
        holder.playAudioImgView.setContentDescription(getString(R.string.play_sample_audio));

        holder.speakerNameTv.setText(mContentDesc[position]);
        holder.speakerNameTv.setTag(LIST_TTS_NAME_PREFIX + position);
        holder.speakerNameTv.setContentDescription(mContentDesc[position]);

        holder.speakerAccentTv.setText(mSpeakerAccents[position]);

        holder.downloadBtn.setOnClickListener(new AudioDownloadClickListener(position));
        holder.downloadBtn.setTag(LIST_TTS_DOWNLOAD_PREFIX + position);

        holder.downloadProcessImgView.setTag(LIST_TTS_PROCESS_PREFIX + position);

        holder.speakerSwitchImgView.setOnClickListener(new SpeakerSwitchClickListener(position));
        holder.speakerSwitchImgView.setTag(LIST_TTS_SWITCH_PREFIX + position);

        if (isAudioExist(mFileNames[position])) {
            holder.speakerSwitchImgView.setVisibility(View.GONE);
            holder.downloadBtn.setVisibility(View.VISIBLE);
            holder.downloadBtn.setText(R.string.use_tts);
            holder.downloadBtn.setContentDescription(getString(R.string.use_tts));
        } else {
            holder.downloadBtn.setText(R.string.download_tts);
            holder.downloadBtn.setContentDescription(getString(R.string.download_tts));
            holder.speakerSwitchImgView.setVisibility(View.GONE);
        }
        if (mSelectedIndex == position) {
            holder.downloadBtn.setVisibility(View.INVISIBLE);
            holder.speakerSwitchImgView.setVisibility(View.VISIBLE);
        }
        if(mTTSPlayIndex == position) {
            holder.playAudioImgView.setImageLevel(TTS_STATE_PLAYING);
            holder.playAudioImgView.setContentDescription(getString(R.string.pause_sample_audio));
        }
    }

    private boolean isItemVisibleByPosition(int position) {
        return (position >= mSpeakerListView.getFirstVisiblePosition()) && (position <= mSpeakerListView.getLastVisiblePosition());
    }

    public void prepareSampleAudio() {
        if (!mIsSampleAudioDownloading) {
            mIsSampleAudioDownloading = true;
            requestResource(ResourceServiceUtil.LISTEN, mTTSPlayIndex);
        } else {
            LogTag.w(TAG, "prepareAudio: the resource is downloading.");
        }
    }

    /**
     * play the sample audio, cached in SD card.
     * @param path the audio path.
     */
    private void playSampleAudio(String path) {
        releaseMediaPlayer();
        mAudioMediaPlayer = new MediaPlayer();
        mAudioMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                releaseMediaPlayer();
                handleAudioListenEnd();
            }
        });
        mAudioMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                releaseMediaPlayer();
                handleAudioListenEnd();
                return false;
            }
        });

        mAudioMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        File file = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            mAudioMediaPlayer.setDataSource(fis.getFD());
            mAudioMediaPlayer.prepare();
            mAudioMediaPlayer.start();

        } catch (Exception e) {
            mTTSPlayIndex = -1;
            LogTag.e(TAG, "playSampleAudio:", e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                LogTag.e(TAG, "playSampleAudio:", e);
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mAudioMediaPlayer != null) {
            mAudioMediaPlayer.release();
            mAudioMediaPlayer = null;
        }
    }

    /**
     * Click to listen the sample audio
     */
    private class AudioPlayClickListener implements OnClickListener {
        private static final String TAG = "AudioPlayClickListener";
        int position;

        AudioPlayClickListener(int index) {
            this.position = index;
        }

        @Override
        public void onClick(View v) {
            if (!Utils.isNetAvailable(mContext)) {
                showTips(getString(R.string.net_error_tips));
                return;
            }
            // pause self
            if (mTTSPlayIndex == position) {
                releaseMediaPlayer();
                handleAudioListenEnd();
                return;
            }
            handleAudioListenEnd();
            mTTSPlayIndex = position;
            handleAudioListenStart();
            if (mListData == null) {
                getTTSList();
                return;
            }
            if (mIsSampleAudioDownloading) {
                cancelResourceRequest(ResourceServiceUtil.LISTEN);
                mIsSampleAudioDownloading = false;
            }
            prepareSampleAudio();
        }
    }

    /**
     * Click to down the audio whole resource
     */
    private class AudioDownloadClickListener implements OnClickListener {
        private static final String TAG = "AudioDownloadClickListener";
        int position;

        AudioDownloadClickListener(int index) {
            position = index;
        }

        @Override
        public void onClick(View v) {
            if (isAudioExist(mFileNames[position])) {
                switchSpeaker(position);
                return;
            }
            if (!Utils.isNetAvailable(mContext)) {
                showTips(getString(R.string.net_error_tips));
                return;
            }
            if (mDownloadList.size() == 0) {
                mDownloadList.add(position);
                handlePrepareDownload();
                return;
            }
            if (mDownloadList.contains(position)) {
                // just waiting till download success.
                showTips(getResources().getString(R.string.downloading_tts));
                return;
            }
            mDownloadList.add(position);
        }
    }

    private class SpeakerSwitchClickListener implements View.OnClickListener {
        private static final String TAG = "SpeakerSwitchClickListener";
        private int position;
        SpeakerSwitchClickListener(int index) {
            position = index;
        }

        @Override
        public void onClick(View v) {
            String name = mFileNames[position];
            if (!isAudioExist(name)) {
                showTips(getString(R.string.speaker_switch_error));
                return;
            }
            if (mSelectedIndex != position) {
                switchSpeaker(position);
            }
        }
    }

    public void switchSpeaker(int newIndex) {
        if(newIndex < 0 || newIndex >= mFileNames.length) {
            LogTag.w(TAG, "switchSpeaker: index out of range. newIndex:" + newIndex);
            return;
        }
        if (Utils.switchSpeakerName(mResourceServiceUtil, mFileNames[newIndex])) {
            Button downloadButton = (Button) mSpeakerListView.findViewWithTag(LIST_TTS_DOWNLOAD_PREFIX + mSelectedIndex);
            ImageView switchImgView = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_SWITCH_PREFIX + mSelectedIndex);
            if(switchImgView != null) {
                switchImgView.setVisibility(View.GONE);
            }
            if(downloadButton != null) {
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setText(R.string.use_tts);
            }

            // select index updated
            mSelectedIndex = newIndex;
            downloadButton = (Button) mSpeakerListView.findViewWithTag(LIST_TTS_DOWNLOAD_PREFIX + mSelectedIndex);
            switchImgView = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_SWITCH_PREFIX + mSelectedIndex);
            if(switchImgView != null) {
                switchImgView.setVisibility(View.VISIBLE);
            }
            if(downloadButton != null) {
                downloadButton.setVisibility(View.INVISIBLE);
            }
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.IFLY_SPEAKER_INDEX, mSelectedIndex);
        } else {
            showTips(getResources().getString(R.string.speaker_switch_error));
        }
    }

    private void getTTSList() {
        if (!mIsGettingList) {
            mIsGettingList = true;
            Bundle params = new Bundle();
            params.putInt(ResourceServiceUtil.KEY_TIMEOUT, GET_LIST_TIMEOUT);
            int errorCode = mResourceServiceUtil.getList(params);
            if (SpeechError.SUCCESS != errorCode) {
                try {
                    mResourceServiceListener.onEvent(ResourceServiceUtil.EVENT_GET_LIST_ERROR, errorCode, 0, null);
                } catch (Throwable e) {
                    LogTag.e(TAG, "", e);
                }
            }
        } else {
            LogTag.w(TAG, "get list processing...");
        }
    }

    private void handleAudioListenStart() {
        if (isItemVisibleByPosition(mTTSPlayIndex)) {
            ImageView imagePlayView = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PLAY_PREFIX + mTTSPlayIndex);
            if(imagePlayView != null) {
                imagePlayView.setImageLevel(TTS_STATE_PLAYING);
                imagePlayView.setContentDescription(getString(R.string.pause_sample_audio));
            }
        }
    }

    private void handleAudioListenEnd() {
        if (isItemVisibleByPosition(mTTSPlayIndex)) {
            ImageView imagePlayView = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PLAY_PREFIX + mTTSPlayIndex);
            if(imagePlayView != null) {
                imagePlayView.setImageLevel(TTS_STATE_IDLE);
                imagePlayView.setContentDescription(getString(R.string.play_sample_audio));
            }
        }
        mTTSPlayIndex = -1;
    }

    /**
     * Start a new download if list is not empty.
     */
    private void handlePrepareDownload() {
        if (mDownloadList.size() <= 0) {
            return;
        }
        mDownSpeakerPos = mDownloadList.get(0);
        sendMessage(MSG_DOWNLOAD_START);
        if (mListData == null) {
            getTTSList();
            return;
        }
        if (mDownSpeakerPos != -1) {
            audioDownloadCore();
        }
    }

    private void audioDownloadCore() {
        if (!mIsDownloadingResource) {
            mIsDownloadingResource = true;
            if (requestResource(ResourceServiceUtil.RESOURCE, mDownSpeakerPos)) {
                sendMessage(MSG_DOWNLOAD_START);
            }
        } else {
            LogTag.w(TAG, "lib downloading...");
        }
    }

    private void handleDownloadStart() {
        if (isItemVisibleByPosition(mDownSpeakerPos)) {
            Button downloadView = (Button) mSpeakerListView.findViewWithTag(LIST_TTS_DOWNLOAD_PREFIX + mDownSpeakerPos);
            ImageView processView = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PROCESS_PREFIX + mDownSpeakerPos);
            if (downloadView != null) {
                downloadView.setVisibility(View.GONE);
            }
            if(processView != null) {
                processView.setVisibility(View.VISIBLE);
                processView.setContentDescription(mContext.getString(R.string.downloading_tts));
            }
        }
        mDownProgress = 0;
    }

    private void handleDownloadProgress(final int progress) {
        if (isItemVisibleByPosition(mDownSpeakerPos)) {
            // update progress
            LogTag.v(TAG, "downloading progress is: " + progress);
            ImageView processImg = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PROCESS_PREFIX + mDownSpeakerPos);
            if(processImg != null) {
                processImg.setImageLevel((int)(progress / DOWNLOAD_PROCESS_OFFSET));
            }
        }
    }

    private void handleDownloadError() {
        if (isItemVisibleByPosition(mDownSpeakerPos)) {
            Button downloadButton = (Button) mSpeakerListView.findViewWithTag(LIST_TTS_DOWNLOAD_PREFIX + mDownSpeakerPos);
            ImageView processImg = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PROCESS_PREFIX + mDownSpeakerPos);
            if (downloadButton != null) {
                downloadButton.setText(R.string.download_tts);
                downloadButton.setVisibility(View.VISIBLE);
            }
            if(processImg != null) {
                processImg.setVisibility(View.GONE);
            }
        }
        if (mDownloadList.size() > 0) {
            mDownloadList.remove(0);
        }
        mDownSpeakerPos = -1;
    }

    private void handleDownloadSuccess() {
        if (isItemVisibleByPosition(mDownSpeakerPos)) {
            Button downloadButton = (Button) mSpeakerListView.findViewWithTag(LIST_TTS_DOWNLOAD_PREFIX + mDownSpeakerPos);
            ImageView processImg = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PROCESS_PREFIX + mDownSpeakerPos);
            ImageView playImageView = (ImageView) mSpeakerListView.findViewWithTag(LIST_TTS_PLAY_PREFIX + mDownSpeakerPos);
            if(downloadButton != null) {
                downloadButton.setEnabled(true);
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setText(R.string.use_tts);
                downloadButton.setContentDescription(getString(R.string.use_tts));
            }
            if(playImageView != null) {
                playImageView.setEnabled(true);
            }
            if(processImg != null) {
                processImg.setVisibility(View.GONE);
            }
        }
        if (mDownloadList.size() > 0) {
            mDownloadList.remove(0);
        }
        mDownSpeakerPos = -1;
    }

    private void showTips(final String msg) {
        sendMessage(MSG_TIPS, msg);
    }

    private void sendMessage(int what) {
        Message.obtain(mHandler, what).sendToTarget();
    }

    private void sendMessage(int what, int arg1, int arg2) {
        Message.obtain(mHandler, what, arg1, arg2).sendToTarget();
    }

    private void sendMessage(int what, Object obj) {
        Message msg = Message.obtain(mHandler, what);
        msg.obj = obj;
        msg.sendToTarget();
    }

    private boolean requestResource(String subResType, int requestIndex) {
        boolean result = true;
        Bundle params = new Bundle();
        params.putString(ResourceServiceUtil.KEY_SUB_RES_TYPE, subResType);
        params.putString(ResourceServiceUtil.KEY_NAME, mFileNames[requestIndex]);
        int errorCode = mResourceServiceUtil.download(params);
        if (SpeechError.SUCCESS != errorCode) {
            try {
                mResourceServiceListener.onEvent(ResourceServiceUtil.EVENT_DOWNLOAD_ERROR, errorCode, 0, params);
            } catch (Throwable e) {
                LogTag.e(TAG, "requestResource: ", e);
            }
            result = false;
        }
        LogTag.d(TAG, "resource request, subType:" + subResType + ", index:" + requestIndex + ", errorCode:" + errorCode);
        return result;
    }

    private boolean cancelResourceRequest(String subResType) {
        boolean result = true;
        Bundle params = new Bundle();
        params.putString(ResourceServiceUtil.KEY_SUB_RES_TYPE, subResType);
        int errorCode = mResourceServiceUtil.cancelDownload(params);
        if (SpeechError.SUCCESS != errorCode) {
            try {
                mResourceServiceListener.onEvent(ResourceServiceUtil.EVENT_DOWNLOAD_ERROR, errorCode, 0, params);
            } catch (Throwable e) {
                LogTag.e(TAG, "", e);
            }
            result = false;
        }
        LogTag.d(TAG, "cancelResourceRequest:  errorCode:" + errorCode);
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mResourceServiceUtil.cancelGetList(null);
        mIsGettingList = false;
        cancelResourceRequest(ResourceServiceUtil.RESOURCE);
        mIsDownloadingResource = false;
        cancelResourceRequest(ResourceServiceUtil.LISTEN);
        mIsSampleAudioDownloading = false;
        // release service.
        mResourceServiceUtil.destroy();
        mDownloadList.clear();
        releaseMediaPlayer();
        LogTag.d(TAG, "onDestroy");
    }
}
