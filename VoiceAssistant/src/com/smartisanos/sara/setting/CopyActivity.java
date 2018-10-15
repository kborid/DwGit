
package com.smartisanos.sara.setting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.view.WindowManager;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraUtils;
import smartisanos.api.ToastSmt;

public class CopyActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        CharSequence charSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        SaraUtils.copyText(this, charSequence);
        Toast.makeText(this, R.string.toast_text_copied, Toast.LENGTH_SHORT).show();
        finish();
    }
}
