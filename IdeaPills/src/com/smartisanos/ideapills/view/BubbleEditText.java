package com.smartisanos.ideapills.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.common.util.CommonUtils;

import java.lang.reflect.Field;


public class BubbleEditText extends EditText implements TextWatcher {

    private static final LOG log = LOG.getInstance(BubbleEditText.class);

    private int mMaxHeight;
    boolean mNeedMaxHeight = false;
    private int mLastCursorDrawableResId;
    private int mCurrentLineCount;

    private Rect mTempRect = new Rect();
    private IBubbleStateListener mBubbleStateListener;
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        try {
            Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            mLastCursorDrawableResId = field.getInt(this);
        } catch (Exception e) {

        }
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        ViewParent parent = getParent();
        if (parent instanceof BubbleScrollView) {
            // Just equest to the parent sco
            mTempRect.set(rectangle);
            return parent.requestChildRectangleOnScreen(this, mTempRect, immediate);
        }
        return super.requestRectangleOnScreen(rectangle, immediate);
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mEditModeChangeListener != null) {
            String showText = getShowText();
            mEditModeChangeListener.onEditText(showText);

            if (mOnEditLineChangeListener != null) {
                int scrollRange = this.getLineCount();
                if (scrollRange != mCurrentLineCount && scrollRange != 0) {
                    mCurrentLineCount = scrollRange;
                    mOnEditLineChangeListener.onEditLineChange();
                }
            }
        }
    }

    public  int getTextHeight() {
        Layout layout = this.getLayout();
        if(layout != null){
            int desired = layout.getLineTop(this.getLineCount());
            int padding = this.getCompoundPaddingTop() + this.getCompoundPaddingBottom();
            return desired + padding;
        }
       return getMeasuredHeight();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mCurrentLineCount = this.getLineCount();
        if (isInputMethodTarget()) {
            int afterInputLength = CommonUtils.getStringLength(s.toString()) + after - count;
            if (afterInputLength > GlobalBubbleManager.BUBBLE_TEXT_MAX) {
                GlobalBubbleManager.getInstance().showTextLimitToast();
                int cursorPosition = getSelectionStart();
                setText(s);
                setSelection(cursorPosition);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        MenuItem item = menu.findItem(android.R.id.shareText);
        if (item != null) {
            item.setEnabled(!TextUtils.isEmpty(getSelectedText(false)));
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        switch (id) {
            case android.R.id.paste:
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = clipboard.getPrimaryClip();
                String content = getShowText() + clip.toString();
                if (content.length() > GlobalBubbleManager.BUBBLE_TEXT_MAX && CommonUtils.getStringLength(content) > GlobalBubbleManager.BUBBLE_TEXT_MAX) {
                    GlobalBubbleManager.getInstance().showTextLimitToast();
                    return true;
                }
                break;
            case android.R.id.shareText:
                shareSelectedText();
                return true;
        }
        return super.onTextContextMenuItem(id);
    }

    public String getShowText() {
        return getText() == null ? "" : getText().toString();
    }

    public interface EditModeChangeListener {
        void onEditText(String txt);
        boolean onFinishEdit();
    }

    public interface OnEditLineChangeListener{
        void onEditLineChange();
    }

    private EditModeChangeListener mEditModeChangeListener = null;
    private OnEditLineChangeListener mOnEditLineChangeListener = null;

    public BubbleEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleEditText(Context context, AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setDrawScrollBarEnable(boolean flag) {
        try {
            Field field = View.class.getDeclaredField("mDrawScrollBar");
            if(field != null) {
                field.setAccessible(true);
                field.set(this, flag);
            }
        } catch (NoSuchFieldException e) {

        } catch (IllegalAccessException e) {

        }
    }

    private void init() {
        setDrawScrollBarEnable(false);
        setBackground(null);
        addTextChangedListener(this);
        mMaxHeight = getResources().getDimensionPixelOffset(R.dimen.bubble_text_max_height);
    }

    public void setNeedMaxHeight(boolean need) {
        if (mNeedMaxHeight != need) {
            if (need) {
                setMaxHeight(mMaxHeight);
            } else {
                setMaxHeight(Integer.MAX_VALUE);
            }
            mNeedMaxHeight = need;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (BubbleController.getInstance().isInputting()) {
            return super.onTouchEvent(event);
        } else {
            return false;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (mEditModeChangeListener != null) {
                    if (mEditModeChangeListener.onFinishEdit()) {
                        return true;
                    }
                }
            }
            break;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                if (mBubbleStateListener != null) {
                    mBubbleStateListener.onDeleteEvent();
                }
                return true;
            default:{
                log.info("receive keycode down:"+keyCode);
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void registerOnEditLineChangeListener(OnEditLineChangeListener listener) {
        this.mOnEditLineChangeListener = listener;
    }

    public void registerEditModeChangeListener(EditModeChangeListener listener) {
        this.mEditModeChangeListener = listener;
    }

    public void callInputMethodDelay(long times) {
        times = times < 0 ? 0 : times;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                requestFocus();
                //in order call method mEditor.makeBlink();
                setEnabled(false);
                setEnabled(true);
                //in order call method mEditor.makeBlink();
                Utils.callInputMethod(BubbleEditText.this);
            }
        }, times);
        String displayText = getShowText();
        if(!TextUtils.isEmpty(displayText)){
            setText(displayText);
            setSelection(displayText.length());
        } else {
            setSelection(0);
        }
    }

    public void updateHintText(BubbleItem item) {
        if (TextUtils.isEmpty(item.getText()) && !item.isEmptyBubble()) {
            setHint(item.getSingleText());
        } else {
            setHint("");
        }
    }

    public void show(BubbleItem item) {
        toDoOver(item.isToDoOver());
        showText(item.getText(), !item.isNeedInput());
        updateHintText(item);
    }

    public void showText(String text, boolean isResetSelection) {
        if (text == null) {
            setText("");
            return;
        }

        // if not changed, not set text again.
        // Because this will change text selection
        if (!text.equals(getText().toString())) {
            setText(text);
        }

        if (isResetSelection) {
            // show current page start position
            setSelection(0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        } catch (Resources.NotFoundException e) {
            //fixme! mCursorDrawableRes may not found, why??
            log.error("not found:" + e.getMessage() + ",last:" + mLastCursorDrawableResId);
            modifyCursorDrawable(0);
            super.onDraw(canvas);
        }
    }

    public void modifyCursorDrawable(int drawableResId) {
        if (mLastCursorDrawableResId != drawableResId) {
            mLastCursorDrawableResId = drawableResId;
            try {
                // Get the drawables
                Drawable drawable0;
                Drawable drawable1;
                if (drawableResId == 0) {
                    drawable0 = null;
                    drawable1 = null;
                } else {
                    drawable0 = getResources().getDrawable(drawableResId);
                    drawable1 = getResources().getDrawable(drawableResId);
                    if (drawable0 == null || drawable1 == null) {
                        return;
                    }
                }
                Drawable[] drawables = {drawable0, drawable1};

                // Get the editor
                Field field = TextView.class.getDeclaredField("mEditor");
                field.setAccessible(true);
                Object editor = field.get(this);

                // Set the drawables
                field = editor.getClass().getDeclaredField("mCursorDrawable");
                field.setAccessible(true);

                // Get the cursor resource id
                Field drawableResField = TextView.class.getDeclaredField("mCursorDrawableRes");
                drawableResField.setAccessible(true);

                field.set(editor, drawables);
                drawableResField.setInt(this, drawableResId);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
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

    public void resetAlpha(boolean over) {
        if (over) {
            setAlpha(0.4f);
        } else {
            setAlpha(1.0f);
        }
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        return false;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (isCursorVisible()) {
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
        }
        return handled;
    }

    private boolean handleShortcutKeyEvent(int keyCode, boolean shift) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_D:
                if (mBubbleStateListener != null) {
                    mBubbleStateListener.toggleCheckState();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_Y:
                if (!shift) {
                    onTextContextMenuItem(android.R.id.redo);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_Z:
                if (shift) {
                    onTextContextMenuItem(android.R.id.redo);
                } else {
                    onTextContextMenuItem(android.R.id.undo);
                }
                return true;
        }
        return false;
    }

    private String getSelectedText(boolean removeSelection) {
        final int selectionStart = getSelectionStart();
        final int selectionEnd = getSelectionEnd();
        if (selectionStart >= 0 && selectionStart != selectionEnd) {
            final Spannable text = getText();
            if (!TextUtils.isEmpty(text)) {
                if (removeSelection) {
                    Selection.removeSelection(text);
                }
                return String.valueOf(selectionStart > selectionEnd
                        ? text.subSequence(selectionEnd, selectionStart)
                        : text.subSequence(selectionStart, selectionEnd));
            }
        }
        return null;
    }

    private void shareSelectedText() {
        String selectedText = getSelectedText(true);
        if (!TextUtils.isEmpty(selectedText)) {
            InputMethodManager imm = InputMethodManager.peekInstance();
            if (imm != null && imm.isActive(this)) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
            GlobalBubbleUtils.shareTextToApps(mContext, selectedText);
        }
    }

    public void setIBubbleStateListener(IBubbleStateListener listener) {
        mBubbleStateListener = listener;
    }

    public interface IBubbleStateListener {
        void onDeleteEvent();

        void toggleCheckState();
    }
}
