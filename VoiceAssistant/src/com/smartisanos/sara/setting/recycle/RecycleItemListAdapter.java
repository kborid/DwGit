package com.smartisanos.sara.setting.recycle;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.net.Uri;
import android.service.onestep.GlobalBubble;
import com.smartisanos.sara.R;
import com.smartisanos.sara.setting.RecycleBinActivity;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.pinnedHeadList.HeadersAdapter;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.BubbleItemView.BubbleSate;
import com.smartisanos.sara.widget.HorizontalScrollListView;
import com.smartisanos.sara.util.BubbleSpeechPlayer;

import java.util.List;

import static com.smartisanos.sara.setting.RecycleBinActivity.BUBBLE_RIRECTION_USED;
import static com.smartisanos.sara.setting.RecycleBinActivity.sBubbleDirection;

public class RecycleItemListAdapter extends BaseAdapter implements HeadersAdapter,
        HorizontalScrollListView.ScrollStateListener {

    protected List<RecycleItem> mRecycleItems;
    protected Context mContext;
    protected Activity mActivity;
    private boolean mIsEdit = false;
    private Dialog mDialog;
    private int mContentTranslateX;
    public RecycleItemListAdapter(Activity activity, List<RecycleItem> bubbles) {
        mRecycleItems = bubbles;
        mActivity = activity;
        mContext = activity.getBaseContext();
        Resources res = mActivity.getResources();
        mContentTranslateX = res.getDimensionPixelSize(R.dimen.recycle_list_margin_left_icon) - res.getDimensionPixelSize(R.dimen.recycle_list_margin_left);
    }

    @Override
    public int getCount() {
        if (mRecycleItems == null) {
            return 0;
        } else {
            return mRecycleItems.size();
        }
    }

    @Override
    public RecycleItem getItem(int arg0) {
        if (mRecycleItems == null || mRecycleItems.size() == 0 || arg0 >= mRecycleItems.size()) {
            return null;
        } else {
            return mRecycleItems.get(arg0);
        }
    }

    @Override
    public long getItemId(int arg0) {
        return mRecycleItems == null ? -1 : mRecycleItems.get(arg0).bubbleId;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.header, null);
        }
        RecycleItem recycleItem = mRecycleItems.get(position);
        if(sBubbleDirection == RecycleBinActivity.BUBBLE_RIRECTION_RECYCLE) {
            ((TextView) (convertView.findViewById(R.id.text))).setText(recycleItem
                    .getSectionHeader(mContext));
        }else{
            ((TextView) (convertView.findViewById(R.id.text))).setText(recycleItem
                    .getTodoOverSectionHeader(mContext));
        }
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        if (mRecycleItems == null) {
            return -1;
        }
        if (position < 0 || position > mRecycleItems.size()) {
            return -1;
        }
        return mRecycleItems.get(position).section;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup arg2) {
        ViewHolder view;
        if (convertView == null || convertView.getTag() == null) {
            view = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bubble_recycle_list_item,
                    null);
            view.checkBoxFrame = (RelativeLayout) convertView.findViewById(R.id.checkBoxFrame);
            view.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
            view.item = (RelativeLayout) convertView.findViewById(R.id.item);
            view.text = (TextView) convertView.findViewById(R.id.bubble_text);
            view.recycleDate = (TextView) convertView.findViewById(R.id.recycle_date);
            SaraUtils.setMaxTextSize(view.text, ViewUtils.dp2px(mContext, 18 * 1.2));
            view.color = convertView.findViewById(R.id.bubble_color);
            view.todo = convertView.findViewById(R.id.bubble_todo);
            view.voice = convertView.findViewById(R.id.bubble_voice);
            view.attach = convertView.findViewById(R.id.bubble_attach);
            view.reminder = convertView.findViewById(R.id.bubble_reminder);
            view.bubbleContent = (LinearLayout) convertView.findViewById(R.id.bubble_content);
            convertView.setTag(view);
        } else {
            view = (ViewHolder) convertView.getTag();
        }

        final RecycleItem recycleItem = mRecycleItems.get(position);
        if (TextUtils.isEmpty(recycleItem.bubbleText)) {
            view.text.setText(SaraUtils.getHintText(mContext, recycleItem.getBubbleAttaches()));
        } else {
            view.text.setText(recycleItem.bubbleText);
        }
        if (((RecycleBinActivity)mActivity).getBubbleDirection() == RecycleBinActivity.BUBBLE_RIRECTION_RECYCLE){
            view.recycleDate.setText(mContext.getResources().getString(R.string.delete_time) +
                    " " + recycleItem.recycleFormattedDate);
        } else if (((RecycleBinActivity)mActivity).getBubbleDirection() == RecycleBinActivity.BUBBLE_RIRECTION_USED) {
            view.recycleDate.setText(mContext.getResources().getString(R.string.handled_time) +
                    " " + recycleItem.recycleFormattedDate);
        } else {
            view.recycleDate.setText(mContext.getResources().getString(R.string.handled_ok_time) +
                    " " + recycleItem.recycleFormattedDate);
        }
        if (recycleItem.bubbleTodo == GlobalBubble.TODO_OVER) {
            view.text.setTextColor(mContext.getResources().getColorStateList(R.color.recycle_strike_text_color));
            view.text.setPaintFlags(view.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            view.text.setTextColor(mContext.getResources().getColorStateList(R.drawable.recycle_option_text_color));
            view.text.setPaintFlags(view.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        view.recycleItem = recycleItem;
        view.color.setBackgroundResource(getBackgroudRes(recycleItem.bubbleColor));
        view.todo.setVisibility((recycleItem.bubbleTodo == GlobalBubble.TODO_OVER) ? View.VISIBLE
                : View.GONE);
        List<GlobalBubbleAttach> attaches = recycleItem.getBubbleAttaches();
        final int attachCount = attaches == null ? 0 : attaches.size();
        boolean remind = recycleItem.remindTime > 0 || recycleItem.dueDate > 0;
        view.attach.setVisibility(attachCount > 0 ? View.VISIBLE : View.GONE);
        view.reminder.setVisibility((recycleItem.remindTime > 0 || recycleItem.dueDate > 0) ? View.VISIBLE : View.GONE);
        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) view.bubbleContent
                .getLayoutParams();
        if (recycleItem.bubbleType ==GlobalBubble.TYPE_TEXT){
            view.voice.setVisibility(View.GONE);
        } else {
            view.voice.setVisibility(View.VISIBLE);
        }
        if (mIsEdit) {
            view.checkBoxFrame.setVisibility(View.VISIBLE);
            view.checkBox.setChecked(isSelected(position));
            view.checkBoxFrame.setAlpha(1);
            view.checkBoxFrame.setScaleX(1);
            view.checkBoxFrame.setScaleY(1);
            view.bubbleContent.setTranslationX(mContentTranslateX);
        } else {
            view.checkBoxFrame.setVisibility(View.GONE);
            view.bubbleContent.setTranslationX(0);
        }
        return convertView;
    }

    @Override
    public void onScrolling(int position, View view) {

    }

    @Override
    public void onScrollCompleted(View v) {

    }

    @Override
    public void onScrollRestored(View v) {

    }

    @Override
    public void onScrollStateChanged(View view, int beforeState, int afterState) {
        if (beforeState == HorizontalScrollListView.IDLE) {
            ViewGroup vg = (ViewGroup) view;
            View v;
            for (int i = 0, n = vg.getChildCount(); i < n; i++) {
                v = vg.getChildAt(i);
                Object tag = v.getTag();
                if (tag != null && tag instanceof ViewHolder) {
                    beforeHorizontalScrollOpen((ViewHolder) tag);
                    break;
                }
            }
        }
    }

    private void beforeHorizontalScrollOpen(ViewHolder viewHolder) {
        View root = (View) viewHolder.item.getParent().getParent();
        bindBackgroundItemButtons(root, viewHolder.recycleItem);
    }

    private View bindBackgroundItemButtons(final View root, final RecycleItem item) {
        View bg = root.findViewById(R.id.item_background);
        if (bg == null) {
            ViewStub bgStub = (ViewStub) root.findViewById(R.id.item_background_stub);
            bg = bgStub.inflate();
        }

        bg.findViewById(R.id.umb_restore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.umbButtonClicked(root, item, R.id.umb_restore);
            }
        });

        bg.findViewById(R.id.umb_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.umbButtonClicked(root, item, R.id.umb_clear);
            }
        });

        return bg;
    }

    public class ViewHolder {
        public TextView text;
        public TextView recycleDate;
        public View color;
        public View todo;
        public View voice;
        public View reminder;
        public View attach;
        public CheckBox checkBox;
        public RelativeLayout checkBoxFrame;
        private RelativeLayout item;
        public RecycleItem recycleItem;
        private LinearLayout bubbleContent;
    }

    public void setList(List<RecycleItem> bubbles) {
        mRecycleItems = bubbles;
        notifyDataSetChanged();
    }

    public List<RecycleItem> getList() {
        return this.mRecycleItems;
    }

    private RecycleItemListener mListener;

    public void setEdit(boolean isEdit) {
        mIsEdit = isEdit;
    }

    public boolean isEditMode() {
        return mIsEdit;
    }

    public void setRecycleItemListener(RecycleItemListener listener) {
        mListener = listener;
    }

    protected boolean isSelected(int position) {
        return mListener.isSelected(getItem(position));
    }

    private int getBackgroudRes(int color) {
        switch (color) {
            case GlobalBubble.COLOR_RED: {
                return R.drawable.bin_sign_red;
            }
            case GlobalBubble.COLOR_ORANGE: {
                return R.drawable.bin_sign_orange;
            }
            case GlobalBubble.COLOR_GREEN: {
                return R.drawable.bin_sign_green;
            }
            case GlobalBubble.COLOR_PURPLE: {
                return R.drawable.bin_sign_purple;
            }
            case GlobalBubble.COLOR_NAVY_BLUE: {
                return R.drawable.ppt_bin_sign;
            }
            case GlobalBubble.COLOR_SHARE: {
                return R.drawable.bin_sign_share;
            }
            default: {
                return R.drawable.bin_sign_blue;
            }
        }
    }

    public void initDialog(final RecycleItem item) {
        if (mDialog == null) {
            mDialog = new Dialog(mActivity, R.style.CustomDialog);
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            BubbleItemView view = (BubbleItemView) inflater.inflate(R.layout.bubble_item, null);
            view.setVisibility(View.VISIBLE);
            view.setAttachmentList(item.getBubbleAttaches());
            GlobalBubble bubble= null;
            if (item.bubbleType != GlobalBubble.TYPE_TEXT){
                bubble = SaraUtils.toGlobalBubble(mActivity, item.bubbleText, item.bubbleType,
                    Uri.parse(SaraUtils.formatFilePath2Content(mActivity, item.bubblePath)),
                    item.bubbleColor, item.remindTime, item.dueDate);
            } else {
                bubble = SaraUtils.toGlobalBubbleText(mActivity,item.bubbleColor);
                bubble.setRemindTime(item.remindTime);
                bubble.setDueDate(item.dueDate);
                bubble.setText(item.bubbleText);
                bubble.setRemindTime(item.remindTime);
                bubble.setDueDate(item.dueDate);
            }
            view.findViewById(R.id.tv_title_small).setEnabled(false);
            view.setGlobalBubble(bubble, item.bubbleType == GlobalBubble.TYPE_VOICE_OFFLINE);
            view.setDialogListener(new SaraUtils.DialogListener() {
                @Override
                public void onBubbleRestore() {
                    if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT) {
                        int insertNum = BubbleDataRepository.getCanInsertGlobalBubbleNum(mActivity, 1);
                        if (insertNum > 0) {
                            mDialog.dismiss();
                            mListener.onBubbleRestore(item);
                        } else {
                            ToastUtil.showToast(R.string.max_global_bubbles_tips);
                        }
                    } else {
                        mDialog.dismiss();
                        mListener.onBubbleRestore(item);
                    }
                }

                @Override
                public void onBubbleDelete() {
                    mDialog.dismiss();
                    mListener.onBubbleDelete(item);
                }
            });
            view.setBubbleState(BubbleSate.RECYCLE, false, false, false);
            view.toDoOver(item.bubbleTodo == GlobalBubble.TODO_OVER);
            if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                TextView restoreview = (TextView) view.findViewById(R.id.iv_bubble_restore);
                if (restoreview != null) {
                    restoreview.setText(R.string.btn_cancel_complete);
                }
            }
            mDialog.addContentView(view, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            mDialog.setCanceledOnTouchOutside(true);
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    BubbleSpeechPlayer.getInstance(mActivity).stop();
                    mDialog = null;
                }
            });
            mDialog.show();
        }
    }
}
