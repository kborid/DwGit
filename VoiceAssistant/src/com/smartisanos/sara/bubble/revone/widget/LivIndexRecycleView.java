package com.smartisanos.sara.bubble.revone.widget;

import android.view.View;
import android.widget.AbsListView;

import java.util.Map;

public class LivIndexRecycleView {

    private final AbsListView lvContacts;

    private final LetterIndexView livIndex;

    private final Map<String, Integer> mapABC;

    public LivIndexRecycleView(AbsListView contactsListView, LetterIndexView letterIndexView, Map<String, Integer> abcMap) {
        this.lvContacts = contactsListView;
        this.livIndex = letterIndexView;
        this.mapABC = abcMap;
        this.livIndex.setOnTouchingLetterChangedListener(new LetterChangedListener());
    }

    public void show() {
        this.livIndex.setVisibility(View.VISIBLE);
    }

    public void hide() {
        this.livIndex.setVisibility(View.GONE);
    }

    private class LetterChangedListener implements LetterIndexView.OnTouchingLetterChangedListener {

        @Override
        public void onHit(String letter) {
            int index = -1;
            if ("↑".equals(letter)) {
                index = 0;
            } else if (mapABC.containsKey(letter)) {
                index = mapABC.get(letter);
            }
            if (index < 0) {
                return;
            }
            if (index >= 0 && index < lvContacts.getAdapter().getCount()) {
                lvContacts.setSelection(index);
            }
        }

        @Override
        public void onCancel() {

        }
    }

}
