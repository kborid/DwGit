package com.smartisanos.ideapills.remind;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.SmtPCUtils;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.WallpaperUtils;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.util.BubbleSpeechPlayer;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.view.BubbleAttachmentLayout;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.view.BubbleSpeechWaveView;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import smartisanos.api.IntentSmt;

public class RemindBubbleShowActivity extends Activity implements View.OnClickListener, Runnable {
    public static final String TAG = "RemindBubbleShowActivity";
    public static final String EXTRA_FROM = "extra_from";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_ID = "extra_id";
    private View mContainer;
    private View mContentView;
    private Animator mStartAnim;
    private static int flagIdeaRemind = 10;
    private BubbleItem mCurrentBubbleItem;
    private View mFakeBubbleView;
    private View mItemView;
    private Anim mAnimAlpha = null;
    private BubbleItem mClickItem = null;
    private BubbleSpeechWaveView mSpeechWaveView = null;
    private ImageView mPlayView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (SmtPCUtils.isValidExtDisplayId(this)) {
            setTheme(R.style.RevoneRemindActivity);
        }
        super.onCreate(savedInstanceState);
        Tracker.onEvent("A420022");
        if (!initFromIntent(getIntent())) {
            superFinish();
            return;
        }
        mContentView = LayoutInflater.from(this).inflate(R.layout.remind_act_bubble_show, null);
        setContentView(mContentView);
        mContainer = findViewById(R.id.container);
        mFakeBubbleView = findViewById(R.id.fake_bubble_content);
        mCurrentBubbleItem = getBublleAndInitView(getIntent());
        if (mCurrentBubbleItem == null) {
            superFinish();
            return;
        }
        BubbleController.getInstance().hideBubbleListImmediately();
        if (SmtPCUtils.isValidExtDisplayId(this)) {
            WallpaperUtils.gaussianBlurWallpaper(this, mContentView);
            mContainer.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
        } else {
            findViewById(R.id.rl_whole_layout).setOnClickListener(this);
        }
        startWithAnim();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tracker.onEvent("A420022");
        if (!initFromIntent(intent)) {
            return;
        }
        mCurrentBubbleItem = getBublleAndInitView(intent);
        if (mCurrentBubbleItem == null) {
            superFinish();
            return;
        }
    }

    private boolean initFromIntent(Intent intent) {
        if (intent == null || !Utils.isIdeaPillsEnable(this)) {
            return false;
        }
        boolean showNotification = intent.getBooleanExtra(EXTRA_FROM, false);
        if (showNotification) {
            showNotification(intent);
            return false;
        }
        return true;
    }

    private BubbleItem getBublleAndInitView(Intent intent) {
        int alertID = intent.getIntExtra(EXTRA_ID, -1);
        BubbleItem item = GlobalBubbleManager.getInstance().getBubbleItemById(alertID);
        if (item != null) {
            initView(item);
        }
        return item;
    }

    private void showNotification(Intent intent){
        if (intent == null) {
            return;
        }

        long alertTime = intent.getLongExtra(EXTRA_TIME, System.currentTimeMillis());
        List<BubbleItem> bubbles = GlobalBubbleManager.getInstance().getAlertBubbles(
                alertTime);
        if (bubbles.isEmpty()) {
            return;
        }
        // alert all bubble
        for(BubbleItem item : bubbles){
            if (item == null) {
                return;
            }
            flagIdeaRemind++;
            flagIdeaRemind = flagIdeaRemind >= Integer.MAX_VALUE ? 10 : flagIdeaRemind;
            Context context;
            if (BubbleController.getInstance().isExtDisplay()) {
                context = BubbleController.getInstance().getContext();
            } else {
                context = this;
            }
            Intent notifyActivity = new Intent(context, RemindBubbleShowActivity.class);
            IntentSmt.putSmtExtra(notifyActivity, "window-type", "window_without_caption_view");
            notifyActivity.putExtra(EXTRA_ID, item.getId());
            PendingIntent remindPendingIntent = PendingIntent.getActivity(context, flagIdeaRemind, notifyActivity,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            String strUri = Settings.System.getString(getContentResolver(),
                    smartisanos.api.SettingsSmt.System.IDEA_PILLS_RINGTONE_URI);

            String title = item.getSingleText();
            String content = CommonUtils.getNotifyDate(context, item.getDueDate(), true);

            final Notification.Builder builder = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setContentIntent(remindPendingIntent)
                    .setSmallIcon(R.drawable.pill_small_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.pill_large_icon))
                    .setWhen(System.currentTimeMillis())
                    .setTicker(title)
                    .setAutoCancel(true)
                    .setSound(TextUtils.isEmpty(strUri) ? null : Uri.parse(strUri));

            String channelId = "idea_pills_remind_Id"+ SystemClock.currentThreadTimeMillis();
            CharSequence name = "IdeaPillsName"+ SystemClock.currentThreadTimeMillis();

            MultiSdkUtils.sendNotification(this, channelId, name, flagIdeaRemind, builder);
        }
    }

    private void startWithAnim() {
        mContainer.setVisibility(View.INVISIBLE);
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                View whole = findViewById(R.id.rl_whole_layout);
                ViewGroup.LayoutParams params = whole.getLayoutParams();
                params.width = whole.getWidth();
                params.height = whole.getHeight();
                whole.setLayoutParams(params);

                mStartAnim = runEnterAnim();
            }
        });
    }

    private Animator runEnterAnim() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        final View tipView = mContainer.findViewById(R.id.tip_content);
        final ViewGroup.MarginLayoutParams tipViewParams = (ViewGroup.MarginLayoutParams) tipView.getLayoutParams();

        final View largeView = mItemView.findViewById(R.id.large_content);

        final int containerWidth = mContainer.getWidth();
        final int containerHeight = mContainer.getHeight();
        final int tipViewMargin = tipViewParams.topMargin;

        Log.d(TAG, String.format("runEnterAnim container view: w=%1$d, h=%2$d", containerWidth, containerHeight));

        int normalWidth = mFakeBubbleView.getWidth();
        int normalHeight = mFakeBubbleView.getHeight();

        final float scaleX = normalWidth / (float) containerWidth;
        final float scaleY = normalHeight / (float) containerHeight;

        Log.d(TAG, String.format("runEnterAnim normal view: w=%1$d, h=%2$d", normalWidth, normalHeight));
        Log.d(TAG, String.format("runEnterAnim scale: x=%1$f, y=%2$f", scaleX, scaleY));

        Animator translate = ObjectAnimator.ofFloat(mFakeBubbleView, "translationX", (screenWidth + containerWidth) / 2, 0);
        translate.setDuration(250);
        translate.setInterpolator(new DecelerateInterpolator(1.5f));
        translate.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mContainer.setBackgroundResource(0);
                mContainer.setVisibility(View.VISIBLE);
                largeView.setVisibility(View.INVISIBLE);
                mFakeBubbleView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFakeBubbleView.setTranslationX(0);
            }
        });

        final ValueAnimator scale = ValueAnimator.ofFloat(0, 1);
        scale.setDuration(200);
        scale.setStartDelay(100);
        scale.setInterpolator(new DecelerateInterpolator(1.5f));
        scale.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                mContainer.setBackgroundResource(Utils.getBackgroudRes(true, mCurrentBubbleItem));
                largeView.setVisibility(View.VISIBLE);
                mFakeBubbleView.setVisibility(View.INVISIBLE);

                largeView.findViewById(R.id.ll_date).setAlpha(0);
                largeView.findViewById(R.id.bubble_attach_layout).setAlpha(0);
                largeView.findViewById(R.id.large_footer).setAlpha(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                updateContainerSize(containerWidth, containerHeight);

                tipViewParams.topMargin = tipViewParams.bottomMargin = tipViewMargin;
                tipView.setLayoutParams(tipViewParams);

                largeView.findViewById(R.id.ll_date).setAlpha(1f);
                largeView.findViewById(R.id.bubble_attach_layout).setAlpha(1f);
                largeView.findViewById(R.id.large_footer).setAlpha(1f);
            }

        });
        scale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();

                int width = (int) (((1 - scaleX) * value + scaleX) * containerWidth);
                int height = (int) (((1 - scaleY) * value + scaleY) * containerHeight);
                updateContainerSize(width, height);

                float marginScale = (1 - scaleY) * value + scaleY;
                tipViewParams.topMargin = (int) (marginScale * tipViewMargin);
                tipViewParams.bottomMargin = tipViewParams.topMargin;
                tipView.setLayoutParams(tipViewParams);

                if (value > 0.8) {
                    float fraction = (value - 0.8f) * 5;
                    largeView.findViewById(R.id.ll_date).setAlpha(fraction);
                    largeView.findViewById(R.id.bubble_attach_layout).setAlpha(fraction);
                    largeView.findViewById(R.id.large_footer).setAlpha(fraction);
                }
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mContainer.setBackgroundResource(Utils.getBackgroudRes(true, mCurrentBubbleItem));
                updateContainerSize(containerWidth, containerHeight);

                tipViewParams.topMargin = tipViewParams.bottomMargin = tipViewMargin;
                tipView.setLayoutParams(tipViewParams);

                mFakeBubbleView.setVisibility(View.INVISIBLE);
                largeView.setVisibility(View.VISIBLE);
                largeView.findViewById(R.id.ll_date).setAlpha(1f);
                largeView.findViewById(R.id.bubble_attach_layout).setAlpha(1f);
                largeView.findViewById(R.id.large_footer).setAlpha(1f);
            }
        });

        set.playSequentially(translate, scale);
        set.start();
        return set;
    }

    private Animator runExitAnimAndFinish() {
        if (mContainer == null) {
            superFinish();
            return null;
        }
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        final View tipView = mContainer.findViewById(R.id.tip_content);
        final ViewGroup.MarginLayoutParams tipViewParams = (ViewGroup.MarginLayoutParams) tipView.getLayoutParams();

        final View largeView = mItemView.findViewById(R.id.large_content);

        final int containerWidth = mContainer.getWidth();
        final int containerHeight = mContainer.getHeight();
        final int tipViewMargin = tipViewParams.topMargin;

        Log.d(TAG, String.format("runExitAnimAndFinish container view: w=%1$d, h=%2$d", containerWidth, containerHeight));

        int normalWidth = mFakeBubbleView.getWidth();
        int normalHeight = mFakeBubbleView.getHeight();

        final float scaleX = normalWidth / (float) containerWidth;
        final float scaleY = normalHeight / (float) containerHeight;

        Log.d(TAG, String.format("runExitAnimAndFinish normal view: w=%1$d, h=%2$d", normalWidth, normalHeight));
        Log.d(TAG, String.format("runExitAnimAndFinish scale: x=%1$f, y=%2$f", scaleX, scaleY));

        final ValueAnimator scale = ValueAnimator.ofFloat(0, 1);
        scale.setDuration(250);
        scale.setInterpolator(new DecelerateInterpolator(1.5f));

        scale.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                largeView.findViewById(R.id.ll_date).setVisibility(View.GONE);
                largeView.findViewById(R.id.bubble_attach_layout).setVisibility(View.GONE);
                largeView.findViewById(R.id.large_footer).setVisibility(View.GONE);

                TextView contentTv = (TextView) largeView.findViewById(R.id.tv_content);
                contentTv.setSingleLine(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mContainer.setBackgroundResource(0);
                updateContainerSize(containerWidth, containerHeight);

                tipViewParams.topMargin = tipViewMargin;
                tipViewParams.bottomMargin = tipViewMargin;
                tipView.setLayoutParams(tipViewParams);

                largeView.setVisibility(View.INVISIBLE);
                mFakeBubbleView.setVisibility(View.VISIBLE);
            }
        });

        scale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();

                float width = (1 - (1 - scaleX) * value) * containerWidth;
                float height = (1 - (1 - scaleY) * value) * containerHeight;

                updateContainerSize(width, height);

                float marginScale = 1 - (1 - scaleY) * value;
                tipViewParams.topMargin = (int) (marginScale * tipViewMargin);
                tipViewParams.bottomMargin = tipViewParams.topMargin;
                tipView.setLayoutParams(tipViewParams);
            }
        });

        Animator translate = ObjectAnimator.ofFloat(mFakeBubbleView, "translationX", 0, (screenWidth + containerWidth) / 2);
        translate.setDuration(250);
        translate.setStartDelay(100);
        translate.setInterpolator(new DecelerateInterpolator(1.5f));

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                superFinish();
            }
        });
        set.playSequentially(scale, translate);
        set.start();
        return set;
    }

    private void updateContainerSize(float width, float height) {
        ViewGroup.LayoutParams params = mContainer.getLayoutParams();
        params.width = (int) width;
        params.height = (int) height;
        mContainer.setLayoutParams(params);
    }


    private void finishWithAnim() {
        if (mStartAnim != null && mStartAnim.isRunning()) {
            mStartAnim.cancel();
        }
        runExitAnimAndFinish();
    }

    @Override
    protected void onDestroy() {
        AlarmUtils.scheduleNextAlarm(this, null);
        super.onDestroy();
    }

    @Override
    public void finish() {
        finishWithAnim();
    }

    private void superFinish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.rl_whole_layout) {
            finish();
        } else if (v.getId() == R.id.tv_ok) {
            finish();
        }
    }

    private void initView(BubbleItem item) {
        mContainer.setBackgroundResource(Utils.getBackgroudRes(true, item));
        if (item.isShareColor()) {
            mItemView = findViewById(R.id.share_content_layout_real);
            if (mItemView == null) {
                ViewStub content = (ViewStub) findViewById(R.id.share_content_layout);
                mItemView = content.inflate();
            }
        } else {
            mItemView = findViewById(R.id.content_layout_real);
            if (mItemView == null) {
                ViewStub content = (ViewStub) findViewById(R.id.content_layout);
                mItemView = content.inflate();
            }
        }

        TextView dateTv = (TextView) mItemView.findViewById(R.id.tv_date);
        TextView showDateTv = (TextView) mItemView.findViewById(R.id.tv_show_date);
        if (item.getType() == BubbleItem.TYPE_VOICE_OFFLINE) {
            dateTv.setVisibility(View.GONE);
        } else {
            Date date = new Date(item.getTimeStamp());
            SimpleDateFormat dateFormat = new SimpleDateFormat(getResources().getString(R.string.bubble_datetime_format));
            dateTv.setText(dateFormat.format(date));
        }

        if (item.getDueDate() > 0) {
            mItemView.findViewById(R.id.show_date_layout).setVisibility(View.VISIBLE);

            long remindTime = item.getRemindTime();
            long dueTime = item.getDueDate();

            if (remindTime == 0L) {
                // detail set is total day notify
                showDateTv.setText(CommonUtils.getNotifyDate(getApplicationContext(), dueTime, false));
            } else if (remindTime > 0L) {
                long currentTime = System.currentTimeMillis();
                if (remindTime > currentTime) {
                    // detail set is need notify time
                    showDateTv.setText(CommonUtils.getNotifyDate(getApplicationContext(), dueTime, true));
                } else {
                    // detail notify time already overdue
                    showDateTv.setText(CommonUtils.getNotifyDate(getApplicationContext(), dueTime, true));
                }
            }
        }

        TextView contentTv = (TextView) mItemView.findViewById(R.id.tv_content);
        if (!TextUtils.isEmpty(item.getText())) {
            contentTv.setText(item.getText());
        } else {
            contentTv.setVisibility(View.GONE);
        }

        if (item.haveAttachments()) {
            BubbleAttachmentLayout attachmentLayout = (BubbleAttachmentLayout) mItemView.findViewById(R.id.bubble_attach_layout);
            attachmentLayout.setAttachmentList(item.getAttachments());
            attachmentLayout.setVisibility(View.VISIBLE);
            contentTv.setMaxLines(3);
        }

        TextView tvOk = (TextView) mItemView.findViewById(R.id.tv_ok);
        tvOk.setOnClickListener(this);
        mFakeBubbleView.setBackgroundResource(Utils.getBackgroudRes(false, item));
        TextView mFakeTv = (TextView) mFakeBubbleView.findViewById(R.id.fake_bubble_text);
        mFakeTv.setText(item.getSingleText());
        ImageView fakePlayBtn = (ImageView) mFakeBubbleView.findViewById(R.id.fak_bubble_play);

        if (item.isShareColor()) {
            mItemView.setPadding(getResources().getDimensionPixelSize(R.dimen.drag_padding_left_share), 0, 0, 0);
            if(!item.isShareFromOthers()){
                dateTv.setTextColor(getResources().getColor(R.color.bubble_date_text_color_share));
                contentTv.setTextColor(getResources().getColor(R.color.bubble_text_color_share));
                tvOk.setTextColor(getResources().getColor(R.color.bubble_text_color_share));
                showDateTv.setTextColor(getResources().getColor(R.color.bubble_date_text_color_share));
                mFakeTv.setTextColor(getResources().getColor(R.color.bubble_text_color_share));
                fakePlayBtn.setImageResource(R.drawable.play_icon_share);
            }
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) mFakeTv.getLayoutParams();
            param.leftMargin = getResources().getDimensionPixelSize(R.dimen.remind_fake_bubble_margin_left_share);
            mFakeTv.setLayoutParams(param);
        }

        View voiceLayout = findViewById(R.id.voice_bubble_layout);
        if (item.isVoiceBubble()) {
            voiceLayout.setVisibility(View.VISIBLE);
            mClickItem = item;
            mSpeechWaveView = (BubbleSpeechWaveView) findViewById(R.id.v_bubble_speech_wave);
            mPlayView = (ImageView) findViewById(R.id.iv_bubble_play);
            mSpeechWaveView.setMaxDuration((int) item.getVoiceDuration());
            mSpeechWaveView.setWaveData(item.getWaveData());
            mSpeechWaveView.setCurPosition(0);
            if (mAnimAlpha != null) {
                mAnimAlpha.cancel();
            }
            mSpeechWaveView.setShowMiddle(false);
            mPlayView.setVisibility(View.VISIBLE);

            mPlayView.setImageDrawable(getResources().getDrawable(getPlayImageRes(item)));
            mPlayView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (BubbleSpeechPlayer.getInstance(getApplicationContext()).isPlayingBubble(mClickItem)) {
                        stopPlay();
                    } else {
                        playSpeech();
                    }
                }
            });

            mSpeechWaveView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    stopPlay();
                }
            });
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) mFakeTv.getLayoutParams();
            param.rightMargin = 0;
            mFakeTv.setLayoutParams(param);
        } else {
            voiceLayout.setVisibility(View.GONE);
            fakePlayBtn.setVisibility(View.GONE);
        }
    }

    private int getPlayImageRes(BubbleItem bubbleItem) {
        if (BubbleSpeechPlayer.getInstance(this).isPlayingBubble(bubbleItem)) {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return R.drawable.bubble_pop_stop_share;
            } else {
                return R.drawable.bubble_pop_stop;
            }
        } else {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return R.drawable.bubble_pop_start_share;
            } else {
                return R.drawable.bubble_pop_start;
            }
        }
    }

    private int getPlayImageRes(BubbleItem bubbleItem, boolean isStart) {
        if (!isStart) {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return R.drawable.bubble_pop_stop_share;
            } else {
                return R.drawable.bubble_pop_stop;
            }
        } else {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return R.drawable.bubble_pop_start_share;
            } else {
                return R.drawable.bubble_pop_start;
            }
        }
    }

    @Override
    public void run(){
        if (BubbleSpeechPlayer.getInstance(getApplicationContext()).isPlayingBubble(mClickItem)) {
            mSpeechWaveView.setCurPosition(BubbleSpeechPlayer.getInstance(getApplicationContext()).getCurrentPosition(mClickItem));
            UIHandler.post(this);
        } else {
            mSpeechWaveView.setCurPosition(0);
            if (mAnimAlpha != null) {
                mAnimAlpha.cancel();
            }
            mSpeechWaveView.setShowMiddle(false);
            mPlayView.setVisibility(View.VISIBLE);
        }
    }

    private void stopPlay(){
        if (BubbleSpeechPlayer.getInstance(getApplicationContext()).isPlayingBubble(mClickItem)) {
            BubbleSpeechPlayer.getInstance(getApplicationContext()).stop();
            run();
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mPlayView.setImageDrawable(getResources().getDrawable(getPlayImageRes(mClickItem, true)));
        } else {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPlayView.setImageDrawable(getResources().getDrawable(getPlayImageRes(mClickItem, true)));
                }
            });
        }
    }

    private void playSpeech() {
        BubbleSpeechPlayer.getInstance(getApplicationContext()).playSpeech(mClickItem, new BubbleSpeechPlayer.SpeechPlayerCallBack() {

            @Override
            public void onStarted(BubbleItem item, boolean isStarted) {
                if (isStarted && item == mClickItem) {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mPlayView.setImageDrawable(getResources().getDrawable(getPlayImageRes(mClickItem, false)));
                            if (mAnimAlpha != null) {
                                mAnimAlpha.cancel();
                                mAnimAlpha = null;
                            }
                            mAnimAlpha = new Anim(mPlayView, Anim.TRANSPARENT, 150, Anim.CIRC_OUT, new Vector3f(0, 0, mPlayView.getAlpha()), Anim.INVISIBLE);
                            mAnimAlpha.setListener(new SimpleAnimListener() {
                                public void onStart() {
                                    mSpeechWaveView.setShowMiddle(true);
                                }

                                public void onComplete(int type) {
                                    if (type == Anim.ANIM_FINISH_TYPE_COMPLETE) {
                                        mPlayView.setAlpha(1.0f);
                                        mPlayView.setVisibility(View.INVISIBLE);
                                    }
                                }
                            });
                            mAnimAlpha.start();
                            RemindBubbleShowActivity.this.run();
                        }
                    });
                }
            }

            @Override
            public void onCompleted(BubbleItem item) {
                if (item == mClickItem) {
                    stopPlay();
                }
            }

            @Override
            public void onDisconnected(BubbleItem item) {
                if (item == mClickItem) {
                    onCompleted(mClickItem);
                }
            }

            @Override
            public void onFocusChanged(boolean isLossFocus) {
                if (isLossFocus) {
                    stopPlay();
                }
            }
        });
    }
}
