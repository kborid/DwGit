package com.smartisanos.ideapills.view;

import android.content.ClipDescription;
import android.content.Context;
import android.service.onestep.GlobalBubble;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.common.util.BubbleMimeUtils;
import com.smartisanos.ideapills.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class BubbleHeadOptView extends LinearLayout {
    private static final LOG log = LOG.getInstance(BubbleHeadOptView.class);

    private static final int STATE_NONE = 0;
    private static final int STATE_MOVE = 1;

    private int mCurState;
    private float mDownRawX;
    private float mDownY;
    private GlobalBubbleManager mManager;
    private boolean mAcceptDragEvent = false;
    private BubbleListView mBubbleListView = null;
    private View mfl_head;

    public BubbleHeadOptView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleHeadOptView(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mManager =  GlobalBubbleManager.getInstance();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (STATE_MOVE == mCurState) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mfl_head = findViewById(R.id.fl_head_normal);
        mfl_head.setOnTouchListener(new OnTouchListener() { //head always consume the touch event.
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        LinearLayout.LayoutParams headParam = (LinearLayout.LayoutParams) mfl_head.getLayoutParams();
        headParam.topMargin = Utils.getHeadMarginTop(mContext);
        mfl_head.setLayoutParams(headParam);
    }

    public int getTranslateXMax() {
        return mfl_head.getWidth();
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (BubbleController.getInstance().isInPptContext(getContext())) {
            return super.dispatchTouchEvent(event);
        }
        int action = event.getAction();
        if ((action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN)
                && (BubbleController.getInstance().isInputting() || StatusManager.getStatus(StatusManager.BUBBLE_DRAGGING))) {
            return super.dispatchTouchEvent(event);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurState = STATE_NONE;
                mDownRawX = event.getRawX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float curX = event.getRawX();
                float curY = event.getY();
                if (Math.abs(mDownRawX - curX) > 10 || Math.abs(mDownY - curY) > 10) {
                    mCurState = STATE_MOVE;
                }
                if (STATE_MOVE == mCurState) {
                    float moveX = curX - mDownRawX;
                    if (moveX > 0) {
                        float move = moveX / BubbleAdapter.DAMPING;
                        setTranslationX(move);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (STATE_MOVE == mCurState) {
                    if (getTranslationX() >= BubbleAdapter.MOVE_THRESHOLD + 20) {
                        BubbleController.getInstance().playHideAnimation(false);
                    } else {
                        if (getTranslationX() != 0) {
                            Vector3f from = new Vector3f(getTranslationX(), 0);
                            Anim anim = new Anim(this, Anim.TRANSLATE, 200, Anim.CUBIC_OUT, from, Anim.ZERO);
                            anim.start();
                        }
                    }
                }
                mCurState = STATE_NONE;
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    public boolean isMovingHorizontal() {
        return mCurState == STATE_MOVE;
    }

    public void clearMovingState() {
        mCurState = STATE_NONE;
    }

    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
        int translateMax = getTranslateXMax();
        if (translateMax > 0) {
            BubbleController.getInstance().dimBackgroundByMove((translateMax - translationX) / translateMax);
        }
    }

    @Override
    public boolean onDragEvent(final DragEvent event) {
        if (BubbleController.getInstance().isInputting()) {
            if (LOG.DBG) {
                log.info("stop handle dragevent while inputting");
            }
            return false;
        }
        switch (event.getAction()) {
        case DragEvent.ACTION_DRAG_STARTED: {
            if (mBubbleListView == null || !mBubbleListView.isEmpty() || Utils.getDragGlobalBubbleNumber(event) > 0) {
                mAcceptDragEvent = false;
                return false;
            }
            String mimeType = BubbleMimeUtils.getCommonMimeType(event);
            if (!ClipDescription.MIMETYPE_TEXT_PLAIN.equals(mimeType)) {
                if (LOG.DBG) {
                    log.info("stop handle dragevent while have the wrong mimetype");
                }
                mAcceptDragEvent = false;
            } else {
                mAcceptDragEvent = true;
            }
        }
        }
        if (mAcceptDragEvent) {
            switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: {
                log.info("ACTION_DRAG_STARTED");
                break;
            }
            case DragEvent.ACTION_DRAG_ENDED: {
                log.info("ACTION_DRAG_END");
                break;
            }
            case DragEvent.ACTION_DROP: {
                boolean handle = handleHeaderViewDragEventDrop(event);
                log.info("ACTION_DRAG");
                return handle;
            }
            default:
                return false;
            }
            return true;
        } else {
            return false;
        }
    }


    private boolean handleHeaderViewDragEventDrop(DragEvent event) {
        if (LOG.DBG) {
            log.info("handleHeaderViewDragEventDrop --> " + event);
        }
        if(event == null){
            return false;
        }
        List<GlobalBubble> items = Utils.convertToBubbleItems(event
                .getClipData());
        if (items != null) {
            List<BubbleItem> bubbleItems = mManager.getBubbleItemsFrom(items);
            List<BubbleItem> toAdd = new ArrayList<BubbleItem>();
            for (BubbleItem item : bubbleItems) {
                if (!GlobalBubbleManager.getInstance()
                        .checkIsTextLengthInLimit(item)) {
                    log.error("the bubble contains too much words");
                    continue;
                }
                if (item.getTimeStamp() == 0) {
                    item.setTimeStamp(System.currentTimeMillis());
                }
                toAdd.add(item);
            }
            if (GlobalBubble.COLOR_SHARE == Constants.getDefaultBubbleColor()) {
                GlobalBubbleManager.getInstance().handleShareItems(bubbleItems);
            }
            if (toAdd.size() > 0) {
                mManager.addBubbleItems(toAdd);
                if (LOG.DBG) log.info("handleHeaderViewDragEventDrop onAddBubble from headerView--> " + toAdd);
            }
            return true;
        }
        return false;
    }

    public void setBubbleListView(BubbleListView listView) {
        mBubbleListView = listView;
    }
}
