package com.smartisanos.ideapills;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.SmtPCUtils;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.RemoteException;
import android.pc.ISmtPCManager;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.support.v4.content.LocalBroadcastManager;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.WindowManagerSmt;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.view.BubbleAttachmentView;
import com.smartisanos.ideapills.view.BubbleFrameLayout;
import com.smartisanos.ideapills.view.BubbleItemViewHelper;
import com.smartisanos.ideapills.view.BubbleListView;
import com.smartisanos.ideapills.view.BubbleOptLayout;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

public class BubbleController {
    private static final LOG log = LOG.getInstance(BubbleController.class);

    public static final int BUBBLE_WINDOW_TYPE = WindowManagerSmt.LayoutParamsSmt.TYPE_IDEA_PILLS;

    public static final int HIDE_BUBBLE_STATUS_NONE = 0;
    public static final int HIDE_BUBBLE_STATUS_NORMAL = 1;
    public static final int HIDE_BUBBLE_STATUS_IMMEDIATELY = 2;

    private volatile static BubbleController sInstance = null;
    private volatile static BubbleController sInstanceExt = null;
    private volatile static boolean sActiveExtDisplay;
    private Context mContext = null;
    private WindowManager mWindowManager = null;
    private BubbleFrameLayout mBubbleFrameLayout;
    private BubbleOptLayout mBubbleOptLayout;
    private boolean mSecondBoom = false;
    private boolean mInputting = false;
    private AlertDialog mDialog = null;
    private volatile boolean mBubbleHandleBySidebar = false;
    private boolean mIsExtDisplay;

    private int mPillListRightMargin;
    private String mPillListCurPkg;
    private AnimatorSet mPillListRightTransAnim;

    private boolean mIsInPptMode;
    private WeakReference<PptActivity> mPptActivityRef;
    private int mCurPptAddBubbleId = -1;
    private int mCurPptType = -1;

    private BubbleItemViewHelper mBubbleItemViewHelper;

    public static BubbleController getInstance() {
        synchronized (BubbleController.class) {
            if (sActiveExtDisplay) {
                return getExtInstance();
            } else {
                return getPhoneInstance();
            }
        }

    }

    static BubbleController getPhoneInstance() {
        if (sInstance == null) {
            synchronized(BubbleController.class){
                if(sInstance == null){
                    sInstance = new BubbleController(IdeaPillsApp.getInstance(), false);
                }
            }
        }
        return sInstance;
    }

    public static BubbleController getExtInstance() {
        synchronized (BubbleController.class) {
            return sInstanceExt;
        }
    }

    public static BubbleItemViewHelper getBubbleItemViewHelperByContext(Context context) {
        BubbleController extController = getExtInstance();
        if (extController != null && SmtPCUtils.isValidExtDisplayId(context)) {
            return extController.getBubbleItemViewHelper();
        } else {
            return getPhoneInstance().getBubbleItemViewHelper();
        }
    }

    static void switchInstance(boolean isExtDisplayActive) {
        log.d("switchInstance:" + isExtDisplayActive);
        BubbleController changedInstance = null;
        synchronized (BubbleController.class) {
            if (sActiveExtDisplay != isExtDisplayActive) {
                changedInstance = getInstance();
            }
            sActiveExtDisplay = isExtDisplayActive;
            if (changedInstance != null) {
                log.d("changedInstance:" + changedInstance);
                changedInstance.hidePptMode(true);
                changedInstance.hideAllBubble();
                changedInstance.unObserveBubbleItemChanged();
                GlobalBubbleManager.getInstance().clearAllBubblesCache();
                BubbleAttachmentView.releaseCaches();
                getInstance().observeBubbleItemChanged();
            }
            log.d("now instance ext:" + getInstance().isExtDisplay());
        }
    }

    static BubbleController createExtInstance(Context extContext) {
        synchronized (BubbleController.class) {
            sInstanceExt = new BubbleController(extContext, true);
        }
        return sInstanceExt;
    }

    static void destroyExtInstance() {
        synchronized (BubbleController.class) {
            if (sActiveExtDisplay) {
                switchInstance(false);
            } else {
                sInstanceExt.unObserveBubbleItemChanged();
                getPhoneInstance().observeBubbleItemChanged();
            }
            sInstanceExt = null;
        }
    }

    private BubbleController(Context context, boolean isExtDisplay) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mIsExtDisplay = isExtDisplay;
    }

    public boolean isExtDisplay() {
        return mIsExtDisplay;
    }

    public Context getContext() {
        return mContext;
    }

    public void handleObsChanged(Context context, String type) {
        if (type == null) {
            return;
        }
        if (InterfaceDefine.SETTINGS_VOICE_INPUT.equals(type)) {
            Constants.IS_IDEA_PILLS_ENABLE = Utils.isIdeaPillsEnable(context);
            updateVisibility();
            if (Constants.IS_IDEA_PILLS_ENABLE && mBubbleFrameLayout != null) {
                boolean isCurEnable = isBubbleWindowEnable();
                if (!isCurEnable) {
                    refreshPullViewAlpha();
                }
            }
            boolean needRestart = false;
            if (Constants.IS_IDEA_PILLS_ENABLE && !Constants.WINDOW_READY) {
                needRestart = true;
            } else if (!Constants.IS_IDEA_PILLS_ENABLE && Constants.WINDOW_READY) {
                needRestart = true;
            }
            if (needRestart) {
                Utils.unregisterIdeaPills();
                BubbleDisplayManager.INSTANCE.stop();
                removeIdeaPillsWindow();
                IdeaPillsApp app = IdeaPillsApp.getInstance();
                if (app != null) {
                    app.unregisterContentObserver();
                }
                log.error("restart self IS_IDEA_PILLS_ENABLE ["+Constants.IS_IDEA_PILLS_ENABLE+
                        "], WINDOW_READY ["+Constants.WINDOW_READY+"]");
                Utils.killSelf(context);
            }
        } else if (Constants.IS_IDEA_PILLS_ENABLE) {
            if (InterfaceDefine.SETTINGS_USER_SETUP_COMPLETE.equals(type)) {
                if (Utils.isSetupCompelete(mContext)) {
                    StatusManager.setStatus(StatusManager.FORCE_HIDE_WINDOW, false);
                }
                updateVisibility();
            } else if (InterfaceDefine.SETTINGS_FEATURE_PHONE_MODE.equals(type)) {
                boolean isFeaturePhoneMode = Constants.settingGlobalGetInt(context, InterfaceDefine.SETTINGS_FEATURE_PHONE_MODE, 0) == 1;
                StatusManager.setStatus(StatusManager.FEATURE_PHONE_MODE, isFeaturePhoneMode);
                updateVisibility();
            } else if (InterfaceDefine.SETTINGS_DEFAULT_BUBBLE_COLOR.equals(type)) {
                Constants.DEFAULT_BUBBLE_COLOR = Constants.settingGlobalGetInt(context, InterfaceDefine.SETTINGS_DEFAULT_BUBBLE_COLOR, GlobalBubble.COLOR_BLUE);
            } else if (Constants.VOICE_TODO_OVER_CYCLE_TYPE.equals(type)) {
                Constants.updateTodoOverType();
            } else if (Constants.STATUS_BAR_EXPAND.equals(type)) {
                boolean expand = Constants.settingGlobalGetInt(context, Constants.STATUS_BAR_EXPAND, 0) == Constants.STATUS_BAR_EXPANDED;
                if (expand) {
                    hideBubbleListImmediately();
                }
            }
        }
    }

    public void refreshPullViewAlpha() {
        if (!StatusManager.getStatus(StatusManager.FORCE_HIDE_WINDOW)) {
            if (mBubbleOptLayout != null) {
                mBubbleOptLayout.refreshPullViewAlpha();
            }
        }
    }

    public void clearForceHideWindow() {
        if (StatusManager.getStatus(StatusManager.FORCE_HIDE_WINDOW)) {
            StatusManager.setStatus(StatusManager.FORCE_HIDE_WINDOW, false);
            setBubbleWindowEnable(true);
            refreshPullViewAlpha();
        }
    }

    public void requestBubbleOptLayoutUpdateRegion() {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.postUpdateRegionAction(0);
        }
    }

    private void unObserveBubbleItemChanged() {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.getBubbleListView().unObserveBubbleItemChanged();
        }
    }

    private void observeBubbleItemChanged() {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.getBubbleListView().observeBubbleItemChanged();
        }
    }

    public WindowManager getWindowManager() {
        return mWindowManager;
    }

    public void addIdeaPillsWindow() {
        if (mBubbleFrameLayout != null) {
            try {
                mWindowManager.removeView(mBubbleFrameLayout);
            } catch (Throwable e) {
                // ignore remove fail result
            }
            IdeaPillsApp.getInstance().getBubbleObserverManager().clearBubbleObserver();
        }
        mBubbleFrameLayout = (BubbleFrameLayout) View.inflate(mContext, R.layout.bubble_opt_layout, null);
        mBubbleOptLayout = (BubbleOptLayout) mBubbleFrameLayout.findViewById(R.id.bubble_opt_layout);
        final WindowManager.LayoutParams lp;
        if (!mIsExtDisplay) {
            lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    BUBBLE_WINDOW_TYPE,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                    PixelFormat.TRANSLUCENT);
        } else {
            DisplayInfo displayInfo = new DisplayInfo();
            SmtPCUtils.getExtDisplay(mContext).getDisplayInfo(displayInfo);
            int navBarHeight = SmtPCUtils.getSmtNavigationBarPixelHeight(mContext);
            int statusBarHeight = SmtPCUtils.getSmtStatusBarPixelHeight(mContext);
            int phoneHeight =  displayInfo.smallestNominalAppWidth - navBarHeight;
            lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, phoneHeight,
                    BUBBLE_WINDOW_TYPE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                    PixelFormat.TRANSLUCENT);
            ((ViewGroup.MarginLayoutParams) mBubbleOptLayout.getLayoutParams()).topMargin = statusBarHeight;
        }
        lp.gravity = Gravity.RIGHT | Gravity.TOP;
        lp.setTitle("idea_pills");
        lp.packageName = mContext.getPackageName();
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        lp.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_EXT_NOT_CAPTURE;
        mBubbleFrameLayout.setAlwaysCanAcceptDrag(true);
        mWindowManager.addView(mBubbleFrameLayout, lp);
        setBubbleWindowEnable(false);
        setWindowTouchEnable(false);
        if (!mIsExtDisplay) {
            updateVisibility();
            refreshPullViewAlpha();
            Constants.WINDOW_READY = true;
        } else {
            int sidebarWidth = MultiSdkUtils.GetSidebarWidth();
            if (sidebarWidth > 0) {
                mPillListRightMargin = sidebarWidth;
                setPillListRightTransAnim(false);
            }
        }
    }

    void removeIdeaPillsWindow() {
        //remove window
        if (mWindowManager != null && mBubbleFrameLayout != null) {
            mWindowManager.removeViewImmediate(mBubbleFrameLayout);
        }
    }

    public boolean isCurControllerContext(Context context) {
        if (context != null && mContext.getDisplay() != null
                && context.getDisplay() != null) {
            return mContext.getDisplay().getDisplayId()
                    == context.getDisplay().getDisplayId();
        } else {
            return !isExtDisplay();
        }
    }

    public boolean isInPptMode() {
        return mIsExtDisplay && mIsInPptMode;
    }

    public boolean isInPptContext(Context context) {
        return context instanceof PptActivity;
    }

    public void showInPptMode(PptActivity pptActivity) {
        if (!isExtDisplay()) {
            return;
        }
        if (mIsInPptMode) {
            return;
        }
        mIsInPptMode = true;
        mPptActivityRef = new WeakReference<PptActivity>(pptActivity);
        hideAllBubble();
        unObserveBubbleItemChanged();
        GlobalBubbleManager.getInstance().clearAllBubblesCache();
//        if ("com.android.gallery3d".equals(mPillListCurPkg)) {
//            UIHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    toggleGallerySidebar();
//                }
//            }, 300);
//        }
    }

    public void setPptBubble(BubbleItem bubbleItem) {
        int bubbleIndex = GlobalBubbleManager.getInstance().indexOfPptBubble(bubbleItem);
        if (bubbleIndex < 0) {
            setPptBubbleId(bubbleItem.getId(), 0);
        } else {
            if (!"com.android.gallery3d".equals(mPillListCurPkg)) {
                toggleGallerySidebar();
            }
            setPptBubbleId(bubbleItem.getId(), bubbleIndex + 1);
        }
    }

    public void setPptBubbleId(int bubbleId, int type) {
        if(!isInPptMode()) {
            return;
        }
        boolean changedBubble = mCurPptAddBubbleId != bubbleId;
        boolean changedType = mCurPptType != type;

        mCurPptAddBubbleId = bubbleId;
        mCurPptType = type;
        if (mCurPptAddBubbleId < 0) {
            Settings.Global.putString(getContext().getContentResolver(), "idea_pills_get_content",
                    null);
        } else {
            Settings.Global.putString(getContext().getContentResolver(), "idea_pills_get_content",
                    System.currentTimeMillis() + "_" + type);
        }
        if (changedBubble && mPptActivityRef != null) {
            PptActivity pptActivity = mPptActivityRef.get();
            if (pptActivity != null) {
                pptActivity.invalidateList();
            }
        }
    }

    public int getCurPptAddBubbleId() {
        return mCurPptAddBubbleId;
    }

    public void hidePptMode(boolean isForceFinish) {
        if (!isExtDisplay()) {
            return;
        }
        if (isForceFinish && mPptActivityRef != null) {
            PptActivity pptActivity = mPptActivityRef.get();
            if (pptActivity != null) {
                pptActivity.release();
                pptActivity.finish();
            }
        }
        setPptBubbleId(-1, -1);
        mPptActivityRef = null;
        mIsInPptMode = false;
        synchronized (BubbleController.class) {
            if (sActiveExtDisplay) {
                observeBubbleItemChanged();
                GlobalBubbleManager.getInstance().clearAllBubblesCache();
            }
        }
    }

    private void toggleGallerySidebar() {
        MultiSdkUtils.toggleGallerySidebar();
    }

    public int getIdeaPillsRightTrans() {
        if (!isExtDisplay()) {
            return 0;
        }
        return mPillListRightMargin;
    }

    public void setIdeaPillsRightTrans(final int pillListRightMargin, final String curPkg) {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                mPillListCurPkg = curPkg;
                if (pillListRightMargin == mPillListRightMargin) {
                    return;
                }
                cancelPillListRightTransAnim();
                mPillListRightMargin = pillListRightMargin;
                if (mPillListRightMargin == 0) {
                    setPptBubbleId(-1, -1);
                }

                if (mBubbleOptLayout == null) {
                    return;
                }
                if (!isExtDisplay()) {
                    return;
                }
                setPillListRightTransAnim(!isInPptMode() && isAlreadyShow());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("IDEAPILL_RIGHT_TRANSLATION"));
            }
        });
    }

    private void cancelPillListRightTransAnim() {
        if (mPillListRightTransAnim != null) {
            mPillListRightTransAnim.cancel();
            mPillListRightTransAnim = null;
            if (mBubbleOptLayout != null) {
                mBubbleOptLayout.setAlpha(1f);
            }
        }
    }

    private void setPillListRightTransAnim(boolean withAnim) {
        mBubbleOptLayout.getBubbleListView().hideFiltrateWindow();
        if (withAnim) {
            final ViewGroup.MarginLayoutParams optLp = (ViewGroup.MarginLayoutParams) mBubbleOptLayout.getLayoutParams();
            mPillListRightTransAnim = new AnimatorSet();
            int trans = optLp.rightMargin - mPillListRightMargin;
            ValueAnimator transAnim = ValueAnimator.ofFloat(0, trans);
            transAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            transAnim.setDuration(300);
            transAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mBubbleOptLayout.setTranslationX((float)animation.getAnimatedValue());
                }
            });
            transAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    internalUpdatePillListRightTrans();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    internalUpdatePillListRightTrans();
                }
            });
            mPillListRightTransAnim.playTogether(transAnim);
            mPillListRightTransAnim.start();
        } else {
            internalUpdatePillListRightTrans();
        }
    }

    private void internalUpdatePillListRightTrans() {
        ViewGroup.MarginLayoutParams optLp = (ViewGroup.MarginLayoutParams) mBubbleOptLayout.getLayoutParams();
        optLp.rightMargin = mPillListRightMargin;
        mBubbleOptLayout.setTranslationX(0);
        mBubbleOptLayout.setAlpha(1f);
        mBubbleOptLayout.requestLayout();
        mBubbleOptLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mBubbleOptLayout.removeOnLayoutChangeListener(this);
                requestBubbleOptLayoutUpdateRegion();
            }
        });
    }

    public int getBubbleListEditWidth() {
        return mBubbleOptLayout.getBubbleListView().getEditWidth();
    }

    public boolean isBubbleWindowEnable() {
        return mBubbleFrameLayout.getVisibility() == View.VISIBLE;
    }

    public void setBubbleWindowEnable(boolean enable) {
        if (mBubbleFrameLayout == null) {
            log.error("mBubbleFrameLayout is still null");
            return;
        }
        if (isInPptMode() && enable) {
            return;
        }
        if (enable) {
            mBubbleFrameLayout.setVisibility(View.VISIBLE);
        } else {
            mBubbleFrameLayout.setVisibility(View.GONE);
        }
    }

    public void updateVisibility() {
        updateVisibility(HIDE_BUBBLE_STATUS_NONE);
    }

    // on update visibility for the whole view!
    // if you want to show list, call some other mothod, like playShowAnimation()...
    public void updateVisibility(int hideBubbleStatus) {
        if (!Utils.isSetupCompelete(mContext)) {
            log.error("setup is not completed for this device !");
            StatusManager.setStatus(StatusManager.FORCE_HIDE_WINDOW, true);
            return;
        }
        if (!isWindowTouchEnable()) {
            return;
        }

        boolean isFeaturePhoneMode = StatusManager.getStatus(StatusManager.FEATURE_PHONE_MODE);
        if (!isShieldShowList()
                && hideBubbleStatus == HIDE_BUBBLE_STATUS_NONE
                && !isFeaturePhoneMode
                && (Constants.IS_IDEA_PILLS_ENABLE)) {
            if (!StatusManager.getStatus(StatusManager.FORCE_HIDE_WINDOW)) {
                setBubbleWindowEnable(true);
            }
        } else {
            if (hideBubbleStatus == HIDE_BUBBLE_STATUS_IMMEDIATELY) {
                hideAllBubble();
            } else if (hideBubbleStatus == HIDE_BUBBLE_STATUS_NORMAL) {
                mBubbleOptLayout.playHideAnimationAndResumeToNormal();
            } else {
                mBubbleOptLayout.playHideAnimation();
                setBubbleWindowEnable(false);
            }
        }
    }

    public boolean isShieldShowList() {
        return StatusManager.getStatus(StatusManager.SHIELD_SHOW_LIST) && !isExtDisplay();
    }

    public void handleBubbleBySelf(Set<Integer> ids) {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.handleBubbleBySelf(ids);
        }
    }

    public void playHideAnimation() {
        playHideAnimation(true);
    }

    public void playHideAnimation(boolean isForceStopLastAnimIfRunning) {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.playHideAnimation(isForceStopLastAnimIfRunning);
        }
    }

    public void playHideAnimationWithTask(Runnable task) {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.playHideAnimationWithTask(task);
        }
    }

    public boolean isAlreadyShow() {
        return mBubbleOptLayout != null && mBubbleOptLayout.getBubbleListView().isAlreadyShow();
    }

    public void showOrHide() {
        if (isInPptMode()) {
            return;
        }
        if (isAlreadyShow()) {
            playHideAnimation();
        } else {
            if (mBubbleOptLayout != null
                    && !isShieldShowList()) {
                mBubbleOptLayout.getBubbleListView().playShowAnimation(null);
                StatusBarManager sbm = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
                sbm.collapsePanels();
                Tracker.onEvent("A420019","type",1);
            }
        }
    }

    public void setWillAcceptKeyCode(boolean willAcceptKeyCode) {
        FrameLayout parent = (FrameLayout) mBubbleOptLayout.getParent();
        if (willAcceptKeyCode) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) parent.getLayoutParams();
            lp.flags &= (~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            if (!mIsExtDisplay) {
                lp.flags &= (~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            }
            lp.privateFlags &= (~WindowManager.LayoutParams.PRIVATE_FLAG_EXT_NOT_CAPTURE);
            mWindowManager.updateViewLayout(parent, lp);
        } else {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) parent.getLayoutParams();
            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            lp.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_EXT_NOT_CAPTURE;
            mWindowManager.updateViewLayout(parent, lp);
        }
    }

    public void updateInputStatus(boolean needInput) {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.onInputStatusChange(needInput);
        }
    }

    public boolean isInputting() {
        return mBubbleOptLayout != null && mBubbleOptLayout.findInputtingView() != null;
    }

    public void setSecondBoom(boolean secondBoom) {
        mSecondBoom = secondBoom;
    }

    public boolean isSecondBoom() {
        return mSecondBoom;
    }

    public PointF getToAddLoc() {
        if (!Constants.WINDOW_READY || mBubbleOptLayout == null) {
            log.error("window still not ready");
            return null;
        }
        return mBubbleOptLayout.getToAddLoc();
    }

    public Bundle addGlobalBubbles(final List<GlobalBubble> globalBubbles,
                                   final List<GlobalBubbleAttach> globalAttaches, Bundle extra) {
        if (globalBubbles == null || globalBubbles.size() == 0) {
            log.error("addGlobalBubble null");
            return null;
        }
        /*
        boolean addUsedBubble = false;
        int count = globalBubbles.size();
        for (int i = 0; i < count; i++) {
            GlobalBubble bubble = globalBubbles.get(i);
            if (bubble == null) {
                continue;
            }
            if (bubble.getUsedTime() > 0) {
                addUsedBubble = true;
                break;
            }
        }
        if (addUsedBubble) {
            List<BubbleItem> bubbleItems = GlobalBubbleManager.getInstance().
                    getBubbleItemsFrom(globalBubbles, globalAttaches);
            //just save to db
            GlobalBubbleManager.getInstance().saveBubbleItems(bubbleItems, true);
            return null;
        }*/

        Bundle result = new Bundle();
        /**
         * if recovery from trash, the bubbles have ids
         */
        final boolean doAnim = extra == null ? false:extra.getBoolean("KEY_ANIM", false);
        if (doAnim && globalBubbles.size() > 0 && !isInPptMode()) {
            UIHandler.post(new Runnable() {
                public void run() {
                    if (mBubbleOptLayout == null) {
                        return;
                    }
                    mBubbleOptLayout.playAnimBubbleFlyIn(new BubbleItem(globalBubbles.get(0)));
                }
            });
        }
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GlobalBubbleManager.getInstance().addGlobalBubblesFromSara(globalBubbles, globalAttaches);
            }
        }, 300);
        result.putParcelable("KEY_LOC", BubbleController.getInstance().getToAddLoc());
        return result;
    }

    public void requestShowBubble(Context context, final float speed) {
        LOG.d("request show bubble ext: " + isExtDisplay());
        if (Constants.IS_IDEA_PILLS_ENABLE && StatusManager.canShowAllBubbles()
                && !Utils.isKeyguardLocked()
                && !Utils.isShieldSlidingShowIdeapills(context)
                && !StatusManager.getStatus(StatusManager.FORCE_HIDE_WINDOW)
                && !isShieldShowList()) {
            if (!isExtDisplay() && IdeaPillsApp.isShieldShowIdeapillsByTopAppInfo(context)) {
                //is game
                return;
            }

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBubbleOptLayout == null) {
                        return;
                    }
                    setBubbleWindowEnable(true);
                    mBubbleOptLayout.refreshPullViewAlpha();
                    if (!mBubbleOptLayout.isBubbleListVisible()) {
                        boolean result = requestTouchFocus();
                        final boolean leftCornerOneHanded = (mBubbleOptLayout.getViewRootImpl().getThumbModeState() & ViewRootImpl.BIT_WINDOW_IN_THUMB_MODE_TYPE_L_CORNER_ONEHANDED) != 0;
                        float velocity = BubbleOptLayout.MIN_VELOCITY;
                        if (leftCornerOneHanded) {
                            velocity = BubbleOptLayout.SCALE_MIN_VELOCITY;
                            if (speed > velocity) {
                                mBubbleOptLayout.getBubbleListView().playShowAnimation(null);
                            }
                        } else {
                            if (result) {
                                if (speed > velocity) {
                                    mBubbleOptLayout.setForceShow(true);
                                }
                                mBubbleFrameLayout.setTouchEventReceiver(mBubbleOptLayout.new TouchReceiver());
                            } else if (speed > velocity) {
                                mBubbleOptLayout.getBubbleListView().playShowAnimation(null);
                            }
                            if (LOG.DBG) {
                                log.info("requestTouchFocus result=" + result + " speed=" + speed);
                            }
                        }
                    }
                    Tracker.onEvent("A420019","type",0);
                    Tracker.onEvent("A420028","type",1);
                }
            });
        }
    }

    public void showConfirmDialog(Context context, CharSequence title, CharSequence message, final Runnable positive, final Runnable negative) {
        showConfirmDialog(context, title, message, positive, negative, android.R.string.ok, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

    public void showConfirmDialog(Context context, CharSequence title, CharSequence message, final Runnable positive, final Runnable negative, int posiTextId, int windowType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton(posiTextId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (positive != null) {
                    positive.run();
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (negative != null) {
                    negative.run();
                }
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (negative != null) {
                    negative.run();
                }
            }
        });
        mDialog = builder.create();
        mDialog.getWindow().getAttributes().type = windowType;
        mDialog.show();
    }

    public void dismissConfirmDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public void setBubbleHandleBySidebar(boolean handleBySidebar) {
        mBubbleHandleBySidebar = handleBySidebar;
    }

    public boolean isBubbleHandleBySidebar() {
        return mBubbleHandleBySidebar;
    }

    public void onPhoneBusy() {
        dismissConfirmDialog();
        if (!Constants.WINDOW_READY) {
            log.error("window still not ready");
            return;
        }
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                playHideAnimation(false);
            }
        }, 300L);
    }

    public boolean isWindowTouchEnable() {
        if (mBubbleOptLayout == null) {
            return false;
        }
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) mBubbleFrameLayout.getLayoutParams();
        return (lp.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0;
    }

    public void setWindowTouchEnable(boolean enable) {
        if (isInPptMode() && enable) {
            return;
        }
        if (enable) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) mBubbleFrameLayout.getLayoutParams();
            lp.flags &= (~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mWindowManager.updateViewLayout(mBubbleFrameLayout, lp);
        } else {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) mBubbleFrameLayout.getLayoutParams();
            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            mWindowManager.updateViewLayout(mBubbleFrameLayout, lp);
        }
    }

    public void dump() {
        log.info("mBubbleFrameLayout visible=" + mBubbleFrameLayout.getVisibility());
        log.info("mBubbleOptLayout visible=" + mBubbleOptLayout.getVisibility());
    }

    public void onPackageChange() {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.onPackageChanged();
        }
    }

    public boolean requestTouchFocus() {
        return Utils.requestViewTouchFocus(mBubbleFrameLayout);
    }

    public void dimBackgroundByMove(float interpolator) {
        if (!mIsExtDisplay) {
            int current = (int)(BubbleListView.sDarkColor * interpolator);
            mBubbleFrameLayout.setBackgroundColor(current<<24);
        }
    }

    public void hideBubbleListImmediately() {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.hideBubbleListImmediately();
        }
    }

    public void hideBubbleListWithAnim() {
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.hideBubbleListWithAnim();
        }
    }

    public void showBubbleList(long syncId){
        if (mBubbleOptLayout != null) {
            mBubbleOptLayout.showBubbleList(syncId);
        }
    }

    public boolean isBubbleListVisible() {
        return mBubbleOptLayout != null && mBubbleOptLayout.isBubbleListVisible();
    }

    public void hideAllBubble() {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBubbleOptLayout != null) {
                    mBubbleOptLayout.forceHideAll();
                    dimBackgroundByMove(0);
                }
                setWindowTouchEnable(false);
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setBubbleWindowEnable(false);
                    }
                }, 50);
            }
        });
    }

    public BubbleItemViewHelper getBubbleItemViewHelper() {
        if (mBubbleItemViewHelper == null) {
            mBubbleItemViewHelper = new BubbleItemViewHelper(mContext);
        }
        return mBubbleItemViewHelper;
    }
}