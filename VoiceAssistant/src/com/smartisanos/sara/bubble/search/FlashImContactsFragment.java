package com.smartisanos.sara.bubble.search;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Space;
import android.widget.TextView;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimInterpolator;
import com.smartisanos.ideapills.common.event.Event;
import com.smartisanos.ideapills.common.event.android.BaseEventFragment;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.contact.view.IMPickContactView;
import com.smartisanos.sara.bullet.contact.view.IPickContactView;
import com.smartisanos.sara.bullet.util.DisplayUtils;
import com.smartisanos.sara.lock.LockContainerView;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;

import java.util.ArrayList;


public class FlashImContactsFragment extends BaseEventFragment implements IMPickContactView.OnPickedListener {

    public static final int ACTION_UNLOCK_SUCCESS = 1;
    public static final int ACTION_FINSH_VIEW = 2;
    public static final String SARA_LOCK_DIALOG_SHOW = "sara_lock_dialog_show";

    private IMPickContactView imPickContactView;
    private View rootView;
    private boolean isSendAnimRunning = false;
    private boolean isHasOpenLocked;
    private ViewGroup mContaierView;
    private boolean isAuthenticationStart = false;
    private boolean isInstallFlashIm = false;
    private boolean isLoginFlashIm = false;
    private boolean mHasShowLockView = false;

    private final PopUpHandler mPopUpHandler = new PopUpHandler();

    private LockContainerView mLockContainerView;
    private Space holdPlaceSpace;
    private View  mBulletTipLayout;
    private TextView mTipText;
    private TextView mTipLogin;
    private ViewStub mLockContainerViewStub;
    private ViewStub mBulletTipLayoutStub;
    private boolean mHasSendDialogShowBroadcast = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_flash_im_contacts, container, false);
        mContaierView =(ViewGroup) rootView.findViewById(R.id.contaier_view);
        holdPlaceSpace = (Space) rootView.findViewById(R.id.holdPlaceSpace);
        mBulletTipLayoutStub = (ViewStub) rootView.findViewById(R.id.bullet_tip_stub);
        mLockContainerViewStub = (ViewStub) rootView.findViewById(R.id.lock_container_stub);
        initBulletContactView();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        attachListener(activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshBulletView(true);
        if (isInstallFlashIm && isLoginFlashIm) {
            if (null != imPickContactView && !imPickContactView.hasContacts()) {
                imPickContactView.load();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d("onResume()");
        if (isVisible()) {
            postVisibleEvent(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        refreshBulletView(false);
        sendDismissDialogBroadcast();
    }

    private void attachListener(Context context) {
    }

    public static FlashImContactsFragment newInstance() {
        return new FlashImContactsFragment();
    }

    private void initBulletContactView() {
        imPickContactView = (IMPickContactView) rootView.findViewById(R.id.pick_contact);
        imPickContactView.setCheckEffect(false);
        imPickContactView.registerPickListener(this);
        imPickContactView.setOnScrollListener(new IPickContactView.OnScrollListener() {
            @Override
            public void onScrolled() {
                Event event = new Event(FlashImContactsEvent.ACTION_SEND_BULLET_HIDE_IUPUT_METHOD);
                Bundle bundle = new Bundle();
                bundle.putBoolean("hideInputMethod", true);
                event.putExtra(bundle);
                post(event);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("onDestroy()");
        if (null != imPickContactView) {
            imPickContactView.onDestroy();
        }
    }

    @Override
    public void OnPickedContact(ArrayList<ContactItem> selected) {
        LogUtils.i("OnPickedContact() " + selected);
    }

    @Override
    public void OnCheckContact(AbsContactItem absContactItem, View view) {
        LogUtils.i("OnCheckContact() " + absContactItem.toString());
        Event finishAnimEvent = new Event(FlashImContactsEvent.ACTION_SEND_BULLET_MESSAGE_PRE);
        Bundle bundle = new Bundle();
        bundle.putBoolean("withFlyAnim", true);
        final int[] selectLoc = new int[2];
        view.getLocationOnScreen(selectLoc);
        selectLoc[0] = selectLoc[0] + view.getMeasuredWidth() / 2;
        selectLoc[1] = selectLoc[1] + view.getMeasuredHeight() / 2;
        bundle.putIntArray("selectLoc", selectLoc);
        bundle.putParcelable("contactItem", absContactItem);
        finishAnimEvent.putExtra(bundle);
        post(finishAnimEvent);

        startSelectedContactViewAnim(absContactItem, view);
    }

    public boolean hideViewFromAction(int bubbleItemTranslateY, int from, boolean finish, Runnable runnable) {
        if (from == 0) {
            if (mContaierView != null && mContaierView.getVisibility() == View.VISIBLE) {
                if (finish) {
                    AnimManager.HideViewWithAlphaAnim(mContaierView, 200, 250);
                    return true;
                } else {
                    runnable.run();
                    AnimManager.HideViewWithAlphaAnim(mContaierView, 200, 250);
                }
            }
        }
        return false;
    }

    public boolean clearBulletViewAnimation(boolean isClearAnimation) {
        if (mContaierView != null) {
            if (mContaierView.getAnimation() != null && !mContaierView.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mContaierView.clearAnimation();
        }
        return isClearAnimation;
    }

    public void resetBulletState() {
        clearBulletViewAnimation(true);
        if (mContaierView != null) {
            mContaierView.setVisibility(View.GONE);
            if (null != imPickContactView) {
                imPickContactView.resetSearchResult();
            }
        }
    }

    private void refreshBulletView(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            LogUtils.d("Fragment is visible.");
            isInstallFlashIm = PackageUtils.isAvilibleApp(getActivity(), SaraConstant.PACKAGE_NAME_BULLET);
            if (isInstallFlashIm) {
                isLoginFlashIm = PackageUtils.isBulletAppLogin(getActivity());
                if (isLoginFlashIm) {
                    if (Build.VERSION.SDK_INT >= 25) {
                        if (isKeyguardVerified()&& !isHasOpenLocked) {
                            //需要开启
                            isAuthenticationStart = true;
                        } else {
                            isAuthenticationStart = false;
                        }
                    }else {
                        isAuthenticationStart =false;
                    }
                }
            }
            LogUtils.d("Fragment is visible.isInstall:" + isInstallFlashIm + ", isLogin:" + isLoginFlashIm);
            showBulletView();
        } else {
            LogUtils.d("Fragment is not visible.");
            isLoginFlashIm = false;
            isInstallFlashIm = false;
            isAuthenticationStart = false;
            mHasShowLockView = false;
        }
    }

    public void showBulletView() {
        if (null != mContaierView) {
            mContaierView.setVisibility(View.VISIBLE);
        }
        if (!isInstallFlashIm || !isLoginFlashIm) {
            setBulletTipLayoutViewVisiable(View.VISIBLE);
            setLockContainerViewVisiable(View.INVISIBLE);
            updateBulletTipText(isInstallFlashIm, isLoginFlashIm);
            imPickContactView.setVisibility(View.INVISIBLE);
        } else {
            setBulletTipLayoutViewVisiable(View.INVISIBLE);
            mHasShowLockView = isAuthenticationStart && !isHasOpenLocked && isKeyguardVerified();
            if (mHasShowLockView) {
                setLockContainerViewVisiable(View.VISIBLE);
                getLockContainerView().unlockIfNeedly();
                imPickContactView.setVisibility(View.INVISIBLE);
                sendShowDialogBroadcast();
            } else {
                setLockContainerViewVisiable(View.INVISIBLE);
                imPickContactView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 当StubView设置VISIBLE的时候 ,或者view已经加载过 ,就要需要执行,
     * 否则,没有必要执行,作用懒加载StubView
     *
     * @param visible
     */
    public void setLockContainerViewVisiable(int visible) {
        if (visible == View.VISIBLE || mLockContainerView != null) {
            getLockContainerView().setVisibility(visible);
        }
    }

    public void setBulletTipLayoutViewVisiable(int visible) {
        if (visible == View.VISIBLE || mBulletTipLayout != null) {
            getBulletTipLayoutView().setVisibility(visible);
        }
    }

    private boolean isKeyguardVerified() {
        if (getLockContainerView() == null) {
            return false;
        }
        return getLockContainerView().isKeyguardVerified();
    }

    private void updateBulletTipText(boolean isInstall, boolean isLogin) {
        if (getActivity() == null) {
            return;
        }
        String tipStr = getResources().getString(R.string.search_flash_im_uninstall_tip);
        SpannableStringBuilder span = new SpannableStringBuilder();
        String actStr = getResources().getString(R.string.search_flash_im_go_to_install_tip);
        ClickableSpan clickableSpan = new InstallClickable();
        if (isInstall && !isLogin) {
            tipStr = getResources().getString(R.string.search_flash_im_unlogin_tip);
            actStr = getResources().getString(R.string.search_flash_im_go_to_login_tip);
            clickableSpan = new LoginClickable();
        }
        mTipText.setText(tipStr);
        span.append(actStr);
        span.setSpan(clickableSpan, 0, span.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mTipLogin.setText(span, TextView.BufferType.SPANNABLE);
        mTipLogin.setVisibility(View.VISIBLE);
    }

    class LoginClickable extends ClickableSpan {

        @Override
        public void onClick(View v) {

            if (SaraUtils.isKeyguardLocked()) {
                CommonUtils.keyguardWaitingForActivityDrawn();
            }
            if (SaraUtils.isKeyguardSecureLocked()) {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }

            startFlashAPP();
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(getActivity().getResources().getColor(R.color.bottom_link_tv_color));
            ds.setUnderlineText(true);
        }
    }


    class InstallClickable extends LoginClickable {
        @Override
        public void onClick(View v) {
            jumpToAppStore(getActivity(), SaraConstant.PACKAGE_NAME_BULLET);
        }
    }

    public void showBulletViewWithAnim() {
        if (mContaierView != null && mContaierView.getVisibility() == View.VISIBLE) {
            AnimManager.showViewWithAlphaAndTranslate(mContaierView, 200, 250, 150);
        }
    }

    private void startSelectedContactViewAnim(AbsContactItem absContactItem, final View view) {
        if (isSendAnimRunning) {
            return;
        }
        isSendAnimRunning = true;
        startContactLargeScaleAnim(absContactItem, view);
    }

    private void startContactLargeScaleAnim(final AbsContactItem absContactItem, final View view) {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(300);
        set.setInterpolator(new AnimInterpolator.Interpolator(Anim.CUBIC_OUT));
        set.playTogether(scaleX, scaleY);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                startContactSmallScaleAnim(absContactItem, view);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    private void startContactSmallScaleAnim(final AbsContactItem absContactItem, final View view) {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.1f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(200);
        set.setInterpolator(new AnimInterpolator.Interpolator(Anim.CUBIC_OUT));
        set.playTogether(scaleX, scaleY);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Event sendEvent = new Event(FlashImContactsEvent.ACTION_SEND_BULLET_MESSAGE);
                Bundle bundle = new Bundle();
                bundle.putParcelable("contact", absContactItem);
                sendEvent.putExtra(bundle);
                post(sendEvent);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isSendAnimRunning = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        LogUtils.d("setUserVisibleHint() isVisibleToUser = " + isVisibleToUser);
        refreshBulletView(isVisibleToUser);
        postVisibleEvent(isVisibleToUser);
        if (!isVisibleToUser) {
            if (null != imPickContactView) {
                imPickContactView.resetSearchResult();
            }
        }
    }

    private void postVisibleEvent(boolean isVisible) {
        Event event = new Event(FlashImContactsEvent.ACTION_SEND_BULLET_FRAGMENT_VISIBLE_STATE);
        Bundle bundle = new Bundle();
        bundle.putBoolean("showVoiceSearch", isVisible && isInstallFlashIm && isLoginFlashIm && !mHasShowLockView);
        event.putExtra(bundle);
        post(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogUtils.d("onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        getLockContainerView().onConfigurationChanged(newConfig);
    }

    public class PopUpHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_UNLOCK_SUCCESS:
                    isAuthenticationStart = false;
                    isHasOpenLocked = true;
                    mHasSendDialogShowBroadcast = false;
                    showBulletView();
                    postVisibleEvent(true);
                    break;
                case ACTION_FINSH_VIEW:
                    // 结束Activity
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void refreshView(int resultHeight) {
        if (resultHeight <= 0) {
            holdPlaceSpace.setVisibility(View.GONE);
            return;
        }

        if (holdPlaceSpace == null) {
            LogUtils.d("mResultEmpty cannot be null");
            return;
        }

        FrameLayout.LayoutParams emptyParams = (FrameLayout.LayoutParams) holdPlaceSpace.getLayoutParams();
        emptyParams.height = resultHeight;
        holdPlaceSpace.setLayoutParams(emptyParams);
        holdPlaceSpace.setVisibility(View.VISIBLE);

        if (mContaierView != null) {
            FrameLayout.LayoutParams fingerParams = (FrameLayout.LayoutParams) mContaierView.getLayoutParams();
            fingerParams.topMargin = resultHeight + DisplayUtils.dp2px(-23);
            mContaierView.setLayoutParams(fingerParams);
        }
    }

    public void startFlashAPP() {
        try {
            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(SaraConstant.PACKAGE_NAME_BULLET);
            intent.putExtra("com.smartisanos.doppelganger.had_choose", true);
            getActivity().startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LockContainerView getLockContainerView() {
        if (mLockContainerView == null) {
            initContaierView();
        }
        return mLockContainerView;
    }

    public void initContaierView() {
        if (mLockContainerViewStub != null && mLockContainerViewStub.getParent() != null) {
            mLockContainerView = (LockContainerView) mLockContainerViewStub.inflate();
        } else {
            mLockContainerView = (LockContainerView) rootView.findViewById(R.id.lock_container_layout);
        }
        LogUtils.d("initContaierView");
        mLockContainerView.setHandler(mPopUpHandler);
        mLockContainerView.reset();
    }

    private View getBulletTipLayoutView() {
        if (mBulletTipLayout == null) {
            initBulletTipLayoutView();
        }
        return mBulletTipLayout;
    }

    private void initBulletTipLayoutView() {
        if (mBulletTipLayoutStub != null && mBulletTipLayoutStub.getParent() != null) {
            mBulletTipLayout = mBulletTipLayoutStub.inflate();
        } else {
            mBulletTipLayout = (LockContainerView) rootView.findViewById(R.id.bullet_tip_layout);
        }
        mTipText = (TextView) rootView.findViewById(R.id.tip_text);
        mTipLogin = (TextView) rootView.findViewById(R.id.tips_login);
        mTipLogin.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void jumpToAppStore(Context context, String packageName) {
        SaraUtils.dismissKeyguardOnNextActivity();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String str = "smartisan://smartisan.com/details?id=" + packageName;
        intent.setData(Uri.parse(str));
        try {
            intent.putExtra("from_package",context.getPackageName());
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDismissDialogBroadcast() {
        if (mHasSendDialogShowBroadcast) {
            sendLockDialogBroadcast(false);
            mHasSendDialogShowBroadcast = false;
        }
    }

    private void sendShowDialogBroadcast() {
        if (!mHasSendDialogShowBroadcast) {
            sendLockDialogBroadcast(true);
            mHasSendDialogShowBroadcast = true;
        }
    }

    /**
     * 当在显示锁屏倒计时的时候,跳转到其他页面，系统拦截，会到先来到系统解锁页面，
     * 需要通知系统解锁页面同步倒计时信息，只有当前未解锁时发送广播才有意义．
     * Ticketid : 0293211
     * @param show
     */
    private void sendLockDialogBroadcast(boolean show) {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(SARA_LOCK_DIALOG_SHOW);
        intent.putExtra("show", show);
        getActivity().sendBroadcast(intent);
    }

}
