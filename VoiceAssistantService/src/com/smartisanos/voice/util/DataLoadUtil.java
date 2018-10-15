package com.smartisanos.voice.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import smartisanos.app.numberassistant.YellowPageResult;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.MediaStruct;

import com.smartisanos.voice.R;

public class DataLoadUtil {
    static final LogUtils log = LogUtils.getInstance(DataLoadUtil.class);
    public  static ArrayList<ApplicationStruct> loadApps(Context context, String str, String packageName) {
        ArrayList<String> realName = new ArrayList<String>();
        String tmp = StringUtils.trimPunctuation(str);
        ArrayList<ApplicationStruct> totalApps = new ArrayList<>();
        ArrayList<ApplicationStruct> tmpapps = ApplicationUtil.getSearchedLocalAppNameList(context, tmp, realName);
        if (tmpapps != null && tmpapps.size() > 0) {
            totalApps.addAll(ApplicationUtil.OrderApps(context,tmpapps, realName));
        }
        log.d("packageName:" + packageName);
        if (VoiceConstant.PACKAGE_NAME_SARA.equals(packageName)) {
            boolean isNetworkAvailable = VoiceUtils.isNetworkAvailable(context);
            log.d("isNetworkAvailable:" + isNetworkAvailable);
            if (isNetworkAvailable) {
                ArrayList<ApplicationStruct> uninstalledList = ApplicationUtil.getSearchedUninstallAppNameList(context, str);
                if (uninstalledList != null && uninstalledList.size() > 0) {
                    totalApps.addAll(uninstalledList);
                }
            }
        }
        return totalApps;
    }


    public  static ArrayList<ContactStruct> loadContacts(Context context, String[] str) {
        return loadContacts(context, str, false);
    }

    public static ArrayList<ContactStruct> loadContacts(Context context, String[] str, boolean isBulletContacts) {
        ArrayList<ContactStruct> contactlist = new ArrayList<ContactStruct>();
        ArrayList<String> temp = new ArrayList<String>();

        String realName = StringUtils.trimPunctuation(str[0].split(",")[0]);
        Map<Character, Character> reals = PinYinUtil.getRealByAliasName(realName);
        Iterator<Entry<Character, Character>> ite = reals.entrySet().iterator();
        while (ite.hasNext()) {
            Entry<Character, Character> entry = ite.next();
            String alias = realName.replaceAll(entry.getKey().toString(), entry.getValue().toString());
            String aliasPinyin = PinYinUtil.getPinYin(alias);
            String realPinyin = PinYinUtil.getPinYin(realName);
            if (!aliasPinyin.equals(realPinyin)) {
                contactlist = ContactsUtil.getContacts(context, null, contactlist, alias, true, isBulletContacts);
            } else {
                realName = alias;
            }
            break;
        }
        for (int i = 0; i < str.length; i++) {
            String tmp = PinYinUtil.getPinYin(StringUtils.trimPunctuation(str[0].split(",")[0]));
            if (i == 0) {
                temp.add("%" + tmp + "%");
            } else if (i < str.length) {
                temp.add(tmp);
            }
        }
        contactlist= ContactsUtil.getContacts(context, temp, contactlist, realName, false, isBulletContacts);
        if (!contactlist.isEmpty() && !isBulletContacts) {
            ContactsUtil.validateContacts(context, contactlist);
        }
        return contactlist;
    }

    public  static ArrayList<MediaStruct>  loadMusics(Context context, final String[] str) {
    ArrayList<MediaStruct>  musics = new ArrayList<MediaStruct>();
    Resources mResources = context.getResources();
       String randomPlay = mResources.getString(R.string.random_play);
       String random = mResources.getString(R.string.random);
       String listenMusic = mResources.getString(R.string.listen_music);
       String music = mResources.getString(R.string.music);
       boolean isInWhiteList = false;
       for (int i = 0; i < str.length; i++) {
           String tmp = StringUtils.trimPunctuation(str[0].split(",")[0]);
           String pinyinTmp = PinYinUtil.getPinYin(StringUtils.trimPunctuation(str[0].split(",")[0]));
           if (listenMusic.equals(tmp)
                   || PinYinUtil.getPinYin(listenMusic).equals(pinyinTmp)
                   || music.equals(tmp)
                   || PinYinUtil.getPinYin(music).equals(pinyinTmp)) {
               MediaUtil.getAllMediaUriByMusicName(
                       context, musics);
               isInWhiteList = true;
               break;
           } else if (randomPlay.equals(tmp)
                   || PinYinUtil.getPinYin(randomPlay).equals(pinyinTmp)
                   || random.equals(tmp)
                   || PinYinUtil.getPinYin(random).equals(pinyinTmp)) {
               MediaUtil.getRandomMediaStructList(context, musics);
               isInWhiteList = true;
               break;
           }
       }
       if (!isInWhiteList) {
           String resultName = PinYinUtil.getPinYin(StringUtils.trimPunctuation(str[0].split(",")[0]));
           MediaUtil.getMediaStructList(context, resultName, musics, StringUtils.trimPunctuation(str[0].split(",")[0]));
       }

       final boolean needOrder = !isInWhiteList;
       if (musics != null && musics.size() > 0) {
           Comparator<MediaStruct> comparator = new Comparator<MediaStruct>() {
               @Override
               public int compare(MediaStruct s1, MediaStruct s2) {
                   if (s1.mFlagType > s2.mFlagType) {
                       return -1;
                   } else if (s1.mFlagType < s2.mFlagType) {
                       return 1;
                   } else {
                       if (!needOrder) {
                           return 0;
                       } else {
                           return MediaUtil.orderMusic(s1, s2, StringUtils.trimPunctuation(str[0].split(",")[0]));
                       }
                   }
               }
           };
           System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
           Collections.sort(musics, comparator);
       }
       return musics;
    }

    public static  List<YellowPageResult> loadYellowPages(Context context, String str) {
        CootekManager cootekManager = CootekManager.getInstance(context);
        String tmp = StringUtils.trimPunctuation(str);
        if (Locale.TAIWAN.equals(Locale.getDefault())) {
            tmp = ZhConverter.convert(context, tmp);
        }
        return cootekManager.queryYellowPage(tmp, 0);
    }
    static class LaunchCountComparator implements Comparator<ApplicationStruct> {
        public final int compare(ApplicationStruct a, ApplicationStruct b) {
            return a.mLaunchCount - b.mLaunchCount;
        }
    }

    public static String getMatchString(String displayName, String keyName) {
        keyName = StringUtils.trimPunctuation(keyName).trim();
        ArrayList<String> keyPinyins = new ArrayList<String>(keyName.length());
        for (int i = 0; i < keyName.length(); i++) {
            String pinyin = PinYinUtil.getPinYin(keyName.substring(i, i+1));
            if (TextUtils.isEmpty(pinyin)) {
                continue;
            }

            keyPinyins.add(pinyin);
        }

        if (keyPinyins.size() > 0) {
            String punc = VoiceConstant.REGEX_PUNCTUATION + "*?";
            Pattern keyPinyinPattern = Pattern.compile(TextUtils.join(punc, keyPinyins));
            Matcher matcher = keyPinyinPattern.matcher(PinYinUtil.getPinYin(displayName));
            if (matcher.find()) {
                int displayLen = displayName.length();
                int pinYinMatchLength = 0;
                int start = -1;
                for (int i = 0; i < displayLen; i++) {
                    if (matcher.start() == pinYinMatchLength) {
                        start = i;
                    }

                    pinYinMatchLength += PinYinUtil.getPinYin(String.valueOf(displayName.charAt(i))).length();

                    if (matcher.end() == pinYinMatchLength) {
                        if (start >= 0) {
                            return displayName.substring(start, i + 1);
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static String normalizeKey(String key) {
        List<String> list = normalizeKeys(Arrays.asList(key));
        return list.size() > 0 ? list.get(0) : "";
    }

    public static List<String> normalizeKeys(List<String> keys) {
        ArrayList<String> nKeys = new ArrayList<String>(keys != null ? keys.size() : 0);
        if (keys != null) {
            Pattern pSpecial = Pattern.compile(VoiceConstant.REGEX_SPECIAL);
            Pattern pNotNormal = Pattern.compile(VoiceConstant.REGEX_NOT_NORMAL);
            Pattern pSpace = Pattern.compile("\\s+");
            String space = "\u0020";
            for (String key : keys) {
                if (TextUtils.isEmpty(key)) {
                    continue;
                }
                key = pSpecial.matcher(key).replaceAll(space).trim(); // filter special
                key = pNotNormal.matcher(key).replaceAll(space).trim(); // filter not normal
                key = pSpace.matcher(key).replaceAll(space); // remove extra spaces.
                if (!TextUtils.isEmpty(key) && !nKeys.contains(key)) {
                    nKeys.add(key);
                }
            }
        }

        return nKeys;
    }
}
