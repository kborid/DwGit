package com.smartisanos.sara.shell;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.smartisanos.sara.entity.ShortcutApp;
import com.smartisanos.sara.widget.DragGridView;

import java.util.List;


public interface INotificationCustomLayoutStrategy {

    long getLongPressTriggerTimeMs();

    View onInitRootChildView(Context context, ViewGroup parent);

    DragGridView getDragGridView(View rootChild);

    ViewGroup getCandidateContainer(View rootChild);

    void onLayoutCandidateContainer(ViewGroup candidateContainer, List<ShortcutApp> candidates);

    ShortcutApp getCandidateWidgetByChildViewPosition(ViewGroup candidateContainer, List<ShortcutApp> candidates, int childViewPosition);

    void onCandidateWidgetChanged(ViewGroup candidateContainer, int changedChildPosition, ShortcutApp oldWidget, ShortcutApp newWidget);

    boolean allowCandidateExchangeSelfWidget(ViewGroup candidateContainer);

    View onGetDragGridItemView(ShortcutApp item, int position, View convertView, ViewGroup parent);

    ImageView getCandidateViewIcon(View candidateView);

    ImageView getDragGridViewItemIcon(View candidateView);

    void updateGridItemViewVisibility(View childItem, int visibility);

    void updateCandidateChildViewVisibility(View candidateView, int visibility);

}
