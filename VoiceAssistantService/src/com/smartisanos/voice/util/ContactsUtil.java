package com.smartisanos.voice.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.smartisanos.voice.VoiceApplication;
import com.smartisanos.voice.engine.GrammarManager;
import com.smartisanos.voice.providers.VoiceSettings;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.net.Uri;
import android.os.SystemProperties;

import org.json.JSONException;
import org.json.JSONObject;

import smartisanos.app.numberassistant.CallerIdDetail;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.text.SmartWordIterator;

public class ContactsUtil {
    public static final String CONTACT_NAME = "contactsList";
    public static final String BULLET_MIMETYPE = "type_bullet";
    public static final int MAX_COUNT_PER_QUERY = 2000;
    public static final int MAX_DISPLAY_NAME_COUNT = 8000;
    static final LogUtils log = LogUtils.getInstance(ContactsUtil.class);

    private static Pattern sNotChinese = Pattern.compile(VoiceConstant.REGEX_NOT_CHINESE);

    private static final String SELF_TYPE = "0";
    private static final String OTHER_TYPE = "7";

    private static final String[] PROJECTION_PHONENUMBER_CONTACT = {
            Phone.NUMBER, Phone.TYPE, Phone.LABEL, "_id"
    };
    private static final String[] PROJECTION_CONTACT= {
        Data.CONTACT_ID, Contacts.DISPLAY_NAME,ContactsContract.Data.PHOTO_ID ,Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,Data.MIMETYPE, Phone.NUMBER, Phone.TYPE, Phone.LABEL, "_id"
    };

    private static final String[] PROJECTION_DISPLAYNAME_CONTACT = {
        Contacts.DISPLAY_NAME , Contacts.CONTACT_LAST_UPDATED_TIMESTAMP, ContactsContract.Data.PHOTO_ID
    };
    private static final String[] PROJECTION_PHONENUMBER_AND_DISPLAYNAME_CONTACT = {
            Phone.NUMBER, Phone.TYPE, Phone.LABEL,StructuredName.DISPLAY_NAME
    };
    private static final String[] PROJECTION_EAMIL_CONTACT = {
            Email.DATA1, Email.TYPE, Email.LABEL
    };

    private static final String[] PROJECTION_IM_CONTACT = new String[] {
            Im.DATA, Im.TYPE, Im.LABEL, Im.PROTOCOL
    };

    private static final String[] PROJECTION_ADDRESS_CONTACT = new String[] {
            StructuredPostal.STREET, StructuredPostal.CITY, StructuredPostal.REGION,
            StructuredPostal.POSTCODE, StructuredPostal.COUNTRY, StructuredPostal.TYPE,
            StructuredPostal.LABEL, StructuredPostal.POBOX, StructuredPostal.NEIGHBORHOOD,
    };

    private static final String[] PROJECTION_ORGANIZATION_CONTACT = new String[] {
            Organization.COMPANY, Organization.TYPE, Organization.LABEL, Organization.TITLE
    };

    private static final String[] PROJECTION_NOTES_CONTACT = new String[] {
        Note.NOTE
    };

    private static final String[] PROJECTION_NICKNAMES_CONTACT = new String[] {
            Nickname.NAME, Nickname.TYPE, Nickname.LABEL
    };

    private static final String[] PROJECTION_WEBSITES_CONTACT = new String[] {
            Website.URL, Website.TYPE, Website.LABEL
    };
    private static final String[] PROJECTION_CONTACTID_VERSION_CONTACTS = {
        Data.CONTACT_ID, Contacts.DISPLAY_NAME, RawContacts.VERSION
    };
    private static final HashSet<String> sLastNameList = new HashSet<String>(2);
    private static final boolean VOICE_SPECIAL_CONTACT = SystemProperties.getInt("ro.voice_special_contact_enable", 0) == 1;

    static boolean isUpdating = false;
    public static boolean buildContactNameList(final Context context, boolean checkUpdate, boolean isLastNameList) {
        if (context == null || isUpdating) {
            log.e("buildContactNameList return false! context=" + context + ", isUpdating=" + isUpdating);
            return false;
        }

        isUpdating = true;
        SharePrefUtil.putBoolean(context, VoiceConstant.KEY_CONTACT, false);
        long oldTime = SharePrefUtil.getLong(context, VoiceConstant.KEY_CONTACT_UPDATE_TIMESTAMP, 0);
        long newTime = System.currentTimeMillis();
        boolean updated = false;

        log.infoRelease("buildContactNameList is enter, old time=" + new Date(oldTime) + ", new time=" + new Date(newTime));

        oldTime -= 1000 * 5; // 5s冗余查询，不然数据查不完整，可能是contacts provider的bug

        // handle deleted contact id.
        if (newTime - oldTime > ContactsContract.DeletedContacts.DAYS_KEPT_MILLISECONDS) {
            oldTime = 0L;
            log.w("buildContactNameList: delete all from sara db...");
            updated |= context.getContentResolver().delete(
                    VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                    VoiceSettings.ContactColumns.MIMETYPE + "!=?", new String[]{BULLET_MIMETYPE}) > 0;
        } else {
            updated |= snippetQuery(context, ContactsContract.DeletedContacts.CONTENT_URI,
                    new String[]{ContactsContract.DeletedContacts.CONTACT_ID},
                    ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP + " > " + oldTime +
                            " AND " + ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP + " < " + newTime,
                    null, Data.CONTACT_ID + " ASC",
                    MAX_COUNT_PER_QUERY, new SnippetQueryListener() {
                        @Override
                        public boolean onQuery(Cursor cursor) {
                            return onQueryDeletedContact(context, cursor);
                        }
                    });
        }

        // handle updated contact id.
        Uri uri = Data.CONTENT_URI.buildUpon().appendQueryParameter("use_distinct", "true").build();
        updated |= snippetQuery(context, uri, new String[]{Data.CONTACT_ID},
                Data.CONTACT_LAST_UPDATED_TIMESTAMP + " > " + oldTime +
                        " AND " + Data.CONTACT_LAST_UPDATED_TIMESTAMP + " < " + newTime,
                null, Data.CONTACT_ID + " ASC",
                MAX_COUNT_PER_QUERY, new SnippetQueryListener() {
                    @Override
                    public boolean onQuery(Cursor updatedCursor) {
                        return onQueryUpdatedContact(context, updatedCursor);
                    }
                });

        if (updated) {
            SharePrefUtil.putLong(context, VoiceConstant.KEY_CONTACT_UPDATE_TIMESTAMP, newTime);
            SharePrefUtil.putBoolean(context, VoiceConstant.KEY_CONTACT, true);
        }

        if (sLastNameList.size() == 0 || updated) {
            updateContactNameList(context);
        }
        isUpdating = false;
        return updated;
    }

    private static boolean onQueryDeletedContact(Context context, Cursor cursor) {
        if (context == null || cursor == null || cursor.getCount() == 0)
            return false;

        StringBuilder condition = new StringBuilder(VoiceSettings.ContactColumns.CONTACT_ID);
        condition.append(" IN (");
        while (cursor.moveToNext()) {
            condition.append(cursor.getLong(cursor.getColumnIndex(ContactsContract.DeletedContacts.CONTACT_ID)))
                    .append(",");
        }

        if (condition.charAt(condition.length() - 1) != ',') {
            return false;
        }

        condition.deleteCharAt(condition.length() - 1);
        condition.append(")");
        int deleted = context.getContentResolver().delete(
                VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                condition.toString(), null);
        log.infoRelease("onQueryDeletedContact " + cursor.getCount() + " -> " + deleted + log.getReleaseString(" : " + condition));
        return true;
    }

    private static boolean onQueryUpdatedContact(Context context, Cursor cursor) {
        if (context == null || cursor == null || cursor.getCount() == 0)
            return false;

        List<Long> changedContactId = new ArrayList<Long>(cursor.getCount());
        while (cursor.moveToNext()) {
            changedContactId.add(cursor.getLong(
                    cursor.getColumnIndex(ContactsContract.DeletedContacts.CONTACT_ID)));
        }
        cursor.close();
        log.infoRelease("onQueryUpdatedContact " + changedContactId.size() + log.getReleaseString(" : " + changedContactId));
        if (changedContactId.size() == 0)
            return false;

        // firstly, delete this from sara db.
        String idCondition = " IN (" + TextUtils.join(",", changedContactId) + ")";
        String condition = VoiceSettings.ContactColumns.CONTACT_ID + idCondition;
        int deleted = context.getContentResolver().delete(
                VoiceSettings.ContactColumns.CONTENT_URI_CONTACT, condition, null);
        log.infoRelease("onQueryUpdatedContact deleted: " + deleted);

        // then, query and save changed contact id info to sara db.
        String[] projection = {Data.CONTACT_ID, Data.RAW_CONTACT_ID, Data.MIMETYPE,
                Contacts.DISPLAY_NAME, Phone.NUMBER, Phone.TYPE, Phone.LABEL,
                ContactsContract.Data.PHOTO_ID, "_id"};
        String[] acceptMimeTypes = {StructuredName.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, VoiceConstant.WEIXIN_MIMETYPE};
        condition = Data.CONTACT_ID + idCondition + " AND " + Data.MIMETYPE + " IN (?, ?, ?)";

        Cursor infoCursor = null;
        try {
            infoCursor = context.getContentResolver().query(Data.CONTENT_URI, projection,
                    condition, acceptMimeTypes,
                    TextUtils.join(",", new String[]{Data.CONTACT_ID, Data.RAW_CONTACT_ID, Data.MIMETYPE}));

            if (infoCursor != null && infoCursor.getCount() > 0) {
                List<ContentValues> dataValues = new ArrayList<ContentValues>();

                long contactId = 0;
                long rawContactId = 0;
                ContentValues nameValues = new ContentValues();
                HashMap<String, ContentValues> phoneValues = new HashMap<String, ContentValues>();
                while (infoCursor.moveToNext()) {
                    boolean contactIdChanged = false;
                    long tmp = infoCursor.getLong(infoCursor.getColumnIndex(Data.CONTACT_ID));
                    if (tmp != contactId) {
                        contactId = tmp;
                        dataValues.addAll(phoneValues.values());
                        phoneValues.clear();
                        contactIdChanged = true;
                    }

                    if ((tmp = infoCursor.getLong(infoCursor.getColumnIndex(Data.RAW_CONTACT_ID))) != rawContactId
                            || contactIdChanged) {
                        rawContactId = tmp;
                        nameValues.clear();
                    }

                    String mimeType = infoCursor.getString(infoCursor.getColumnIndex(Data.MIMETYPE));
                    String dataId = infoCursor.getString(infoCursor.getColumnIndex("_id"));

                    if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        String displayName = infoCursor.getString(infoCursor.getColumnIndex(Contacts.DISPLAY_NAME));
                        String pinyin = PinYinUtil.getPinYin(DataLoadUtil.normalizeKey(displayName));
                        String photoId = infoCursor.getString(infoCursor.getColumnIndex(ContactsContract.Data.PHOTO_ID));

                        nameValues.put(VoiceSettings.ContactColumns.CONTACT_ID, contactId);
                        nameValues.put(VoiceSettings.ContactColumns.NAME, StringUtils.getStringOrEmpty(displayName));
                        nameValues.put(VoiceSettings.ContactColumns.ITEM_NAME_PINYIN, pinyin);
                        nameValues.put(VoiceSettings.ContactColumns.PHOTE_ID, !TextUtils.isEmpty(photoId) ? Long.parseLong(photoId) : -1);
                    } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        String number = infoCursor.getString(infoCursor.getColumnIndex(Phone.NUMBER));
                        String phone = getPhone(number);
                        if (TextUtils.isEmpty(phone) ||
                                TextUtils.isEmpty(nameValues.getAsString(VoiceSettings.ContactColumns.NAME)) ||
                                phoneValues.containsKey(phone)) {
                            continue;
                        }

                        String type = infoCursor.getString(infoCursor.getColumnIndex(Phone.TYPE));
                        String label = infoCursor.getString(infoCursor.getColumnIndex(Phone.LABEL));

                        ContentValues values = new ContentValues(nameValues);
                        values.put(VoiceSettings.ContactColumns.NUMBER, phone);
                        values.put(VoiceSettings.ContactColumns.NUMBER_LOCATION_INFO, getNumberLocation(context, number));
                        values.put(VoiceSettings.ContactColumns.LABEL, getTypeLabel(context, type, label));
                        values.put(VoiceSettings.ContactColumns.DATA_ID, !TextUtils.isEmpty(dataId) ? dataId : String.valueOf(-1));
                        values.put(VoiceSettings.ContactColumns.MIMETYPE, mimeType);
                        phoneValues.put(phone, values);
                    } else if (VoiceConstant.WEIXIN_MIMETYPE.equals(mimeType)) {
                        // just update data id and mime type for exist phone items.
                        String number = infoCursor.getString(infoCursor.getColumnIndex(Phone.NUMBER));
                        String phone = getPhone(number);
                        if (!phoneValues.containsKey(phone)) {
                            continue;
                        }

                        // update data id and mime type to wechat type.
                        ContentValues values = phoneValues.get(phone);
                        values.put(VoiceSettings.ContactColumns.MIMETYPE, mimeType);
                        values.put(VoiceSettings.ContactColumns.DATA_ID, !TextUtils.isEmpty(dataId) ? dataId : String.valueOf(-1));
                    }
                }
                dataValues.addAll(phoneValues.values());

                if (dataValues.size() > 0) {
                    ContentValues[] values = new ContentValues[dataValues.size()];
                    int inserted = context.getContentResolver().bulkInsert(
                            VoiceSettings.ContactColumns.CONTENT_URI_CONTACT, dataValues.toArray(values));
                    log.infoRelease("onQueryUpdatedContact inserted: " + inserted);
                    return true;
                }
            }
        } finally {
            if (infoCursor != null) {
                infoCursor.close();
            }
        }

        return false;
    }

    private static String getPhone(String number) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }
        String phone = PhoneNumberUtils.formatNumber(number, VoiceApplication.getInstance().getCountryIso());
        return TextUtils.isEmpty(phone) ? "" : phone;
    }

    private static String getNumberLocation(Context context, String number) {
        CallerIdDetail callerIdDetail = CootekManager.getInstance(context).queryCallerIdDetailOffline(number);
        return callerIdDetail != null ? callerIdDetail.getAttribute() : "";
    }

    private static String getTypeLabel(Context context, String type, String label) {
        if (!isValidPhoneType(type)) {
            type = OTHER_TYPE;
        }
        return Phone.getTypeLabel(context.getResources(), Integer.parseInt(type), label).toString();
    }

    public interface SnippetQueryListener {
        boolean onQuery(Cursor cursor);
    }
    private static boolean snippetQuery(Context context, Uri uri, String[] projection, String selection,
                                        String[] selectionArgs, String sortOrder,
                                        final int maxCountPerQuery, final SnippetQueryListener listener) {
        if (maxCountPerQuery < 1)
            return false;

        final long MAX_OFFSET = 1000 * maxCountPerQuery;
        boolean updated = false;
        long offset = 0L;
        int count = 0;
        do {
            String limit = String.format(" LIMIT %d OFFSET %d", maxCountPerQuery, offset);
            String combine = sortOrder;
            if (TextUtils.isEmpty(sortOrder)) {
                if (projection == null) {
                    combine = "_id" + limit;
                } else if (projection.length > 0) {
                    combine = projection[0] + limit;
                }
            } else {
                combine = sortOrder + limit;
            }
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, combine);
                count = cursor != null ? cursor.getCount() : 0;
                if (listener != null) {
                    updated |= listener.onQuery(cursor);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.e("snippetQuery error : " + e);
                updated = false;
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            offset += count;
        } while (updated && count >= maxCountPerQuery && offset < MAX_OFFSET);

        return updated;
    }

    public static ArrayList<String> getContactNameList(Context context) {
        synchronized (sLastNameList) {
            return new ArrayList<String>(sLastNameList);
        }
    }

    private static void updateContactNameList(Context context) {
        if (context == null) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver()
                    .query(VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                            VoiceSettings.ContactColumns.PROJECTION_LAST_CONTACT,
                            VoiceSettings.ContactColumns.MIMETYPE + "!=?", new String[]{BULLET_MIMETYPE}, "_id LIMIT " + MAX_DISPLAY_NAME_COUNT);
            synchronized (sLastNameList) {
                sLastNameList.clear(); // 更新之前清空数据，因为有时是删除联系人，不然旧的数据会一直存在
                if (cursor != null && cursor.getCount() > 0) {
                    log.infoRelease("updateContactNameList count = " + cursor.getCount());
                    SmartWordIterator smartWordIterator = new SmartWordIterator(context);
                    while (cursor.moveToNext()) {
                        String displayName = cursor.getString(cursor.getColumnIndex(VoiceSettings.ContactColumns.NAME));
                        sLastNameList.add(displayName);
                        sLastNameList.addAll(getCustomNames(displayName, smartWordIterator));
                        sLastNameList.addAll(getMultiPinyinNames(displayName));
                    }
                    VoiceUtils.buildGrammar(GrammarManager.LEXICON_CONTACT);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.e("updateContactNameList error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static ArrayList<ContactStruct> getContacts(Context context, ArrayList<String> pinyin, ArrayList<ContactStruct> contactlist,
            String firstRealName,boolean isMultiple, boolean isBulletContacts) {
        // " mimetype = ? AND " or " mimetype != ? AND "
        String bulletCondition = String.format(" %s %s ? AND ",
                VoiceSettings.ContactColumns.MIMETYPE, isBulletContacts ? "=" : "!=");
        String groupby = !isBulletContacts ?
                ") group by (" + VoiceSettings.ContactColumns.DATA_ID + "), (" +VoiceSettings.ContactColumns.NUMBER : "";
        Cursor c = null;
        if (isMultiple){
            c = context.getContentResolver().query(
                    VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                    VoiceSettings.ContactColumns.PROJECTION_CONTACT,
                    bulletCondition + VoiceSettings.ContactColumns.NAME + " LIKE ?" + groupby,
                    new String[] {BULLET_MIMETYPE, "%" + firstRealName + "%"},  VoiceSettings.ContactColumns.CONTACT_ID+" , "+VoiceSettings.ContactColumns.DATA_ID);
        } else {
            if (pinyin == null || pinyin.size() <= 0) {
                return contactlist;
            }
            StringBuffer condition = new StringBuffer(bulletCondition);
            for (int i = 0; i < pinyin.size(); i++) {
                if (i == 0) {
                    condition.append("( " + VoiceSettings.ContactColumns.ITEM_NAME_PINYIN + " LIKE ? ");
                } else {
                    condition.append("or " + VoiceSettings.ContactColumns.ITEM_NAME_PINYIN + " =? ");
                }
            }
            condition.append("or " + VoiceSettings.ContactColumns.NAME + " LIKE ? )");
            condition.append(groupby);
            String[] projection = new String[pinyin.size() + 2];
            projection[0] = BULLET_MIMETYPE;

            System.arraycopy(pinyin.toArray(new String[pinyin.size()]), 0, projection, 1, pinyin.size());
            projection[projection.length - 1] = firstRealName;

             c = context.getContentResolver().query(
                    VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                    VoiceSettings.ContactColumns.PROJECTION_CONTACT,
                    condition.toString(),
                     projection, VoiceSettings.ContactColumns.ITEM_NAME_PINYIN +" , "
                    +VoiceSettings.ContactColumns.CONTACT_ID+" , "+VoiceSettings.ContactColumns.DATA_ID);
        }
        ArrayList<ContactStruct> tempList = new ArrayList<ContactStruct>();
        ArrayList<ContactStruct> containsList = new ArrayList<ContactStruct>();
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                ContactStruct contactStruct = new ContactStruct();

                contactStruct.displayName = c.getString(c.getColumnIndex(VoiceSettings.ContactColumns.NAME));
                contactStruct.contactId = c.getLong(c.getColumnIndex(VoiceSettings.ContactColumns.CONTACT_ID));
                contactStruct.photoId = c.getLong(c.getColumnIndex(VoiceSettings.ContactColumns.PHOTE_ID));
                contactStruct.phoneNumber = c.getString(c.getColumnIndex(VoiceSettings.ContactColumns.NUMBER));
                contactStruct.phoneLabel = c.getString(c.getColumnIndex(VoiceSettings.ContactColumns.LABEL));
                contactStruct.dataId = c.getString(c.getColumnIndex(VoiceSettings.ContactColumns.DATA_ID));
                contactStruct.mimeType = c.getString(c.getColumnIndex(VoiceSettings.ContactColumns.MIMETYPE));
                contactStruct.numberLocationInfo= c.getString(c.getColumnIndex(VoiceSettings.ContactColumns.NUMBER_LOCATION_INFO));
                if (contactStruct.displayName == null) {
                    continue;
                }

                if (pinyin == null || pinyin.size() == 1) {
                    contactStruct.matchName = DataLoadUtil.getMatchString(contactStruct.displayName,firstRealName);
                }

                if (contactStruct.displayName.contains(firstRealName)) {
                    containsList.add(contactStruct);
                } else {
                    tempList.add(contactStruct);
                }
            } while (c.moveToNext());
        }
        if (c != null) {
            c.close();
        }
        contactlist.addAll(containsList);
        contactlist.addAll(tempList);
        containsList.clear();
        tempList.clear();
        return contactlist;
    }

    public static boolean hasContact(Context context){
        boolean existContact = true;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(Data.CONTENT_URI,null, null, null, null);
            if (c == null || c.getCount() <= 0) {
                existContact = false;
            }
        } catch (Exception e) {
              existContact = false;
              log.e("exception is "+e.getMessage());
        } finally{
            if (c != null) {
                c.close();
            }
        }
        return existContact;
    }

    public static void validateContacts(Context context, List<ContactStruct> contacts) {
        if (context == null || contacts == null) {
            log.w("validateContacts return for invalid params");
            return;
        }

        // 通过查询lookup_key来验证contactid是否是有效的
        // 处理时注意！contacts中的可能会存在多个ContactStruct都是同一contactid的情况
        int totalSize = contacts.size();
        ArrayList<ContactStruct> resultContacts = new ArrayList<ContactStruct>(totalSize);
        int index = 0;
        while (index < totalSize) {
            int currentSize = Math.min(500, totalSize - index); // 单次最多查询500条
            int start = index;
            int end = index + currentSize;
            index = end;
            HashSet<Long> ids = new HashSet<Long>(currentSize);
            for (int i = start; i < end; i++) {
                ids.add(contacts.get(i).contactId);
            }

            String selection =  String.format("%1$s IS NOT NULL AND %1$s != '' AND %2$s IN(%3$s)",
                    Contacts.LOOKUP_KEY, Contacts._ID, TextUtils.join(",", ids));

            Cursor c = null;
            try {
                c = context.getContentResolver().query(Contacts.CONTENT_URI,
                        new String[] { Contacts._ID }, selection, null, Contacts._ID);

                if (c != null && c.getCount() > 0) {
                    while (c.moveToNext()) {
                        ids.remove(c.getLong(0)); // 移除存在的contactid，最后剩下的即是失效的contactid
                    }
                }
            } catch (Exception e) {
                log.e("validateContacts exception!", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            for (int i = start; i < end; i++) {
                ContactStruct contact = contacts.get(i);
                if (!ids.contains(contact.contactId)) { // 如果不是失效的contactid，就加入到结果集中
                    resultContacts.add(contact);
                }
            }
        }

        log.infoRelease("validateContacts : " + contacts.size() + " -> "  + resultContacts.size());
        if (contacts.size() != resultContacts.size() && log.DEBUG) {
            contacts.removeAll(resultContacts);
            log.i("validateContacts remove invalid contacts : " + contacts);
        }
        contacts.clear();
        contacts.addAll(resultContacts);
    }

    public static boolean isValidPhoneType(String type) {
        try {
            return Integer.valueOf(type).intValue() > 0;
        } catch (NumberFormatException e) {
            // NA
        }
        return false;
    }

    /**
     * 获取通过截取前面或者后面的几个字派生出来的名字
     */
    private static List<String> getCustomNames(String name, SmartWordIterator swi) {
        List<String> customList = new ArrayList<String>();
        name = sNotChinese.matcher(StringUtils.getStringOrEmpty(name)).replaceAll("");
        if (!TextUtils.isEmpty(name)) {
            String tmp = null;
            int len = name.length();

            if (len == 3) { // 取名字后两字（除去姓）
                addIfNotExist(customList, name.substring(1));
            } else if (len == 4) { // 复姓，取前面的姓或者后两个字
                addIfNotExist(customList, name.substring(0, 2));
                addIfNotExist(customList, name.substring(2));
            } else if (len > 4) { // 其他超过四个字的名字使用智能分词处理下
                swi.setCharSequence(name, 0, len);
                int i = 0;
                while (i < len) {
                    int[] seg = swi.getSegment(i);
                    if (seg != null && seg[0] == i && seg[1] <= len && seg[1] - seg[0] > 1) {
                        addIfNotExist(customList, name.substring(seg[0], seg[1]));
                        i = seg[1];
                    } else {
                        i++;
                    }
                }
            }
        }
        return customList;
    }

    /**
     * 获取多音字派生名字
     */
    private static List<String> getMultiPinyinNames(String name) {
        List<String> pinyinList = new ArrayList<String>();
        List<Character> aliasList = PinYinUtil.getMultipleYin(name);
        if (aliasList.size() > 0) {
            // default handle first alias now
            List<Character> pinyinAndAlias = PinYinUtil.getAliasByRealName(aliasList.get(0));
            for (int j = 0; j < pinyinAndAlias.size(); j++) {
                String tmp = name.replaceAll(aliasList.get(0).toString(), pinyinAndAlias.get(j).toString());
                addIfNotExist(pinyinList, tmp);
            }
        }
        return pinyinList;
    }

    private static void addIfNotExist(Collection<String> collection, String item) {
        if (!TextUtils.isEmpty(item) && !collection.contains(item)) {
            collection.add(item);
        }
    }

    public static List<String> getBulletNameList(Context context) {
        HashSet<String> contacts = new HashSet<String>();
        if (VoiceUtils.isPackageExist(context, VoiceConstant.PACKAGE_NAME_BULLET)
                && Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0) == 1) {
            try {
                // 先删除数据库中的旧数据
                ContentResolver cr = context.getContentResolver();
                int deleted = cr.delete(VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                        VoiceSettings.ContactColumns.MIMETYPE + "=?", new String[]{BULLET_MIMETYPE});
                log.infoRelease("getBulletNameList: delete old data from db, size = " + deleted);

                final int pageSize = 1000;
                Bundle pageBundle = new Bundle();
                pageBundle.putInt("pageSize", pageSize);

                int currentPage = 1; // page约定从1开始，不然子弹短信会异常
                boolean haveMoreData = false;
                do {
                    haveMoreData = false; // 重置为false，最开始忘记这个被坑惨了...
                    log.infoRelease("getBulletNameList: query page : " + currentPage);
                    pageBundle.putInt("currentPage", currentPage++);
                    Bundle result = context.getContentResolver().call(Uri.parse("content://com.bullet.messenger"),
                            "FLASHIM_ALL_BULLET_CONTACT", null, pageBundle);

                    if (result != null) {
                        ArrayList<String> data = result.getStringArrayList("KEY_CONTENT_ALL_BULLET");
                        int dataSize = 0;
                        if (data != null && (dataSize = data.size()) > 0) {
                            log.infoRelease("getBulletNameList: get data size = " + dataSize + " for page = " + (currentPage-1));
                            SmartWordIterator smartWordIterator = new SmartWordIterator(context);
                            for (int i = 0; i < dataSize; i++) {
                                String item = null;
                                try {
                                    item = new JSONObject(data.get(i)).optString("contactName");
                                } catch (JSONException e) {
                                    log.e("getBulletNameList: parse name failed!", e);
                                }

                                if (data.contains(item)) { // 过滤重复元素
                                    log.w("getBulletNameList: mark duplicate item to null! " + item);
                                    item = null;
                                }
                                data.set(i, item);
                                if (!TextUtils.isEmpty(item)) {
                                    contacts.add(item);
                                    contacts.addAll(getCustomNames(item, smartWordIterator));
                                    contacts.addAll(getMultiPinyinNames(item));
                                }
                            }
                            saveBulletContacts(cr, data);

                            if (dataSize == pageSize && isBulletSupportPage(context) && currentPage < 10) { // 最多10次循环
                                haveMoreData = true;
                            }
                        }
                    }
                } while (haveMoreData);
            } catch (Exception e) {
                log.e("getBulletNameList error!", e);
            }
        }

        return new ArrayList<String>(contacts);
    }

    private static int sBulletSupportPage = -1;
    /**
     * 判断当前子弹短信版本是否支持分页
     */
    private static boolean isBulletSupportPage(Context context) {
        if (sBulletSupportPage < 0) { // 未初始化
            boolean supported = false;
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo packinfo = pm.getPackageInfo(VoiceConstant.PACKAGE_NAME_BULLET, 0);
                if (packinfo != null) {
                    String versionName = packinfo.versionName;
                    //v0.8.6 第一次使用这个
                    if (!TextUtils.isEmpty(versionName) && versionName.compareTo("0.8.6") >= 0) {
                        supported = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (supported) {
                sBulletSupportPage = 1;
            } else {
                sBulletSupportPage = 0;
            }
        }

        return sBulletSupportPage > 0;
    }

    /**
     * 将子弹短信联系人存储到数据库以便后续getContacts时匹配
     */
    private static void saveBulletContacts(ContentResolver cr, List<String> contacts) {
        List<ContentValues> dataValues = new ArrayList<ContentValues>(contacts.size());
        for (String contact : contacts) {
            if (TextUtils.isEmpty(contact))
                continue;

            ContentValues cv = new ContentValues();
            cv.put(VoiceSettings.ContactColumns.MIMETYPE, BULLET_MIMETYPE);
            cv.put(VoiceSettings.ContactColumns.NAME, contact);
            cv.put(VoiceSettings.ContactColumns.ITEM_NAME_PINYIN,
                    PinYinUtil.getPinYin(DataLoadUtil.normalizeKey(contact)));
            dataValues.add(cv);
        }

        int inserted = cr.bulkInsert(VoiceSettings.ContactColumns.CONTENT_URI_CONTACT,
                dataValues.toArray(new ContentValues[dataValues.size()]));
        log.infoRelease("saveBulletContacts: " + dataValues.size() + " -> " + inserted);
    }
}
