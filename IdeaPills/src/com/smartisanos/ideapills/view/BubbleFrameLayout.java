package com.smartisanos.ideapills.view;

import android.annotation.ColorInt;
import android.annotation.DrawableRes;
import android.annotation.LayoutRes;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.internal.view.FloatingActionMode;
import com.android.internal.widget.SmartisanFloatingToolbar;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;

import smartisanos.view.PressGestureDetector;

public class BubbleFrameLayout extends FrameLayout{
    private static final LOG log = LOG.getInstance(BubbleFrameLayout.class);

    private View mFloatingActionModeOriginatingView;
    private FloatingActionMode mFloatingActionMode;
    private SmartisanFloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
    PressGestureDetector mPressGestureDetector;
    private boolean mDiscardNextActionUp;
    private OnTouchListener mTouchEventRecevier = null;

    public BubbleFrameLayout(Context context) {
        this(context, null);
    }

    public BubbleFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFrameLayout(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPressGestureDetector = new PressGestureDetector(context, this);
        mPressGestureDetector.setBoomDelegate(new PressGestureDetector.BoomDelegate() {
            @Override
            public boolean onTextBoom(View touchView) {
                String text = touchView instanceof BubbleEditText
                        ? ((BubbleEditText) touchView).getShowText() : ((TextView) touchView).getText().toString();
                View view = (View) touchView.getParent();
                for (; view != null; ) {
                    if (view instanceof BubbleItemView) {
                        BubbleItemView itemView = (BubbleItemView) view;
                        Utils.startBoomActivity(getContext(), itemView, text, itemView.getBubbleItem(),Utils.isEditable(touchView));
                        break;
                    }
                    view = (View) view.getParent();
                }
                mDiscardNextActionUp = true;
                return true;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mTouchEventRecevier != null) {
            boolean handle = mTouchEventRecevier.onTouch(this, ev);
            switch (ev.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    mTouchEventRecevier = null;
                }
            }
            return handle;
        }
        final int oldAction = ev.getAction();
        if (oldAction == MotionEvent.ACTION_DOWN) {
            mDiscardNextActionUp = false;
        }
        mPressGestureDetector.dispatchTouchEvent(ev, isHandlingTouchEvent());
        if (mDiscardNextActionUp) {
            ev.setAction(MotionEvent.ACTION_CANCEL);
            log.info("action=" + ev.getAction());
        }
        setHandlingTouchEvent(true);
        boolean ret = super.dispatchTouchEvent(ev);
        setHandlingTouchEvent(false);
        ev.setAction(oldAction);
        return ret;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            mPressGestureDetector.handleBackKey();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPressGestureDetector.onAttached(null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPressGestureDetector.onDetached();
    }

    /** @hide */
    @Override
    public boolean isLongPressSwipe() {
        return mPressGestureDetector.isLongPressSwipe();
    }


    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback,
                                              int type) {
        if (type == ActionMode.TYPE_FLOATING) {
            return startActionMode(originalView, callback, type);
        }
        return super.startActionModeForChild(originalView, callback, type);
    }

    private FloatingActionMode createFloatingActionMode(
            View originatingView, ActionMode.Callback2 callback) {
        if (mFloatingActionMode != null) {
            mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        final FloatingActionMode mode =
                new FloatingActionMode(mContext, callback, originatingView);
        mFloatingActionModeOriginatingView = originatingView;
        mFloatingToolbarPreDrawListener =
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mode.updateViewLocationInWindow();
                        return true;
                    }
                };
        return mode;
    }

    private void setHandledFloatingActionMode(FloatingActionMode mode) {
        mFloatingActionMode = mode;
        mFloatingToolbar = new SmartisanFloatingToolbar(mContext, mFakeWindow);
        mFloatingActionMode.setFloatingToolbar(mFloatingToolbar);
        mFloatingActionMode.invalidate();  // Will show the floating toolbar if necessary.
        mFloatingActionModeOriginatingView.getViewTreeObserver()
                .addOnPreDrawListener(mFloatingToolbarPreDrawListener);
    }

    private void cleanupFloatingActionModeViews() {
        if (mFloatingToolbar != null) {
            mFloatingToolbar.dismiss();
            mFloatingToolbar = null;
        }
        if (mFloatingActionModeOriginatingView != null) {
            if (mFloatingToolbarPreDrawListener != null) {
                mFloatingActionModeOriginatingView.getViewTreeObserver()
                        .removeOnPreDrawListener(mFloatingToolbarPreDrawListener);
                mFloatingToolbarPreDrawListener = null;
            }
            mFloatingActionModeOriginatingView = null;
        }
    }

    private ActionMode startActionMode(
            View originatingView, ActionMode.Callback callback, int type) {
        ActionMode.Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
        FloatingActionMode mode = createFloatingActionMode(originatingView, wrappedCallback);
        if (mode != null && wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
            setHandledFloatingActionMode(mode);
        } else {
            mode = null;
        }
        return mode;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            requestFitSystemWindows();
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            if (mode == mFloatingActionMode) {
                cleanupFloatingActionModeViews();
                mFloatingActionMode = null;
            }
            requestFitSystemWindows();
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (mWrapped instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) mWrapped).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    /**
     * Minimal window to satisfy FloatingToolbar.
     */
    private Window mFakeWindow = new Window(mContext) {

        public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {

        }

        public void takeSurface(SurfaceHolder.Callback2 callback) {
        }

        public void takeInputQueue(InputQueue.Callback callback) {
        }

        public boolean isFloating() {
            return false;
        }

        public void alwaysReadCloseOnTouchAttr() {
        }

        public void setContentView(@LayoutRes int layoutResID) {
        }

        public void setContentView(View view) {
        }

        public void setContentView(View view, ViewGroup.LayoutParams params) {
        }

        public void addContentView(View view, ViewGroup.LayoutParams params) {
        }

        public void clearContentView() {
        }

        public View getCurrentFocus() {
            return null;
        }

        public LayoutInflater getLayoutInflater() {
            return null;
        }

        public void setTitle(CharSequence title) {
        }

        public void setTitleColor(@ColorInt int textColor) {
        }

        public void openPanel(int featureId, KeyEvent event) {
        }

        public void closePanel(int featureId) {
        }

        public void togglePanel(int featureId, KeyEvent event) {
        }

        public void invalidatePanelMenu(int featureId) {
        }

        public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
            return false;
        }

        public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
            return false;
        }

        public void closeAllPanels() {
        }

        public boolean performContextMenuIdentifierAction(int id, int flags) {
            return false;
        }

        public void onConfigurationChanged(Configuration newConfig) {
        }

        public void setBackgroundDrawable(Drawable drawable) {
        }

        public void setFeatureDrawableResource(int featureId, @DrawableRes int resId) {
        }

        public void setFeatureDrawableUri(int featureId, Uri uri) {
        }

        public void setFeatureDrawable(int featureId, Drawable drawable) {
        }

        public void setFeatureDrawableAlpha(int featureId, int alpha) {
        }

        public void setFeatureInt(int featureId, int value) {
        }

        public void takeKeyEvents(boolean get) {
        }

        public boolean superDispatchKeyEvent(KeyEvent event) {
            return false;
        }

        public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
            return false;
        }

        public boolean superDispatchTouchEvent(MotionEvent event) {
            return false;
        }

        public boolean superDispatchTrackballEvent(MotionEvent event) {
            return false;
        }

        public boolean superDispatchGenericMotionEvent(MotionEvent event) {
            return false;
        }

        public View getDecorView() {
            return BubbleFrameLayout.this;
        }

        public View peekDecorView() {
            return null;
        }

        public Bundle saveHierarchyState() {
            return null;
        }

        public void restoreHierarchyState(Bundle savedInstanceState) {
        }

        protected void onActive() {
        }

        public void setChildDrawable(int featureId, Drawable drawable) {
        }

        public void setChildInt(int featureId, int value) {
        }

        public boolean isShortcutKey(int keyCode, KeyEvent event) {
            return false;
        }

        public void setVolumeControlStream(int streamType) {
        }

        public int getVolumeControlStream() {
            return 0;
        }

        public int getStatusBarColor() {
            return 0;
        }

        public void setStatusBarColor(@ColorInt int color) {
        }

        public int getNavigationBarColor() {
            return 0;
        }

        public void setNavigationBarColor(@ColorInt int color) {
        }

        public void setDecorCaptionShade(int decorCaptionShade) {
        }

        public void setResizingCaptionDrawable(Drawable drawable) {
        }

        public void onMultiWindowModeChanged() {
        }

        public void reportActivityRelaunched() {
        }
    };

    public void setTouchEventReceiver(OnTouchListener listener) {
        mTouchEventRecevier = listener;
    }
}
