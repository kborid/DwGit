package com.smartisanos.sara.bubble.revone.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.RelativeLayout;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.adapter.ContactAdapter;
import com.smartisanos.sara.bubble.revone.entity.GlobalContact;
import com.smartisanos.sara.bullet.util.PinYinUtils;
import com.smartisanos.sara.bubble.revone.widget.LetterIndexView;
import com.smartisanos.sara.bubble.revone.widget.LivIndexRecycleView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.smartisanos.sara.bubble.revone.entity.GlobalContact.CONTACT_COMPARATOR;

public class AllContactsViewManager extends ImBaseViewManager {
    private final Map<String, Integer> indexesMap = new HashMap<>();

    public AllContactsViewManager(Context context, View view, boolean mode) {
        super(context, view, mode);
    }

    @Override
    protected View getView() {
        View view = null;
        if (mRootView != null) {

            ViewStub contentStup = (ViewStub) mRootView.findViewById(R.id.all_contact_stub);
            if (contentStup != null && contentStup.getParent() != null) {
                view = contentStup.inflate();
            } else {
                view = mRootView.findViewById(R.id.all_contact);
            }
            CommonUtils.setAlwaysCanAcceptDragForAll(view, true);
            View divider = view.findViewById(R.id.divider);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) divider.getLayoutParams();
            lp.topMargin = mTopPadding;
            lp.bottomMargin = mBottomPadding + mDividerBottomMargin;
            divider.setLayoutParams(lp);
            View contactContent = view.findViewById(R.id.contact_content);
            contactContent.setPadding(0, mTopPadding, 0, mBottomPadding);
            mListView = (AbsListView) view.findViewById(R.id.list_view);
            mContactAdapter = new ContactAdapter(mContext);
            mListView.setOnItemClickListener(mOnItemClickListener);
            mListView.setAdapter(mContactAdapter);
            loadData(ImBaseViewManager.TYPE_CONTENT_ALL);
        }
        return view;
    }

    protected void onContactDataChange(ArrayList<GlobalContact> contacts) {
        if (contacts != null) {
            indexesMap.clear();
            String firstLetter = null;
            for (GlobalContact contact : contacts) {
                String pinyin = PinYinUtils.getPinYin(contact.getContactName());
                if (TextUtils.isEmpty(pinyin)) {
                    firstLetter = pinyin = "#";
                } else {
                    firstLetter = getFirstLetter(pinyin);
                }
                contact.setPinyin(pinyin);
                contact.setFirstLetter(firstLetter);
            }
            Collections.sort(contacts, CONTACT_COMPARATOR);
            int count = 0;
            for (GlobalContact contact : contacts) {
                firstLetter = contact.getFirstLetter();
                if (indexesMap.get(firstLetter) == null) {
                    indexesMap.put(firstLetter, count);
                }
                count++;
            }
            updateLivIndex();
        }
        super.onContactDataChange(contacts);
    }

    private void updateLivIndex() {
        if (mView != null) {
            LetterIndexView idxView = (LetterIndexView) mView.findViewById(R.id.liv_index);
            idxView.setLetters(mContext.getResources().getStringArray(R.array.letter_list));
            LivIndexRecycleView livIndex = new LivIndexRecycleView(mListView, idxView, indexesMap);
            livIndex.show();
        }
    }

    public String getFirstLetter(String pinyin) {
        if (TextUtils.isEmpty(pinyin)) return null;
        char character = pinyin.charAt(0);
        return Character.toString(Character.toUpperCase(character));
    }

    public List<GlobalContact> getAllContacts() {
        return mContactAdapter.getAllContacts();
    }
}
