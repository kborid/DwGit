package com.smartisanos.ideapills.common.util;

import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.DragEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import smartisanos.util.MimeUtils;

/**
 * Utilities for dealing with MIME types.
 */
public final class BubbleMimeUtils {

    public static String getCommonMimeType(List<String> mimeTypes) {
        if (mimeTypes == null || mimeTypes.size() <= 0) {
            return null;
        }

        boolean sameType = true;
        for (int i = 1; i < mimeTypes.size(); ++i) {
            if (!mimeTypes.get(0).equals(mimeTypes.get(i))) {
                sameType = false;
                break;
            }
        }
        if (sameType) {
            return mimeTypes.get(0);
        }

        int index0 = mimeTypes.get(0).indexOf("/");
        if (index0 == -1) {
            return null;
        }
        for (int i = 1; i < mimeTypes.size(); ++i) {
            int indexNow = mimeTypes.get(i).indexOf("/");
            if (indexNow == -1) {
                return null;
            }
            if (indexNow != index0
                    || !mimeTypes.get(0).regionMatches(0, mimeTypes.get(i), 0,
                            indexNow)) {
                return "*/*";
            }
        }
        return mimeTypes.get(0).substring(0, index0 + 1) + "*";
    }

    public static String getCommonMimeType(DragEvent event) {
        if (event == null) {
            return null;
        }
        ClipDescription desc = event.getClipDescription();
        if (desc == null) {
            return null;
        }
        List<String> mimeTypes = new ArrayList<String>();
        int count = desc.getMimeTypeCount();
        for (int i = 0; i < count; ++i) {
            mimeTypes.add(desc.getMimeType(i));
        }
        return BubbleMimeUtils.getCommonMimeType(mimeTypes);
    }

    public static Bitmap getFileThumbIcon(Context context, String mimeType, File file) {
        int resId = MimeUtils.getFileIconResId(mimeType, file);
        Bitmap fileIcon = null;
        if (resId != 0) {
            fileIcon = BitmapFactory.decodeResource(context.getResources(), resId);
        }
        return fileIcon;
    }

    public static Bitmap getFileThumbIcon(Context context, String mimeType, String suffix) {
        int resId = MimeUtils.getFileIconResId(mimeType, suffix);
        Bitmap fileIcon = null;
        if (resId != 0) {
            fileIcon = BitmapFactory.decodeResource(context.getResources(), resId);
        }
        return fileIcon;
    }
}
