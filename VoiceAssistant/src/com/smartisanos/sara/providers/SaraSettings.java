package com.smartisanos.sara.providers;

import android.net.Uri;

public class SaraSettings {
    static interface BaseColumns {
        public static final String NAME = "name";
        public static final String _ID = "_id";
        public static final String ITEM_TYPE = "type";
        public static final String ITEM_NAME_PINYIN = "titlePinyin";
        public static final Uri CONTENT_URI_CONTACT = Uri.parse("content://"
                + SaraProvider.AUTHORITY + "/" + SaraProvider.TABLE_SARA_CONTACT);
        public static final Uri CONTENT_URI_MUSIC = Uri.parse("content://"
                + SaraProvider.AUTHORITY + "/" + SaraProvider.TABLE_SARA_MUSIC);
        public static final Uri CONTENT_URI_APPLICATION = Uri.parse("content://"
                + SaraProvider.AUTHORITY + "/" + SaraProvider.TABLE_SARA_APPLICATION);
        public static final Uri CONTENT_URI_CONTACT_VERSION = Uri.parse("content://"
                + SaraProvider.AUTHORITY + "/" + SaraProvider.TABLE_CONTACT_VERSION);
        public static final Uri CONTENT_URI_MUSIC_VERSION = Uri.parse("content://"
                + SaraProvider.AUTHORITY + "/" + SaraProvider.TABLE_MUSIC_VERSION);
        public static final int ITEM_TYPE_APPLICATION = 0;
        public static final int ITEM_TYPE_CONTACT = 1;
        public static final int ITEM_TYPE_MUSIC = 2;
    }

    public static final class ApplicationColumns implements BaseColumns {
        public static final String ICON_URI = "icon";
        public static final String START_URI = "uri";
        public static final String ITEM_APPLICATOIN_TYPE = "applicationType";
        public static final String REAL_NAME = "realName";
        public static final String TAG_ALIAS = "alias";
        public static final String TAG_FAVORITES = "favorites";
        public static final String INDEX = "appIndex";
        public static final int ITEM_TYPE_APP = 0;
        public static final int ITEM_TYPE_ALIAS = 1;
        public static final int ITEM_TYPE_WHITE_LIST = 2;

        public static final String[] PROJECTION_APP_NAME_MAP = new String[] {NAME, ITEM_NAME_PINYIN ,REAL_NAME};
        public static final String[] PROJECTION_LAST_APP = new String[] { NAME };
        public static final String[] PROJECTION_APP_STRUCT_LIST = new String[] {NAME, ICON_URI, START_URI, INDEX};
        public static final String[] PROJECTION_REAL_NAME = new String[] { REAL_NAME };
    }

    public static final class MusicColumns implements BaseColumns {
        public static final String TITLE_NAME = "titleName";
        public static final String ALBUM_NAME = "albumName";
        public static final String ARTIST_NAME = "artistName";
        public static final String TITLE_PINYIN = "titlePinyin";
        public static final String ALBUM_PINYIN = "albumPinyin";
        public static final String ARTIST_PINYIN = "artistPinyin";
        public static final String MODIFY_TIME = "column_1";
        public static final String VERSION= "version";
        public static final String[] PROJECTION_MUSIC_NAME_MAP = new String[] {TITLE_NAME,ALBUM_NAME,ARTIST_NAME
                ,TITLE_PINYIN,ALBUM_PINYIN,ARTIST_PINYIN};
        public static final String[] PROJECTION_MUSIC_LAST_LIST = new String[] { TITLE_NAME,ALBUM_NAME,ARTIST_NAME};
        public static final String MEDIA_ID = "media_id";
        public static final String[] PROJECTION_MEDIAID_VERSION_SARA = new String[] { MEDIA_ID, VERSION};
    }

    public static final class ContactColumns implements BaseColumns {
        public static final String CONTACT_TYPE = "contactType";
        public static final String CONTACT_ID = "contactId";
        public static final String PHOTE_ID = "photoId";
        public static final String NUMBER = "number";
        public static final String LABEL = "label";
        public static final String DATA_ID = "dataId";
        public static final String MIMETYPE = "mimeType";
        public static final String NUMBER_LOCATION_INFO = "numberLocationInfo";
        public static final String VERSION = "version";
        public static final String DISPLAY_NAME = "display_name";
        public static final String CONTACT_ID_VERSION = "contact_id";

        public static final int ITEM_TYPE_SINGLE = 0;
        public static final int ITEM_TYPE_FIRST = 1;
        public static final int ITEM_TYPE_OTHER = 2;
        public static final String[] PROJECTION_CONTACT = new String[] {
                NAME, CONTACT_ID, PHOTE_ID, NUMBER, LABEL, DATA_ID,
                MIMETYPE, NUMBER_LOCATION_INFO };
        public static final String[] PROJECTION_LAST_CONTACT = new String[] { NAME };
        public static final String[] PROJECTION_CONTACTID_VERSION_SARA = new String[] { CONTACT_ID,VERSION };
        public static final String[] PROJECTION_CONTACTID_VERSION_CONTACTS = new String[] { CONTACT_ID_VERSION,DISPLAY_NAME,VERSION };
    }
    public static final class GlobleBubbleColumns {
        public static final String _ID = "_id";
        public static final String BUBBLE_ID = "bubbleId";
        public static final String BUBBLE_TEXT = "bubbleText";
        public static final String BUBBLE_PATH = "bubblePath";
        public static final String BUBBLE_ON_LINE = "onLine";
        public static final String BUBBLE_DELETE_TIME = "deleteTime";
        public static final String BUBBLE_TYPE = "bubbleType";
        public static final String BUBBLE_COLOR = "color";
        public static final String BUBBLE_TODO = "todo";
        public static final String BUBBLE_TIMESTAMP = "timeStamp";
    }
}
