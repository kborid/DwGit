package com.smartisanos.sara.util;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

public class SoftKeyboardUtil {

    public static void observeSoftKeyboard(final View decorView,
            final OnSoftKeyboardChangeListener listener) {
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    int previousKeyboardHeight = -1;
                    @Override
                    public void onGlobalLayout() {
                        Rect rect = new Rect();
                        decorView.getWindowVisibleDisplayFrame(rect);
                        int displayHeight = rect.bottom - rect.top;
                        int height = decorView.getHeight();
                        int keyboardHeight = height - displayHeight;
                        if (previousKeyboardHeight != keyboardHeight) {
                            boolean hide = (double) displayHeight / height > 0.8;
                            listener.onSoftKeyBoardChange(keyboardHeight, !hide);
                        }
                        previousKeyboardHeight = keyboardHeight;
                    }
                });
    }

    public interface OnSoftKeyboardChangeListener {
        void onSoftKeyBoardChange(int softKeybardHeight, boolean visible);
    }

    public static void hideInputMethod(View view) {
        final InputMethodManager imm = InputMethodManager.peekInstance();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
