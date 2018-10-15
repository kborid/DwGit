package com.smartisanos.ideapills.common.util;

import android.text.TextUtils;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.provider.MediaStore;
import android.service.onestep.GlobalBubbleAttach;
import android.util.Log;
import android.database.Cursor;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AttachmentUtil {
    public static final String TAG = "VoiceAss.AttachmentUtils";

    public static final char EXTENSION_SEPARATOR = '.';
    public static final long ATTACHMENT_SIZE_LIMIT = 30 * 1024 * 1024;

    /**
     * Extract and return filename's extension, converted to lower case, and not including the "."
     *
     * @return extension, or null if not found (or null/empty filename)
     */
    public static String getFilenameExtension(String fileName) {
        String extension = null;
        if (!TextUtils.isEmpty(fileName)) {
            int lastDot = fileName.lastIndexOf(EXTENSION_SEPARATOR);
            if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
                extension = fileName.substring(lastDot + 1).toLowerCase();
            }
        }
        return extension;
    }

    public static String queryFileType(Context context, Uri uri, String name) {
        ContentResolver contentResolver = context.getContentResolver();
        String contentType = contentResolver.getType(uri);
        if (contentType == null) {
            if (name != null) {
                String extension = getFilenameExtension(name);
                if (TextUtils.isEmpty(extension)) {
                    contentType = "";
                } else if (extension.equalsIgnoreCase("dts")) {
                    contentType = "audio/vnd.dts";
                } else if (extension.equalsIgnoreCase("ac3") || extension.equalsIgnoreCase("eac3")) {
                    contentType = "audio/x-aac";
                } else if (extension.equalsIgnoreCase("rm") || extension.equalsIgnoreCase("flv") || extension.equalsIgnoreCase("m4v")) {
                    contentType = "video/x-msvideo";
                } else if (extension.equalsIgnoreCase("eml")) {
                    contentType = "message/rfc822";
                } else if (extension.equalsIgnoreCase("vcf")) {
                    contentType = "text/x-vcard";
                } else if (extension.equalsIgnoreCase("vcs")) {
                    contentType = "text/x-vcalendar";
                } else {
                    contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                }
                Log.d(TAG, "contentType = " + contentType);
            }
        }

        if (TextUtils.isEmpty(contentType)) {
            Log.e(TAG, "extra logic to get file contentType");
        }
        return contentType;
    }

    public static long queryFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri == null) {
            return size;
        }
        if (uri.toString().toLowerCase().startsWith("file://")) {
            String filePath = uri.toString().substring("file://".length());
            size = new File(Uri.decode(filePath)).length();
            return size;
        }

        Cursor metadataCursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        try {
            metadataCursor = contentResolver.query(
                    uri, new String[]{OpenableColumns.SIZE},
                    null, null, null);
            if (metadataCursor != null) {
                try {
                    if (metadataCursor.moveToNext()) {
                        size = metadataCursor.getLong(0);
                    }
                } finally {
                    metadataCursor.close();
                }
            }
        } catch (Exception ex) {
            // ignore
        }

        if (size == 0) {
            InputStream in = null;
            try {
                in = context.getContentResolver().openInputStream(uri);
                size = in.available();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                quietClose(in);
            }
        }

        if (size == 0) {
            size = ATTACHMENT_SIZE_LIMIT;
        }
        return size;
    }

    public static String queryFileName(Context context, Uri uri) {
        String name = null;
        Cursor metadataCursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        try {
            metadataCursor = contentResolver.query(
                    uri, new String[]{OpenableColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.DATA, OpenableColumns.SIZE},
                    null, null, null);
            if (metadataCursor != null) {
                try {
                    if (metadataCursor.moveToNext()) {
                        name = metadataCursor.getString(0);
                        if (name == null) {
                            name = Uri.parse(metadataCursor.getString(1)).getLastPathSegment();
                        }
                    }
                } finally {
                    metadataCursor.close();
                }
            }
        } catch (Exception ex) {
            if ((ex instanceof SQLiteException)
                    || (ex instanceof IllegalArgumentException)) {
                try {
                    metadataCursor = contentResolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                    if (metadataCursor != null && metadataCursor.moveToNext()) {
                        name = (metadataCursor.getString(0));
                    }
                } catch (SQLiteException e) {
                } catch (IllegalArgumentException e) {
                } finally {
                    if (metadataCursor != null) metadataCursor.close();
                }

                // Let's try to get DATA if DISPLAY_NAME is missing.
                if (name == null) {
                    try {
                        metadataCursor = contentResolver.query(uri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
                        if (metadataCursor != null && metadataCursor.moveToNext()) {
                            name = Uri.parse(metadataCursor.getString(0)).getLastPathSegment();
                        }
                    } catch (SQLiteException e) {
                    } catch (IllegalArgumentException e) {
                    } finally {
                        if (metadataCursor != null) metadataCursor.close();
                    }
                }
            }
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }

        if (name == null) {
            name = " ";
        }

        if (name != null && name.length() > 120) {
            name = name.substring(name.length() - 120);
        }

        return new File(name).getName();
    }

    public static int getType(String contentType) {
        if (contentType != null && contentType.startsWith("image")) {
            return GlobalBubbleAttach.TYPE_IMAGE;
        } else {
            return GlobalBubbleAttach.TYPE_FILE;
        }
    }

    public static Uri copyFileToInnerDir(Context context, Uri oriUri, String fileName) {
        InputStream in = null;
        OutputStream out = null;
        File toFile = getSaveTargetFile(context, fileName);
        try {
            in = context.getContentResolver().openInputStream(oriUri);
            out = new FileOutputStream(toFile);
            copyFile(in, out);
            return (Uri.fromFile(toFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            quietClose(in);
            quietClose(out);
        }
        return null;
    }

    public static boolean copyFileToSpecifiedInnerDir(Context context, Uri originalUri, Uri destUri) {
        if (originalUri == null || destUri == null) {
            return false;
        }
        boolean restored = false;
        InputStream in = null;
        OutputStream out = null;
        File toFile = new File(destUri.getPath());
        try {
            FileUtils.createParentDirs(toFile);
            in = context.getContentResolver().openInputStream(originalUri);
            out = new FileOutputStream(toFile);
            copyFile(in, out, 8 * 1024);
            restored = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            quietClose(in);
            quietClose(out);
        }
        return restored;
    }

    public static File getSaveTargetFile(Context context, String fileName) {
        long id = System.currentTimeMillis();
        File root = context.getExternalCacheDir();
        File fileDir = new File(root, String.valueOf(id));
        if (!fileDir.isDirectory()) {
            if (!fileDir.mkdirs()) {
                Log.e(TAG, "mkdir fail");
            }
        }
        File saveFile = new File(fileDir, fileName);
        int index = 1;
        File tempDir = null;
        while (saveFile.isFile()) {
            tempDir = new File(fileDir, "(" + index + ")");
            if (!tempDir.isDirectory()) {
                tempDir.mkdirs();
            }
            saveFile = new File(tempDir, fileName);
            index++;
        }
        return saveFile;
    }

    static void copyFile(InputStream in, OutputStream out) throws IOException {
        copyFile(in, out, 1024);
    }

    static void copyFile(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buff = new byte[bufferSize];
        for (; ; ) {
            int len = in.read(buff);
            if (len < 0) {
                break;
            }
            out.write(buff, 0, len);
        }
    }

    public static void quietClose(Closeable io) {
        if (io != null) {
            try {
                io.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isSpecialType(String fileName) {
        String extension = getFilenameExtension(fileName);
        return "vcf".equals(extension) || "vcs".equals(extension);
    }

    public static int dip2px(Context context, double dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
}
