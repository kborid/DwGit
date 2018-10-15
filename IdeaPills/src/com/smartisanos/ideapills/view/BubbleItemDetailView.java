package com.smartisanos.ideapills.view;

import android.app.SmtPCUtils;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrixColorFilter;
import android.os.Looper;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.DisplayInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.view.menu.ContextMenuBuilder;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.remind.RemindAlarmSettingActivity;
import com.smartisanos.ideapills.remind.RemindAlarmSettingDialog;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.util.AttachmentUtils;
import com.smartisanos.ideapills.util.BubbleSpeechPlayer;
import com.smartisanos.ideapills.util.BubbleTrackerID;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.util.ViewUtils;
import com.smartisanos.ideapills.common.util.UIHandler;

import smartisanos.util.BulletSmsUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.smartisanos.ideapills.common.util.TimeUtils;

public class BubbleItemDetailView extends RelativeLayout implements View.OnClickListener, BubbleSpeechPlayer.SpeechPlayerCallBack,
        Runnable, BubbleAttachmentLayout.OnBubbleChangedListener {
    private static LOG log = LOG.getInstance(BubbleItemDetailView.class);

    private BubbleItem mBubbleItem = null;

    protected BubbleEditText mTvTitle = null;
    protected BubbleScrollView mTvScroll = null;
    private View mDivideLineBeforeEdit;
    private ImageView mIvBubbleEdit = null;
    private ImageView mIvBubbleDele = null;
    private View mLineBeforeShare;
    private ImageView mIvShare = null;
    private View mLineBeforWeixin;
    private ImageView mIvBullet = null;
    private ImageView mIvWeixin = null;
    private View mLineBeforeCalendar;
    private ImageView mIvCalendar = null;
    private ImageView mIvAttach = null;
    private ImageView mIvOver = null;
    private TextView mTvDate = null;
    private LinearLayout mlloptBottons = null;
    private LinearLayout mllinputopt = null;
    private ImageView mIvCancel = null;
    private ImageView mIvEditAttach = null;
    private View mDivideLine = null;

    private BubbleSpeechWaveView mVBubbleSpeechWave = null;
    private ImageView mIvBubblePlay = null;
    private TextView mTVOfflineTag = null;
    private View mVofflineTagLine = null;
    private TextView mTVOfflineHint = null;
    private FrameLayout mFlBubbleSpeechWave = null;
    private View mDateView;
    private View mConflictTag;
    private BubbleAttachmentLayout mBubbleAttachmentLayout;

    private View mNotification;

    private BubbleItemView parent;
    private Anim mAnimAlpha = null;
    private boolean  mCurrentNeedAnim = false;
    private boolean  mCurrentIsMaxHeight = false;

    private BubbleEditText.EditModeChangeListener mEditModeChangeListener = new BubbleEditText.EditModeChangeListener() {
        @Override
        public void onEditText(String txt) {
            if (txt != null && !txt.equals(mBubbleItem.getText())) {
                mBubbleItem.setText(txt);
                mBubbleItem.setEdited();
            }
        }

        public boolean onFinishEdit() {
            return finishInput();
        }
    };

    protected OnTouchListener mIconTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((ImageView) v).setBackgroundResource(R.drawable.down_setting);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ((ImageView) v).setBackground(null);
                    break;
            }
            return false;
        }
    };

    public BubbleItemDetailView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleItemDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mDateView = findViewById(R.id.ll_date);
        mTvTitle = (BubbleEditText) findViewById(R.id.tv_title);
        mTvScroll = (BubbleScrollView) findViewById(R.id.tv_scroll);
        mTvTitle.setIBubbleStateListener(mBubbleStateListener);
        mTvTitle.registerEditModeChangeListener(mEditModeChangeListener);
        mTvTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_NEXT) {
                    finishInput(true);
                    return true;
                }
                return false;
            }
        });

        mTvTitle.registerOnEditLineChangeListener(new BubbleEditText.OnEditLineChangeListener() {
            @Override
            public void onEditLineChange() {
                setMaxHeight(mCurrentIsMaxHeight, mCurrentNeedAnim);
            }
        });
        mNotification = findViewById(R.id.notification);

        mTvDate = (TextView) findViewById(R.id.tv_date);
        mIvBubbleEdit = (ImageView) findViewById(R.id.iv_bubble_edit);
        mIvBubbleEdit.setOnTouchListener(mIconTouchListener);
        mIvBubbleEdit.setOnClickListener(this);
        mDivideLineBeforeEdit = findViewById(R.id.v_line_before_edit);
        mIvBubbleDele = (ImageView) findViewById(R.id.iv_bubble_del);
        mIvBubbleDele.setOnTouchListener(mIconTouchListener);
        mIvBubbleDele.setOnClickListener(this);
        mIvShare = (ImageView) findViewById(R.id.iv_bubble_share);
        mIvShare.setOnTouchListener(mIconTouchListener);
        mIvShare.setOnClickListener(this);
        mLineBeforeShare = findViewById(R.id.iv_bubble_share_divider);
        mIvBullet = (ImageView) findViewById(R.id.iv_bubble_bullet);
        mIvBullet.setOnTouchListener(mIconTouchListener);
        mIvBullet.setOnClickListener(this);
        mIvWeixin = (ImageView) findViewById(R.id.iv_weixin);
        mIvWeixin.setOnTouchListener(mIconTouchListener);
        mIvWeixin.setOnClickListener(this);
        mIvAttach = (ImageView) findViewById(R.id.iv_attach);
        mIvAttach.setOnTouchListener(mIconTouchListener);
        mIvAttach.setOnClickListener(this);
        mLineBeforWeixin = findViewById(R.id.v_line_before_weixin);
        mIvCalendar = (ImageView) findViewById(R.id.iv_calendar);
        mIvCalendar.setOnTouchListener(mIconTouchListener);
        mIvCalendar.setOnClickListener(this);
        mLineBeforeCalendar = findViewById(R.id.iv_calendar_divider);
        mIvOver = (ImageView) findViewById(R.id.iv_over);
        mIvOver.setOnClickListener(this);
        mlloptBottons = (LinearLayout) findViewById(R.id.ll_optbottons);
        mllinputopt = (LinearLayout) findViewById(R.id.ll_inputopt);
        mIvCancel = (ImageView) findViewById(R.id.iv_cancel);
        mIvCancel.setOnClickListener(this);
        mIvEditAttach = (ImageView) findViewById(R.id.iv_edit_attach);
        mIvEditAttach.setOnClickListener(this);
        mDivideLine = findViewById(R.id.v_divider_line);

        mVBubbleSpeechWave = (BubbleSpeechWaveView) findViewById(R.id.v_bubblespeechwave);
        mVBubbleSpeechWave.setOnClickListener(this);
        mIvBubblePlay = (ImageView) findViewById(R.id.iv_bubble_play);
        mIvBubblePlay.setOnClickListener(this);
        mIvBubblePlay.setImageDrawable(getResources().getDrawable(getPlayImageRes(mBubbleItem)));
        mTVOfflineTag = (TextView) findViewById(R.id.tv_offline_tag);
        mVofflineTagLine = findViewById(R.id.v_offline_line);
        mTVOfflineHint = (TextView) findViewById(R.id.tv_offline_hint);
        mFlBubbleSpeechWave = (FrameLayout) findViewById(R.id.fl_bubblespeechwave);
        mConflictTag = findViewById(R.id.iv_conflict);
        mTvTitle.setCursorVisible(false);
        mBubbleAttachmentLayout = (BubbleAttachmentLayout) findViewById(R.id.bubble_attach_layout);
        mBubbleAttachmentLayout.setOnBubbleChangedListener(this);

        mLineBeforWeixin.setVisibility(GONE);
        mIvWeixin.setVisibility(GONE);
    }

    public void setBubbleItemView(BubbleItemView bubbleItemView) {
        parent = bubbleItemView;
    }

    public void setBubbleItem(BubbleItem bubbleItem) {
        mBubbleItem = bubbleItem;
        mVBubbleSpeechWave.setWaveData(mBubbleItem.getWaveData());
        refreshShowStyle(BubbleController.getInstance().isInPptContext(getContext()));
    }

    public void updatePptAddFocus(boolean hasFocus) {
        mBubbleAttachmentLayout.updatePptAddFocus(hasFocus);
    }

    public void forceChangeToPptMode() {
        refreshShowStyle(true);
        mBubbleAttachmentLayout.reAddPptHolder(true);
    }

    private void refreshShowStyle(boolean isInPptMode) {
        if (mBubbleItem.isText()) {
            mFlBubbleSpeechWave.setVisibility(GONE);
        } else {
            mFlBubbleSpeechWave.setVisibility(VISIBLE);
        }
        List<AttachMentItem> list = mBubbleItem.getAttachments();
        if ((list == null || list.isEmpty()) && !isInPptMode) {
            mBubbleAttachmentLayout.setVisibility(GONE);
        } else {
            mBubbleAttachmentLayout.setVisibility(VISIBLE);
            if (mBubbleItem.needRefreshAttachment()) {
                mBubbleItem.setRefreshAttachment(false);
                mBubbleAttachmentLayout.forceRefresh();
            }
            mBubbleAttachmentLayout.setAttachmentList(list);
        }

        refreshNotiView();

        mIvCalendar.setVisibility(isInPptMode ? GONE : VISIBLE);
        mLineBeforeCalendar.setVisibility(isInPptMode ? GONE : VISIBLE);
        mIvShare.setVisibility(isInPptMode ? GONE : VISIBLE);
        mLineBeforeShare.setVisibility(isInPptMode ? GONE : VISIBLE);
        mIvBullet.setVisibility(isInPptMode ? GONE : VISIBLE);
    }

    public void refreshColorStyle() {
        if (mBubbleItem.isShareColor()) {
            mIvBubbleEdit.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_btn_bang : R.drawable.bubble_btn_bang_share);
            mIvEditAttach.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_btn_attach : R.drawable.bubble_btn_attach_share);
            mIvAttach.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_btn_attach : R.drawable.bubble_btn_attach_share);
            mIvCalendar.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_btn_calendar : R.drawable.bubble_btn_calendar_share);
            mIvShare.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_share : R.drawable.bubble_share_share);
            mIvBullet.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bullet_icon : R.drawable.bullet_icon_share);
            mTvTitle.setTextColor(getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_text_color : R.color.bubble_text_color_share));
            mTvTitle.setHintTextColor(getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_hint_text_color : R.color.bubble_hint_share_text_color));
            mTVOfflineTag.setTextColor(getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_text_color : R.color.bubble_text_color_share));
            mTVOfflineHint.setTextColor(getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_date_text_color : R.color.bubble_date_text_color_share));
            mTvDate.setTextColor(getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_date_text_color : R.color.bubble_date_text_color_share));
            mDivideLineBeforeEdit.setVisibility(INVISIBLE);
            mVBubbleSpeechWave.setPaintColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_wave_line_text_color : R.color.bubble_wave_line_text_color_share,
                    mBubbleItem.isShareFromOthers() ? R.color.bubble_wave_line_text_color_pass : R.color.bubble_wave_line_text_color_pass_share);
            mNotification.findViewById(R.id.noti_icon).setBackgroundResource(mBubbleItem.isShareFromOthers() ? R.drawable.unfold_remind_icon : R.drawable.unfold_remind_icon_share);
            ((TextView) mNotification.findViewById(R.id.noti_time)).setTextColor(
                    getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.noti_text_color : R.color.share_noti_text_color));
            mTvTitle.modifyCursorDrawable(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_cursor : R.drawable.bubble_cursor_share);

            mIvCancel.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_btn_cancel : R.drawable.bubble_btn_cancel_share);
            mIvOver.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_btn_finish : R.drawable.bubble_btn_finish_share);
            mIvBubbleDele.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.bubble_pop_del_share : R.drawable.bubble_pop_del);
        } else {
            mIvBubbleEdit.setImageResource(R.drawable.bubble_btn_bang);
            mIvEditAttach.setImageResource(R.drawable.bubble_btn_attach);
            mIvAttach.setImageResource(R.drawable.bubble_btn_attach);
            mIvCalendar.setImageResource(R.drawable.bubble_btn_calendar);
            mIvShare.setImageResource(R.drawable.bubble_share);
            mIvBullet.setImageResource(R.drawable.bullet_icon);
            mTvTitle.setTextColor(getResources().getColor(R.color.bubble_text_color));
            mTvTitle.setHintTextColor(getResources().getColor(R.color.bubble_hint_text_color));
            mTVOfflineTag.setTextColor(getResources().getColor(R.color.bubble_text_color));
            mTVOfflineHint.setTextColor(getResources().getColor(R.color.bubble_date_text_color));
            mTvDate.setTextColor(getResources().getColor(R.color.bubble_date_text_color));
            mDivideLineBeforeEdit.setVisibility(VISIBLE);
            mVBubbleSpeechWave.setPaintColor(R.color.bubble_wave_line_text_color,
                    R.color.bubble_wave_line_text_color_pass);
            mNotification.findViewById(R.id.noti_icon).setBackgroundResource(R.drawable.unfold_remind_icon);
            ((TextView) mNotification.findViewById(R.id.noti_time)).setTextColor(
                    getResources().getColor(R.color.noti_text_color));
            mTvTitle.modifyCursorDrawable(R.drawable.bubble_cursor);

            mIvCancel.setImageResource(R.drawable.bubble_btn_cancel);
            mIvOver.setImageResource(R.drawable.bubble_btn_finish);
            mIvBubbleDele.setImageResource(R.drawable.bubble_pop_del);
        }

        mIvBubblePlay.setImageDrawable(getResources().getDrawable(getPlayImageRes(mBubbleItem)));

        MarginLayoutParams llInputOptParam = (MarginLayoutParams) mllinputopt.getLayoutParams();
        if (mBubbleItem.isShareColor()) {
            llInputOptParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_left_margin_share);
            mllinputopt.setLayoutParams(llInputOptParam);
        } else {
            llInputOptParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_left_margin_full);
            mllinputopt.setLayoutParams(llInputOptParam);
        }
        MarginLayoutParams dateParam = (MarginLayoutParams) mDateView.getLayoutParams();
        if (mBubbleItem.isShareColor()) {
            dateParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.date_margin_share);
            mDateView.setLayoutParams(dateParam);
        } else {
            dateParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.date_margin_share_full);
            mDateView.setLayoutParams(dateParam);
        }
        LinearLayout.LayoutParams deleteParam = (LinearLayout.LayoutParams) mIvBubbleDele.getLayoutParams();
        if (mBubbleItem.isShareColor()) {
            deleteParam.weight = (float) 0;
            deleteParam.width = getResources().getDimensionPixelSize(R.dimen.delete_icon_width);
            mIvBubbleDele.setLayoutParams(deleteParam);
        } else {
            deleteParam.weight = (float) 1;
            deleteParam.width = 0;
            mIvBubbleDele.setLayoutParams(deleteParam);
        }
        MarginLayoutParams waveParam = (MarginLayoutParams) mFlBubbleSpeechWave.getLayoutParams();
        if (mBubbleItem.isShareColor()) {
            waveParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_left_margin_share);
            mFlBubbleSpeechWave.setLayoutParams(waveParam);
        } else {
            waveParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_left_margin_full);
            mFlBubbleSpeechWave.setLayoutParams(waveParam);
        }
    }

    private void refreshNotiView() {
        boolean show = false;
        TextView notifyTextView = ((TextView) mNotification.findViewById(R.id.noti_time));
        long remindTime = mBubbleItem.getRemindTime();
        long dueTime = mBubbleItem.getDueDate();
        boolean showNotifyIcon = false;

        if (dueTime > 0L) {
            if (remindTime == 0L) {
                // detail set is total day notify
                notifyTextView.setText(CommonUtils.getNotifyDate(getContext(), dueTime, false));
            } else if (remindTime > 0L) {
                long currentTime = System.currentTimeMillis();
                if (remindTime > currentTime) {
                    // detail set is need notify time
                    notifyTextView.setText(CommonUtils.getNotifyDate(getContext(), dueTime, true));
                    showNotifyIcon = true;
                } else {
                    // detail notify time already overdue
                    notifyTextView.setText(CommonUtils.getNotifyDate(getContext(), dueTime, true));
                }
            }

            if (showNotifyIcon) {
                mNotification.findViewById(R.id.noti_icon).setVisibility(VISIBLE);
            } else {
                mNotification.findViewById(R.id.noti_icon).setVisibility(GONE);
            }

            //cause recalculate normal width for show alert icon on normal size state.
            mBubbleItem.invalidateNormalWidth();
            show = true;
        }

        if (show) {
            mNotification.setVisibility(VISIBLE);
        } else {
            mNotification.setVisibility(GONE);
        }
    }

    public void onClick(View v) {
        if (!ViewUtils.isClickAvailable()) {
            return;
        }
        switch (v.getId()) {
            case R.id.iv_bubble_edit: {
                handleStartBoom();
            }
            break;
            case R.id.iv_bubble_del: {
                Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_DEL, "source", 1);
                parent.deleteItemAnim(BubbleItem.FLAG_NEEDTRASH);
            }
            break;
            case R.id.iv_bubble_share: {
                Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_SHARE, "source", 1);
                Utils.hideInputMethod(v);
                if (mBubbleItem != null) {
                    if (mBubbleItem.haveAttachments()) {
                        GlobalBubbleUtils.showSystemToast(getContext(),
                                getContext().getString(R.string.bubble_share_tip), Toast.LENGTH_SHORT);
                    }

                    if (!TextUtils.isEmpty(mBubbleItem.getText())) {
                        GlobalBubbleUtils.shareToApps(mContext, Arrays.asList(mBubbleItem));
                    }
                }
//                shareToApps();
            }
            break;
            case R.id.iv_bubble_bullet: {
                if (BulletSmsUtils.isBulletSmsInstalled(getContext())) {
                    if (mBubbleItem.haveAttachments()) {
                        GlobalBubbleUtils.showSystemToast(getContext(),
                                getContext().getString(R.string.bubble_share_tip), Toast.LENGTH_SHORT);
                    }
                    if (mBubbleItem.isTextAvailable()) {
                        GlobalBubbleUtils.shareToApp(getContext(), mBubbleItem, Constants.BULLET_MESSENGER);
                    }
                } else {
                    BubbleController.getInstance().hideBubbleListWithAnim();
                    BulletSmsUtils.popUpNeedInstallDialog(getContext(), true);
                }
            }
            case R.id.iv_over: {
                finishInput(true);
            }
            break;
            case R.id.iv_weixin: {
                GlobalBubbleUtils.shareToApp(getContext(), mBubbleItem, new ComponentName(Constants.WECHAT, Constants.WECHAT_SHARE_COMPONENT));
            }
            break;
            case R.id.iv_calendar: {
                showRemindDialog();
//                GlobalBubbleUtils.shareToApp(getContext(), mBubbleItem, new ComponentName(Constants.CALENDAR, Constants.CALENDAR_SHARE_COMPONENT));
            }
            break;
            case R.id.iv_edit_attach:
            case R.id.iv_attach: {
                Utils.hideInputMethod(this);
                if (mBubbleItem.getAttachments() == null || mBubbleItem.getAttachments().size() < AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT) {
                    if (mBubbleItem.getId() > 0) {
                        mBubbleItem.setAddingAttachment(true);
                        GlobalBubbleUtils.pickAttachment(getContext(), mBubbleItem.getId());
                    }
                } else {
                    GlobalBubbleUtils.showSystemToast(getContext(),
                            getContext().getString(R.string.bubble_attachment_limit,
                                    AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT), Toast.LENGTH_SHORT);
                }
            }
            break;
            case R.id.iv_cancel: {
                parent.deleteItemAnim(BubbleItem.FLAG_NEEDDELE);
            }
            break;
            case R.id.iv_bubble_play: {
                if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mBubbleItem)) {
                    stopPlay();
                } else {
                    playSpeech();
                }
            }
            break;
            case R.id.v_bubblespeechwave: {
                stopPlay();
            }
            break;
        }
    }

    @Override
    public void onBubbleAttachDelete() {
        parent.onBubbleAttachDelete();
    }

    @Override
    public void onBubblePptAdd() {
        if (parent.isModeEdit()) {
            return;
        }
        final InputMethodManager imm = InputMethodManager.peekInstance();
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
        BubbleController.getInstance().setPptBubble(mBubbleItem);
    }

    public void setMaxHeight(boolean isMaxHeight, boolean needAnim) {
        mCurrentNeedAnim = needAnim;
        mCurrentIsMaxHeight = isMaxHeight;
        setScrollViewMaxHeight();
        mTvScroll.setMaxHeight(isMaxHeight, needAnim);
    }

    private void setScrollViewScrollbarVisible(boolean visible) {
        mTvScroll.setDrawScrollBarEnable(visible);
    }

    protected boolean finishInput() {
        // when in ext display, set finished force as default
        return finishInput(BubbleController.getInstance().isExtDisplay());
    }

    protected boolean finishInput(boolean ignoreSync) {
        boolean result = false;
        mBubbleItem.judgeTextChangedWhenInput();
        if (mBubbleItem.hasModificationFlagChanged()) {
            if (mBubbleItem.getModificationFlag() != BubbleItem.MF_TIME_STAMP) {
                if (GlobalBubbleManager.getInstance().handleConflict(mBubbleItem)) {
                    showDate();
                }
            }
            if ((mBubbleItem.getModificationFlag(BubbleItem.MF_TEXT) || mBubbleItem.getSecondaryModificationFlag(BubbleItem.MF_TEXT))
                    && mBubbleItem.getDueDate() > 0) {
                AlarmUtils.replaceAlarmToCalendar(getContext(), mBubbleItem);
            }
        }
        mBubbleAttachmentLayout.finishPopup();
        if (mBubbleItem.isNeedInput()) {
            if (mBubbleItem.isEmptyBubble() && !mBubbleItem.isAddingAttachment() && !mBubbleItem.isAttachLocked()
                    && (ignoreSync || !mBubbleItem.isSyncLocked())) {
                parent.deleteItemAnim(BubbleItem.FLAG_NEEDDELE);
                return true;
            } else {
                setMaxHeight(false, true);
                result = true;
                mBubbleItem.setNeedInput(false);
                showDate();
                parent.toInput(mBubbleItem, true);
                if (mBubbleItem.getId() == 0) {
                    GlobalBubbleManager.getInstance().insertBubbleItem(mBubbleItem);
                } else {
                    GlobalBubbleManager.getInstance().updateBubbleItem(mBubbleItem);
                }
                IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_INPUT_OVER);
            }
            GlobalBubbleUtils.trackBubbleChange(mBubbleItem);
        } else {
            setMaxHeight(false, true);
        }

        mllinputopt.setVisibility(GONE);
        mlloptBottons.setVisibility(VISIBLE);
        mTvTitle.showText(mBubbleItem.getText(), !mBubbleItem.isNeedInput());
        mTvTitle.setCursorVisible(false);
        mTvTitle.updateHintText(mBubbleItem);
        updateBoomBtn();
        updateBulletBtn();
        refreshNotiView();
        setScrollViewScrollbarVisible(false);
        return result;
    }

    private void updateBoomBtn() {
        if (!Constants.BOOM_INSTALLED) {
            mIvBubbleEdit.setAlpha(0.15f);
        } else {
            mIvBubbleEdit.setAlpha(1f);
            mIvBubbleEdit.setEnabled(TextUtils.isEmpty(mBubbleItem.getText()) ? false : true);
        }
    }

    private void updateBulletBtn() {
        if (!Constants.BULLET_MESSENGER_INSTALLED) {
            mIvBullet.setAlpha(0.15f);
        } else {
            mIvBullet.setAlpha(1f);
            mIvBullet.setEnabled(true);
        }
    }

    private void shareToApps() {
        final List<BubbleItem> bubbles = new ArrayList<BubbleItem>();
        bubbles.add(mBubbleItem);
        final Context context = getContext();
        TaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = GlobalBubbleUtils.createShareIntent(getContext(), bubbles);
                context.startActivity(intent);
            }
        });
    }

    public void onInstalledPackageChanged() {
        updateBoomBtn();
        updateBulletBtn();
    }

    public void setPress(ColorMatrixColorFilter colorMatrixColorFilter) {
        mTvScroll.setPress(colorMatrixColorFilter);
        if (mBubbleItem == null || !mBubbleItem.isToDoOver()) {
            mTvTitle.setAlpha(0.7f);
        }
    }

    public void clearPress() {
        mTvScroll.clearPress();
        mTvTitle.resetAlpha(mBubbleItem != null && mBubbleItem.isToDoOver());
    }

    public int[] measureTextInLargeMode(BubbleItem item) {
        if (TextUtils.isEmpty(item.getText())) {
            return null;
        }
        Editable editable = mTvTitle.getText();
        CharSequence text = null;
        if (editable != null && editable instanceof SpannableStringBuilder) {
            text = (SpannableStringBuilder) editable;
        }
        if (text == null) {
            text = editable.toString();
        }
        if (text == null) {
            // still null , should never happen
            return null;
        }
        TextPaint paint = mTvTitle.getPaint();
        final int textViewWidth = getEditTextWidthInLarge();
        Layout layout = new DynamicLayout(text, paint,
                textViewWidth - mTvTitle.getPaddingLeft() - mTvTitle.getPaddingRight(),
                Layout.Alignment.ALIGN_NORMAL, mTvTitle.getLineSpacingMultiplier(),
                mTvTitle.getLineSpacingExtra(), mTvTitle.getIncludeFontPadding());
        int height = layout.getHeight();
        int width = layout.getWidth();
        return new int[]{width, height};
    }

    private int getEditTextWidthInLarge() {
        return getResources().getDimensionPixelSize(
                R.dimen.bubble_item_edit_text_width_in_large);
    }

    public void callInputMethod() {
        mTvTitle.setCursorVisible(true);
        mTvTitle.callInputMethodDelay(150);
        BubbleController.getInstance().updateInputStatus(true);
    }

    public void focusInputMethod() {
        mTvTitle.setCursorVisible(true);
        mTvTitle.requestFocus();
        if(!TextUtils.isEmpty(mTvTitle.getShowText())){
            mTvTitle.setSelection(mTvTitle.getShowText().length());
        } else {
            mTvTitle.setSelection(0);
        }
    }

    private void refreshCancelShowState() {
        if (mBubbleItem.isText() && mBubbleItem.isEmptyBubble()) {
            mIvCancel.setVisibility(VISIBLE);
            mDivideLine.setVisibility(VISIBLE);
        } else {
            mIvCancel.setVisibility(GONE);
            mDivideLine.setVisibility(GONE);
        }
    }

    public void needInput(boolean flag) {
        if (flag) {
            mllinputopt.setVisibility(VISIBLE);
            mlloptBottons.setVisibility(GONE);
            mNotification.setVisibility(GONE);
            refreshCancelShowState();
            parent.toInput(mBubbleItem, true);
            callInputMethod();
        } else {
            mllinputopt.setVisibility(GONE);
            mlloptBottons.setVisibility(VISIBLE);
            refreshNotiView();
        }
    }

    public void showStaticInput(boolean flag) {
        if (flag) {
            mllinputopt.setVisibility(VISIBLE);
            mlloptBottons.setVisibility(GONE);
        } else {
            mllinputopt.setVisibility(GONE);
            mlloptBottons.setVisibility(VISIBLE);
        }
        parent.toInput(mBubbleItem, false);
        if (flag) {
            refreshCancelShowState();
        }
    }

    public void showImage() {
        mVBubbleSpeechWave.setMaxDuration((int) mBubbleItem.getVoiceDuration());
        mVBubbleSpeechWave.setWaveData(mBubbleItem.getWaveData());
        run();
    }

    public void playSpeech() {
        Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_PLAY, "source", 1);
        BubbleSpeechPlayer.getInstance(getContext()).playSpeech(mBubbleItem, this);
    }

    public void stopPlay() {
        if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mBubbleItem)) {
            BubbleSpeechPlayer.getInstance(getContext()).stop();
            run();
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mIvBubblePlay.setImageDrawable(getResources().getDrawable(getPlayImageRes(mBubbleItem, true)));
        } else {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mIvBubblePlay.setImageDrawable(getResources().getDrawable(getPlayImageRes(mBubbleItem, true)));
                }
            });
        }
    }

    @Override
    public void onStarted(BubbleItem item, boolean isStarted) {
        if (isStarted && item == mBubbleItem) {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mIvBubblePlay.setImageDrawable(getResources().getDrawable(getPlayImageRes(mBubbleItem, false)));
                    if (mAnimAlpha != null) {
                        mAnimAlpha.cancel();
                    }
                    mAnimAlpha = new Anim(mIvBubblePlay, Anim.TRANSPARENT, 150, Anim.CIRC_OUT, new Vector3f(0, 0, mIvBubblePlay.getAlpha()), Anim.INVISIBLE);
                    mAnimAlpha.setListener(new SimpleAnimListener() {
                        public void onStart() {
                            mVBubbleSpeechWave.setShowMiddle(true);
                        }

                        public void onComplete(int type) {
                            if (type == Anim.ANIM_FINISH_TYPE_COMPLETE) {
                                mIvBubblePlay.setAlpha(1.0f);
                                mIvBubblePlay.setVisibility(INVISIBLE);
                            }
                        }
                    });
                    mAnimAlpha.start();
                    BubbleItemDetailView.this.run();
                }
            });
        }
    }

    @Override
    public void onCompleted(BubbleItem item) {
        if (item == mBubbleItem) {
            stopPlay();
        }
    }

    @Override
    public void onDisconnected(BubbleItem item) {
        if (item == mBubbleItem) {
            onCompleted(item);
        }
    }

    @Override
    public void onFocusChanged(boolean isLossFocus) {
        if (isLossFocus) {
            stopPlay();
        }
    }

    public void run() {
        if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mBubbleItem)) {
            mVBubbleSpeechWave.setCurPosition(BubbleSpeechPlayer.getInstance(getContext()).getCurrentPosition(mBubbleItem));
            UIHandler.post(this);
        } else {
            mVBubbleSpeechWave.setCurPosition(0);
            if (mAnimAlpha != null) {
                mAnimAlpha.cancel();
            }
            mVBubbleSpeechWave.setShowMiddle(false);
            mIvBubblePlay.setVisibility(VISIBLE);
        }
    }

    public void showDate() {
        if (mBubbleItem.isShareColor() && mBubbleItem.getSharePendingStatus() == BubbleItem.SHARE_PENDING_INVITATION) {
            mConflictTag.setVisibility(GONE);
            mTVOfflineTag.setVisibility(GONE);
            mVofflineTagLine.setVisibility(GONE);
            mTVOfflineHint.setVisibility(GONE);
            mTvDate.setVisibility(VISIBLE);
            mTvDate.setText(R.string.sync_share_waiting);
        } else if (mBubbleItem.getType() == BubbleItem.TYPE_VOICE_OFFLINE) {
            mConflictTag.setVisibility(GONE);
            mTVOfflineTag.setVisibility(VISIBLE);
            mVofflineTagLine.setVisibility(VISIBLE);
            mTVOfflineHint.setVisibility(VISIBLE);
            mTvDate.setVisibility(GONE);
        } else {
            mTVOfflineTag.setVisibility(GONE);
            mVofflineTagLine.setVisibility(GONE);
            mTVOfflineHint.setVisibility(GONE);
            if (!TextUtils.isEmpty(mBubbleItem.getConflictSyncId())
                    && !BubbleItem.CONFLICT_HANDLED_TAG.equals(mBubbleItem.getConflictSyncId())) {
                mConflictTag.setVisibility(VISIBLE);
            } else {
                mConflictTag.setVisibility(GONE);
            }
            mTvDate.setVisibility(VISIBLE);
            Date date = new Date(mBubbleItem.getTimeStamp());
            SimpleDateFormat dateFormat = new SimpleDateFormat(getResources().getString(R.string.bubble_datetime_format_year));
            mTvDate.setText(dateFormat.format(date) + " " + TimeUtils.buildDetailTime(getContext(), mBubbleItem.getTimeStamp()));
        }
    }

    public void checkIsPlaying() {
        if (mBubbleItem.isVoiceBubble()) {
            if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mBubbleItem)) {
                BubbleSpeechPlayer.getInstance(getContext()).replaceSpeechPlayCallBack(this);
            }
            mIvBubblePlay.setImageDrawable(getResources().getDrawable(getPlayImageRes(mBubbleItem)));
            showImage();
        }
    }

    public void setShadeColor() {
        mTvScroll.setShadeColor(mBubbleItem);
    }

    public void showText() {
        mTvTitle.show(mBubbleItem);
        updateBoomBtn();
    }

    public void toDoOver(boolean isOver) {
        mTvTitle.toDoOver(isOver);
    }

    public View getDateView() {
        return mDateView;
    }

    public View getNotiView() {
        return mNotification;
    }

    public void refreshChildViewAlphaDuringAnim(float percent) {
        if (mDateView.getVisibility() != VISIBLE) {
            mDateView.setVisibility(VISIBLE);
        }
        mDateView.setAlpha(percent);
        if (mNotification.getVisibility() == VISIBLE) {
            mNotification.setAlpha(percent);
        }
        if (mBubbleItem != null && !mBubbleItem.isText()) {
            if (mFlBubbleSpeechWave.getVisibility() != VISIBLE) {
                mFlBubbleSpeechWave.setVisibility(VISIBLE);
            }
            mFlBubbleSpeechWave.setAlpha(percent);
        }
        if (mBubbleItem != null && mBubbleItem.getAttachments() != null
                && !mBubbleItem.getAttachments().isEmpty()) {
            if (mBubbleAttachmentLayout.getVisibility() != VISIBLE) {
                mBubbleAttachmentLayout.setVisibility(VISIBLE);
            }
            mBubbleAttachmentLayout.setAlpha(percent);
        }
    }

    public void showChildViewDuringAnim() {
        mDateView.setAlpha(1f);
        mNotification.setAlpha(1f);
        mFlBubbleSpeechWave.setAlpha(1f);
        mBubbleAttachmentLayout.setAlpha(1f);
    }

    public void hideChildViewDuringAnim() {
        mDateView.setVisibility(INVISIBLE);
        mDateView.setAlpha(0f);
        mNotification.setAlpha(0f);
        if (mBubbleItem != null && !mBubbleItem.isText()) {
            mFlBubbleSpeechWave.setVisibility(INVISIBLE);
            mFlBubbleSpeechWave.setAlpha(0f);
        }
        if (mBubbleItem != null && mBubbleItem.getAttachments() != null
                && !mBubbleItem.getAttachments().isEmpty()) {
            mBubbleAttachmentLayout.setVisibility(INVISIBLE);
            mBubbleAttachmentLayout.setAlpha(0f);
        }
    }

    public void resetChildViewStatusWhenEndAnim() {
        mDateView.setVisibility(VISIBLE);
        mDateView.setAlpha(1f);
        mNotification.setAlpha(1f);
        mFlBubbleSpeechWave.setAlpha(1f);
        if (mBubbleItem == null || mBubbleItem.isText()) {
            mFlBubbleSpeechWave.setVisibility(GONE);
        } else {
            mFlBubbleSpeechWave.setVisibility(VISIBLE);
        }
        mBubbleAttachmentLayout.setAlpha(1f);
        if (mBubbleItem != null) {
            if ((mBubbleItem.getAttachments() != null && !mBubbleItem.getAttachments().isEmpty())
                    || BubbleController.getInstance().isInPptContext(getContext())) {
                mBubbleAttachmentLayout.setVisibility(VISIBLE);
            } else {
                mBubbleAttachmentLayout.setVisibility(GONE);
            }
        } else {
            mBubbleAttachmentLayout.setVisibility(GONE);
        }
    }

    public void setOptEnable(boolean enable) {
        ViewUtils.setViewEnable(mIvBubblePlay, enable);
        if (Constants.BOOM_INSTALLED && !enable) {
            mIvBubbleEdit.setAlpha(1f);
            mIvBubbleEdit.setEnabled(false);
        } else {
            updateBoomBtn();
        }
        if (Constants.BULLET_MESSENGER_INSTALLED && !enable) {
            mIvBullet.setAlpha(0.15f);
            mIvBullet.setEnabled(false);
        } else {
            updateBulletBtn();
        }
        ViewUtils.setViewEnable(mIvBubbleDele, enable);
        ViewUtils.setViewEnable(mIvCalendar, enable);
        ViewUtils.setViewEnable(mIvWeixin, enable);
        ViewUtils.setViewEnable(mIvShare, enable);
        ViewUtils.setViewEnable(mIvAttach, enable);
    }

    private void showRemindDialog() {
        Intent intent = new Intent(mContext, RemindAlarmSettingActivity.class);
        intent.putExtra(AlarmUtils.KEY_ALARM_ID, mBubbleItem.getId());
        intent.putExtra(RemindAlarmSettingDialog.EXTRA_SET_BUBBLE_WINDOW_TYPE, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (SmtPCUtils.isValidExtDisplayId(mContext)) {
            int width = 0;
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) {
                width = parent.getWidth();
            }
            DisplayInfo displayInfo = new DisplayInfo();
            SmtPCUtils.getExtDisplay(getContext()).getDisplayInfo(displayInfo);
            int x = displayInfo.largestNominalAppWidth - width - BubbleController.getInstance().getIdeaPillsRightTrans();
            intent.putExtra(RemindAlarmSettingDialog.EXTRA_WINDOW_POSITION, x);
            intent.putExtra(RemindAlarmSettingDialog.EXTRA_END_POSITION, true);
        }
        GlobalBubbleUtils.startActivityMayInExtDisplay(mContext, intent);
    }

    private int getPlayImageRes(BubbleItem bubbleItem) {
        if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(bubbleItem)) {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return bubbleItem.isShareFromOthers() ? R.drawable.bubble_pop_stop : R.drawable.bubble_pop_stop_share;
            } else {
                return R.drawable.bubble_pop_stop;
            }
        } else {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return bubbleItem.isShareFromOthers() ? R.drawable.bubble_pop_start : R.drawable.bubble_pop_start_share;
            } else {
                return R.drawable.bubble_pop_start;
            }
        }
    }

    private int getPlayImageRes(BubbleItem bubbleItem, boolean isStart) {
        if (!isStart) {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return bubbleItem.isShareFromOthers() ? R.drawable.bubble_pop_stop : R.drawable.bubble_pop_stop_share;
            } else {
                return R.drawable.bubble_pop_stop;
            }
        } else {
            if (bubbleItem != null && bubbleItem.isShareColor()) {
                return bubbleItem.isShareFromOthers() ? R.drawable.bubble_pop_start : R.drawable.bubble_pop_start_share;
            } else {
                return R.drawable.bubble_pop_start;
            }
        }
    }


    /**
     * 	Obtain the  height of  removing the Scroll View outside and the other children
     * @return
     */
    public int getDetailViewFixedChildenHeight() {
        int resultHight = 0;

        for (int index = 0; index < getChildCount(); index++) {
            View childView = getChildAt(index);
            if (childView == null || childView.getVisibility() == View.GONE) {
                continue;
            }
            if (childView == mTvScroll) {
                continue;
            }

            RelativeLayout.LayoutParams layoutParams = (LayoutParams) childView.getLayoutParams();
            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            childView.measure(w, h);
            int height = childView.getMeasuredHeight();

            resultHight = resultHight + (height + layoutParams.bottomMargin + layoutParams.topMargin);
        }
        return resultHight;
    }

    @Override
    public boolean showContextMenuForChild(View originalView, float x, float y) {
        if (SmtPCUtils.isValidExtDisplayId(mContext)) {
            ContextMenuBuilder builder = new ContextMenuBuilder(mContext);
            builder.showPopup(mContext, originalView, x, y);
            return true;
        } else {
            return super.showContextMenuForChild(originalView, x, y);
        }
    }

    public void setScrollViewMaxHeight() {
        if (!mCurrentIsMaxHeight) {
            return;
        }
        RelativeLayout.LayoutParams layoutParams = (LayoutParams) mTvScroll.getLayoutParams();

        int bubbleItemDetailMaxHeight = getResources().getDimensionPixelOffset(R.dimen.bubble_item_detail_input_max_hight);
        int fixedChildHeight = getDetailViewFixedChildenHeight();

        //The sum of all the children height is greater than the maximum height,
        // Need to retrench m Tv Scroll height;
        if (fixedChildHeight + layoutParams.bottomMargin + layoutParams.topMargin + mTvScroll.getFixedChildenHeight() > bubbleItemDetailMaxHeight) {
            int maxScrollHight = bubbleItemDetailMaxHeight - fixedChildHeight - layoutParams.bottomMargin - layoutParams.topMargin;
            mTvScroll.setMaxHeight(maxScrollHight);
        }
    }

    private BubbleEditText.IBubbleStateListener mBubbleStateListener = new BubbleEditText.IBubbleStateListener() {
        @Override
        public void onDeleteEvent() {
            parent.deleteItemAnim(BubbleItem.FLAG_NEEDTRASH);
        }

        @Override
        public void toggleCheckState() {
            parent.toggleCheckState();
        }
    };

    private void handleStartBoom() {
        try {
            Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_BOOM, "source", 1);
            String text = mBubbleItem.getText();
            Utils.startBoomActivity(getContext(), mTvTitle, text, mBubbleItem, true);
        } catch (ActivityNotFoundException e) {
            BubbleController.getInstance().showConfirmDialog(getContext(),
                    getContext().getText(R.string.bubble_notice), getContext().getText(R.string.install_bigbang_message),
                    new Runnable() {
                        public void run() {
                            Utils.launchAppStoreForPackage(getContext().getApplicationContext(), Constants.BOOM);
                            BubbleController.getInstance().hideBubbleListImmediately();
                        }
                    }, null, R.string.install, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
    }
}
