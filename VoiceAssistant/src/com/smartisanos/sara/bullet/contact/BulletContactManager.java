package com.smartisanos.sara.bullet.contact;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public enum BulletContactManager {
    instance;

    private static final String AUTHORITY = "content://com.bullet.messenger";
    private static final String METHOD_CONTENT_ALL = "FLASHIM_ALL_CONTACT";
    private static final String METHOD_CONTENT_RECENT = "FLASHIM_RECENT_CONTACT";
    private static final String METHOD_CONTENT_STAR = "FLASHIM_STAR_CONTACT";
    private static final String KEY_CONTENT_ALL = "KEY_CONTENT_ALL";
    private static final String KEY_CONTENT_RECENT = "KEY_CONTENT_RECENT";
    private static final String KEY_CONTENT_STAR = "KEY_CONTENT_STAR";

    private volatile List<ContactItem> mAllContactsForSearch;
    private volatile HashMap<String, List<ContactItem>> mAllContactsForSearchHashMap;

    public static List<ContactItem> loadStarContacts(Bundle bundleExt) {
        ContentResolver resolver = SaraApplication.getInstance().getContentResolver();
        Uri uri = Uri.parse(AUTHORITY);
        Bundle bundle = resolver.call(uri, METHOD_CONTENT_STAR, null, bundleExt);
        if (bundle != null) {
            return parseContactsJson(bundle.getStringArrayList(KEY_CONTENT_STAR));
        }
        return null;
    }

    public static List<ContactItem> loadRecentContacts(Bundle bundleExt) {
        ContentResolver resolver = SaraApplication.getInstance().getContentResolver();
        Uri uri = Uri.parse(AUTHORITY);
        Bundle bundle = resolver.call(uri, METHOD_CONTENT_RECENT, null, bundleExt);
        if (bundle != null) {
            return parseContactsJson(bundle.getStringArrayList(KEY_CONTENT_RECENT));
        }
        return null;
    }

    public static List<ContactItem> loadAllContacts(Bundle bundleExt) {
        ContentResolver resolver = SaraApplication.getInstance().getContentResolver();
        Uri uri = Uri.parse(AUTHORITY);
        Bundle bundle = resolver.call(uri, METHOD_CONTENT_ALL, null, bundleExt);
        if (bundle != null) {
            return parseContactsJson(bundle.getStringArrayList(KEY_CONTENT_ALL));
        }
        return null;
    }

    private static List<ContactItem> parseContactsJson(ArrayList<String> bulletJsons) {
        List<ContactItem> contactItems = null;
        if (null != bulletJsons && bulletJsons.size() > 0) {
            contactItems = new ArrayList<>();
            for (String bulletJson : bulletJsons) {
                try {
                    JSONObject jsonObject = new JSONObject(bulletJson);
                    ContactItem item = ContactItem.toBulletContactItem(jsonObject);
                    item.belongsGroup();
                    contactItems.add(item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return contactItems;
    }

    public static Bundle buildPageBundle(int currentPage, int pageSize) {
        Bundle bundlePage = new Bundle();
        bundlePage.putInt("currentPage", currentPage);
        bundlePage.putInt("pageSize", pageSize);
        return bundlePage;
    }

    public boolean hasAllContacts() {
        return null != mAllContactsForSearch && mAllContactsForSearch.size() > 0;
    }

    public void setAllContactsForSearch(List<ContactItem> contacts) {
        mAllContactsForSearch = contacts;
        if (null != contacts) {
            mAllContactsForSearchHashMap = new HashMap<>();
            for (ContactItem contactItem : contacts) {
                contactItem.getPinyin();
                String name = contactItem.getContactName();
                if (!mAllContactsForSearchHashMap.containsKey(name) || null == mAllContactsForSearchHashMap.get(name)) {
                    mAllContactsForSearchHashMap.put(name, new ArrayList<ContactItem>());
                }
                List<ContactItem> contactItems = mAllContactsForSearchHashMap.get(name);
                contactItem.setItemType(AbsContactItem.ItemType.SEARCH);
                contactItems.add(contactItem);
            }
        }
    }

    public List<ContactItem> getAllContactsForSearch() {
        return mAllContactsForSearch;
    }

    public HashMap<String, List<ContactItem>> getAllContactsForSearchHashMap() {
        return mAllContactsForSearchHashMap;
    }

    public void destroy() {
        if (null != mAllContactsForSearch) {
            mAllContactsForSearch = null;
        }
        if (null != mAllContactsForSearchHashMap) {
            mAllContactsForSearchHashMap = null;
        }
    }
}
