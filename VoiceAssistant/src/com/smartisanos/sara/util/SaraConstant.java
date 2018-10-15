package com.smartisanos.sara.util;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.view.WindowManager;
import smartisanos.util.DeviceType;

import com.smartisanos.sara.R;

public class SaraConstant {
    /*
     * osborn : add smart key keycode
     */
    public static final int KEYCODE_SMART = SaraUtils.getSmartKeyCode();
    public static final int TIME_LONG_PRESS_KEYCODE_SMART = 400;

    /*
     * action : this action means music.album art changed
     */
    public static final String ACTION_MUSIC_ALBUME_ART_CHANGE= "com.smartisanos.music.album_art_changed";
    /*
     * action : this action means start menu mode
     */
    public static final String ACTION_MENU_UP= "smartisanos.android.intent.action.MENU_UP";
    /*
    *  action : action start or stop sara from bluetooth
    */
    public static final String ACTION_BLUETOOTH_CALL = "com.smartisanos.sara.bluetooth_call";
    /*
     * action : this action means start headset mode
     */
    public static final String COM_START_HEADSET_MODE = "com.start.headset.mode";

    /*
     * action : this action means start music service
     */
    public static final String ACTION_MUSIC= "com.smartisanos.music.action";

    /*
     * action : this action means clear package data
     */
    public static final String ACTION_PACKAGE_DATA_CLEARED= "com.smartisanos.intent.action.ClearPakageData";

    /*
     * action : this action means play music
     */
    public static final String ACTION_MUSIC_PLAYER = "android.intent.action.MUSIC_PLAYER";

    /*
     * action : this action means view playback
     */
    public static final String ACTION_PLAYBACK_VIEWER = "com.smartisanos.music.PLAYBACK_VIEWER";

    /*
     * action : this action means music artist detail browser
     */
    public static final String ACTION_BROWSER_ARTIST_ALBUM= "com.smartisan.music.BROWSER_ARTIST_ALBUM";
    public static final String ARTIST_ID = "artist_id";
    public static final String ARTIST_KEY = "artist";
    public static final String PARAM = "param";
    /*
     * action : this action means music artist detail browser
     */
    public static final String ACTION_BROWSER_ALBUM= "com.smartisan.music.BROWSER_ALBUM";
    public static final String ALBUM_ID = "album_id";
    public static final String ALBUM_KEY = "album_name";
    /*
     * action : this action delete global bubbles
     */
    public static final String ACTION_DELETE_GLOBAL_BUBBLE = "intent.action.DELETE_GLOBAL_BUBBLE";
    /*
     * action : this action todo_over change
     */
    public static final String ACTION_TODO_CHANGE_GLOBAL_BUBBLE = "intent.action.TODO_CHANGE_GLOBAL_BUBBLE";
    /*
     * action : this action means record error
     */
    public static final String ACTION_RECORD_ERROR = "intent.action.audio.record.error";


    /*
     * action : this action means image load cache change
     */
    public static final String IMAGE_LOADER_CACHE_CHANGE = "com.smartisanos.sara.image_loader_cache_change";

    /*
     * uri : contact data uri
     */
    public static final String URI_CONTACT_DATA = "content://com.android.contacts/data/";

    /*
     * mimetype: tencent mm mimetype
     */
    public static final String WEIXIN_MIMETYPE = "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.profile";

    /*
     * packageName: tencent mm package name
     */
    public static final String WEIXIN_PACKAGE_NAME = "com.tencent.mm";

    public static final String PILLS_PACKAGE_NAME = "com.smartisanos.ideapills";

    /*
     * className: tencent mm class name
     */
    public static final String WEIXIN_CLASS_NAME = "com.tencent.mm.plugin.accountsync.ui.ContactsSyncUI";
    public static final String WEIXIN_CLASS_NAME_1281 = "com.tencent.mm.plugin.account.ui.ContactsSyncUI";

    /*
     * path: music pic uri
     */
    public static final String MUSIC_PIC_URI= "content://media/external/audio/media/";
    /*
     * path: music pic path
     */
    public static final String MUSIC_PIC_PATH = "/albumart";
    public static final String SMARTISAN_MUSIC_ALL_URI = "content://com.smartisanos.music.SearchSuggestionProvider/query_all_track_from_third_app";
    public static final String SMARTISAN_MUSIC_ALBUM_URI = "content://com.smartisanos.music.SearchSuggestionProvider/query_album_track_from_third_app";
    public static final String SMARTISAN_MUSIC_ARTIST_URI = "content://com.smartisanos.music.SearchSuggestionProvider/query_artist_track_from_third_app";
    // key : the key name of sharedPreferences
    public static final String KEY_TAB_INDEX = "show_tab_at";
    public static final String PREF_KEY_OLD_TIME = "old_time";
    public static final String PREF_KEY_RECOGNIZE_NUM = "recognize_num";
    public static final String PREF_HEADSET_LAUNCH_TIME = "headset_launch_time";
    public static final String PREF_SYSTEM_VERSION = "system_version";
    public static final String DEFAULT_SELECT_LANGUAGE = "domain=iat,language=zh_cn,accent=mandarin";
    public static final String PREF_OPEN_MODE_KEY_IS_CHECKED_BEFORE_CLOSING = "open_mode_key_is_checked_before_closing";
    /*
     * action : this action for update date
     */
    public static final String ACTION_UPDATE_DATE = "smartisan.intent.action.update_date";
    public static final String PREF_KEY_CURRENT_DAY= "currentDay";
    public static final long INIT_TIME_SLOP = 200;
    public static final String PACKAGE_NAME_CONTACT = "com.android.contacts";
    public static final String PACKAGE_NAME_SEARCH = "com.smartisanos.quicksearch";
    public static final String PACKAGE_NAME_MUSIC = "com.smartisanos.music";

    /**
     * home address key for quick command.
     */
    public static final String HOME_ADDRESS_KEY = "HomeAddress";

    public static final String SCHEME_TEL = "tel";
    public static boolean ANDROID50 = android.os.Build.VERSION.SDK_INT > 20;
    public static final String PHONE_PACKAGE_NAME = ANDROID50 ? "com.android.server.telecom" : "com.android.phone";
    public static final String ROOT_DIR = Environment.getExternalStorageDirectory().toString();
    public static final int SIZE_100_M = 100 * 1024 * 1024;
    public static final String WAV_FILE = "record.wav";
    public static final String WAVE_FILE = "/record.wave";
    public static final String APP_RECORDER_DATA_PATH = ROOT_DIR + "/smartisan/Recorder/";
    public static final String VOICE_NAME_KEY = "VoiceName";
    public static final String PING_COMMAND="ping -c 3 -w 50 dev.voicecloud.cn";
    public static final String WAVE_FILE_SUFFIX = ".wave";
    public static final String WAV_FILE_SUFFIX =".wav";
    public static final String PCM_FILE_SUFFIX =".pcm";
    public static final String BAK_FILE_SUFFIX =".bak";
    public static final String MP3_FILE_SUFFIX = ".mp3";
    public static final String IDEA_PILL_SUFFIX ="IdeaPills_";
    public static final String SPECIAL_PREFIX_MATCH="^IdeaPills_\\d{3}_[a-zA-Z0-9]{6}";
    public static final String CONTENT_FILE_PATH ="content://com.smartisanos.sara/wav/";
    public static final String ACTION_PCM_RERECOGNIZE_ONLINE= "smartisanos.android.intent.action.RECOGNIZE_OFFLINE";
    public static final long MP3_BAUD_RATE = 160 * 1000 / 8;
    public static final long MSEC_PER_SEC = 1000;
    /*
     * constant : this constant for web search
     */
    public static final String SEARCH_TYPE = "search_type";
    public static final String SEARCH_DICT_KEY = "big_bang_default_dict";
    public static final String FINISH_ON_STOP = "finish_on_stop";
    public static final String SETTINGS_PACKAGE_NAME ="com.android.settings";
    public static final String SETTINGS_VOICEINPUT_CLASS_NAME ="com.android.settings.VoiceInputSettingsActivity";
    public static final String KEY_WAVE_DATA = "wave_data";
    public static final String  KEY_MARK_DATA = "mark_data";
    public static final String  KEY_TOTAL_POINT_NUM = "total_point";
    public static final String MUSIC_PACKAGE = "com.smartisanos.music";
    public static final String META_CHANGED = "com.smartisanos.music.metachanged";
    public static final String PLAYSTATE_CHANGED = "com.smartisanos.music.playstatechanged";

    public final static  String TITLE_TYPE = "title";
    public final static  String ARTIST_TYPE = "artist";
    public final static  String ALBUM_TYPE = "album";
    public static final int DISPLAY_AS_ALBUM = 1;
    public static final int DISPLAY_AS_ARTIST = 2;
    public static final int DISPLAY_AS_MUISC = 3;
    public static final long REPEAT_CONNECT_SERVER_MAX_TIME = 3 * 60 * 60 * 1000;
    public static final int REPEAT_CONNECT_MAX_NUMS = 3;
    public static final int PLAYMODEL_NONE = 0;
    public static final int PLAYMODEL_REPEAT_ALL = 1;
    public static final int PLAYMODEL_SHUFFLEMODE = 4;
    public static final boolean CTA_ENABLE = SystemProperties.getInt("persist.radio.cta.test.mode", 0) == 1;
    public static final String ACTION_REASON_HOME_KEY = "homekey";
    public static final String ACTION_REASON_RECENT = "recentapps";
    public static final String ACTION_FINISH_BUBBLE_ACTIVITY= "smartisanos.android.intent.action.FINISH_BUBBLE_ACTIVITY";
    public static final String ACTION_FINISH_SETTINGS_ACTIVITY= "smartisanos.android.intent.action.FINISH_SETTINGS_ACTIVITY";
    public static final String ACTION_IDEAPILLS_SHARE_INVITATION_SEND = "com.smartisanos.ideapills.share_invitation";
    public static final String ACTION_UPDATE_BUBBLE= "smartisanos.android.intent.action.UPDATE_BUBBLE";
    public static final String TEXTBOOM_PKG_NAME = "com.smartisanos.textboom";
    public static final String EXTRA_OUTER = "extra_outer";
    public static final int INTERVAL_SHORT = 10;
    public static final int INTERVAL_LONG = 20;
    public static final String SECURITY_PACKAGE_NAME = "com.smartisanos.security";
    public static final String SECURITY_FROM_KEY = "from_security_center";
    public static final String ACTION_SHARE_RECORD="smartisan.action.VIEW_VOICE_INPUT_RECORD";
    public static final String RECORDER_PACKAGE_NAME = "com.smartisanos.recorder";
    public static final String SHARE_AUDIO_TYPE ="audio/x-wav";
    public static final String SHARE_AUDIO_KEY="voicebubble";
    public static final String METHOD_UPDATE_BUBBLE = "METHOD_UPDATE_BUBBLE";
    public static final String METHOD_HIDE_BUBBLE_LIST = "METHOD_HIDE_BUBBLE_LIST";
    public static final String METHOD_RESTORE_BUBBLES = "METHOD_RESTORE_BUBBLES";
    public static final String METHOD_RESTORE_LEGACY_BUBBLES = "METHOD_RESTORE_LEGACY_BUBBLES";
    public static final String METHOD_RESTORE_DELETE_BUBBLES  = "METHOD_RESTORE_DELETE_BUBBLES";
    public static final String METHOD_DESTROY_BUBBLES = "METHOD_DESTROY_BUBBLES";
    public static final String METHOD_UPDATE_VOICE_BUBBLE_URI = "METHOD_UPDATE_VOICE_BUBBLE_URI";
    public static final String METHOD_VISIBLE_BUBBLE_COUNT = "METHOD_VISIBLE_BUBBLE_COUNT";
    public static final String METHOD_LIST_BUBBLES = "METHOD_LIST_BUBBLES";
    public static final String METHOD_LIST_BUBBLE_ATTACHMENTS = "METHOD_LIST_BUBBLE_ATTACHMENTS";
    public static final String METHOD_MERGE_VOICE_WAVE_DATA = "METHOD_MERGE_VOICE_WAVE_DATA";
    public static final String METHOD_CAN_BUBBLE_SHARE = "METHOD_CAN_BUBBLE_SHARE";
    public static final String METHOD_SEND_SHARE_INVITE = "METHOD_SEND_SHARE_INVITE";
    public static final String METHOD_LIST_BUBBLES_ATTACHMENTS = "METHOD_LIST_BUBBLES_ATTACHMENTS";

    public static final String LIST_TYPE_REMOVED   = "removed"; //recycle
    public static final String LIST_TYPE_OFFLINE   = "offline"; //offline
    public static final String LIST_TYPE_USED      = "used"; //used
    public static final String LIST_TYPE_LEGACY_USED = "legacy_used"; //legacy_used
    public static final String LIST_TYPE_ALL_VOICE = "all_voice"; // all bubbles

    public static final int MAX_VISIBLE_BUBBLE_COUNT = 500;
    public static final String DESTROY_TYPE_REMOVED = "destroy_removed";
    public static final String DESTROY_TYPE_USED = "destroy_used";
    public static final String DESTROY_TYPE_LEGACY_USED = "destroy_legacy_used";
    public static final String KEY_DESTROY_TYPE = "destroy_type";
    public static final String KEY_BUBBLE_BEFORE = "before";
    public static final String KEY_BUBBLES = "bubbles";
    public static final String KEY_BUBBLE_ATTACHMENTS = "bubble_attachments";
    public static final String KEY_BUBBLES_ATTACHMENTS = "bubbles_attachments";
    public static final String KEY_BUBBLE_COUNT = "count";
    public static final String KEY_BUBBLE_IDS = "bubble_ids";
    public static final String BUBBLE_ID_LIST = "BUBBLE_ID_LIST";
    public static final String BUBBLE_OFF_TO_ONLINE = "BUBBLE_OFF_TO_ONLINE";
    public static final String KEY_IDS = "KEY_IDS" ;
    public static final String KEY_LOC = "KEY_LOC" ;
    public static final String KEY_DELAY = "KEY_DELAY";
    public static final String KEY_PKG = "KEY_PKG";
    public static final String KEY_ANIM = "KEY_ANIM";
    public final static int KEYBOARD_INPUT_MODE = 0;
    public final static int VOICE_INPUT_MODE = 1;
    public final static int AUTO_BACKUP_TODO = 0;
    public final static int MAUNAL_BACKUP_TODO = 1;
    public static final String LUNCH_KEY = "VoiceType";
    public static final String ACTION_CHOOSE_RESULT = "com.action.ACTION_CHOOSE_RESULT";
    public static final String BROWSER_PACKAGE_NAME_SMARTISAN = "com.android.browser";
    public static final int BUBBLE_TEXT_MAX = 5000;
    public static final String SCREEN_OFF_KEY = "ScreenOff";
    public static final String BUBBLE_DIRECTION = "BubbleDirection";
    public static final String PACKAGE_NAME_HANDBOOK = "com.smartisanos.manual";
    public static final String CLASS_NAME_HANDBOOK = "com.smartisanos.manual.activity.StartVideoActivity";
    public static final String VIDEO_NAME_KEY = "video_name";
    public static final String PARAMS_PILLS_VIDEO = "ideapills_video";
    public static final String PACKAGE_NAME_CALENDAR= "com.android.calendar";
    public static final String CLASS_NAME_CALENDAR= "com.android.calendar.event.EditEventActivity";
    public static final String PACKAGE_NAME_WEIXIN= "com.tencent.mm";
    public static final String CLASS_NAME_WEIXIN= "com.tencent.mm.ui.tools.ShareImgUI";
    public static final String PACKAGE_NAME_BULLET = "com.bullet.messenger";
    public static final int SMS_LEFT_SLIDE_PILLS_NOTIFICATION_ID = 240;
    public static final int REQUEST_SELECT_FILE = 100;
    public static final String EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT";

    public static final String ACTION_ATTACH_PICK_RESULT = "ACTION_ATTACH_PICK_RESULT";
    public static final int BUBBLE_WINDOW_TYPE = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
    public static final int RESULT_PICK_ATTACHMENT = 1;
    public static final int RESULT_IMAGE_PREVIEW = 2;
    public static final int RESULT_PICK_REMIND = 4;
    public static final int RESULT_FILE_PREVIEW = 3;
    public static final int RESULT_CHANGE_SHORTCUTAPP = 5;
    public static final String ACTION_FROM_SIDEBAR = "ACTION_FROM_SIDEBAR";
    public static final String ACTION_FROM_SIDEBAR_CONTENT = "ACTION_FROM_SIDEBAR_CONTENT";
    public static final String ACTION_TO_REMIND = "com.smartisan.ideapills.SET_REMIND";
    public static final String ACTION_CLOSE_REMIND = "com.smartisan.ideapills.CLOSE_REMIND";
    public static final String REMIND_TIME_KEY = "remind_time";
    public static final String DUE_DATE_KEY = "due_date";
    public static final String EXTRA_TARGET_INTENT = "com.smartisan.action.VIEW_FILE_FLOATING";
    public static final String VOICE_TODO_OVER_CYCLE_TYPE = "voice_todo_over_cycle_type";
    public static final int OPEN_MODE_ENABLE = 1;
    public static final int OPEN_MODE_DISABLE = 0;
    public static final int VOICE_TODO_OVER_IMMEDIATELY = 0;
    public static final int VOICE_TODO_OVER_DAYLY = 1;
    public static final int VOICE_TODO_OVER_WEEKLY = 2;
    public static String ABBREV_YEAR_MONTH = "yyyy/MM";
    public static String ABBREV_MONTH_NO_YEAR = "yyyy/MM";

    public static final String ACTION_VOICE_BUTTON_RESULT = "ACTION_VOICE_BUTTON_RESULT";
    public static final String EXTRA_VOICE_BUTTON_RESULT = "EXTRA_VOICE_BUTTON_RESULT";

    public static void init(Context context) {
        ABBREV_YEAR_MONTH = context.getString(R.string.abbrev_wday_year_month);
        ABBREV_MONTH_NO_YEAR = context.getString(R.string.abbrev_wday_month_no_year);
    }
    public final static String APP_DRAWER_ENABLE = "app_drawer_enable";

    public static final String CLASS_NAME_IDEAPILL = "com.smartisanos.ideapills";

    public static final String BROWSER_PACKAGE_NAME = "com.android.browser";
    public static final String BROWSER_ACTIVITY_NAME = "com.android.browser.BrowserActivity";
    public static final int VIBRATE_TIME = 50;
    public static final int MAXIMUM_RECORDING_TIME = 60000;

    /**
     * RecycleBinActivity type
     */
    public static final int RECYCLE_BIN_ACTIVITY = 0;
    public static final int HANDLED_ACTIVITY = 1;
    public static final int HANDLED_OK_ACTIVITY = 2;

    public static final String SHORTCUTS_DIRECTION = "ShortcutsDirection";

    public static final String SAVE_SEARCH_TYPE = "save_search_type";

    public static final String HOLD_CURRENT_ACTIVITY = "hold";

    public static final String ACTION_RECEIVE_TEXTBOOM_CALLBACK = "intent.action.receive.textboom.callback";
}
