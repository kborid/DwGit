package com.smartisanos.voice.util;

import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
public class VoiceConstant {
    /*
     * action : this action means music.album art changed
     */
    public static final String ACTION_MUSIC_ALBUME_ART_CHANGE= "com.smartisanos.music.album_art_changed";

    /*
     * action : this action means start headset mode
     */
    public static final String COM_START_HEADSET_MODE = "com.start.headset.mode";

    /*
     * action : this action means start voice service
     */
    public static final String LAUNCH_VOICE_SERVICE_ACTION = "android.intent.action.LaunchVoiceService";

    /*
     * action : this action means clear package data
     */
    public static final String ACTION_PACKAGE_DATA_CLEARED= "com.smartisanos.intent.action.ClearPakageData";
    /*
     * action : this action means record error
     */
    public static final String ACTION_RECORD_ERROR = "intent.action.audio.record.error";

    /*
     * action : this action means finish tts
     */
    public static final String ACTION_TTS_FINISH = "com.finish.tts";

    /*
     * action : this action means image load cache change
     */
    public static final String IMAGE_LOADER_CACHE_CHANGE = "com.smartisanos.sara.image_loader_cache_change";

    /*
     * uri : contact data uri
     */
    public static final String URI_CONTACT_DATA = "content://com.android.contacts/data/";

    /*
     * uri : iflytek speechcloud uri
     */
    public static final String URI_IFLYTEK_SPEECHCLOUD = "package:com.iflytek.speechsuite";

    /*
     * packageName: sara package name
     */
    public static final String SARA_PACKAGE_NAME = "com.smartisanos.sara";

    /*
     * mimetype: tencent mm mimetype
     */
    public static final String WEIXIN_MIMETYPE = "vnd.android.cursor.item/vnd.com.tencent.mm.chatting.profile";

    /*
     * className: tencent mm class name
     */
    public static final String WEIXIN_CLASS_NAME = "com.tencent.mm.plugin.accountsync.ui.ContactsSyncUI";

    public static String REGEX_PUNCTUATION = "[,.!?;:，。！？；：、%／/]";
    /*
     * regex: filter double byte
     */
    public static final String REGEX_SPECIAL = "[`~!@#$%^&*()+=|{}':;',//[//].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？°]";
    /*
     * regex: filter double byte for yellow page
     */
    public static final String REGEX_SPECIAL2 ="[|)(`~!@#$%^&*+={}':;',//[//].<>/?~！@#￥%……&*——+{}【】‘；：”“’。，、？°-]";
    public static final String YELLOW_DATA ="yellow.txt";
    /*
     * regex: filter out not normal characters
     */
    public static final String REGEX_NOT_NORMAL = "[^(\\u4e00-\\u9fa5)a-zA-Z]|穦";
    public static final String REGEX_NOT_CHINESE = "[^(\\u4e00-\\u9fa5)]|穦";
    public static final String REGEX_PINYIN = "[^a-zA-Z]";

    /*
     * regex: filter single byte
     */
    public static final String REGEX_SEPARATOR = "\\—|\\→| |\\+|\\.|\\-|\\(|\\)|\\!|\\;|\\《|\\》|\\（|\\)";

    /*
     * custom: alias custom
     */
    public static final String ALIAS_FILE = "alias.txt";

    /*
     * custom: applicaion whitelist custom
     */
    public static final String APP_WHITELIST_FILE = "appwhitelist.txt";

    /*
     * custom: music whitelist custom
     */
    public static final String MUSIC_WHITELIST_FILE = "musicwhitelist.txt";

    /*
     * path: music pic uri
     */
    public static final String MUSIC_PIC_URI= "content://media/external/audio/media/";
    /*
     * path: music pic path
     */
    public static final String MUSIC_PIC_PATH = "/albumart";
    public static final String SMARTISAN_MUSIC_DATA_URI = "content://com.smartisanos.music.data.provider/audio";
    public static final String SMARTISAN_MUSIC_ALL_URI = "content://com.smartisanos.music.SearchSuggestionProvider/query_all_track_from_third_app";
    public static final String SMARTISAN_MUSIC_ALBUM_URI = "content://com.smartisanos.music.SearchSuggestionProvider/query_album_track_from_third_app";
    public static final String SMARTISAN_MUSIC_ARTIST_URI = "content://com.smartisanos.music.SearchSuggestionProvider/query_artist_track_from_third_app";
    // key : the key name of sharedPreferences
    public static final String KEY_CONTACT_UPDATE_TIMESTAMP = "contact_update_timestamp";
    public static final String KEY_CONTACT = "contactData";
    public static final String PERMISSION_TIP = "permission";
    public static final String KEY_APP = "appData";
    public static final String KEY_MUSIC = "musicData";
    public static final String KEY_TAB_INDEX = "show_tab_at";
    public static final String PREF_KEY_OLD_TIME = "old_time";
    public static final String PREF_KEY_RECOGNIZE_NUM = "recognize_num";
    public static final String PREF_KEY_STATE= "state";
    public static final String PREF_HEADSET_LAUNCH_TIME = "headset_launch_time";
    public static final String PREF_SYSTEM_VERSION = "system_version";
    public static final String DEFAULT_SELECT_LANGUAGE = "domain=iat,language=zh_cn,accent=mandarin";
    /*
     * action : this action for update date
     */
    public static final String ACTION_UPDATE_DATE = "smartisan.intent.action.update_date";
    public static final String PREF_KEY_CURRENT_DAY= "currentDay";
    public static final long INIT_TIME_SLOP = 200;
    public static final String PACKAGE_NAME_CONTACT = "com.android.contacts";
    public static final String PACKAGE_NAME_SEARCH = "com.smartisanos.quicksearch";
    public static final String PACKAGE_NAME_MUSIC = "com.smartisanos.music";
    public static final String PACKAGE_NAME_SARA = "com.smartisanos.sara";
    public static final String PACKAGE_NAME_SARA_BULLET = "com.smartisanos.sara:bullet";
    public static final String PACKAGE_NAME_BULLET = "com.bullet.messenger";
    public static final String PACKAGE_NAME_ANDROID = "android";
    public static final String VOICE_ASSISTANT_EXTRA = "voice_extra";
    public static final String SCHEME_TEL = "tel";
    public static boolean ANDROID50 = android.os.Build.VERSION.SDK_INT > 20;
    public static final String PHONE_PACKAGE_NAME = ANDROID50 ? "com.android.server.telecom" : "com.android.phone";
    public static final String ROOT_DIR = Environment.getExternalStorageDirectory().toString();
    public static final String SPECIAL_PREFIX_MATCH="^IdeaPills_\\d{3}_[a-zA-Z0-9]{6}";

    public final static  String TITLE_TYPE = "title";
    public final static  String ARTIST_TYPE = "artist";
    public final static  String ALBUM_TYPE = "album";
    public static final int DISPLAY_AS_ALBUM = 1;
    public static final int DISPLAY_AS_ARTIST = 2;
    public static final int DISPLAY_AS_MUISC = 3;
    public static final int REPEAT_CONNECT_MAX_NUMS = 3;
    public static final int PLAYMODEL_NONE = 0;
    public static final int PLAYMODEL_REPEAT_ALL = 1;
    public static final int PLAYMODEL_SHUFFLEMODE = 4;
    public static final boolean CTA_ENABLE = SystemProperties.getInt("persist.radio.cta.test.mode", 0) == 1;
    public static final int INTERVAL_SHORT = 10;
    public static final int LOLLIPOP_VERSION = 21;
}
