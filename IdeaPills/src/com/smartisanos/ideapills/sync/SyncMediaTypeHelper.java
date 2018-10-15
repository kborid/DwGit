package com.smartisanos.ideapills.sync;

import java.util.HashMap;

public class SyncMediaTypeHelper {
    // 任意二进制
    public static final int ATTACHMENT_TYPE_BLOB = 0;
    // audio starts from 1
    // 用于生成胶囊的录音特别类型
    public static final int ATTACHMENT_TYPE_VOICE_RECORD = 1;
    // 录音
    public static final int ATTACHMENT_TYPE_RECORD = 2;
    public static final int ATTACHMENT_TYPE_MP3 = 3;
    public static final int ATTACHMENT_TYPE_M4A = 4;
    public static final int ATTACHMENT_TYPE_AAC = 5;
    public static final int ATTACHMENT_TYPE_FLAC = 6;
    public static final int ATTACHMENT_TYPE_MID = 7;
    public static final int ATTACHMENT_TYPE_WAV = 8;
    public static final int ATTACHMENT_TYPE_APE = 9;

    // document starts from 20
    public static final int ATTACHMENT_TYPE_3GP = 20;
    public static final int ATTACHMENT_TYPE_MP4 = 21;
    public static final int ATTACHMENT_TYPE_TS = 22;
    public static final int ATTACHMENT_TYPE_WEBM = 23;
    public static final int ATTACHMENT_TYPE_MKV = 24;

    // pictures starts from 40
    public static final int ATTACHMENT_TYPE_JPG = 41;
    public static final int ATTACHMENT_TYPE_JPEG = 42;
    public static final int ATTACHMENT_TYPE_PNG = 43;
    public static final int ATTACHMENT_TYPE_GIF = 44;
    public static final int ATTACHMENT_TYPE_BMP = 45;
    public static final int ATTACHMENT_TYPE_WEBP = 46;
    public static final int ATTACHMENT_TYPE_TIFF = 47;
    public static final int ATTACHMENT_TYPE_TIF = 48;
    public static final int ATTACHMENT_TYPE_WBMP = 49;

    // document starts from 60
    public static final int ATTACHMENT_TYPE_DOC = 60;
    public static final int ATTACHMENT_TYPE_DOCM = 61;
    public static final int ATTACHMENT_TYPE_DOCX = 62;
    public static final int ATTACHMENT_TYPE_DOT = 63;
    public static final int ATTACHMENT_TYPE_DOTM = 64;
    public static final int ATTACHMENT_TYPE_DOTX = 65;
    public static final int ATTACHMENT_TYPE_KEY = 66;
    public static final int ATTACHMENT_TYPE_NUMBERS = 67;
    public static final int ATTACHMENT_TYPE_PAGES = 68;
    public static final int ATTACHMENT_TYPE_PDF = 69;
    public static final int ATTACHMENT_TYPE_RTF = 70;
    public static final int ATTACHMENT_TYPE_POT = 71;
    public static final int ATTACHMENT_TYPE_POTX = 72;
    public static final int ATTACHMENT_TYPE_PPT = 73;
    public static final int ATTACHMENT_TYPE_PPTM = 74;
    public static final int ATTACHMENT_TYPE_PPTX = 75;
    public static final int ATTACHMENT_TYPE_XLS = 76;
    public static final int ATTACHMENT_TYPE_XLSX = 77;
    public static final int ATTACHMENT_TYPE_XLT = 78;
    public static final int ATTACHMENT_TYPE_XLTX = 79;
    public static final int ATTACHMENT_TYPE_TXT = 80;
    public static final int ATTACHMENT_TYPE_MD = 81;
    public static final int ATTACHMENT_TYPE_ADOC = 82;

    // other starts from 100
    public static final int ATTACHMENT_TYPE_7Z = 100;
    public static final int ATTACHMENT_TYPE_APK = 101;
    public static final int ATTACHMENT_TYPE_RAR = 102;
    public static final int ATTACHMENT_TYPE_ZIP = 103;

    private static final HashMap<String, Integer> sMimeTypeMap
            = new HashMap<String, Integer>();
    private static final HashMap<Integer, String> sFormatToMimeTypeMap
            = new HashMap<Integer, String>();

    static {
        addFileType(ATTACHMENT_TYPE_BLOB, "application/octet-stream");

        addFileType(ATTACHMENT_TYPE_MP3, "audio/mpeg");
        addFileType(ATTACHMENT_TYPE_RECORD, "audio/x-wav");
        addFileType(ATTACHMENT_TYPE_WAV, "audio/x-wav");
        addFileType(ATTACHMENT_TYPE_M4A, "audio/mp4");
        addFileType(ATTACHMENT_TYPE_AAC, "audio/aac");
        addFileType(ATTACHMENT_TYPE_MID, "audio/midi");
        addFileType(ATTACHMENT_TYPE_FLAC, "audio/flac");

        addFileType(ATTACHMENT_TYPE_JPEG, "image/jpeg");
        addFileType(ATTACHMENT_TYPE_JPG, "image/jpeg");
        addFileType(ATTACHMENT_TYPE_GIF, "image/gif");
        addFileType(ATTACHMENT_TYPE_BMP, "image/x-ms-bmp");
        addFileType(ATTACHMENT_TYPE_BMP, "image/bmp");
        addFileType(ATTACHMENT_TYPE_PNG, "image/png");
        addFileType(ATTACHMENT_TYPE_WEBP, "image/webp");
        addFileType(ATTACHMENT_TYPE_WBMP, "image/vnd.wap.wbmp");
        addFileType(ATTACHMENT_TYPE_TIFF, "image/tiff");
        addFileType(ATTACHMENT_TYPE_TIF, "image/tiff");

        addFileType(ATTACHMENT_TYPE_MP4, "video/mp4");
        addFileType(ATTACHMENT_TYPE_MKV, "video/x-matroska");
        addFileType(ATTACHMENT_TYPE_WEBM, "video/webm");
        addFileType(ATTACHMENT_TYPE_TS, "video/mp2ts");
        addFileType(ATTACHMENT_TYPE_3GP, "video/3gpp");

        addFileType(ATTACHMENT_TYPE_DOCM, "application/msword");
        addFileType(ATTACHMENT_TYPE_DOCX, "application/msword");
        addFileType(ATTACHMENT_TYPE_DOT, "application/msword");
        addFileType(ATTACHMENT_TYPE_DOTM, "application/msword");
        addFileType(ATTACHMENT_TYPE_DOTX, "application/msword");
        addFileType(ATTACHMENT_TYPE_DOC, "application/msword");
        addFileType(ATTACHMENT_TYPE_XLSX, "application/vnd.ms-excel");
        addFileType(ATTACHMENT_TYPE_XLT, "application/vnd.ms-excel");
        addFileType(ATTACHMENT_TYPE_XLTX, "application/vnd.ms-excel");
        addFileType(ATTACHMENT_TYPE_XLS, "application/vnd.ms-excel");
        addFileType(ATTACHMENT_TYPE_PPTM, "application/vnd.ms-powerpoint");
        addFileType(ATTACHMENT_TYPE_PPTX, "application/vnd.ms-powerpoint");
        addFileType(ATTACHMENT_TYPE_PPT, "application/vnd.ms-powerpoint");
        addFileType(ATTACHMENT_TYPE_TXT, "text/plain");
        addFileType(ATTACHMENT_TYPE_PDF, "application/pdf");
        addFileType(ATTACHMENT_TYPE_RTF, "application/rtf");
        addFileType(ATTACHMENT_TYPE_KEY, "application/x-iwork-keynote-sffkey");
        addFileType(ATTACHMENT_TYPE_PAGES, "application/x-iwork-pages-sffpages");
        addFileType(ATTACHMENT_TYPE_NUMBERS, "application/x-iwork-numbers-sffnumbers");
        addFileType(ATTACHMENT_TYPE_MD, "text/markdown");

        addFileType(ATTACHMENT_TYPE_APK, "application/vnd.android.package-archive");
        addFileType(ATTACHMENT_TYPE_ZIP, "application/zip");
        addFileType(ATTACHMENT_TYPE_RAR, "application/x-rar-compressed");
    }

    static void addFileType(int fileType, String mimeType) {
        sFormatToMimeTypeMap.put(fileType, mimeType);
        sMimeTypeMap.put(mimeType, fileType);
    }

    static final String attachmentTypeToMimeType(int attachmentType, String fileName) {
        String mimeType = sFormatToMimeTypeMap.get(attachmentType);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        } else if ("text/plain".equals(mimeType) && (fileName != null && fileName.endsWith("eml"))) {
            mimeType = "message/rfc822";
        }
        return mimeType;
    }

    static final int mimeTypeToAttachmentType(String mimeType, String fileName) {
        Integer type = sMimeTypeMap.get(mimeType);
        if (type == null) {
            if ("message/rfc822".equals(mimeType)
                    || (fileName != null && fileName.endsWith("eml"))) {
                type = ATTACHMENT_TYPE_TXT;
            } else {
                type = ATTACHMENT_TYPE_BLOB;
            }
        }
        return type;
    }
}
