
package com.smartisanos.ideapills.common.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

public class OtherIconView extends View {
    private Drawable mBg;
    private TextPaint paint = new TextPaint();
    private String mtext;
    private float mTextWidth;
    private final static int COUNT = 4;
    private final static float TEXT_SIZE = 7;
    public OtherIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OtherIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OtherIconView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setBg(getResources().getDrawable(R.drawable.document_default));
        paint.setTextSize(AttachmentUtil.sp2px(getContext(), TEXT_SIZE));
        paint.setColor(getResources().getColor(R.color.other_color));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setAntiAlias(true);
    }

    public void setBg(Drawable drawable) {
        mBg = drawable;
        mBg.setBounds(0, 0, mBg.getIntrinsicWidth(), mBg.getIntrinsicHeight());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        if (mBg != null) {
            mBg.draw(canvas);
        }
        if (!TextUtils.isEmpty(mtext)) {
            float half = (getWidth() - mTextWidth) / 2;
            int y = getHeight() - AttachmentUtil.dip2px(getContext(), 7.5);
            canvas.drawText(mtext, half, y, paint);
        }
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(mBg != null){
            setMeasuredDimension(mBg.getIntrinsicWidth(), mBg.getIntrinsicHeight());
        }
    }
    public void setText(String extension){
        if(extension == null){
            return;
        }
        extension = extension.toUpperCase();
        int i = paint.breakText(extension, true, mBg.getIntrinsicWidth() / 2, null);
        extension = extension.substring(0, i);
        mtext = TextUtils.ellipsize(extension, paint, mBg.getIntrinsicWidth() / 2, TextUtils.TruncateAt.END).toString();
        mTextWidth = paint.measureText(mtext);
        invalidate();
    }
}
