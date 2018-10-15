package com.smartisanos.sara.util;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.service.onestep.GlobalBubbleAttach;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import com.smartisanos.ideapills.common.util.ShareUtils;
import com.smartisanos.ideapills.common.util.UriUtils;
import com.smartisanos.sara.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import smartisanos.api.IntentSmt;

public class AttachmentUtils {
    public static final int ATTACHMENT_QUANTITY_LIMIT = 14;
    public static final int CHILD_COUNT_PER_ROW = 7;
    public static final long ATTACHMENT_SIZE_LIMIT = 30 * 1024 * 1024;

    public static boolean removeFromListOverSize(List<GlobalBubbleAttach> list, int limit) {
        if (limit < 0) {
            limit = 0;
        }
        final int sizeorg = list.size();
        for (int i = list.size() - 1; i >= limit; i--) {
            list.remove(i);
        }
        return list.size() != sizeorg;
    }

    public static Intent setIntentDataAndTypeAndNormalize(Intent intent, Uri data, String type) {
        Uri uri = normalizeUri(data);
        return intent.setDataAndType(uri, normalizeMimeType(type));
    }

    /**
     * (copied from {@link Uri#normalize()} for pre-J)
     *
     * Return a normalized representation of this Uri.
     *
     * <p>A normalized Uri has a lowercase scheme component.
     * This aligns the Uri with Android best practices for
     * intent filtering.
     *
     * <p>For example, "HTTP://www.android.com" becomes
     * "http://www.android.com"
     *
     * <p>All URIs received from outside Android (such as user input,
     * or external sources like Bluetooth, NFC, or the Internet) should
     * be normalized before they are used to create an Intent.
     *
     * <p class="note">This method does <em>not</em> validate bad URI's,
     * or 'fix' poorly formatted URI's - so do not use it for input validation.
     * A Uri will always be returned, even if the Uri is badly formatted to
     * begin with and a scheme component cannot be found.
     *
     * @return normalized Uri (never null)
     * @see {@link android.content.Intent#setData}
     * @see {@link #setNormalizedData}
     */
    public static Uri normalizeUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return uri;  // give up
        String lowerScheme = scheme.toLowerCase(Locale.US);
        if (scheme.equals(lowerScheme)) return uri;  // no change

        return uri.buildUpon().scheme(lowerScheme).build();
    }

    /**
     * (copied from {@link Intent#normalizeMimeType(String)} for pre-J)
     *
     * Normalize a MIME data type.
     *
     * <p>A normalized MIME type has white-space trimmed,
     * content-type parameters removed, and is lower-case.
     * This aligns the type with Android best practices for
     * intent filtering.
     *
     * <p>For example, "text/plain; charset=utf-8" becomes "text/plain".
     * "text/x-vCard" becomes "text/x-vcard".
     *
     * <p>All MIME types received from outside Android (such as user input,
     * or external sources like Bluetooth, NFC, or the Internet) should
     * be normalized before they are used to create an Intent.
     *
     * @param type MIME data type to normalize
     * @return normalized MIME data type, or null if the input was null
     * @see {@link #setType}
     * @see {@link #setTypeAndNormalize}
     */
    public static String normalizeMimeType(String type) {
        if (type == null) {
            return null;
        }

        type = type.trim().toLowerCase(Locale.US);

        final int semicolonIndex = type.indexOf(';');
        if (semicolonIndex != -1) {
            type = type.substring(0, semicolonIndex);
        }
        return type;
    }

    public static void shareToApp(Context context, Uri uri, String type) {
        if (type == null) {
            ToastUtil.showToast(context, context.getString(R.string.bubble_share_attachment_unknown_type));
            return;
        }
        Intent target = ShareUtils.createShareAttachIntent(uri, type, null);
        context.startActivity(target);
    }

    private static List<GlobalBubbleAttach> getAttachList(Context context, ClipData cd) {
        List<GlobalBubbleAttach> listAttach = new ArrayList<GlobalBubbleAttach>();
        for (int i = 0; i < cd.getItemCount(); i++) {
            Uri uri = cd.getItemAt(i).getUri();
            if (uri == null) {
                break;
            }
            GlobalBubbleAttach item = generateAndAddAttachment(context, uri);
            if (item != null) {
                listAttach.add(item);
            }
        }
        return listAttach;
    }

    private static GlobalBubbleAttach generateAndAddAttachment(Context context, Uri uri) {
        if (uri == null) {
            LogUtils.d("pick uri must not be null");
            return null;
        }
        if (UriUtils.isIllegal(uri)) {
            LogUtils.d("illegal uri");
            return null;
        }
        GlobalBubbleAttach item = new GlobalBubbleAttach();
        item.setUri(uri);
        item.setFilename(AttachmentUtil.queryFileName(context, uri));
        item.setContentType(AttachmentUtil.queryFileType(context, uri, item.getFilename()));
        item.setType(AttachmentUtil.getType(item.getContentType()));
        Uri saveUri = AttachmentUtil.copyFileToInnerDir(context, uri, item.getFilename());
        item.setLocalUri(saveUri);
        if (saveUri != null) {
            item.setStatus(GlobalBubbleAttach.STATUS_SUCCESS);
        } else {
            item.setStatus(GlobalBubbleAttach.STATUS_FAIL);
        }
        item.setNeedDel(true);
        return item;
    }

    public static List<GlobalBubbleAttach> mergeImageAttachList(Context context, final List<GlobalBubbleAttach> listAttachBefore, ClipData cd) {
        List<GlobalBubbleAttach> listAttach = new ArrayList<GlobalBubbleAttach>();
        listAttach.addAll(getAttachList(context, cd));
        int beforeCount = listAttachBefore == null ? 0 : listAttachBefore.size();
        for (int i = 0; i < beforeCount; i++) {
            if (listAttachBefore.get(i).getType() == GlobalBubbleAttach.TYPE_FILE) {
                listAttach.add(listAttachBefore.get(i));
            }
        }
        return listAttach;
    }

    public static List<GlobalBubbleAttach> mergeAttachmentList(Context context, final List<GlobalBubbleAttach> listAttachBefore, Intent data) {
        List<GlobalBubbleAttach> realAttachList = new ArrayList<GlobalBubbleAttach>();
        List<GlobalBubbleAttach> listAttach = new ArrayList<GlobalBubbleAttach>();
        if (listAttachBefore != null && listAttachBefore.size() > 0) {
            listAttach.addAll(listAttachBefore);
        }
        ClipData cd = data.getClipData();
        if (cd != null) {
            listAttach.addAll(getAttachList(context, cd));
        } else {
            GlobalBubbleAttach item = generateAndAddAttachment(context, data.getData());
            if (item != null) {
                listAttach.add(item);
            }
        }

        boolean isFileTooLarge = false;
        if (listAttach.size() > 0) {
            long addSize = 0;
            for (GlobalBubbleAttach item : listAttach) {
                addSize += (AttachmentUtil.queryFileSize(context, item.getUri()));
                if (addSize > ATTACHMENT_SIZE_LIMIT) {
                    isFileTooLarge = true;
                    break;
                }
            }

            boolean isFileTooMuch = listAttach.size() > ATTACHMENT_QUANTITY_LIMIT;

            if (isFileTooMuch  && isFileTooLarge) {
                String showToast = context.getString(R.string.bubble_attachment_size_count_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                ToastUtil.showToast(context, showToast);
                realAttachList = listAttachBefore;
            } else if (isFileTooMuch) {
                String showToast = context.getString(R.string.bubble_attachment_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                ToastUtil.showToast(context, showToast);
                realAttachList = listAttachBefore;
            } else if (isFileTooLarge) {
                String showToast = context.getString(R.string.bubble_attachment_size_limit, AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT);
                ToastUtil.showToast(context, showToast);
                realAttachList = listAttachBefore;
            } else {
                realAttachList = listAttach;
            }
        }
        return realAttachList;
    }
}
