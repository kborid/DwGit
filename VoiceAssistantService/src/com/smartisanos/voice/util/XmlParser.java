package com.smartisanos.voice.util;

import android.text.TextUtils;
import android.util.Pair;
import android.util.Xml;
import android.net.Uri;

import com.smartisanos.voice.engine.GrammarManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.RecognizeResult;

public class XmlParser {
    static final LogUtils log = LogUtils.getInstance(XmlParser.class);

    static final int ID_NOT_DEFINE = 65535;
    private static Pattern REPLACE_GROUP_PATTERN = Pattern.compile("^\\[([0-9]+),([0-9]+)\\]$");

    public static RecognizeResult parseNluResult(String xml, boolean isCandidate) {
        log.i("xml is " + xml);
        boolean offline = false;
        try {
            List<RecognizeResult.Item> items = new ArrayList<RecognizeResult.Item>();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xml));
            int type = parser.getEventType();
            boolean isLocalObjectTag = false;
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        if ("rawtext".equals(parser.getName())) {
                            items.add(new RecognizeResult.Item(parser.nextText()));
                        } else if ("object".equals(parser.getName()) && offline) {
                            isLocalObjectTag = true;
                        } else if (isLocalObjectTag) {
                            int id = RecognizeResult.ID_INVALID;
                            try {
                                id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                            } catch (NumberFormatException e) {
                                // NA
                            }

                            String name = parser.getName();
                            String value = parser.nextText();

                            if (id == ID_NOT_DEFINE) {
                                id = RecognizeResult.ID_INVALID;
                            } else if (id > ID_NOT_DEFINE) {
                                id -= GrammarManager.Command.ID_OFFSET;
                            } else if (!TextUtils.equals(name, value) && id >= 0) {
                                String lexicon = "<" + name + ">";
                                if (GrammarManager.isPreloadLexicon(lexicon)) {
                                    id = GrammarManager.getPreloadLexiconId(lexicon);
                                }
                            }

                            if (id != RecognizeResult.ID_INVALID) {
                                items.add(new RecognizeResult.Item(id, value));
                            }
                        } else if ("engine".equals(parser.getName())) {
                            offline = "local".equals(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("object".equals(parser.getName())) {
                            isLocalObjectTag = false;
                        }
                        break;
                }
                type = parser.next();
            }

            RecognizeResult result = new RecognizeResult(items.toArray(new RecognizeResult.Item[items.size()]));
            result.setOffline(offline);
            return result;
        } catch (Exception e) {
            log.i("exception and need to parse by iat! error: " + e.getMessage());
            return parseIatResult(xml, isCandidate);
        }
    }

    public static RecognizeResult parseIatResult(String json, boolean isCandidate) {
    	log.i("json is " + json);
    	RecognizeResult result = new RecognizeResult();
        try {
            List<RecognizeResult.Item> resultItems = new ArrayList<RecognizeResult.Item>();
            JSONObject joResult = new JSONObject(json);
            int id = joResult.optInt("sn");
            if ("rpl".equals(joResult.optString("pgs"))) {
                Matcher matcher = REPLACE_GROUP_PATTERN.matcher(joResult.optString("rg"));
                if (matcher.find()) {
                    result.setReplaceStart(Integer.parseInt(matcher.group(1)));
                    result.setReplaceEnd(Integer.parseInt(matcher.group(2)));
                }
            }
            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                for (int j = 0; j < items.length(); j++) {
                    resultItems.add(new RecognizeResult.Item(id, items.getJSONObject(j).getString("w")));
                }
            }

            result.setItems(resultItems.toArray(new RecognizeResult.Item[resultItems.size()]));
            if (isCandidate) {
                result.setCandidate(true);
            } else {
                result.setItems(new RecognizeResult.Item(id, TextUtils.join("", result.getContents())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<ApplicationStruct> parseApp(String jsonString) {
        if (TextUtils.isEmpty(jsonString)) {
            return null;
        }
        ArrayList<ApplicationStruct> appList = new ArrayList<ApplicationStruct>();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String content = jsonObject.getString("body");
            JSONObject contentJson = new JSONObject(content);
            JSONArray jsonArray = contentJson.getJSONArray("apps");
            if (jsonArray == null || jsonArray.length() <= 0) {
                return null;
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = new JSONObject(jsonArray.getString(i));
                ApplicationStruct as = new ApplicationStruct();
                as.setAppName(jo.getString("name"));
                as.setPackageName(jo.getString("package"));
                as.setIconUri(Uri.parse(jo.getString("logo")));
                as.setInstalledState(0);
                as.setAppSize(jo.getString("filesize"));
                appList.add(as);
            }
        } catch (JSONException e) {
            log.d("JSONException e = " + e.toString());
        }
        return appList;
    }
}
