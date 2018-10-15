package com.smartisanos.ideapills.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.smartisanos.ideapills.common.data.AttachmentColumn;
import com.smartisanos.ideapills.common.data.BubbleColumn;

import com.smartisanos.ideapills.data.ATTACHMENT;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.data.DBHelper;
import com.smartisanos.ideapills.util.LOG;

public class IdeaPillsCallProvider extends ContentProvider {
    private static final LOG log = LOG.getInstance(IdeaPillsCallProvider.class);

    private static final UriMatcher matcher;

    private static final int BUBBLES = 1;
    private static final int BUBBLE_ID = 2;
    private static final int ATTACHMENTS = 3;
    private static final int ATTACHMENT_ID = 4;

    static {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(BubbleColumn.AUTHORITY, "bubble", BUBBLES);
        matcher.addURI(BubbleColumn.AUTHORITY, "bubble/#", BUBBLE_ID);
        matcher.addURI(BubbleColumn.AUTHORITY, "attachment", ATTACHMENTS);
        matcher.addURI(BubbleColumn.AUTHORITY, "attachment/#", ATTACHMENT_ID);
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int match = matcher.match(uri);
        switch (match) {
            case BUBBLES:
                qb.setTables(BUBBLE.NAME);
                break;
            case BUBBLE_ID:
                qb.setTables(BUBBLE.NAME);
                qb.appendWhere(BubbleColumn._ID + " = "
                        + uri.getPathSegments().get(1));
                break;
            case ATTACHMENTS:
                qb.setTables(ATTACHMENT.NAME);
                break;
            case ATTACHMENT_ID:
                qb.setTables(ATTACHMENT.NAME);
                qb.appendWhere(AttachmentColumn._ID + " = "
                        + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);

        }
        if (TextUtils.isEmpty(sortOrder)) {
            if (match == BUBBLES || match == BUBBLE_ID) {
                sortOrder = BubbleColumn.DEFAULT_SORT_ORDER;
            }
        }
        SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        int match = matcher.match(uri);
        switch (match) {
            case BUBBLES: {
                return BubbleColumn.CONTENT_TYPE;
            }
            case BUBBLE_ID: {
                return BubbleColumn.CONTENT_ITEM_TYPE;
            }
            case ATTACHMENTS: {
                return AttachmentColumn.CONTENT_TYPE;
            }
            case ATTACHMENT_ID: {
                return AttachmentColumn.CONTENT_ITEM_TYPE;
            }
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int match = matcher.match(uri);
        long rowId = 0l;
        Uri insertUri = null;
        if (match == BUBBLES) {
            rowId = db.insert(BUBBLE.NAME, null, values);
            insertUri = ContentUris.withAppendedId(BubbleColumn.CONTENT_URI, rowId);
        } else if (match == ATTACHMENTS) {
            rowId = db.insert(ATTACHMENT.NAME, null, values);
            insertUri = ContentUris.withAppendedId(AttachmentColumn.CONTENT_URI, rowId);
        }
        if (rowId > 0 && uri != null) {
            this.getContext().getContentResolver().notifyChange(uri, null);
        }
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int match = matcher.match(uri);
        int count;
        switch (match) {
            case BUBBLES:
                count = db.delete(BUBBLE.NAME, selection, selectionArgs);
                break;
            case BUBBLE_ID:
                String bubbleId = uri.getPathSegments().get(1);
                count = db.delete(BUBBLE.NAME, BubbleColumn._ID
                        + "="
                        + bubbleId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                        + ')' : ""), selectionArgs);
                break;
            case ATTACHMENTS:
                count = db.delete(ATTACHMENT.NAME, selection, selectionArgs);
                break;
            case ATTACHMENT_ID:
                String attachId = uri.getPathSegments().get(1);
                count = db.delete(ATTACHMENT.NAME, AttachmentColumn._ID
                        + "="
                        + attachId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                        + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);

        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues initialValues, String selection,
                      String[] selectionArgs) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int match = matcher.match(uri);
        int count;
        switch (match) {
            case BUBBLES:
                count = db.update(BUBBLE.NAME, values, selection,
                        selectionArgs);
                break;
            case BUBBLE_ID:
                String bubbleId = uri.getPathSegments().get(1);
                count = db.update(
                        BUBBLE.NAME,
                        values,
                        BubbleColumn._ID
                                + "="
                                + bubbleId
                                + (!TextUtils.isEmpty(selection) ? " AND ("
                                + selection + ')' : ""), selectionArgs);
                break;
            case ATTACHMENTS:
                count = db.delete(ATTACHMENT.NAME, selection, selectionArgs);
                break;
            case ATTACHMENT_ID:
                String attachId = uri.getPathSegments().get(1);
                count = db.update(
                        BUBBLE.NAME,
                        values,
                        AttachmentColumn._ID
                                + "="
                                + attachId
                                + (!TextUtils.isEmpty(selection) ? " AND ("
                                + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}