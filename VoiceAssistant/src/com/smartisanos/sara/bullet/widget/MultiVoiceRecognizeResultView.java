package com.smartisanos.sara.bullet.widget;


import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.VoiceSearchResult;
import com.smartisanos.sara.bullet.contact.view.PickContactView;
import com.smartisanos.sara.bullet.util.AnimationUtils;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.ToastUtil;

import java.util.List;

public class MultiVoiceRecognizeResultView implements VoiceRecognizeResultView<VoiceSearchResult> {
    private static final String TAG = "VoiceAss.MultiVoiceRecognizeResultView";
    private VoiceSearchView mVoiceSearchView;
    private ViewStub mResultStub;
    private ViewStub mEmptyStub;
    private View mResultRoot;
    private PickContactView mPickContactView;
    private ViewGroup mSearchResultView;
    private ViewGroup mEmptyResultView;
    private Select<AbsContactItem> mResultSelect;
    private View mRootBg;
    private boolean mIncludeTeam;
    private Context mContext;

    public MultiVoiceRecognizeResultView(View root, Select<AbsContactItem> select, boolean includeTeam) {
        mRootBg = root;
        mResultSelect = select;
        mIncludeTeam = includeTeam;
    }

    @Override
    public void init(RelativeLayout layout) {
        mContext = layout.getContext();
        mVoiceSearchView = (VoiceSearchView) layout;
        LayoutInflater.from(mContext).inflate(R.layout.multi_voice_recognize_result_view, layout);
        mResultStub = (ViewStub)layout.findViewById(R.id.stub_controller_layout);
        mEmptyStub = (ViewStub)layout.findViewById(R.id.stub_empty_layout);
    }

    private void initResultView() {
        if (mPickContactView != null) {
            updateResultBg();
            return;
        }
        mResultRoot = mResultStub.inflate();
        mPickContactView = (PickContactView)mResultRoot.findViewById(R.id.pick_contact);
        mPickContactView.setAbortPreTask(true);
        mSearchResultView = (ViewGroup)mResultRoot.findViewById(R.id.controller_layout);
        mPickContactView.setOnPickContactListener(new PickContactView.OnPickContactListener() {
            @Override
            public void OnCheckContact(AbsContactItem absContactItem, View view) {
                hideResultDialog();
                mVoiceSearchView.hideResultView();
                if (mResultSelect != null) {
                    mResultSelect.select(absContactItem, view);
                }
            }

            @Override
            public void onResultSize(int size) {
                if (size == 0) {
                    showEmptyDialog();
                } else {
                    showResultDialog();
                }
                LogUtils.d(TAG, "size:" + size);
            }
        });
        updateResultBg();
    }

    private void updateResultBg() {
        if (mRootBg != null && mRootBg.getBackground() != null && mResultRoot != null) {
            mResultRoot.setBackgroundDrawable(mRootBg.getBackground());
        }
    }

    public void hideResultDialog() {
        AnimationUtils.hideResultDialogWithAnim(mSearchResultView);
        AnimationUtils.hideResultDialogWithAnim(mEmptyResultView);
    }

    public void hideResultDialogWithoutAnim() {
        if (null != mSearchResultView && mSearchResultView.getVisibility() == View.VISIBLE) {
            mSearchResultView.setVisibility(View.GONE);
        }
        if (null != mEmptyResultView && mEmptyResultView.getVisibility() == View.VISIBLE) {
            mEmptyResultView.setVisibility(View.GONE);
        }
    }

    private void showEmptyDialog() {
        LogUtils.d(TAG, "showEmptyDialog()");
        if (isResultViewShown()) {
            hideResultDialogWithoutAnim();
        }
        initEmptyView();
        mVoiceSearchView.showCoverWithAnim();
        AnimationUtils.showResultDialogWithAnim(mEmptyResultView);
    }

    private void showResultDialog() {
        LogUtils.d(TAG, "showResultDialog()");
        if (isResultViewShown()) {
            hideResultDialogWithoutAnim();
        }
        mVoiceSearchView.setHasResult(true);
        mVoiceSearchView.showCoverWithAnim();
        AnimationUtils.showResultDialogWithAnim(mSearchResultView);
    }

    private void initEmptyView() {
        if (mEmptyResultView == null) {
            mEmptyResultView = (ViewGroup)mEmptyStub.inflate().findViewById(R.id.empty_layout);
        }
    }

    @Override
    public void resultVoiceRecognize(VoiceSearchResult result) {
        if (result != null) {
            initResultView();
            mPickContactView.query(result);
        }
    }



    @Override
    public void startVoiceRecognize() {
        AnimationUtils.hideResultDialogWithAnim(mSearchResultView);
        AnimationUtils.hideResultDialogWithAnim(mEmptyResultView);
    }

    @Override
    public void shortVoiceRecognize() {
        if (mSearchResultView != null) {
            AnimationUtils.cancelAnimation(mSearchResultView);
            mSearchResultView.clearAnimation();
            mSearchResultView.setVisibility(View.GONE);
        }
        if (mEmptyResultView != null) {
            AnimationUtils.cancelAnimation(mEmptyResultView);
            mEmptyResultView.clearAnimation();
            mEmptyResultView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isResultViewShown() {
        return (mSearchResultView != null && mSearchResultView.getVisibility() == View.VISIBLE)
                || (mEmptyResultView != null && mEmptyResultView.getVisibility() == View.VISIBLE);
    }

    @Override
    public void showTimeOutView() {
        initEmptyView();
        AnimationUtils.showResultDialogWithAnim(mEmptyResultView);
    }
}
