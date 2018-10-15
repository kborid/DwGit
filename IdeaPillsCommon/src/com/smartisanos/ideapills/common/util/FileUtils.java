package com.smartisanos.ideapills.common.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;


import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final int EOF = -1;
    /**
     * The extension separator character.
     *
     * @since Commons IO 1.4
     */
    public static final char EXTENSION_SEPARATOR = '.';

    public static byte[] readFileToByteArray(File file) {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return toByteArray(in, file.length());
        } catch(Exception e){
            Log.e(TAG, "" + e.getMessage());
        } finally {
            closeSilently(in);
        }
        return null;
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file
                        + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file
                    + "' does not exist");
        }
        return new FileInputStream(file);
    }


    public static byte[] toByteArray(InputStream input, long size)
            throws IOException {

        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Size cannot be greater than Integer max value: " + size);
        }

        return toByteArray(input, (int) size);
    }

    public static byte[] toByteArray(InputStream input, int size)
            throws IOException {

        if (size < 0) {
            throw new IllegalArgumentException(
                    "Size must be equal or greater than zero: " + size);
        }

        if (size == 0) {
            return new byte[0];
        }

        byte[] data = new byte[size];
        int offset = 0;
        int readed;

        while (offset < size
                && (readed = input.read(data, offset, size - offset)) != EOF) {
            offset += readed;
        }

        if (offset != size) {
            throw new IOException("Unexpected readed size. current: " + offset
                    + ", excepted: " + size);
        }

        return data;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        for (; ; ) {
            int read = input.read(buff);
            if (read != EOF) {
                outputStream.write(buff, 0, read);
            } else {
                break;
            }
        }
        return outputStream.toByteArray();
    }

    public static boolean deleteLocalFile(Uri uri) {
        if (uri != null) {
            File file = new File(uri.getPath());
            try {
                return file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean deleteFile(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                if (file.isDirectory()) {
                    File files[] = file.listFiles();
                    if (null != files && files.length > 0) {
                        for (File f : files) {
                            deleteFile(f.getAbsolutePath());
                        }
                    }
                }
                return file.delete();
            }
        }
        return false;
    }

    public static boolean deleteFile(File deleteFile) {
        if (deleteFile != null) {
            if (!deleteFile.exists()) {
                return true;
            }

            if (deleteFile.isDirectory()) {
                // process folder
                File[] files = deleteFile.listFiles();
                if (null != files && files.length > 0) {
                    for (File file : files) {
                        deleteFile(file.getAbsolutePath());
                    }
                }
            }
            return deleteFile.delete();
        }

        return false;
    }

    public static long getFileSizeByUri(Context context, Uri uri) {
        if (uri != null) {
            if ("file".equals(uri.getScheme())) {
                return getFileSizeFromFileUri(context, uri);
            } else if ("content".equals(uri.getScheme())) {
                return getFileSizeFromContentUri(context, uri);
            }
        }
        return 0;
    }

    public static long getFileSizeFromFileUri(Context context, Uri uri) {
        long size = 0;
        if (uri != null && "file".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    return file.length();
                }
            }
        }
        return size;
    }

    public static long getFileSizeFromContentUri(Context context, Uri uri) {
        long size = 0;
        if (uri != null && "content".equals(uri.getScheme())) {
            ParcelFileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                if (fileDescriptor != null) {
                    size = fileDescriptor.getStatSize();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                closeSilently(fileDescriptor);
            }
        }
        return size;
    }

    public static String getFileNameByUri(Context context, Uri uri) {
        if (uri != null) {
            if ("file".equals(uri.getScheme())) {
                return getFileNameFromFileUri(context, uri);
            } else if ("content".equals(uri.getScheme())) {
                return getFileNameFromContentUri(context, uri);
            }
        }
        return null;
    }

    public static String getFileNameFromFileUri(Context context, Uri uri) {
        if (uri != null && "file".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    return file.getName();
                }
            }
        }
        return null;
    }

    public static String getFileNameFromContentUri(Context context, Uri uri) {
        String fileName = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri,
                        null, null, null, null);
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    cursor.moveToFirst();
                    fileName = cursor.getString(nameIndex);
                }
            } catch (Exception e) {

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                return fileName;
            }
        }
        return null;
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException t) {
            //ignore
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buff = new byte[1024];
        for (; ; ) {
            int len = in.read(buff);
            if (len < 0) {
                break;
            }
            out.write(buff, 0, len);
        }
    }

    public static boolean isFileExist(Uri uri) {
        boolean exist = false;
        if (null != uri && !TextUtils.isEmpty(uri.getPath())) {
            File file = new File(uri.getPath());
            exist = file.exists();
        }
        return exist;
    }

    /**
     * Creates any necessary but nonexistent parent directories of the specified
     * file. Note that if this operation fails it may have succeeded in creating
     * some (but not all) of the necessary parent directories.
     *
     * @throws IOException if an I/O error occurs, or if any necessary but
     *                     nonexistent parent directories of the specified file could not be
     *                     created.
     */
    public static void createParentDirs(File file) throws IOException {
        if (null == file) {
            return;
        }
        File parent = file.getCanonicalFile().getParentFile();
        if (parent == null) {
            /*
             * The given directory is a filesystem root. All zero of its ancestors
             * exist. This doesn't mean that the root itself exists -- consider x:\ on
             * a Windows machine without such a drive -- or even that the caller can
             * create it, but this method makes no such guarantees even for non-root
             * files.
             */
            return;
        }
        parent.mkdirs();
        if (!parent.isDirectory()) {
            throw new IOException("Unable to create parent directories of " + file);
        }
    }

}
