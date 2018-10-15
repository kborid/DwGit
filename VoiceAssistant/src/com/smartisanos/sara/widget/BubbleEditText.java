package com.smartisanos.sara.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.service.onestep.GlobalBubble;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.common.util.SdkReflectUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraConstant;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import smartisanos.api.ToastSmt;

public class BubbleEditText extends EditText implements TextWatcher {

    private static final String TAG = "VoiceAss.BubbleEditText";
    private boolean mIsFoucsed;
    private Drawable mTopShade = null;
    private Drawable mBottomShade = null;
    private int mColor;
    private Toast mErrorToast;
    private static final int OFF_SET_SHADE = 10;
    private String mBeforeString = "";
    private int bubbleTextMax = SaraConstant.BUBBLE_TEXT_MAX;

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mEditModeChangeListener != null) {
            String showText = getText() == null ? "" : getText().toString();
            if (count > before && CommonUtils.getStringLength(showText) > SaraConstant.BUBBLE_TEXT_MAX) {
                removeTextChangedListener(this);
                setText(mBeforeString);
                setSelection(mBeforeString.length());
                addTextChangedListener(this);
                ToastSmt.getInstance().makeText(getContext(), getContext().getResources().getString(
                        R.string.bubble_add_string_limit), Toast.LENGTH_SHORT,
                        WindowManager.LayoutParams.TYPE_TOAST).show();
                return;
            }
            if (mEditModeChangeListener != null) {
                mEditModeChangeListener.onEditText(this, showText);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mBeforeString = s.toString();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    public interface EditModeChangeListener {
        void onEditText(BubbleEditText edit, String txt);

        void onFinishEdit();
    }

    private EditModeChangeListener mEditModeChangeListener = null;

    public BubbleEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleEditText(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setBubbleTextMax(int bubbleTextMax) {
        this.bubbleTextMax = bubbleTextMax;
    }

    private void setDrawScrollBarEnable(boolean flag) {
        try {
            Field field = View.class.getDeclaredField("mDrawScrollBar");
            if (field != null) {
                field.setAccessible(true);
                field.set(this, flag);
            }
        } catch (NoSuchFieldException e) {

        } catch (IllegalAccessException e) {

        }
    }

    private void init() {
        setDrawScrollBarEnable(true);
        setBackground(null);
        setVerticalScrollBarEnabled(true);
        setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
        addTextChangedListener(this);
        // setPadding(0, getPaddingTop(), 0, getPaddingBottom());
        mTopShade = getResources().getDrawable(R.drawable.shade_top);
        mBottomShade = getResources().getDrawable(R.drawable.shade_bottom);
        try {
            Method voiceInputMethod = SdkReflectUtils.findMethod(View.class, "setVoiceInputVisibility",
                    boolean.class);
            if (voiceInputMethod != null) {
                SdkReflectUtils.invokeMethod(voiceInputMethod, this, false);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isFocus = isFocused();
        boolean handle = false;
        if (isFocus || !event.isFromSource(InputDevice.SOURCE_MOUSE) ||
                (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) == 0) {
            handle = super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getHeight() == getMaxHeight()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                mIsFoucsed = isFocus;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getHeight() == getMaxHeight()) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return handle;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (mEditModeChangeListener != null) {
                mEditModeChangeListener.onFinishEdit();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void registerEditModeChangeListener(EditModeChangeListener listener) {
        this.mEditModeChangeListener = listener;
    }

    public void callInputMethodDelay(long times) {
        times = times < 0 ? 0 : times;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                final InputMethodManager imm = InputMethodManager.peekInstance();
                boolean result = imm.showSoftInput(BubbleEditText.this,
                        InputMethodManager.SHOW_IMPLICIT);
                if (!result) {
                    requestFocus();
                    imm.focusIn(BubbleEditText.this);
                    imm.showSoftInput(BubbleEditText.this, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, times);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getHeight() == getMaxHeight()) {
            canvas.save();
            mTopShade.setBounds(0, getScrollY() + getPaddingTop(), getWidth()- OFF_SET_SHADE,
                    mTopShade.getIntrinsicHeight() + getScrollY() + getPaddingTop());
            mTopShade.draw(canvas);
            mBottomShade.setBounds(0,
                    getScrollY() + getHeight() - mTopShade.getIntrinsicHeight()
                            - getPaddingBottom()
                    , getWidth()- OFF_SET_SHADE, getHeight() + getScrollY() - getPaddingBottom());
            mBottomShade.draw(canvas);
            canvas.restore();
        }
    }

    public void setShadeColor(int color) {
        if (mColor == color) {
            return;
        }
        switch (color) {
            case GlobalBubble.COLOR_RED: {
                mTopShade = getResources().getDrawable(R.drawable.shade_red_top);
                mBottomShade = getResources().getDrawable(R.drawable.shade_red_bottom);
            }
                break;
            case GlobalBubble.COLOR_ORANGE: {
                mTopShade = getResources().getDrawable(R.drawable.shade_orange_top);
                mBottomShade = getResources().getDrawable(R.drawable.shade_orange_bottom);
            }
                break;
            case GlobalBubble.COLOR_GREEN: {
                mTopShade = getResources().getDrawable(R.drawable.shade_green_top);
                mBottomShade = getResources().getDrawable(R.drawable.shade_green_bottom);
            }
                break;
            case GlobalBubble.COLOR_PURPLE: {
                mTopShade = getResources().getDrawable(R.drawable.shade_purple_top);
                mBottomShade = getResources().getDrawable(R.drawable.shade_purple_bottom);
            }
                break;
            case GlobalBubble.COLOR_NAVY_BLUE: {
                mTopShade = getResources().getDrawable(R.drawable.ppt_shade_top);
                mBottomShade = getResources().getDrawable(R.drawable.ppt_shade_bottom);
            }
                break;
            case GlobalBubble.COLOR_SHARE: {
                mTopShade = getResources().getDrawable(R.drawable.shade_top_share);
                mBottomShade = getResources().getDrawable(R.drawable.shade_bottom_share);
            }
            break;
            default: {
                mTopShade = getResources().getDrawable(R.drawable.shade_top);
                mBottomShade = getResources().getDrawable(R.drawable.shade_bottom);
            }
                break;
        }
        mColor = color;
        invalidate();
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        return false;
    }

    public void toDoOver(boolean over) {
        if (over) {
            setPaintFlags(getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            setAlpha(0.4f);
        } else {
            setPaintFlags(getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            setAlpha(1.0f);
        }
        invalidate();
    }

    public void modifyCursorDrawable(int drawableResId){
        if(drawableResId != 0) {
            try {
                Field setCursor = TextView.class.getDeclaredField("mCursorDrawableRes");
                setCursor.setAccessible(true);
                setCursor.set(this, drawableResId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateHintText(String hintStr) {
        setHint(hintStr);
    }

    public int getSingleLineHeight() {
        Layout layout = getLayout();
        if(layout != null) {
            int desired = layout.getLineTop(1);
            return getExtendedPaddingTop() + getExtendedPaddingTop() + desired;
        }
        return 0;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        boolean handled = false;
        //mac mode
        if ((event.getFlags() & MultiSdkUtils.FLAG_MAC_MODE) != 0) {
            if (event.isMetaPressed()) {
                handled = handleShortcutKeyEvent(keyCode, event.hasModifiers(KeyEvent.META_META_ON | KeyEvent.META_SHIFT_ON));
            }
            //windows mode
        } else if (event.hasModifiers(KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON)) {
            handled = handleShortcutKeyEvent(keyCode, true);
        } else if (event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            handled = handleShortcutKeyEvent(keyCode, false);
        }
        if (!handled) {
            return super.onKeyShortcut(keyCode, event);
        }
        return handled;
    }

    private boolean handleShortcutKeyEvent(int keyCode, boolean shift) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_Z:
                if (shift) {
                    onTextContextMenuItem(android.R.id.redo);
                } else {
                    onTextContextMenuItem(android.R.id.undo);
                }
                return true;
            case KeyEvent.KEYCODE_Y:
                if (!shift) {
                    onTextContextMenuItem(android.R.id.redo);
                    return true;
                }
                break;
        }
        return false;
    }
}
