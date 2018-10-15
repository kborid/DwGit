package com.smartisanos.ideapills.common.util;

import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


public class UriUtils {

    // 允许外部传入的 scheme 为 file 的 URI 的绝对路径头
    private static final List<String> URI_PATH_WHITELIST = Arrays.asList(
            "/storage/"
    );

    /**
     * 判断传入的 clipData 中的 uri 是否包含非法路径
     */
    public static boolean isIllegal(ClipData clipData) {
        if (clipData == null) {
            return false;
        }
        boolean illegal = false;
        for (int i = 0; i < clipData.getItemCount(); i++) {
            Item item = clipData.getItemAt(i);
            if (null == item) {
                continue;
            }
            if (isIllegal(item.getUri())) {
                illegal = true;
                break;
            }
        }
        return illegal;
    }

    /**
     * 判断传入的 uri 中是否包含非法路径
     */
    public static boolean isIllegal(Uri uri) {
        if (null == uri) {
            return false;
        }
        boolean illegal = false;
        if (TextUtils.equals(ContentResolver.SCHEME_FILE, uri.getScheme())) {
            try {
                String canonicalPath = new File(uri.getPath()).getCanonicalPath();
                illegal = !isStartWith(canonicalPath, URI_PATH_WHITELIST);
            } catch (Exception e) {
                e.printStackTrace();
                illegal = true;
            }
        }
        return illegal;
    }

    /**
     * 判断传入的 uri 指向的文件是否有效
     */
    public static boolean isValid(Context context, Uri uri) {
        if (null == uri) {
            return false;
        }
        boolean valid = true;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                valid = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            valid = false;
        } finally {
            FileUtils.closeSilently(is);
        }
        return valid;
    }

    private static boolean isStartWith(String s, List<String> heads) {
        if (TextUtils.isEmpty(s)) {
            return false;
        }
        if (null == heads || heads.isEmpty()) {
            return true;
        }
        boolean result = false;
        for (int i = 0; i < heads.size(); i++) {
            String head = heads.get(i);
            if (TextUtils.isEmpty(head)) {
                continue;
            }
            if (s.startsWith(head)) {
                result = true;
                break;
            }
        }
        return result;
    }


}
