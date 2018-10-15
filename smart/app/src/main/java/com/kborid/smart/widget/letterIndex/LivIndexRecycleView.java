package com.kborid.smart.widget.letterIndex;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kborid.smart.widget.letterIndex.LetterIndexView;

import java.util.HashMap;
import java.util.Map;

/**
 * 字母导航，点击字母，列表滑动到指定字母集合上。
 *
 * @author huangjun
 */
public class LivIndexRecycleView {

    private final RecyclerView lvContacts;

    private final LetterIndexView livIndex;

    private final TextView lblLetterHit;

    private final ImageView imgBackLetter;

    private Map<String, Integer> mapABC; // 字母:所在的行的index

    public LivIndexRecycleView(RecyclerView contactsListView, LetterIndexView letterIndexView, TextView letterHit, ImageView
            imgBackLetter, Map<String, Integer> abcMap) {
        this.lvContacts = contactsListView;
        this.livIndex = letterIndexView;
        this.lblLetterHit = letterHit;
        this.imgBackLetter = imgBackLetter;
        this.mapABC = abcMap;
        this.livIndex.setOnTouchingLetterChangedListener(new LetterChangedListener());
    }

    public void updateIndexes(Map<String, Integer> indexes) {
        mapABC = indexes == null ? new HashMap<String, Integer>() : indexes;
    }
    /**
     * 显示
     */
    public void show() {
        this.livIndex.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏
     */
    public void hide() {
        this.livIndex.setVisibility(View.GONE);
    }

    private class LetterChangedListener implements LetterIndexView.OnTouchingLetterChangedListener {

        @Override
        public void onHit(final String letter, int loc) {
            int index = -1;
            if (mapABC.containsKey(letter)) {
                index = mapABC.get(letter);
            }
            if (index < 0) {
                return;
            }
            if (index >= 0 && index < lvContacts.getAdapter().getItemCount()) {
                scrollTo(index);
            }
        }

        @Override
        public void onHit(Drawable drawable, int loc) {
            if(loc == 0) {
                scrollTo(0);
            }
        }

        private void scrollTo(final int pos) {
            try {
                ((LinearLayoutManager) lvContacts.getLayoutManager()).scrollToPositionWithOffset(pos, 0);
            } catch (ClassCastException e) {
                lvContacts.scrollToPosition(pos);
            }
        }

        @Override
        public void onCancel() {
            lblLetterHit.setVisibility(View.INVISIBLE);
            imgBackLetter.setVisibility(View.INVISIBLE);
        }
    }

}
