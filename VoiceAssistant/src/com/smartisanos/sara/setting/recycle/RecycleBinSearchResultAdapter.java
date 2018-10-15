package com.smartisanos.sara.setting.recycle;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.service.onestep.GlobalBubbleAttach;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.net.Uri;
import android.service.onestep.GlobalBubble;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.smartisanos.sara.setting.RecycleBinActivity;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.pinnedHeadList.WrapperView;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.HorizontalScrollListView;
import com.smartisanos.sara.widget.BubbleItemView.BubbleSate;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.BubbleSpeechPlayer;

public class RecycleBinSearchResultAdapter extends BaseAdapter implements
        HorizontalScrollListView.ScrollStateListener {

    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<RecycleItem> mBubbles = null;
    private Activity mActivity;
    private String mSearchKey;
    private Dialog mDialog;

    public RecycleBinSearchResultAdapter(Activity activity, ArrayList<RecycleItem> RecycleItems,
            String searchKey) {
        mContext = activity;
        mActivity = activity;
        mInflater = LayoutInflater.from(mContext);
        mBubbles = RecycleItems;
        mSearchKey = searchKey;
    }

    public class ViewHolder {
        public TextView text;

        public TextView recycleDate;
        public View color;
        public View todo;
        public View voice;
        public View reminder;
        public View attach;
        public LinearLayout item;
        public RecycleItem recycleItem;

    }

    @Override
    public void onScrolling(int position, View v) {

    }

    @Override
    public void onScrollCompleted(View v) {

    }

    @Override
    public void onScrollRestored(View v) {

    }

    @Override
    public void onScrollStateChanged(View v, int beforeState, int afterState) {
        if (beforeState == HorizontalScrollListView.IDLE) {
            if (v instanceof WrapperView) {
                v = ((ViewGroup) v).getChildAt(0);
            }
            Object obj = v.getTag();
            if (obj instanceof ViewHolder) {
                beforeHorizontalScrollOpen((ViewHolder) obj);
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

    private RecycleItemListener mListener;

    public void setRecycleItemListener(RecycleItemListener listener) {
        mListener = listener;
    }

    @Override
    public int getCount() {
        if (mBubbles == null) {
            return 0;
        } else {
            return mBubbles.size();
        }
    }

    @Override
    public RecycleItem getItem(int position) {
        if (position < 0 || position > getCount() - 1) {
            return null;
        } else {
            return mBubbles.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        RecycleItem bubble = getItem(position);
        if (bubble == null) {
            return -1;
        } else {
            return bubble.bubbleId;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater
                    .inflate(R.layout.bubble_recycle_list_search_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.item = (LinearLayout) convertView.findViewById(R.id.item);
            viewHolder.text = (TextView) convertView.findViewById(R.id.bubble_text);
            SaraUtils.setMaxTextSize(viewHolder.text, ViewUtils.dp2px(mContext, 18 * 1.2));
            viewHolder.recycleDate = (TextView) convertView.findViewById(R.id.recycle_date);
            viewHolder.color = convertView.findViewById(R.id.bubble_color);
            viewHolder.todo = convertView.findViewById(R.id.bubble_todo);
            viewHolder.voice = convertView.findViewById(R.id.bubble_voice);
            viewHolder.attach = convertView.findViewById(R.id.bubble_attach);
            viewHolder.reminder = convertView.findViewById(R.id.bubble_reminder);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        RecycleItem recycleItem = mBubbles.get(position);
        if (TextUtils.isEmpty(recycleItem.bubbleText)) {
            viewHolder.text.setText(mContext.getResources().getString(R.string.missing_name));
        } else {
            SpannableStringBuilder nameSpan = new SpannableStringBuilder(recycleItem.bubbleText);
            Pattern pattern = Pattern.compile(".*(" + StringUtils.handleSpecialCharacter(mSearchKey) + ").*", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(recycleItem.bubbleText);
            if (!TextUtils.isEmpty(mSearchKey) && matcher.matches()) {
                String matchedKey = matcher.group(1);
                String lowerBubbleText = recycleItem.bubbleText.toLowerCase();
                int start = lowerBubbleText.indexOf(matchedKey.toLowerCase());
                int end = start + matchedKey.length();
                nameSpan.setSpan(
                        new ForegroundColorSpan(mContext.getResources().getColor(R.color.high_light_red)),
                        start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            viewHolder.text.setText(nameSpan);
        }
        if (((RecycleBinActivity)mActivity).getBubbleDirection() == RecycleBinActivity.BUBBLE_RIRECTION_RECYCLE){
        viewHolder.recycleDate.setText(mContext.getResources().getString(R.string.delete_time)
                + " " + recycleItem.recycleFormattedDate);
        }else {
            viewHolder.recycleDate.setText(mContext.getResources().getString(R.string.handled_time) +
                    " " +recycleItem.recycleFormattedDate);
        }
        if (recycleItem.bubbleTodo == GlobalBubble.TODO_OVER) {
            viewHolder.text.setTextColor(mContext.getResources().getColorStateList(R.color.recycle_strike_text_color));
            viewHolder.text.setPaintFlags(viewHolder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            viewHolder.text.setTextColor(mContext.getResources().getColorStateList(R.drawable.recycle_option_text_color));
            viewHolder.text.setPaintFlags(viewHolder.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        viewHolder.recycleItem = recycleItem;
        viewHolder.color.setBackgroundResource(getBackgroudRes(recycleItem.bubbleColor));
        viewHolder.todo.setVisibility((recycleItem.bubbleTodo == GlobalBubble.TODO_OVER) ? View.VISIBLE : View.GONE);
        List<GlobalBubbleAttach> attaches = recycleItem.getBubbleAttaches();
        final int attachCount = attaches == null ? 0 : attaches.size();
        boolean remind = recycleItem.remindTime > 0 || recycleItem.dueDate > 0;
        viewHolder.attach.setVisibility(attachCount > 0 ? View.VISIBLE : View.GONE);
        viewHolder.reminder.setVisibility((recycleItem.remindTime > 0 || recycleItem.dueDate > 0) ? View.VISIBLE : View.GONE);
        if (recycleItem.bubbleType == GlobalBubble.TYPE_TEXT) {
            viewHolder.voice.setVisibility(View.GONE);
        } else {
            viewHolder.voice.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    public void setList(ArrayList<RecycleItem> bubbles, String searchKey) {
        mBubbles = bubbles;
        mSearchKey = searchKey;
        notifyDataSetChanged();
    }

    public List<RecycleItem> getList() {
        return this.mBubbles;
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
        if (item == null) {
            return;
        }
        if (mDialog == null) {
            mDialog = new Dialog(mActivity, R.style.CustomDialog);
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            BubbleItemView view = (BubbleItemView) inflater.inflate(R.layout.bubble_item, null);
            view.setVisibility(View.VISIBLE);
            view.setAttachmentList(item.getBubbleAttaches());
            GlobalBubble bubble= null;
            if (item.bubbleType != GlobalBubble.TYPE_TEXT){
                bubble = SaraUtils.toGlobalBubble(mActivity, item.bubbleText,item.bubbleType,
                    Uri.parse(SaraUtils.formatFilePath2Content(mActivity, item.bubblePath)),
                    item.bubbleColor, item.remindTime, item.dueDate);
            } else {
                bubble = SaraUtils.toGlobalBubbleText(mActivity,item.bubbleColor);
                bubble.setText(item.bubbleText);
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
            if (RecycleBinActivity.sBubbleDirection == RecycleBinActivity.BUBBLE_RIRECTION_USED) {
                TextView restoreview = (TextView) view.findViewById(R.id.iv_bubble_restore);
                if (restoreview != null) {
                    restoreview.setText(R.string.btn_cancel_complete);
                }
            }
            mDialog.addContentView(view, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    BubbleSpeechPlayer.getInstance(mActivity).stop();
                    mDialog = null;
                }
            });
            mDialog.setCanceledOnTouchOutside(true);
            mDialog.show();
        }
    }
}
