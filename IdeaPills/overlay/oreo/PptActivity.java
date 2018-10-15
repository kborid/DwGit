package com.smartisanos.ideapills;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.SmtPCUtils;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.common.util.TaskUtils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.util.ViewUtils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.view.BubbleFrameLayout;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.view.BubbleItemView;
import com.smartisanos.ideapills.view.BubbleListView;
import com.smartisanos.ideapills.view.BubbleOptLayout;

import java.io.File;
import java.util.List;

public class PptActivity extends Activity implements View.OnClickListener, BubbleListView.OnSelectChangedListener {

    private BubbleFrameLayout mBubbleFrameLayout;
    private BubbleOptLayout mBubbleOptLayout;
    private View mPptFuncLayout;
    private Anim mPptModeHideAnim;

    private View mPptCloseBtn;
    private TextView mPptGenerateBtn;
    private ViewPropertyAnimator mPptTransAnim;

    private int mLastLayoutWidth;
    private int mLastLayoutHeight;
    private int mScreenHeight;


    private boolean mIsTaskListenerRegistered;
    private int mOfficeTaskId = -1;
    private TaskStackListener mTaskStackListener = new TaskStackListener() {

        @Override
        public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
        }

        @Override
        public void onTaskRemoved(int taskId) throws RemoteException {
            if (taskId == mOfficeTaskId) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        hidePpt();
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BubbleDisplayManager.INSTANCE.switchDisplayIfNeeded(true);
        BubbleController extController = BubbleController.getExtInstance();
        if (extController == null || !Utils.isIdeaPillsEnable(this)) {
            Context extContext = BubbleDisplayManager.INSTANCE.getExtContext(this);
            if (extContext != null) {
                Toast.makeText(extContext, R.string.ideapills_not_opened, Toast.LENGTH_SHORT).show();
            }
            finish();
            return;
        }
        try {
            mOfficeTaskId = TaskUtils.getTaskId("com.yozo.sts.office");
            SmtPCUtils.getSmtPCManager().registerSmtTaskStackListener(mTaskStackListener);
            mIsTaskListenerRegistered = true;
        } catch (RemoteException e) {
            // ignore
        }
        BubbleController.getExtInstance().showInPptMode(this);
        setContentView(R.layout.bubble_opt_layout);
        mBubbleFrameLayout = findViewById(R.id.bubble_frame_layout);
        mBubbleOptLayout = (BubbleOptLayout) mBubbleFrameLayout.findViewById(R.id.bubble_opt_layout);
        showPpt();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPptModeHideAnim != null && mPptModeHideAnim.isRunning()) {
            mPptModeHideAnim.cancel();
            mPptModeHideAnim = null;
        }
    }

    @Override
    protected void onDestroy() {
        releasePptMode();
        try {
            if (mIsTaskListenerRegistered) {
                SmtPCUtils.getSmtPCManager().unRegisterSmtTaskStackListener(mTaskStackListener);
                mIsTaskListenerRegistered = false;
            }
        } catch (RemoteException e) {
            // ignore
        }
        super.onDestroy();
    }

    private void updatePptGenBtnPos(boolean isAnim) {
        float nowBottomTrans = mBubbleOptLayout.getTranslationY();
        float newBottomTrans;
        if (mBubbleOptLayout.getBubbleListView().getFirstVisiblePosition() <= 0) {
            int genBtnBottomMargin = ((ViewGroup.MarginLayoutParams) mPptGenerateBtn.getLayoutParams()).bottomMargin;
            int optLayoutBottomMargin = ((ViewGroup.MarginLayoutParams) mBubbleOptLayout.getLayoutParams()).bottomMargin;
            newBottomTrans = -1 * mBubbleOptLayout.getBubbleListView().getEmptyAvailableHeight()
                    - optLayoutBottomMargin - ViewUtils.dp2px(4) + genBtnBottomMargin;
            newBottomTrans = Math.min(newBottomTrans, 0);
        } else {
            newBottomTrans = 0;
        }
        if (newBottomTrans == nowBottomTrans) {
            return;
        }
        if (mPptTransAnim != null) {
            mPptTransAnim.cancel();
        }
        if (isAnim) {
            mPptTransAnim = mPptGenerateBtn.animate().translationY(newBottomTrans);
            mPptTransAnim.setStartDelay(175);
            mPptTransAnim.start();
        } else {
            mPptGenerateBtn.setTranslationY(newBottomTrans);
        }
    }

    private void showPpt() {
        mBubbleOptLayout.getBubbleListView().observeBubbleItemChanged();
        mBubbleOptLayout.getBubbleListView().setOnSelectChangeListener(this);

        int current = BubbleListView.sDarkColor;
        mBubbleFrameLayout.setBackgroundColor(current << 24);
        LayoutInflater factory = LayoutInflater.from(this);
        mPptFuncLayout = factory.inflate(R.layout.rev_vw_ppt_func, mBubbleFrameLayout, false);
        mBubbleOptLayout.setAlpha(1f);

        DisplayInfo displayInfo = new DisplayInfo();
        SmtPCUtils.getExtDisplay(this).getDisplayInfo(displayInfo);
        mScreenHeight = displayInfo.smallestNominalAppWidth;
        final Configuration config = getResources().getConfiguration();
        updatePptLayout(ViewUtils.dp2px(BubbleController.getInstance().getContext(), config.screenWidthDp),
                ViewUtils.dp2px(BubbleController.getInstance().getContext(), config.screenHeightDp),
                mScreenHeight);
        mBubbleFrameLayout.addView(mPptFuncLayout);
        mPptCloseBtn = mPptFuncLayout.findViewById(R.id.ppt_close);
        mPptCloseBtn.setOnClickListener(this);
        mPptCloseBtn.setAlpha(0);
        mPptGenerateBtn = (TextView) mPptFuncLayout.findViewById(R.id.ppt_generate);
        mPptGenerateBtn.setOnClickListener(this);
        mPptGenerateBtn.setAlpha(0);
        Anim bgShowAnim = new Anim(mBubbleFrameLayout, Anim.TRANSPARENT, 300, Anim.EASE_OUT, Anim.INVISIBLE, Anim.VISIBLE);
        mBubbleOptLayout.getBubbleListView().playShowAnimation(new BubbleListView.ShowBubblesListener() {
            @Override
            public void showOver() {
                updatePptGenBtnPos(false);
                int transLength = ViewUtils.dp2px(BubbleController.getInstance().getContext(), 30);
                AnimTimeLine pptBtnTimeLine = new AnimTimeLine();
                Anim pptCloseAlphaAnim = new Anim(mPptCloseBtn, Anim.TRANSPARENT, 300, Anim.EASE_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                Anim pptCloseTransAnim = new Anim(mPptCloseBtn, Anim.TRANSLATE, 300, Anim.EASE_OUT,
                        new Vector3f(transLength, 0), Anim.ZERO);
                Anim pptGenAlphaAnim = new Anim(mPptGenerateBtn, Anim.TRANSPARENT, 300, Anim.EASE_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                Anim pptGenTransAnim = new Anim(mPptGenerateBtn, Anim.TRANSLATE, 300, Anim.EASE_OUT,
                        new Vector3f(transLength, 0), Anim.ZERO);
                pptBtnTimeLine.addAnim(pptCloseAlphaAnim);
                pptBtnTimeLine.addAnim(pptCloseTransAnim);
                pptBtnTimeLine.addAnim(pptGenAlphaAnim);
                pptBtnTimeLine.addAnim(pptGenTransAnim);
                pptBtnTimeLine.setAnimListener(new SimpleAnimListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onComplete(int type) {
                        mBubbleOptLayout.getBubbleListView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                            @Override
                            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                                updatePptGenBtnPos(true);
                            }
                        });
                    }
                });
                pptBtnTimeLine.start();
            }
        });
        bgShowAnim.start();
    }

    private void updatePptLayout(int layoutWidth, int layoutHeight, int screenHeight) {
        int bubbleLayoutWidth = getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
        ViewGroup.MarginLayoutParams optLp = (ViewGroup.MarginLayoutParams) mBubbleOptLayout.getLayoutParams();

        if (layoutWidth != mLastLayoutWidth) {
            int pillListRightMargin = (layoutWidth - bubbleLayoutWidth) / 2;
            if (optLp.rightMargin != pillListRightMargin) {
                optLp.rightMargin = pillListRightMargin;
                FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) mPptFuncLayout.getLayoutParams();
                flp.gravity = Gravity.RIGHT;
                flp.rightMargin = pillListRightMargin - flp.width;
                mBubbleOptLayout.requestLayout();
            }
        }

        if (layoutHeight != mLastLayoutHeight) {
            ViewGroup.MarginLayoutParams funcLp = (ViewGroup.MarginLayoutParams) mPptFuncLayout.getLayoutParams();
            if (layoutHeight >= screenHeight) {
                int navBarHeight = getResources().getDimensionPixelOffset(com.android.internal.R.dimen.desktop_navigation_bar_height);
                int statusBarHeight = getResources().getDimensionPixelOffset(com.android.internal.R.dimen.desktop_status_bar_height);
                optLp.bottomMargin = navBarHeight;
                optLp.topMargin = statusBarHeight;
            } else {
                optLp.bottomMargin = 0;
                optLp.topMargin = 0;
            }
            funcLp.topMargin = optLp.topMargin;
        }

        mLastLayoutWidth = layoutWidth;
        mLastLayoutHeight = layoutHeight;
    }

    public void invalidateList() {
        mBubbleOptLayout.getBubbleListView().notifyChanged();
    }

    public void release() {
        mBubbleOptLayout.getBubbleListView().clearSelectedBubbles();
        mBubbleOptLayout.getBubbleListView().unObserveBubbleItemChanged();
    }

    @Override
    public void finish() {
        if (isFinishing()) {
            return;
        }
        overridePendingTransition(0, 0);
        super.finish();
    }

    private void releasePptMode() {
        BubbleController extController = BubbleController.getExtInstance();
        if (extController != null && extController.isInPptMode()) {
            release();
            extController.hidePptMode(false);
        }
    }

    private void hidePpt() {
        releasePptMode();
        if (mPptModeHideAnim != null && mPptModeHideAnim.isRunning()) {
            mPptModeHideAnim.cancel();
        }

        mBubbleOptLayout.getBubbleListView().setOnSelectChangeListener(null);

        Anim hideAnim = new Anim(mBubbleFrameLayout, Anim.TRANSPARENT, 300, Anim.EASE_OUT, Anim.VISIBLE, Anim.INVISIBLE);
        hideAnim.setListener(new SimpleAnimListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onComplete(int type) {
                mBubbleFrameLayout.setBackgroundColor(0x00000000);
                mPptModeHideAnim = null;
                if (!isFinishing() && !isDestroyed()) {
                    finish();
                }
            }
        });
        hideAnim.start();
        mPptModeHideAnim = hideAnim;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updatePptLayout(ViewUtils.dp2px(BubbleController.getInstance().getContext(),
                newConfig.screenWidthDp), ViewUtils.dp2px(BubbleController.getInstance().getContext(),
                newConfig.screenHeightDp), mScreenHeight);
    }

    @Override
    public void onClick(View v) {
        if (R.id.ppt_close == v.getId()) {
            hidePpt();
        } else if (R.id.ppt_generate == v.getId()) {
            List<BubbleItem> bubbleItemList = mBubbleOptLayout.getBubbleListView().getSelectedBubbles();
            if (bubbleItemList.isEmpty()) {
                mBubbleOptLayout.getBubbleListView().switchHeadOperatorEditStatus(true);
                BubbleItemView view = mBubbleOptLayout.findInputtingView();
                if (view != null) {
                    view.finishInputting();
                }
                BubbleController.getInstance().setPptBubbleId(-1, -1);
                GlobalBubbleUtils.showSystemToast(this, R.string.ppt_generate_empty_tip, Toast.LENGTH_SHORT);
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putInt("Text_Count", bubbleItemList.size());
            int index = 0;
            for (BubbleItem bubbleItem : bubbleItemList) {
                String key = "Text_Key_" + index;
                String[] val = new String[1 + bubbleItem.getAttachmentCount()];
                val[0] = bubbleItem.getText() == null ? "" : bubbleItem.getText();
                int attachIndex = 1;
                for (int j = 0; j < bubbleItem.getAttachmentCount(); j++) {
                    try {
                        AttachMentItem attachMentItem = bubbleItem.getAttachments().get(j);
                        File attachFile = new File(attachMentItem.getUri().getPath());
                        if (attachMentItem.getType() == AttachMentItem.TYPE_IMAGE && attachFile.exists()) {
                            val[attachIndex] = attachFile.getAbsolutePath();
                            attachIndex++;
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
                bundle.putStringArray(key, val);
                index++;
            }
            Intent intent = new Intent();
            intent.putExtra("data", bundle);
            setResult(RESULT_OK, intent);
            mBubbleOptLayout.getBubbleListView().switchHeadOperatorEditStatus(false);
            hidePpt();
        }
    }

    @Override
    public void onSelectChanged(int count) {
        if (count == 0 || !mBubbleOptLayout.getBubbleListView().isInEditMode()) {
            mPptGenerateBtn.setText(getResources().getString(R.string.ppt_generate));
        } else {
            mPptGenerateBtn.setText(getResources().getString(R.string.ppt_generate_formate, count));
        }
    }
}
