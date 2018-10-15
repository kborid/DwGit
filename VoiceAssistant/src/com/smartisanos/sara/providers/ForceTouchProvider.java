package com.smartisanos.sara.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.smartisanos.sara.R;
import com.smartisanos.sara.setting.RecycleBinActivity;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;

import java.util.ArrayList;

import smartisanos.widget.Title;
import smartisanos.widget.sectormenu.ShortcutIconInfo;


public class ForceTouchProvider extends ContentProvider {
    public static final String AUTHORITY = "com.smartisanos.sara.forcetouch";
    public static final String  REQUEST_QUERY_APP_SHORTCUTS = "query_app_shortcuts";

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method != null) {
            if (REQUEST_QUERY_APP_SHORTCUTS.equals(method)) {
                Bundle result = new Bundle();
                ArrayList<ShortcutIconInfo> list = new ArrayList<ShortcutIconInfo>();
                if (SaraUtils.isSettingEnable(getContext())) {
                    list.add(buildShortcutByType(SaraConstant.HANDLED_ACTIVITY));
                    list.add(buildShortcutByType(SaraConstant.HANDLED_OK_ACTIVITY));
                    result.putParcelableArrayList("shortcut_list", list);
                }
                result.putBoolean("islogin", !SaraUtils.isSettingEnable(getContext()));
                return result;
            }
        }
        return null;
    }

    private ShortcutIconInfo buildShortcutByType(int type) {
        ShortcutIconInfo shortcutIconInfo = new ShortcutIconInfo();
        shortcutIconInfo.setId(0);
        shortcutIconInfo.setPackageName("com.smartisanos.sara");
        Intent intent = new Intent();
        intent.setClass(getContext(), RecycleBinActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        switch (type) {
            case SaraConstant.HANDLED_ACTIVITY:
                intent.putExtra(Title.EXTRA_BACK_BTN_RES_ID, R.string.handled);
                intent.putExtra(SaraConstant.BUBBLE_DIRECTION, SaraConstant.HANDLED_ACTIVITY);
                intent.putExtra(SaraConstant.SHORTCUTS_DIRECTION, SaraConstant.HANDLED_ACTIVITY);
                shortcutIconInfo.setShortcutId("handled");
                shortcutIconInfo.setTitle(getContext().getResources().getString(R.string.handled_bubbles));
                shortcutIconInfo.setResId(R.drawable.forcetouch_IdeaPills_complete);
                shortcutIconInfo.setShorcutShortLabel(getContext().getString(R.string.handled_bubbles));
                shortcutIconInfo.setShorcutLongLabel(getContext().getString(R.string.handled_bubbles));
                shortcutIconInfo.setShorcutDisablemessage(getContext().getString(R.string.handled_bubbles));
                break;
            case SaraConstant.HANDLED_OK_ACTIVITY:
                intent.putExtra(Title.EXTRA_BACK_BTN_RES_ID, R.string.handled_ok);
                intent.putExtra(SaraConstant.BUBBLE_DIRECTION, SaraConstant.HANDLED_OK_ACTIVITY);
                intent.putExtra(SaraConstant.SHORTCUTS_DIRECTION, SaraConstant.HANDLED_OK_ACTIVITY);
                shortcutIconInfo.setShortcutId("handled_ok");
                shortcutIconInfo.setTitle(getContext().getResources().getString(R.string.handled_ok_Bubbles));
                shortcutIconInfo.setResId(R.drawable.forcetouch_IdeaPills_processed);
                shortcutIconInfo.setShorcutShortLabel(getContext().getString(R.string.handled_ok_Bubbles));
                shortcutIconInfo.setShorcutLongLabel(getContext().getString(R.string.handled_ok_Bubbles));
                shortcutIconInfo.setShorcutDisablemessage(getContext().getString(R.string.handled_ok_Bubbles));
        }
        shortcutIconInfo.setIntent(intent);
        return shortcutIconInfo;
    }
}
