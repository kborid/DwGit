
package com.smartisanos.sara.lock.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;

public class MyEditText extends EditText {

    public MyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        if (getParent() == null) {
            return false;
        }

        View child = this;

        ViewParent parent = getParent();
        boolean scrolled = false;
        while (parent != null) {
            scrolled |= parent.requestChildRectangleOnScreen(child,
                    null, immediate);
            if (!(parent instanceof View)) {
                break;
            }
            View parentView = (View) parent;
            child = parentView;
            parent = child.getParent();
        }

        return scrolled;
    }
}
