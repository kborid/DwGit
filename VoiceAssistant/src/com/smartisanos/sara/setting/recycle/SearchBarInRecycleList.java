package com.smartisanos.sara.setting.recycle;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.smartisanos.sara.R;

public class SearchBarInRecycleList extends RelativeLayout implements OnClickListener
{

    private Context mContext;
    private RelativeLayout mSearchBarView;

    private LinearLayout mSearchView;
    private TextView mSearchViewRight;
    private TextView mSearchViewLeft;
    private TextView mBtnCancelSearch;
    private TextView mBtnClearSearch;
    private EditText mSearchEdit;
    private boolean mIsSearchMode = false;
    private boolean mIsPlaySearchModeAnimation = false;
    private RelativeLayout mSearchBar = null;
    private int mDuration = 200;
    private AlphaAnimation mVisibleAnimation;
    private AlphaAnimation mGoneAnimation;

    private Listener mListener;

    public interface Listener {
        void onModeChange(int SortMode);

        void setSoftKeyboardHide();

        void startBubbleAnimation();

        void endBubbleAnimationWithoutAnimation();

        void endBubbleAnimation();

        void onQueryTextChange(String newString);
    }

    public SearchBarInRecycleList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchBarView = (RelativeLayout) inflater
                .inflate(R.layout.searchbar_in_recycle_list, this);
        setViews();
        setSearchModeAS();
    }

    private void setViews() {
        mSearchBar = (RelativeLayout) mSearchBarView
                .findViewById(R.id.searchbarframe);
        mSearchView = (LinearLayout) mSearchBarView.findViewById(R.id.search_view);
        mSearchView.setOnClickListener(this);
        mSearchViewRight = (TextView) mSearchBarView
                .findViewById(R.id.searchbarright);
        mSearchViewRight.setOnClickListener(this);
        mSearchViewLeft = (TextView) mSearchBarView
                .findViewById(R.id.searchbarleft);
        mSearchViewLeft.setOnClickListener(this);
        mBtnCancelSearch = (TextView) mSearchBarView
                .findViewById(R.id.btn_cancel);
        mBtnCancelSearch.setOnClickListener(this);

        mBtnClearSearch = (TextView) mSearchBar.findViewById(R.id.btn_clear_search);
        mBtnClearSearch.setOnClickListener(this);

        mSearchEdit = (EditText) mSearchBar.findViewById(R.id.search_edit_text);
        mSearchEdit.setOnClickListener(this);
        mSearchEdit.addTextChangedListener(mSearchWatcher);
        mSearchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mListener.setSoftKeyboardHide();
                }
                return false;
            }
        });
    }

    private TextWatcher mSearchWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mIsSearchMode)
                mListener.onQueryTextChange(String.valueOf(s));
        }

        @Override
        public void afterTextChanged(Editable s) {
            mBtnClearSearch.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
        }
    };

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.searchbarright:
            case R.id.searchbarleft:
                if (mIsSearchMode) {
                    break;
                }
            case R.id.search_edit_text:
                if (!mIsPlaySearchModeAnimation && !mIsSearchMode) {
                    mSearchEdit.setCursorVisible(true);
                    mSearchEdit.setFocusable(true);
                    mSearchEdit.setFocusableInTouchMode(true);
                    mSearchEdit.requestFocus();
                    mListener.startBubbleAnimation();
                    setBtnCancelVisibleAM(mBtnCancelSearch);
                }
                break;
            case R.id.btn_cancel:
                cancelSearch();
                break;
            case R.id.btn_clear_search:
                mSearchEdit.setText("");
                break;
        }
    }

    private void setBtnCancelVisibleAM(final View toVisibleView) {
        mVisibleAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation arg0) {
                mIsPlaySearchModeAnimation = true;
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                mIsPlaySearchModeAnimation = false;
                toVisibleView.setVisibility(View.VISIBLE);
                post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) mContext
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mSearchEdit, 0);
                    }
                });
            }
        });

        Animator searchViewAnimator = ObjectAnimator.ofFloat(mSearchViewRight, "translationX", 0,
                -(toVisibleView.getWidth()));
        searchViewAnimator.setDuration(mDuration / 2);
        searchViewAnimator.start();

        toVisibleView.startAnimation(mVisibleAnimation);

        // reset the searchView's marginRight
        int marginRight = toVisibleView.getWidth();
        LayoutParams params = (LayoutParams) mSearchView.getLayoutParams();
        params.rightMargin = marginRight;
        mSearchView.setLayoutParams(params);

        mIsSearchMode = true;
    }

    private void setBtnCancelGoneAM(final View toGoneView) {
        mGoneAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                mIsPlaySearchModeAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsPlaySearchModeAnimation = false;
                toGoneView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        Animator searchViewAnimator = ObjectAnimator.ofFloat(mSearchViewRight, "translationX",
                -(toGoneView.getWidth()), 0);
        searchViewAnimator.setDuration(mDuration);
        searchViewAnimator.start();

        toGoneView.startAnimation(mGoneAnimation);

        mIsSearchMode = false;
        TextKeyListener.clear(mSearchEdit.getText());
        mListener.setSoftKeyboardHide();
        mSearchEdit.setFocusable(false);
        mSearchEdit.setFocusableInTouchMode(false);

    }

    public void clearSearchText() {
        if (mSearchEdit != null) {
            mSearchEdit.setText("");
        }
    }

    public boolean isInSearchMode() {
        return mIsSearchMode;
    }

    public void cancelSearch() {
        if (!mIsPlaySearchModeAnimation && mIsSearchMode) {
            mListener.endBubbleAnimation();
            setBtnCancelGoneAM(mBtnCancelSearch);
        }
    }

    public void cancelSearchWithoutAnimation() {
        if (mIsSearchMode && mListener != null) {
            mListener.endBubbleAnimationWithoutAnimation();
            mBtnCancelSearch.setVisibility(View.INVISIBLE);
            mSearchViewRight.setTranslationX(0);

            mIsSearchMode = false;
            TextKeyListener.clear(mSearchEdit.getText());
            mListener.setSoftKeyboardHide();
            mSearchEdit.setFocusable(false);
            mSearchEdit.setFocusableInTouchMode(false);
        }
    }

    public void setCursorVisible(boolean isVisible) {
        mSearchEdit.setCursorVisible(isVisible);
    }

    private void setSearchModeAS() {
        mVisibleAnimation = new AlphaAnimation(0, 1);
        mVisibleAnimation.setDuration(mDuration / 2);
        mVisibleAnimation.setStartOffset(mDuration / 2);
        mGoneAnimation = new AlphaAnimation(1, 0);
        mGoneAnimation.setDuration(mDuration / 3);
    }
}
