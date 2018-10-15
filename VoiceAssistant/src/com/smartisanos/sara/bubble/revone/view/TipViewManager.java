package com.smartisanos.sara.bubble.revone.view;

import android.content.Context;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.SearchPresenter;

public class TipViewManager extends ViewManager {
    private View mAddContent;
    private TextView mTitleView;
    private IButtonView mButtonView;
    private View mNoResultView;

    public TipViewManager(Context context, View view, IButtonView buttonView) {
        super(context, view);
        mButtonView = buttonView;
    }

    @Override
    protected View getView() {
        View view = null;
        if (mRootView != null) {
            ViewStub tipContentStup = (ViewStub) mRootView.findViewById(R.id.tips_content_stub);
            if (tipContentStup != null && tipContentStup.getParent() != null) {
                view = tipContentStup.inflate();
            } else {
                view = mRootView.findViewById(R.id.tip_content);
            }
            mTitleView = (TextView) view.findViewById(R.id.title);
            mNoResultView = view.findViewById(R.id.no_result);
            int top = ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR ? ExtScreenConstant.STATUS_BAR_HEIGHT : 0;
            int bottom = ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR ? ExtScreenConstant.NAVIGATION_BAR_HEIGHT : 0;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            lp.topMargin += top;
            lp.bottomMargin += bottom;
            view.setLayoutParams(lp);
        }
        return view;
    }

    public void show(int type, boolean hasSearchEngine, boolean hasResult) {
        super.show();
        if (hasSearchEngine) {
            int resId = R.string.bubble_search_result;
            switch (type) {
                case SearchPresenter.SEARCH_TYPE_LOCAL:
                    resId = R.string.bubble_search_local;
                    break;
                case SearchPresenter.SEARCH_TYPE_WEB:
                    resId = R.string.bubble_search_web;
                    break;
            }
            if (resId > 0 && mTitleView != null) {
                mTitleView.setText(resId);
            }
            if (mAddContent != null) {
                mAddContent.setVisibility(View.GONE);
            }
            if (mTitleView != null) {
                mTitleView.setVisibility(hasResult ? View.VISIBLE : View.GONE);
            }
            if (mNoResultView != null) {
                mNoResultView.setVisibility(hasResult ? View.GONE : View.VISIBLE);
            }
        } else {
            ensureAddButtonLayout();
            if (mAddContent != null) {
                mAddContent.setVisibility(View.VISIBLE);
            }
            if (mTitleView != null) {
                mTitleView.setVisibility(View.GONE);
            }
            if (mNoResultView != null) {
                mNoResultView.setVisibility(View.GONE);
            }
        }
    }

    private void ensureAddButtonLayout() {
        if (mAddContent == null) {
            if (mView != null) {
                ViewStub addTipStup = (ViewStub) mView.findViewById(R.id.none_search_stub);
                if (addTipStup != null && addTipStup.getParent() != null) {
                    mAddContent = addTipStup.inflate();
                } else {
                    mAddContent = mView.findViewById(R.id.none_search);
                }
                View addButton = mAddContent.findViewById(R.id.btn_add);
                addButton.setOnClickListener(mClickListener);
            }
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mButtonView != null) {
                mButtonView.performSetting();
            }
        }
    };
}
