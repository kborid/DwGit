package com.smartisanos.sara.bubble.revone.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smartisanos.ideapills.common.util.BitmapUtils;
import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.entity.DragFileInfo;

import java.util.ArrayList;

import smartisanos.util.MimeUtils;

public class DragViewManager {
    private FrameLayout mContainer;
    private ClipData mClipData;
    private View mDragFileView;
    private Point mLocationPoint = new Point();
    private int mIconWidth;
    private int mIconHeight;
    private AnimatorSet mFlyInAnimator;
    private IDropStateListener mDropStateListener;
    private boolean mIsAnimCancel;

    public DragViewManager(ClipData clipData, FrameLayout view, IDropStateListener listener) {
        mClipData = clipData;
        mContainer = view;
        mDropStateListener = listener;
        mIconWidth = mIconHeight = mContainer.getContext().getResources().getDimensionPixelSize(R.dimen.flash_im_drag_content_icon_size);
    }

    public void setLocation(int x, int y) {
        mLocationPoint.set(x, y);
    }

    public void updateDragContent(Point location) {
        mLocationPoint.set(location.x, location.y);
        updateDragFileContent(null);
    }

    private void updateDragFileContent(View targetView) {
        if (mClipData == null || mContainer == null) {
            return;
        }
        getDragFileInfo(targetView);
    }

    public void doFlyToAnimation(View targetView) {
        if (mClipData == null || targetView == null) {
            return;
        }
        if (mDragFileView == null) {
            updateDragFileContent(targetView);
        } else {
            flyToTargetPosition(targetView);
        }
    }

    private void flyToTargetPosition(final View targetView) {
        if (mDragFileView == null) {
            return;
        }
        if (mFlyInAnimator != null && mFlyInAnimator.isRunning()) {
            mFlyInAnimator.cancel();
        }
        mFlyInAnimator = new AnimatorSet();
        ArrayList<Animator> animatorList = new ArrayList<Animator>();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDragFileView.getLayoutParams();
        Point startPoint = new Point(lp.leftMargin, lp.topMargin);
        Point endPoint = null;
        if (targetView == null) {
            int x = 0;
            if (mDragFileView instanceof LinearLayout) {
                x = mDragFileView.getContext().getResources().getDimensionPixelSize(R.dimen.nomal_bubble_left_margin);
            }
            endPoint = new Point(x, ExtScreenConstant.STATUS_BAR_HEIGHT);
        } else {
            int loc[] = new int[2];
            loc = targetView.getLocationOnScreen();
            endPoint = new Point(loc[0] + targetView.getWidth() / 2 - mDragFileView.getWidth() / 2,
                    loc[1] + targetView.getHeight() / 2 - mDragFileView.getHeight() / 2);
        }
        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
        ValueAnimator positionAnim = ValueAnimator.ofObject(mTypeEvaluator, startPoint, endPoint);
        positionAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Point point = (Point) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mDragFileView.getLayoutParams();
                params.leftMargin = (int) point.x;
                params.topMargin = (int) point.y;
                mDragFileView.setLayoutParams(params);
            }
        });
        positionAnim.setInterpolator(decelerateInterpolator);
        positionAnim.setDuration(ExtScreenConstant.GENERAL_ANIM_TIME);
        animatorList.add(positionAnim);
        if (targetView != null) {
            PropertyValuesHolder scaleOutX = PropertyValuesHolder.ofFloat("scaleX", 1f, ExtScreenConstant.SCALE_OUT_VALUE);
            PropertyValuesHolder scaleOutY = PropertyValuesHolder.ofFloat("scaleY", 1f, ExtScreenConstant.SCALE_OUT_VALUE);
            Animator scaleOut = ObjectAnimator.ofPropertyValuesHolder(targetView, scaleOutX, scaleOutY);
            scaleOut.setInterpolator(decelerateInterpolator);
            scaleOut.setDuration(ExtScreenConstant.GENERAL_ANIM_TIME);
            animatorList.add(scaleOut);

            PropertyValuesHolder dropX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f);
            PropertyValuesHolder dropY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f);
            Animator dropAnim = ObjectAnimator.ofPropertyValuesHolder(mDragFileView, dropX, dropY);
            dropAnim.setInterpolator(new AccelerateInterpolator(1.5f));
            dropAnim.setDuration(ExtScreenConstant.DROP_ANIM_TIME);
            dropAnim.setStartDelay(ExtScreenConstant.GENERAL_ANIM_TIME);
            animatorList.add(dropAnim);

            PropertyValuesHolder scaleInX = PropertyValuesHolder.ofFloat("scaleX", ExtScreenConstant.SCALE_OUT_VALUE, 1f);
            PropertyValuesHolder scaleInY = PropertyValuesHolder.ofFloat("scaleY", ExtScreenConstant.SCALE_OUT_VALUE, 1f);
            Animator scaleIn = ObjectAnimator.ofPropertyValuesHolder(targetView, scaleInX, scaleInY);
            scaleIn.setInterpolator(new AccelerateInterpolator(1.5f));
            scaleIn.setDuration(ExtScreenConstant.DROP_ANIM_TIME);
            scaleIn.setStartDelay(ExtScreenConstant.GENERAL_ANIM_TIME + ExtScreenConstant.DROP_ANIM_TIME);
            animatorList.add(scaleIn);
        } else if (mDragFileView instanceof FrameLayout) {
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
            View backgroud = mDragFileView.findViewById(R.id.background);
            Animator backgroundAnim = ObjectAnimator.ofPropertyValuesHolder(backgroud, alpha);
            animatorList.add(backgroundAnim);
            View descriptionView = mDragFileView.findViewById(R.id.description_content);
            Animator descriptionAnim = ObjectAnimator.ofPropertyValuesHolder(descriptionView, alpha);
            animatorList.add(descriptionAnim);
        }
        mFlyInAnimator.playTogether(animatorList);
        mFlyInAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!mIsAnimCancel) {
                    mIsAnimCancel = false;
                    if (targetView != null && mDropStateListener != null) {
                        mDropStateListener.onDropFinish();
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mIsAnimCancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        mFlyInAnimator.start();
    }

    public void addDragContentView(final DragFileInfo info, final View targetView) {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                Context context = mContainer.getContext();
                int width = 0;
                int height = 0;
                int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                if (info.getMimeType().equals("text/plain")) {
                    mDragFileView = LayoutInflater.from(context).inflate(R.layout.revone_drag_text_content, null, false);
                    TextView textView = (TextView) mDragFileView.findViewById(R.id.text);
                    textView.setText(info.getText());
                    mDragFileView.setBackgroundResource(info.getFirstFileIconRes());
                    mDragFileView.measure(spec, spec);
                    width = mDragFileView.getMeasuredWidth();
                    height = mDragFileView.getMeasuredHeight();
                } else {
                    mDragFileView = LayoutInflater.from(context).inflate(R.layout.revone_drag_file_content, null, false);
                    View background = mDragFileView.findViewById(R.id.background);
                    View content = mDragFileView.findViewById(R.id.container);
                    ImageView ivIcon = (ImageView) mDragFileView.findViewById(R.id.icon);
                    TextView tvName = (TextView) mDragFileView.findViewById(R.id.name);
                    TextView tvSize = (TextView) mDragFileView.findViewById(R.id.size);
                    Bitmap icon = info.getFirstFileIcon();
                    if (icon != null) {
                        ivIcon.setImageBitmap(icon);
                    } else {
                        ivIcon.setImageResource(info.getFirstFileIconRes());
                    }
                    tvName.setText(info.getFirstFileName());
                    tvSize.setText(Formatter.formatFileSize(context, info.getSize()));
                    content.measure(spec, spec);
                    width = content.getMeasuredWidth();
                    height = content.getMeasuredHeight();
                    int backgroundRes = R.drawable.revone_single_drag_file_bg;
                    if (info.getFileCount() > 1) {
                        backgroundRes = R.drawable.revone_drag_multi_file_bg;
                    }
                    Drawable drawable = context.getDrawable(backgroundRes);
                    Rect rect = new Rect();
                    drawable.getPadding(rect);
                    ViewGroup.LayoutParams lp = background.getLayoutParams();
                    lp.height = height + rect.top + rect.bottom;
                    lp.width = width + rect.left + rect.right;
                    background.setBackground(drawable);
                    background.setLayoutParams(lp);
                }
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = mLocationPoint.y - height / 2;
                lp.leftMargin = mLocationPoint.x - width / 2;
                mContainer.addView(mDragFileView, lp);
                mDragFileView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        mDragFileView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        flyToTargetPosition(targetView);
                    }
                });
            }
        });
    }

    private void getDragFileInfo(final View targetView) {
        final int count = mClipData.getItemCount();
        if (count <= 0) {
            return;
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Context context = mContainer.getContext();
                DragFileInfo fileInfo = new DragFileInfo();
                fileInfo.setFileCount(count);
                for (int i = 0; i < count; i++) {
                    Uri uri = mClipData.getItemAt(i).getUri();
                    String mimeType = mClipData.getDescription().getMimeType(0);
                    if (i == 0) {
                        Bitmap icon = null;
                        int resId = 0;
                        fileInfo.setMimeType(mimeType);
                        if (mimeType.equals("text/plain")) {
                            resId = R.drawable.text_popup;
                            fileInfo.setFirstFileIconRes(resId);
                            CharSequence text = mClipData.getItemAt(i).getText();
                            if (text != null) {
                                fileInfo.setText(text.toString());
                            }
                            break;
                        } else if (mimeType.startsWith("image")) {
                            icon = BitmapUtils.getBitmapAtRightSize(context, mIconWidth, mIconHeight, uri);
                        }
                        if (icon == null) {
                            resId = MimeUtils.getFileIconResId(mimeType, "");
                            fileInfo.setFirstFileIconRes(resId);
                        } else {
                            fileInfo.setFirstFileIcon(icon);
                        }
                        String fileName = FileUtils.getFileNameByUri(context, uri);
                        fileInfo.setFirstFileName(fileName);
                    }
                    long fileSize = FileUtils.getFileSizeByUri(context, uri);
                    fileInfo.setSize(fileInfo.getSize() + fileSize);
                }
                addDragContentView(fileInfo, targetView);
            }
        });
    }

    private TypeEvaluator<Point> mTypeEvaluator = new TypeEvaluator<Point>() {
        private Point mPoint = new Point();

        public Point evaluate(float fraction, Point startValue, Point endValue) {
            int cx = (int) ((endValue.x - startValue.x) * fraction) + startValue.x;
            int cy = (int) ((endValue.y - startValue.y) * fraction) + startValue.y;
            mPoint.set(cx, cy);
            return mPoint;
        }
    };

    public interface IDropStateListener {
        void onDropFinish();
    }
}
