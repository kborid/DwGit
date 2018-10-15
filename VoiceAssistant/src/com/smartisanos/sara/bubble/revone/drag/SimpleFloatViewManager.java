
package com.smartisanos.sara.bubble.revone.drag;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import com.smartisanos.sara.R;

/**
 * Simple implementation of the FloatViewManager class. Uses list items as they
 * appear in the ListView to create the floating View.
 */
public class SimpleFloatViewManager implements DragSortListView.FloatViewManager {

    private final static int SHADOW_PADDING = 10;

    private Bitmap mFloatBitmap;

    private ImageView mImageView;

    private int mFloatBGColor = Color.WHITE;

    private ListView mListView;

    private int paddingTop = 0;
    private int paddingBottom = 0;

    public SimpleFloatViewManager(ListView lv) {
        mListView = lv;
    }

    public void setBackgroundColor(int color) {
        mFloatBGColor = color;
    }

    /**
     * This simple implementation creates a Bitmap copy of the list item
     * currently shown at ListView <code>position</code>.
     */
    @Override
    public View onCreateFloatView(int position) {
        // Guaranteed that this will not be null? I think so. Nope, got
        // a NullPointerException once...
        View v = mListView.getChildAt(position + mListView.getHeaderViewsCount()
                - mListView.getFirstVisiblePosition());

        if (v == null) {
            return null;
        }

        paddingTop = paddingBottom = SHADOW_PADDING;
        v.setPressed(false);

        // Create a copy of the drawing cache so that it does not get
        // recycled by the framework when the list tries to clean up memory
        // v.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        v.setDrawingCacheEnabled(true);

        Bitmap body = v.getDrawingCache();

        int width = body.getWidth();
        int height = paddingTop + paddingBottom + body.getHeight();

        mFloatBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        mFloatBitmap.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(mFloatBitmap);

        canvas.drawBitmap(body, new Rect(0, 0, body.getWidth(), body.getHeight()), new Rect(0,
                paddingTop,
                body.getWidth(), body.getHeight() + paddingTop), null);

        v.setDrawingCacheEnabled(false);

        if (mImageView == null) {
            mImageView = new ImageView(mListView.getContext());
        }
        mImageView.setPadding(0, 0, 0, 0);
        mImageView.setImageBitmap(mFloatBitmap);
        mImageView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        ViewGroup parent = (ViewGroup) mImageView.getParent();
        if (parent != null) {
            parent.removeView(mImageView);
        }
        mImageView.setBackgroundColor(Color.TRANSPARENT);

        v.setDrawingCacheEnabled(true);

        FrameLayout frameLayout = new FrameLayout(mListView.getContext());
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        frameLayout.addView(mImageView);
        frameLayout.setBackgroundResource(R.drawable.dslv_item_shadow);
        return frameLayout;
    }

    /**
     * This does nothing
     */
    @Override
    public void onDragFloatView(View floatView, Point position, Point touch) {
        // do nothing
    }

    /**
     * Removes the Bitmap from the ImageView created in onCreateFloatView() and
     * tells the system to recycle it.
     */
    @Override
    public void onDestroyFloatView(View floatView) {
        if (mImageView != null) {
            mImageView.setImageDrawable(null);
        }

        mFloatBitmap.recycle();
        mFloatBitmap = null;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

}
