package com.smartisanos.sara.setting;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SmtPCUtils;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.ClickUtil;
import com.smartisanos.sara.util.LogUtils;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.service.onestep.GlobalBubble;

import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.sara.widget.IGuideAnimView;
import com.smartisanos.sara.widget.LeftSlideGuideView;

import smartisanos.api.IntentSmt;
import smartisanos.util.DeviceType;
import smartisanos.widget.PreviewOptionView;
import smartisanos.api.SettingsSmt;
import smartisanos.widget.Title;
import smartisanos.widget.PreviewOptionsCheckView;
import smartisanos.widget.ListContentItem;
import smartisanos.widget.ListContentItemCheck;
import smartisanos.widget.ListContentItemText;
import smartisanos.widget.ListContentItemSwitch;

public class RealSettingActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener ,View.OnClickListener,
        PreviewOptionsCheckView.OptionCheckListener {

    private final static int DIALOG_PLAY_USAGE_VIDEO = 1;
    private final static int DIALOG_SHORTCUT_KEY_TIPS = 2;
    private final static int DIALOG_SHORTCUT_AS_SIDEKEY = 3;
    private final static int DIALOG_SHORTCUT_AS_HOME = 4;
    private final static int DIALOG_BETA_WARNING = 5;
    private final static int DIALOG_MEMORY_DIALOG_TIPS = 6;
    private static final int REQUEST_CODE_PLAY_VIDEO = 10;
    private final static int CATEGORY_COLOR = 0;
    private final static String VOICE_ASSIST_BETA_ENABLE = "voice_assist_beta_enable";

    private View mBetaIntroView;
    private View mNextStepView;
    private ListView mOptionsList;
    private View mSubHeadSettingsView;
    private View mSubFooterSettingsView;
    private TextView mUsageTipsView;
    private PreviewOptionsCheckView mShortcutKeyChooserView;
    private ListContentItemSwitch mVoiceInputSwitch;
    private ListContentItemSwitch mLeftSlideLunchGloblePill;
    private ListContentItemSwitch mVoiceWebSwitch;
    private ListContentItemSwitch mVoiceLocalSwitch;
    private ListContentItemCheck mOpenModeKey;
    private ListContentItemCheck mOpenModeTouch;
    private ListContentItemCheck mDefaultSearch;
    private ListContentItemCheck mDefaultBullet;
    private ListContentItemText mHandled;
    private ListContentItemText mHandledOk;
    //private SettingItemText mTodoOver;
    private ListContentItemText mRecycleBin;
    private ListContentItemText mAppDrawer;
    private ListContentItemText mShareIdeapill;
    private IGuideAnimView mGuideAnimView;
    private ViewGroup mGuideContainer;
    private OptionsAdapter mAdapter;
    private LeftSlideGuideView mGuideView;
    private List<VoiceInputItem> mVoiceItemList = new ArrayList<VoiceInputItem>();
    private int mCurrentColorValue;
    private boolean mIsVoiceInputEnabled;
    private int mCheckIconPaddingRight;
    private boolean mLeftKeyAsBrightness;
    private LayoutInflater mInfalter;
    private boolean hasIdeapillKeyPhoneType = DeviceType.isSmartKeyProduct();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_FINISH_SETTINGS_ACTIVITY.equals(action)) {
                finish();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason) && SaraUtils.isKillSelf(context)) {
                    SaraUtils.killSelf(context);
                }
            }
        }
    };
    private AlertDialog mWarningDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options_list_layout);
        SaraTracker.onLaunch();
        mInfalter = LayoutInflater.from(this);
        mOptionsList = (ListView) findViewById(R.id.options_list);
        mBetaIntroView = findViewById(R.id.beta_intro_container);
        mNextStepView = findViewById(R.id.next_step);
        mNextStepView.setOnClickListener(this);
        boolean isBetaEnabled = Settings.Global.getInt(getContentResolver(), VOICE_ASSIST_BETA_ENABLE, 0) == 1;
        ImageView appIcon = (ImageView) findViewById(R.id.app_icon);
        appIcon.setImageDrawable(getAppIcon());
        if (!isBetaEnabled && SaraUtils.isSettingEnable(this)) {
            Settings.Global.putInt(getContentResolver(), VOICE_ASSIST_BETA_ENABLE, 1);
            isBetaEnabled = true;
        }
        updateBetaViews(isBetaEnabled);
        initData();
        addHeaderFooterView();
        mAdapter = new OptionsAdapter(this);
        mOptionsList.setAdapter(mAdapter);
        mOptionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                int pos = index - mOptionsList.getHeaderViewsCount();
                if (pos < 0 || l == -1) {
                    return;
                }

                VoiceInputItem selectedItem = mVoiceItemList.get(pos);
                switch (selectedItem.category) {
                    case CATEGORY_COLOR:
                        if (!ClickUtil.isFastClick()) {
                            if (selectedItem.settingsValue == GlobalBubble.COLOR_SHARE
                                    && !BubbleDataRepository.isBubbleCanShare(getApplicationContext(), true)) {
                                return;
                            }
                            mCurrentColorValue = selectedItem.settingsValue;
                            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.DEFAULT_BUBBLE_TYPE, selectedItem.settingsValue);
                        }
                        break;
                    default:
                        break;
                }
                mAdapter.notifyDataSetChanged();
            }
        });

        initTitleBar();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SaraConstant.ACTION_FINISH_SETTINGS_ACTIVITY);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);
        setCaptionTitleToPillInExtDisplay();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateOpenModeView();
        updateDefaultShowView(Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_KEY, SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH),
                Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_CHANGED_BY, SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.AUTO));
    }

    private void updateOpenModeView() {
        if (hasIdeapillKeyPhoneType) {
            ContentResolver cr = getContentResolver();
            String currentFunc = Settings.Global.getString(getContentResolver(), SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION);
            int smartKeyEnable = Settings.Global.getInt(cr, SettingsSmt.Global.SMART_KEY_ENABLED, SaraConstant.OPEN_MODE_ENABLE);
            boolean keymodeOn = SettingsSmt.SHORTCUT_KEY_VALUE.IDEA_PILLS_LIST.equals(currentFunc) && smartKeyEnable == SaraConstant.OPEN_MODE_ENABLE;
            boolean touchmodeOn = Settings.Global.getInt(cr, SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, SaraConstant.OPEN_MODE_ENABLE)
                    == SaraConstant.OPEN_MODE_ENABLE;
            mOpenModeKey.setChecked(keymodeOn);
            mOpenModeTouch.setChecked(touchmodeOn);
            if ((keymodeOn && touchmodeOn) || (!keymodeOn && !touchmodeOn)) {
                mOpenModeKey.setCheckedIconLight(false);
                mOpenModeTouch.setCheckedIconLight(false);
                mOpenModeKey.setOnClickListener(this);
                mOpenModeTouch.setOnClickListener(this);
            } else {
                if(touchmodeOn){
                    mOpenModeKey.setOnClickListener(this);
                    mOpenModeTouch.setOnClickListener(null);
                }else{
                    mOpenModeKey.setOnClickListener(null);
                    mOpenModeTouch.setOnClickListener(this);
                }
                mOpenModeKey.setCheckedIconLight(!touchmodeOn);
                mOpenModeTouch.setCheckedIconLight(!keymodeOn);
            }
            updateHandViewAnim();
        }
    }

    /**
     * 更新默认显示结果的设置项
     * @param checkIndex 设置的Index
     * @param byManual 是否是手动设置
     */
    private void updateDefaultShowView(int checkIndex, int byManual) {
        // 用户未干预过的前提下，如下逻辑；否则参照设定值显示
        if (SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.AUTO == byManual) {
            boolean installedBullet = PackageUtils.isAvilibleApp(this, SaraConstant.PACKAGE_NAME_BULLET);
            if (installedBullet) {
                //用户安装子弹短信，如果未登录，则设定显示为搜索结果；否则如果登录，设定值显示为子弹短信
                boolean isLoginBullet = PackageUtils.isBulletAppLogin(this);
                checkIndex = isLoginBullet ? SettingsSmt.VOICE_SHOW_RESULT_VALUE.BULLET : SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH;
            } else {
                //用户安装子弹短信后，又卸载，则设定值显示为搜索结果
                //用户未安装子弹短信，则设定值显示为搜索结果
                checkIndex = SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH;
            }
        }

        //刷新默认显示结果设置状态
        mDefaultSearch.setChecked(SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH == checkIndex);
        mDefaultBullet.setChecked(SettingsSmt.VOICE_SHOW_RESULT_VALUE.BULLET == checkIndex);

        if (mDefaultSearch.isChecked()) {
            mDefaultSearch.setOnClickListener(null);
            mDefaultBullet.setOnClickListener(this);
        } else {
            mDefaultSearch.setOnClickListener(this);
            mDefaultBullet.setOnClickListener(null);
        }

        // 判断是否是手动设置，如果byManual为true，才向setting设置值;否则,只刷新显示
        if (SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.MANUAL == byManual) {
            if (Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_CHANGED_BY, SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.AUTO) != byManual) {
                Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_CHANGED_BY, byManual);
            }
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_KEY, checkIndex);
        }
    }

    public void enableSmartKeyAsIdeaPills() {
        ContentResolver cr = getContentResolver();
        Settings.Global.putInt(cr, SettingsSmt.Global.SMART_KEY_ENABLED, SaraConstant.OPEN_MODE_ENABLE);
        Settings.Global.putString(cr, SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION, SettingsSmt.SHORTCUT_KEY_VALUE
                .IDEA_PILLS_LIST);
        Settings.Global.putString(cr, SettingsSmt.Global.SMART_KEY_LONG_CLICK_FUNCTION, SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS);
        Settings.Global.putString(cr, SettingsSmt.Global.SMART_KEY_LONG_CLICK_FUNCTION_WHEN_SCREEN_OFF, SettingsSmt
                .LONG_CLICK_TYPE.IDEA_PILLS);
    }

    public void disableSmartKey() {
        ContentResolver cr = getContentResolver();
        Settings.Global.putString(cr, SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION, SettingsSmt.SHORTCUT_KEY_VALUE
                .NONE);
        Settings.Global.putString(cr, SettingsSmt.Global.SMART_KEY_LONG_CLICK_FUNCTION, SettingsSmt.SHORTCUT_KEY_VALUE
                .NONE);
        Settings.Global.putString(cr, SettingsSmt.Global.SMART_KEY_LONG_CLICK_FUNCTION_WHEN_SCREEN_OFF, SettingsSmt
                .SHORTCUT_KEY_VALUE.NONE);
    }

    private void onSmartKeyFuncChange() {
        String currentFunc = Settings.Global.getString(getContentResolver(), SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION);
        boolean keymodeOn = SettingsSmt.SHORTCUT_KEY_VALUE.IDEA_PILLS_LIST.equals(currentFunc);
        if (keymodeOn) {
            disableSmartKey();
        } else {
            enableSmartKeyAsIdeaPills();
        }
        updateOpenModeView();
    }

    private void onCheckedChange(String key, int defaultValue) {
        ContentResolver cr = getContentResolver();
        boolean on = Settings.Global.getInt(cr, key, defaultValue) == SaraConstant.OPEN_MODE_ENABLE;
        Settings.Global.putInt(cr, key, on ? SaraConstant.OPEN_MODE_DISABLE : SaraConstant.OPEN_MODE_ENABLE);
        updateOpenModeView();
    }

    private void updateHandViewAnim(){
        ContentResolver cr = getContentResolver();
        boolean showHandAnim = Settings.Global.getInt(cr, SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, SaraConstant.OPEN_MODE_ENABLE)
                == SaraConstant.OPEN_MODE_ENABLE;
        mGuideAnimView.setShowHandAnim(showHandAnim);
    }

    private void updateBetaViews(boolean isBetaEnabled) {
        if (isBetaEnabled) {
            mBetaIntroView.setVisibility(View.GONE);
            mOptionsList.setVisibility(View.VISIBLE);
        } else {
            mBetaIntroView.setVisibility(View.VISIBLE);
            mOptionsList.setVisibility(View.GONE);

        }
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.idea_pills_beta);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent fromIntent = getIntent();
        //if from launcher, hidden the back button, otherwise set the background
        if (!fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT) && !fromIntent.hasExtra("from_search")) {
            title.getBackButton().setVisibility(View.GONE);
            SaraTracker.onEvent("A420028","type",2);
        } else {
            title.setBackButtonTextByIntent(getIntent());
            if (fromIntent.hasExtra("from_search")) {
                title.setBackBtnArrowVisible(false);
            } else {
                title.setBackBtnArrowVisible(true);
            }
            title.setBackButtonTextGravity(Gravity.CENTER);
            title.getBackButton().setVisibility(View.VISIBLE);
            SaraTracker.onEvent("A420028","type",3);
        }
        setTitleByIntent(title);

        boolean fromLauncher = fromIntent.getBooleanExtra("from_launcher", false);
        if (fromLauncher) {
            SaraTracker.onEvent("A420004");
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initTitleBar();
    }

    private void initData() {
        mVoiceItemList.clear();
        String[] colorTitles = getResources().getStringArray(R.array.bubble_color_name);
        int[] colorValues = getResources().getIntArray(R.array.bubble_color_value);
        TypedArray colorIconArray = getResources().obtainTypedArray(R.array.bubble_color_icon);
        for (int i = 0; i < colorTitles.length; i++) {
            VoiceInputItem item = new VoiceInputItem();
            item.category = CATEGORY_COLOR;
            item.title = colorTitles[i];
            item.icon = colorIconArray.getDrawable(i);
            item.settingsValue = colorValues[i];
            mVoiceItemList.add(item);
        }
        colorIconArray.recycle();
        mCheckIconPaddingRight = getResources().getDimensionPixelSize(R.dimen.setting_item_check_icon_padding_right);
    }

    private void addHeaderFooterView() {
        View transparentHeader = SaraUtils.inflateListTransparentHeader(this);
        transparentHeader.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mOptionsList.addHeaderView(transparentHeader);
        View headerView = getLayoutInflater().inflate(R.layout.voice_input_header_layout, null);
        mVoiceInputSwitch = (ListContentItemSwitch) headerView.findViewById(R.id.voice_input);
        mVoiceInputSwitch.setTitle(R.string.globle_app_name);
        mGuideContainer = (LinearLayout) headerView.findViewById(R.id.guide_container);
        if (SmtPCUtils.isValidExtDisplayId(this)) {
            LayoutInflater.from(this).inflate(R.layout.revone_guide_anim, mGuideContainer, true);
        } else {
            LayoutInflater.from(this).inflate(R.layout.left_slide_bubble_gif_anim_layout, mGuideContainer, true);
        }
        mGuideAnimView = (IGuideAnimView) mGuideContainer.getChildAt(0);
        mLeftSlideLunchGloblePill = (ListContentItemSwitch) headerView.findViewById(R.id.left_slide_lunch_global_pill);
        mLeftSlideLunchGloblePill.setTitle(R.string.left_slide_lunch_global_pill);

        View footerView = getLayoutInflater().inflate(R.layout.voice_input_foot_layout, null);
        mAppDrawer = (ListContentItemText) footerView.findViewById(R.id.app_drawer);
        mAppDrawer.setTitle(R.string.app_drawer_string);

        mVoiceWebSwitch = (ListContentItemSwitch) footerView.findViewById(R.id.web_voice);
        mVoiceWebSwitch.setTitle(R.string.voice_input_web);
        mVoiceLocalSwitch = (ListContentItemSwitch) footerView.findViewById(R.id.local_voice);
        mVoiceLocalSwitch.setTitle(R.string.voice_input_local);
        mHandled = (ListContentItemText) footerView.findViewById(R.id.handled);
        mHandled.setTitle(R.string.handled);
        mHandledOk = (ListContentItemText) footerView.findViewById(R.id.handled_ok);
        mHandledOk.setTitle(R.string.handled_ok);
        //mTodoOver = (SettingItemText) footerView.findViewById(R.id.todo_over);
        mRecycleBin = (ListContentItemText) footerView.findViewById(R.id.recycle_bin);
        mRecycleBin.setTitle(R.string.recycle_bin);
        mVoiceInputSwitch.setFocusable(true);
        mVoiceLocalSwitch.setFocusable(true);
        mVoiceWebSwitch.setFocusable(true);
        mSubHeadSettingsView = headerView.findViewById(R.id.sub_header_view_container);
        mSubFooterSettingsView = footerView.findViewById(R.id.sub_footer_view_container);
        mShareIdeapill = (ListContentItemText) footerView.findViewById(R.id.bubble_share);
        mShareIdeapill.setTitle(R.string.sync_share_main_title);
        mUsageTipsView = (TextView) headerView.findViewById(R.id.id_usage_tips);
        mShortcutKeyChooserView = (PreviewOptionsCheckView) headerView.findViewById(R.id.id_shortcut_key_chooser);
        mShortcutKeyChooserView.setHeadTitle(smartisanos.R.string.idea_pills_shortcut_key_title);
        mShortcutKeyChooserView.setVisibility(SaraUtils.hasNoPowerKey() ? View.VISIBLE : View.GONE);
        mShortcutKeyChooserView.setAutoCheck(false);
        mShortcutKeyChooserView.setOptionCheckListener(this);
        initPreviewOptionViewDimens(mShortcutKeyChooserView.getOptionView(PreviewOptionsCheckView.LEFT),
                smartisanos.R.dimen.sidekey_shortcut_preview_check_icon_margin_right);
        initPreviewOptionViewDimens(mShortcutKeyChooserView.getOptionView(PreviewOptionsCheckView.RIGHT),
                smartisanos.R.dimen.home_shortcut_preview_check_icon_margin_right);

        //mTodoOver.setOnClickListener(this);
        footerView.findViewById(R.id.video_intro).setOnClickListener(this);
        if (hasIdeapillKeyPhoneType) {
            ViewGroup container = (ViewGroup)headerView.findViewById(R.id.open_mode_content);
            View content = LayoutInflater.from(this).inflate(R.layout.open_bubble_setting_content,container);
            mOpenModeKey = (ListContentItemCheck) headerView.findViewById(R.id.open_mode_key);
            mOpenModeKey.setTitle(R.string.bubble_list_open_mode_key);
            mOpenModeTouch = (ListContentItemCheck) headerView.findViewById(R.id.open_mode_touch);
            mOpenModeTouch.setTitle(R.string.bubble_list_open_touch);
        }

        //default show setting
        ViewGroup default_show = (ViewGroup) headerView.findViewById(R.id.default_show);
        LayoutInflater.from(this).inflate(R.layout.search_bullet_default_content, default_show);
        mDefaultSearch = (ListContentItemCheck) headerView.findViewById(R.id.default_search);
        mDefaultSearch.setTitle(R.string.setting_default_show_search);
        mDefaultBullet = (ListContentItemCheck) headerView.findViewById(R.id.default_bullet);
        mDefaultBullet.setTitle(R.string.setting_default_show_bullet);

        if (SmtPCUtils.isValidExtDisplayId(this)) {
            footerView.findViewById(R.id.voice_content).setVisibility(View.GONE);
            footerView.findViewById(R.id.video_content).setVisibility(View.GONE);
            headerView.findViewById(R.id.open_mode_content).setVisibility(View.GONE);
            default_show.setVisibility(View.GONE);
        }

        mOptionsList.addHeaderView(headerView);
        mOptionsList.addFooterView(footerView);
    }

    private void initPreviewOptionViewDimens(PreviewOptionView preview, int checkIconMarginRight) {
        preview.setCheckImgMarginDimens(
                getResources().getDimensionPixelOffset(smartisanos.R.dimen.preview_check_icon_margin_top),
                getResources().getDimensionPixelOffset(checkIconMarginRight), 0, 0);
        preview.setPreviewImgMarginDimens(
                getResources().getDimensionPixelOffset(smartisanos.R.dimen.preview_widget_icon_margin_top),
                0, 0, 0);
        preview.setPreviewTitleMarginDimens(
                getResources().getDimensionPixelOffset(smartisanos.R.dimen.preview_widget_title_margin_top),
                0, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCurrentColorValue = SaraUtils.getDefaultBubbleColor(this);
        updateViews();
        //updateTodoOverSubTitle();
        mGuideAnimView.setBackgroundByLauncherMode();
    }
    /*
    private void updateTodoOverSubTitle() {
        int type = SaraUtils.getTodoOverType(this);
        int resId = R.string.bubble_todo_over_immediately;
        switch (type) {
            case SaraConstant.VOICE_TODO_OVER_IMMEDIATELY:
                resId = R.string.bubble_todo_over_immediately;
                break;
            case SaraConstant.VOICE_TODO_OVER_DAYLY:
                resId = R.string.bubble_todo_over_dayly;
                break;
            case SaraConstant.VOICE_TODO_OVER_WEEKLY:
                resId = R.string.bubble_todo_over_weekly;
                break;
        }
        mTodoOver.setSubTitle(resId);
    }*/

    private void updateViews() {
        if (SaraUtils.isSettingEnable(this)) {
            mVoiceInputSwitch.setChecked(true);
            changeItemState(true);
        } else {
            mVoiceInputSwitch.setChecked(false);
            changeItemState(false);
        }
        String triggerMethod = null;
        mLeftKeyAsBrightness = Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.VOLUME_KEY, 1) == 1;
        if (SaraUtils.hasNoPowerKey()) {
            triggerMethod = mLeftKeyAsBrightness ? getString(R.string.hold_home_or_left_key) : getString(R.string.hold_home_or_right_key);
        } else if (DeviceType.is(DeviceType.U1)) {
            triggerMethod = mLeftKeyAsBrightness ? getString(R.string.volume_key_left) : getString(R.string.volume_key_right);
        } else if (hasIdeapillKeyPhoneType) {
            triggerMethod = getString(smartisanos.R.string.hold_key_of, getString(R.string.idea_pills_key));
        } else {
            triggerMethod = getString(R.string.long_press_home_key);
        }
        if (SmtPCUtils.isValidExtDisplayId(this)) {
            mUsageTipsView.setText(R.string.rev_idea_pills_usage_tip);
        } else {
            String usageTips = getString(R.string.idea_pills_usage_tips, triggerMethod);
            StringBuilder usageTipsBuilder = new StringBuilder(usageTips);
            usageTipsBuilder.append(SaraUtils.isChineseLocale() ? "。" : " ");
            if (!mIsVoiceInputEnabled) {
                usageTipsBuilder.append(getString(R.string.idea_pills_usage_video_tips));
            } else {
            /*boolean isShortcutKeyCallIdeapills = hasIdeapillKeyPhoneType;
            isShortcutKeyCallIdeapills = isShortcutKeyCallIdeapills && Settings.Global.getInt(getContentResolver(),
                    SettingsSmt.Global.SMART_KEY_ENABLED, 1) == 1;
            isShortcutKeyCallIdeapills = isShortcutKeyCallIdeapills &&
                    SettingsSmt.SHORTCUT_KEY_VALUE.IDEA_PILLS_LIST.equals(
                            SettingsSmt.SHORTCUT_KEY_VALUE.getShortcutSetting(getContentResolver(), SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION));
            if(isShortcutKeyCallIdeapills) {
                usageTipsBuilder.append(getString(R.string.click_idealpill_slide_body));
            } else {
                usageTipsBuilder.append(getString(R.string.left_slide_body));
            }
            mLeftSlideGuide.setShowHandAnim(!isShortcutKeyCallIdeapills);*/
                usageTipsBuilder.append(getString(hasIdeapillKeyPhoneType ? R.string.bubble_list_open_mode_tip : R.string.left_slide_body));
            }
            mUsageTipsView.setText(usageTipsBuilder);
        }

        if (mLeftKeyAsBrightness) {
            mShortcutKeyChooserView.bindPreviewOptionView(PreviewOptionsCheckView.LEFT, smartisanos.R.drawable
                            .idea_pills_shortcut_left_key,
                    getString(smartisanos.R.string.hold_key_of, getString(R.string.volume_key_left)));
        } else {
            mShortcutKeyChooserView.bindPreviewOptionView(PreviewOptionsCheckView.LEFT, smartisanos.R.drawable
                            .idea_pills_shortcut_right_key,
                    getString(smartisanos.R.string.hold_key_of, getString(R.string.volume_key_right)));
        }
        mShortcutKeyChooserView.bindPreviewOptionView(PreviewOptionsCheckView.RIGHT, smartisanos.R.drawable
                .idea_pills_shortcut_home_key, R.string.long_press_home_key);
        String longClickHomeFunction = Settings.Global.getString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME);
        boolean isHomeTriggerVAEnabled = SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS.equals(longClickHomeFunction);
        mShortcutKeyChooserView.setCheck(isHomeTriggerVAEnabled ? PreviewOptionsCheckView.RIGHT : PreviewOptionsCheckView.LEFT);

        mSubHeadSettingsView.setVisibility(mIsVoiceInputEnabled ? View.VISIBLE : View.GONE);
        mSubFooterSettingsView.setVisibility(mIsVoiceInputEnabled ? View.VISIBLE : View.GONE);
        mGuideContainer.setVisibility(mIsVoiceInputEnabled ? View.VISIBLE : View.GONE);
        if (mIsVoiceInputEnabled) {
            mGuideAnimView.show();
        } else {
            mGuideAnimView.hide();
        }
        mVoiceInputSwitch.setBackgroundStyle(mIsVoiceInputEnabled ? ListContentItem.BG_STYLE_TOP : ListContentItem.BG_STYLE_SINGLE);
        mLeftSlideLunchGloblePill.setChecked(SaraUtils.getLeftSlideLunchGloblePillEnabled(this));
        mVoiceWebSwitch.setChecked(SaraUtils.getWebInputEnabled(this));
        mVoiceLocalSwitch.setChecked(SaraUtils.getLocalInputEnabled(this));
        mVoiceInputSwitch.setOnCheckedChangeListener(this);
        mLeftSlideLunchGloblePill.setOnCheckedChangeListener(this);
        mVoiceWebSwitch.setOnCheckedChangeListener(this);
        mVoiceLocalSwitch.setOnCheckedChangeListener(this);

        if (mIsVoiceInputEnabled) {
            initData();
        } else {
            mVoiceItemList.clear();
        }
        mAdapter.notifyDataSetChanged();
        if (!hasIdeapillKeyPhoneType && SaraUtils.leftSlideGuideEnable(this) && (mGuideView == null || mGuideView.getParent()== null)) {
            NotificationManager mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNm.cancel(SaraConstant.SMS_LEFT_SLIDE_PILLS_NOTIFICATION_ID);
            mGuideView = new LeftSlideGuideView(this);
            RelativeLayout parentLayout = (RelativeLayout) mOptionsList.getParent();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            mGuideView.setLayoutParams(params);
            parentLayout.addView(mGuideView);
            SaraUtils.setLeftSlideLunchGloblePillEnabled(this, 1);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_UP) {
            if (mGuideView != null && mGuideView.getParent() != null && !mGuideView.clickCloseBtn(ev)) {
                return super.dispatchTouchEvent(ev);
            }
        }
        if (mGuideView != null && mGuideView.getParent() != null) {
            return false;
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mLeftSlideLunchGloblePill.setOnCheckedChangeListener(null);
        mVoiceInputSwitch.setOnCheckedChangeListener(null);
        mVoiceWebSwitch.setOnCheckedChangeListener(null);
        mVoiceLocalSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGuideAnimView.hide();
        unregisterReceiver(mReceiver);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /*case R.id.todo_over:
                Intent todoIntent = new Intent(this, TodoOverActivity.class);
                startActivity(todoIntent);
                break;*/
            case R.id.video_intro:
                if (!ClickUtil.isFastClick()) {
                    playUsageVideo();
                }
                break;
            case R.id.next_step:
                showWarningDialog(DIALOG_BETA_WARNING);
                break;
            case R.id.open_mode_key:
                onSmartKeyFuncChange();
                break;
            case R.id.open_mode_touch:
                onCheckedChange(SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS,SaraConstant.OPEN_MODE_ENABLE);
                break;
            case R.id.default_search:
                updateDefaultShowView(SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH, SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.MANUAL);
                break;
            case R.id.default_bullet:
                updateDefaultShowView(SettingsSmt.VOICE_SHOW_RESULT_VALUE.BULLET, SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.MANUAL);
                break;
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mVoiceInputSwitch.getSwitch()) {
            if (isChecked) {
                if (SaraUtils.isLowConfigDevice()) {
                    showWarningDialog(DIALOG_MEMORY_DIALOG_TIPS);
                } else if (DeviceType.is(DeviceType.OSCAR) && !SharePrefUtil.getBoolean(RealSettingActivity.this, SharePrefUtil.KEY_OSCAR_LOWMEMORY_WARNED, false)) {
                    showWarningDialog(DIALOG_MEMORY_DIALOG_TIPS);
                } else {
                    showVedioOrShortcutDialogIfNeeded();
                }
            } else {
                enableVoiceAssist(isChecked);
                updateViews();
            }
        } else if (buttonView == mVoiceWebSwitch.getSwitch()) {
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_WEB, isChecked ? 1 : 0);
            updateViews();
        } else if (buttonView == mVoiceLocalSwitch.getSwitch()) {
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_LOCAL, isChecked ? 1 : 0);
        } else if (buttonView == mLeftSlideLunchGloblePill.getSwitch()){
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, isChecked ? 1 : 0);
        }
    }

    private void showVedioOrShortcutDialogIfNeeded() {
        if (SmtPCUtils.isValidExtDisplayId(this)) {
            enableVoiceAssist(true);
            updateViews();
        } else {
            boolean videoPlayed = SharePrefUtil.getBoolean(this, SharePrefUtil.KEY_USAGE_VIDEO_PLAYED, false);
            if (!videoPlayed) {
                showWarningDialog(DIALOG_PLAY_USAGE_VIDEO);
            } else if (SaraUtils.hasNoPowerKey()) {
                showWarningDialog(DIALOG_SHORTCUT_KEY_TIPS);
            } else {
                enableVoiceAssist(true);
                updateViews();
            }
        }
    }
    private void enableVoiceAssist(boolean isChecked) {
        ContentResolver cr = getContentResolver();
        Settings.Global.putInt(cr, SettingsSmt.Global.VOICE_INPUT, isChecked ? 1 : 0);
        String longClickHomeFunction = Settings.Global.getString(cr, SettingsSmt.Global.LONG_CLICK_HOME);
        boolean isHomeTriggerVAEnabled = SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS.equals(longClickHomeFunction);
        if (isChecked) {
            Settings.Global.putInt(cr, VOICE_ASSIST_BETA_ENABLE, 1);
            if (hasIdeapillKeyPhoneType) {
                String currentFunc = Settings.Global.getString(getContentResolver(), SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION);
                if (TextUtils.isEmpty(currentFunc) && SharePrefUtil.getBoolean(this, SaraConstant.PREF_OPEN_MODE_KEY_IS_CHECKED_BEFORE_CLOSING, mOpenModeKey.isChecked())) {
                    enableSmartKeyAsIdeaPills();
                } else {
                    int smartKeyEnable = Settings.Global.getInt(cr, SettingsSmt.Global.SMART_KEY_ENABLED, SaraConstant.OPEN_MODE_ENABLE);
                    boolean keymodeOn = SettingsSmt.SHORTCUT_KEY_VALUE.IDEA_PILLS_LIST.equals(currentFunc) && smartKeyEnable == SaraConstant.OPEN_MODE_ENABLE;
                    boolean touchmodeOn = Settings.Global.getInt(cr, SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, SaraConstant.OPEN_MODE_ENABLE)
                            == SaraConstant.OPEN_MODE_ENABLE;
                    if (!keymodeOn && !touchmodeOn) {
                        Settings.Global.putInt(cr, SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, SaraConstant.OPEN_MODE_ENABLE);
                    }
                }
            }
            updateOpenModeView();
            if (SaraUtils.hasNoPowerKey()) {
                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_BRIGHTNESS_KEY,
                        SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS);
                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME,
                        SettingsSmt.LONG_CLICK_TYPE.POWER);
            } else if (DeviceType.is(DeviceType.U1)) {
                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_BRIGHTNESS_KEY,
                        SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS);
            } else if (!SaraUtils.isVirtualHomeKey()) {
                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME,
                        SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS);
            }
            Intent intent = getIntent();
            CharSequence temp = intent.getCharSequenceExtra("EXTRA_SHARE_TEXT");
            if (!TextUtils.isEmpty(temp)) {
                BubbleManager.addOutText2BubbleList(this, temp.toString());
                intent.putExtra("EXTRA_SHARE_TEXT", "");
                setIntent(intent);
            }
        } else {
            if (hasIdeapillKeyPhoneType){
                SharePrefUtil.savePref(this, SaraConstant.PREF_OPEN_MODE_KEY_IS_CHECKED_BEFORE_CLOSING, mOpenModeKey.isChecked());
            }
            Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_BRIGHTNESS_KEY,
                    null);
            if (SaraUtils.hasNoPowerKey()) {
                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME,
                        SettingsSmt.LONG_CLICK_TYPE.POWER);
                if (isHomeTriggerVAEnabled) {
                    applyBrightnessFunction(false);
                }
            } else if (isHomeTriggerVAEnabled) {
                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME, null);
            }
        }
        changeItemState(isChecked);
    }

    public void changeItemState(boolean enabled) {
        mIsVoiceInputEnabled = enabled;
        mLeftSlideLunchGloblePill.setEnabled(enabled);
        mVoiceLocalSwitch.setEnabled(enabled);
        mVoiceWebSwitch.setEnabled(enabled);
    }

    private class VoiceInputItem {
        public int category;
        public String title;
        public Drawable icon;
        public int settingsValue;
        public View titleView;
    }

    class OptionsAdapter extends BaseAdapter implements ListAdapter, SectionIndexer {

        private Context mContext;
        private LayoutInflater mLayoutInflater;

        public OptionsAdapter(Context context) {
            mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mVoiceItemList.size();
        }

        @Override
        public VoiceInputItem getItem(int position) {
            return mVoiceItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VoiceOptionHolder optionHolder = null;
            if (convertView == null || convertView.getTag() == null) {
                convertView = mLayoutInflater.inflate(R.layout.setting_item_layout, null);
                optionHolder = new VoiceOptionHolder();
                optionHolder.itemCheck = (ListContentItemCheck)convertView.findViewById(R.id.item_check);
                convertView.setTag(optionHolder);
            } else {
                optionHolder = (VoiceOptionHolder) convertView.getTag();
            }

            VoiceInputItem item = mVoiceItemList.get(position);
            ListContentItemCheck itemCheck = optionHolder.itemCheck;
            itemCheck.setIcon(item.icon);
            itemCheck.setTitle(item.title);
            itemCheck.getIconView().setPadding(0, 0, 0/*mCheckIconPaddingRight*/, 0);
            if (item.category == CATEGORY_COLOR){
                itemCheck.setChecked(item.settingsValue == mCurrentColorValue);
            }
            if (isFirstOfSection(position)) {
                itemCheck.setBackgroundStyle(ListContentItem.BG_STYLE_TOP);
            } else if (isLastOfSection(position)) {
                itemCheck.setBackgroundStyle(ListContentItem.BG_STYLE_BOTTOM);
            } else {
                itemCheck.setBackgroundStyle(ListContentItem.BG_STYLE_MIDDLE);
            }

            return convertView;
        }

        private boolean isFirstOfSection(int position) {
            int section = getSectionForPosition(position);
            int sectionPos = getPositionForSection(section);
            return position == sectionPos;
        }

        private boolean isLastOfSection(int position) {
            int section = getSectionForPosition(position);
            int nexSectionPos = getPositionForSection(section + 1);
            return position + 1 == nexSectionPos || position + 1 == getCount();
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < mVoiceItemList.size(); i++) {
                if (mVoiceItemList.get(i).category == sectionIndex) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mVoiceItemList.get(position).category;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        private class VoiceOptionHolder {
            public ListContentItemCheck itemCheck;
        }
    }

    @Override
    protected void onStop() {
        SaraTracker.trackVoiceAssistSettings(this);
        super.onStop();
    }

    private Drawable getAppIcon() {
        PackageManager pm = getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return packageInfo.applicationInfo.loadIcon(pm);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showWarningDialog(final int dialogId) {
        if (mWarningDialog != null && mWarningDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_tips)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mWarningDialog = null;
                    }
                });
        int msgId = 0;
        int customViewId = 0;
        String sideKey = mLeftKeyAsBrightness ? getString(R.string.volume_key_left) : getString(R.string.volume_key_right);
        View dialogView = LayoutInflater.from(this).inflate(smartisanos.R.layout.idea_pills_shortcut_dialog_layout, null);
        TextView msgView = (TextView) dialogView.findViewById(smartisanos.R.id.id_message);
        ImageView introImage = (ImageView) dialogView.findViewById(smartisanos.R.id.id_intro_image);

        switch (dialogId) {
            case DIALOG_PLAY_USAGE_VIDEO:
                msgId = R.string.dlg_msg_play_usage_video;
                builder.setNegativeButton(R.string.see_video_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (SaraUtils.hasNoPowerKey()) {
                            if (mWarningDialog != null){
                                mWarningDialog.dismiss();
                                mWarningDialog = null;
                            }
                            showWarningDialog(DIALOG_SHORTCUT_KEY_TIPS);
                        } else {
                            enableVoiceAssist(true);
                            updateViews();
                        }
                    }
                });
                builder.setPositiveButton(R.string.play_video_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableVoiceAssist(true);
                        ToastUtil.showToast(R.string.global_idea_pills_enable);
                        playUsageVideo();
                    }
                });
                break;

            case DIALOG_SHORTCUT_KEY_TIPS:
                String shortcutTips = getString(R.string.enable_idea_pills_dialog_msg, sideKey, sideKey);
                msgView.setText(shortcutTips);
                introImage.setImageResource(mLeftKeyAsBrightness ? smartisanos.R.drawable.tip_ideapills_left : smartisanos.R.drawable.tip_ideapills_right);
                builder.setTitle(R.string.enable_idea_pills_dialog_title)
                        .setView(dialogView)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mVoiceInputSwitch.setChecked(false);
                            }
                        })
                        .setPositiveButton(R.string.confirm_title, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                enableVoiceAssist(true);
                                updateViews();
                            }
                        });
                break;

            case DIALOG_SHORTCUT_AS_SIDEKEY:
                String sideKeyMsg = getString(smartisanos.R.string.idea_pills_shortcut_as_sidekey_msg, sideKey,
                        sideKey);
                msgView.setText(sideKeyMsg);
                introImage.setImageResource(mLeftKeyAsBrightness ? smartisanos.R.drawable.tip_ideapills_left : smartisanos.R.drawable.tip_ideapills_right);
                builder.setView(dialogView)
                        .setPositiveButton(R.string.confirm_title, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_BRIGHTNESS_KEY,
                                        SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS);
                                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME,
                                        SettingsSmt.LONG_CLICK_TYPE.POWER);
                                applyBrightnessFunction(false);
                                updateViews();
                            }
                        });
                break;

            case DIALOG_SHORTCUT_AS_HOME:
                String message = getString(smartisanos.R.string.idea_pills_shortcut_as_home_msg, sideKey);
                msgView.setText(message);
                introImage.setImageResource(smartisanos.R.drawable.tip_ideapills_home);
                builder.setView(dialogView)
                        .setPositiveButton(R.string.confirm_title, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_BRIGHTNESS_KEY,
                                        SettingsSmt.LONG_CLICK_TYPE.POWER);
                                Settings.Global.putString(getContentResolver(), SettingsSmt.Global.LONG_CLICK_HOME,
                                        SettingsSmt.LONG_CLICK_TYPE.IDEA_PILLS);
                                applyBrightnessFunction(true);
                                updateViews();
                            }
                        });
                break;

            case DIALOG_BETA_WARNING:
                customViewId = R.layout.beta_dialog_warning_layout;
                builder.setPositiveButton(R.string.enable_btn_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.Global.putInt(RealSettingActivity.this.getContentResolver(), VOICE_ASSIST_BETA_ENABLE, 1);
                        SaraUtils.setLeftSlideLunchGloblePillEnabled(RealSettingActivity.this, 1);
                        updateBetaViews(true);
                    }
                });
                break;
            case DIALOG_MEMORY_DIALOG_TIPS:
                msgId = R.string.idea_pills_memory_dialog_content;
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mVoiceInputSwitch != null) {
                            mVoiceInputSwitch.setChecked(false);
                        }
                    }
                });
                builder.setPositiveButton(R.string.enable_btn_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mWarningDialog != null){
                            mWarningDialog.dismiss();
                        }
                        if (DeviceType.is(DeviceType.OSCAR)) {
                            SharePrefUtil.savePref(RealSettingActivity.this, SharePrefUtil.KEY_OSCAR_LOWMEMORY_WARNED, true);
                        }
                        showVedioOrShortcutDialogIfNeeded();
                    }
                });
                break;
            default:
                return;
        }
        if (msgId > 0) {
            builder.setMessage(msgId);
        }
        if (customViewId > 0) {
            builder.setView(mInfalter.inflate(customViewId, null));
        }
        mWarningDialog = builder.create();
        mWarningDialog.show();
    }

    private void applyBrightnessFunction(boolean homeLaunchIdeaPills) {
        if (homeLaunchIdeaPills) {
            //save
            int lastValue = Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.BRIGHTNESS_KEY_FUNCTION,
                    SettingsSmt.BRIGHTNESS_KEY_FUNCTION_VALUE.BRIGHTNESS_ADJUSTMENT);
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.LAST_BRIGHTNESS_KEY_FUNCTION,
                    lastValue);
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.BRIGHTNESS_KEY_FUNCTION,
                    SettingsSmt.BRIGHTNESS_KEY_FUNCTION_VALUE.LIGHTEN_DARKEN_SCREEN);
        } else {
            //restore only if last is home trigger
            int lastValue = Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.LAST_BRIGHTNESS_KEY_FUNCTION,
                    SettingsSmt.BRIGHTNESS_KEY_FUNCTION_VALUE.BRIGHTNESS_ADJUSTMENT);
            Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.BRIGHTNESS_KEY_FUNCTION,
                    lastValue);
        }

    }

    private void playUsageVideo() {
        try {
            Intent intent = new Intent();
            ComponentName componentName = new ComponentName(SaraConstant.PACKAGE_NAME_HANDBOOK, SaraConstant.CLASS_NAME_HANDBOOK);
            intent.setComponent(componentName);
            intent.putExtra(SaraConstant.VIDEO_NAME_KEY, SaraConstant.PARAMS_PILLS_VIDEO);
            startActivityForResult(intent, REQUEST_CODE_PLAY_VIDEO);
        } catch (Exception e) {
            LogUtils.e("play video fail");
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.d("resultCode " + resultCode);
        if (requestCode == REQUEST_CODE_PLAY_VIDEO && resultCode == RESULT_OK) {
            SharePrefUtil.savePref(this, SharePrefUtil.KEY_USAGE_VIDEO_PLAYED, true);
        }
    }
    @Override
    public void onOptionChecked(PreviewOptionsCheckView optionsCheckView, int index) {
        switch (index) {
            case PreviewOptionsCheckView.LEFT:
                showWarningDialog(DIALOG_SHORTCUT_AS_SIDEKEY);
                break;

            case PreviewOptionsCheckView.RIGHT:
                showWarningDialog(DIALOG_SHORTCUT_AS_HOME);
                break;
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (SaraUtils.isKillSelf(this)) {
            SaraUtils.killSelf(this);
        }
    }

    public void handleClick(View view) {
        switch (view.getId()) {
            case R.id.app_drawer:
                Intent appDrawerIntent = new Intent(this, DrawerSettingActivity.class);
                startActivity(appDrawerIntent);
                break;
            case R.id.bubble_share:
                Intent shareintent = new Intent();
                shareintent.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), new int[]{
                        smartisanos.R.anim.slide_in_from_left, smartisanos.R.anim.slide_out_to_right});
                shareintent.setComponent(new ComponentName("com.smartisanos.ideapills", "com.smartisanos.ideapills.sync.share.ShareMainActivity"));
                startActivity(shareintent);
                break;
            case R.id.handled:
                Intent handleIntent = new Intent(this, RecycleBinActivity.class);
                handleIntent.putExtra(Title.EXTRA_BACK_BTN_RES_ID, R.string.handled);
                handleIntent.putExtra(SaraConstant.BUBBLE_DIRECTION, SaraConstant.HANDLED_ACTIVITY);
                startActivity(handleIntent);
                SaraTracker.onEvent("A130005", "id", 3001);
                break;
            case R.id.handled_ok:
                Intent handleOkIntent = new Intent(this, RecycleBinActivity.class);
                handleOkIntent.putExtra(Title.EXTRA_BACK_BTN_RES_ID, R.string.handled_ok);
                handleOkIntent.putExtra(SaraConstant.BUBBLE_DIRECTION, SaraConstant.HANDLED_OK_ACTIVITY);
                startActivity(handleOkIntent);
                break;
            case R.id.recycle_bin:
                Intent intent = new Intent(this, RecycleBinActivity.class);
                intent.putExtra(Title.EXTRA_BACK_BTN_RES_ID, R.string.recycle_bin);
                intent.putExtra(SaraConstant.BUBBLE_DIRECTION, SaraConstant.RECYCLE_BIN_ACTIVITY);
                startActivity(intent);
                SaraTracker.onEvent("A420003");
                SaraTracker.onEvent("A130005", "id", 3002);
                break;
        }
    }
}
