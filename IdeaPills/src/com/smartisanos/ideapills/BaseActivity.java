package com.smartisanos.ideapills;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import smartisanos.api.IntentSmt;
import smartisanos.widget.Title;

public class BaseActivity extends Activity {
    public void startActivityForResultWithAnimation(Intent intent, int requestCode) {
        intent.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), new int[]{
                0, smartisanos.R.anim.slide_down_out});
        super.startActivityForResult(intent, requestCode);
        overridePendingTransition(smartisanos.R.anim.pop_up_in, smartisanos.R.anim.fake_anim);
    }

    public void startActivity(Intent intent, boolean isPopup) {
        if (isPopup) {
            intent.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), new int[]{
                    0, smartisanos.R.anim.slide_down_out});
            super.startActivity(intent);
            overridePendingTransition(smartisanos.R.anim.pop_up_in, smartisanos.R.anim.fake_anim);
        } else {
            intent.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), new int[]{
                    smartisanos.R.anim.slide_in_from_left, smartisanos.R.anim.slide_out_to_right});
            super.startActivity(intent);
            overridePendingTransition(smartisanos.R.anim.slide_in_from_right, smartisanos.R.anim.slide_out_to_left);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (getIntent() != null) {
            int[] anims = getIntent().getIntArrayExtra(
                    IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID());
            if (anims != null) {
                overridePendingTransition(anims[0], anims[1]);
            }
        }
    }

    public void setTitleByIntent(Title title) {
        if (getIntent().hasExtra(Title.EXTRA_TITLE_TEXT)) {
            String titleStr = getIntent().getStringExtra(Title.EXTRA_TITLE_TEXT);
            if (!TextUtils.isEmpty(titleStr)) {
                title.setTitle(titleStr);
            }
        } else if (getIntent().hasExtra(Title.EXTRA_TITLE_TEXT_ID)) {
            int titleId = getIntent().getIntExtra(Title.EXTRA_TITLE_TEXT_ID, -1);
            if (titleId > 0) {
                try {
                    title.setTitle(getString(titleId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
