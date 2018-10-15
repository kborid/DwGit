package com.smartisanos.sara.bullet.widget;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.util.AnimationUtils;

public class VoiceRecognizeView extends RelativeLayout implements View.OnTouchListener {
    private static final int WAVE_COVER_DELAY_MILLIS = 150;
    private ImageView mWave;
    private ImageButton mVoiceSearch;
    private Vibrator mVibrator;
    private VoiceRecognizeListener mListener;
    private long mLastStartTime;
    private boolean mCancelNextTouchFlag;
    private boolean mIsLastTouchOutside;

    public interface VoiceRecognizeListener {
        void startRecognize();

        void stopRecognize();

        void shortRecognize();

        void outOfTouchRange();
    }

    public VoiceRecognizeView(Context context) {
        this(context, null);
    }

    public VoiceRecognizeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceRecognizeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.voice_recognize_view, this);
        initChild();
        initVariable();
    }

    private void initChild() {
        mWave = (ImageView)findViewById(R.id.bullet_wave);
        mVoiceSearch = (ImageButton)findViewById(R.id.voice_search);
        mVoiceSearch.setOnTouchListener(this);
        mVoiceSearch.setHapticFeedbackEnabled(true);
    }

    private void initVariable() {
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void registerVoiceRecognizeListener(VoiceRecognizeListener listener) {
        mListener = listener;
    }

    public void startRecognizeWave() {
        AnimationUtils.startRecordWithAnimation(mWave);
    }

    public void stopRecognizeWave() {
        AnimationUtils.stopRecordWithAnimation(mWave);
    }

    public void dismissWave() {
        if (mWave != null) {
            AnimationUtils.cancelAnimation(mWave);
            mWave.clearAnimation();
            mWave.setVisibility(View.GONE);
        }
    }

    public void cancelTouch() {
        mCancelNextTouchFlag = true;
    }

    public void setVoiceEnable(boolean enable) {
        mVoiceSearch.setEnabled(enable);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsLastTouchOutside = false;
                mCancelNextTouchFlag = false;
                mVibrator.vibrate(50);
                mLastStartTime = System.currentTimeMillis();
                if (mListener != null) {
                    mListener.startRecognize();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mCancelNextTouchFlag) {
                    AnimationUtils.stopRecordWithAnimation(mWave);
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mLastStartTime <= WAVE_COVER_DELAY_MILLIS) {
                        v.setClickable(true);
                        if (mListener != null) {
                            mListener.shortRecognize();
                        }
                    } else {
                        if (mListener != null) {
                            mListener.stopRecognize();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mCancelNextTouchFlag) {
                    handleActionMove(v, event);
                }
                break;
            default:
                break;
        }
        return false;
    }

    private void handleActionMove(View view, MotionEvent motionEvent) {
        if (isOutSideVoiceButton(view, motionEvent) && !mIsLastTouchOutside) {
            mIsLastTouchOutside = true;
            mVoiceSearch.setEnabled(false);
            mVoiceSearch.setPressed(false);
            if (mListener != null) {
                mListener.outOfTouchRange();
            }
        }
    }

    private boolean isOutSideVoiceButton(View view, MotionEvent motionEvent) {
        float currentX = motionEvent.getX();
        float currentY = motionEvent.getY();
        int offset = 50;
        if (currentX <= view.getLeft() - offset || currentX >= view.getRight() + offset) {
            return true;
        }
        if (currentY <= view.getTop() - offset || currentY >= view.getBottom() + offset) {
            return true;
        }
        return false;
    }
}
