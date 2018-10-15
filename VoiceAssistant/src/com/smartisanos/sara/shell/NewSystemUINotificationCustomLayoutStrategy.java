package com.smartisanos.sara.shell;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.text.TextUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.entity.ShortcutApp;
import com.smartisanos.sara.widget.AppPickerSubView;
import com.smartisanos.sara.widget.DragGridView;
import com.smartisanos.sara.widget.NoScrollGridView;

import java.util.List;

public class NewSystemUINotificationCustomLayoutStrategy implements INotificationCustomLayoutStrategy {

    @Override
    public long getLongPressTriggerTimeMs() {
        return 50;
    }

    @Override
    public View onInitRootChildView(Context context, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.shortcut_app_setting_view, parent, true);
    }

    @Override
    public DragGridView getDragGridView(View rootChild) {
        return (DragGridView) rootChild.findViewById(R.id.drag_grid_view);
    }

    @Override
    public ViewGroup getCandidateContainer(View rootChild) {
        return (NoScrollGridView) rootChild.findViewById(R.id.candidate_container);
    }

    @Override
    public void onLayoutCandidateContainer(ViewGroup candidateContainer, List<ShortcutApp> candidates) {
        ((NoScrollGridView) candidateContainer).setAdapter(new NewSystemUINotificationCustomLayoutStrategy.CandidateAdapter(candidates));
    }

    @Override
    public ShortcutApp getCandidateWidgetByChildViewPosition(ViewGroup candidateContainer, List<ShortcutApp> candidates, int childViewPosition) {
        return candidates.get(childViewPosition);
    }

    @Override
    public void onCandidateWidgetChanged(ViewGroup candidateContainer, int changedChildPosition, ShortcutApp oldWidget, ShortcutApp newWidget) {
        ((NewSystemUINotificationCustomLayoutStrategy.CandidateAdapter) ((GridView) candidateContainer).getAdapter()).notifyDataSetChanged();
    }

    @Override
    public boolean allowCandidateExchangeSelfWidget(ViewGroup candidateContainer) {
        return true;
    }

    @Override
    public View onGetDragGridItemView(ShortcutApp item, int position, View convertView, ViewGroup parent) {
        return createItemView(parent.getContext(), item);
    }

    @Override
    public ImageView getCandidateViewIcon(View candidateView) {
        return (ImageView) candidateView.findViewById(R.id.icon);
    }

    @Override
    public ImageView getDragGridViewItemIcon(View candidateView) {
        return (ImageView) candidateView.findViewById(R.id.icon);
    }

    @Override
    public void updateGridItemViewVisibility(View childItem, int visibility) {
        updateItemViewVisibility(childItem, visibility);
    }

    @Override
    public void updateCandidateChildViewVisibility(View candidateView, int visibility) {
        updateItemViewVisibility(candidateView, visibility);
    }

    private void updateItemViewVisibility(View itemView, int visibility) {
        View iconView = itemView.findViewById(R.id.icon);
        View nameView = itemView.findViewById(R.id.app_name);
        iconView.clearAnimation();
        iconView.setVisibility(visibility);
        nameView.setVisibility(visibility);
    }

    private static View createItemView(Context context, ShortcutApp item) {
        View convertView = View.inflate(context, R.layout.shortcut_picker_sub_item, null);
        setupItemView(context, convertView, item);
        return convertView;
    }

    private static void setupItemView(Context context, View itemView, ShortcutApp item) {
        if (itemView instanceof AppPickerSubView) {
            AppPickerSubView view = (AppPickerSubView) itemView;
            if (TextUtils.isEmpty(item.getDispalyName())) {
                view.setVisibility(View.INVISIBLE);
            } else {
                view.setVisibility(View.VISIBLE);
                view.setImageDrawable(item.getDrawable());
                view.setText(item.getDispalyName());
            }
        }
    }

    private static class CandidateAdapter extends BaseAdapter {

        private List<ShortcutApp> mCandidates;

        private CandidateAdapter(List<ShortcutApp> candidates) {
            this.mCandidates = candidates;
        }

        @Override
        public int getCount() {
            return mCandidates == null ? 0 : mCandidates.size();
        }

        @Override
        public ShortcutApp getItem(int position) {
            return mCandidates.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createItemView(parent.getContext(), getItem(position));
        }
    }
}