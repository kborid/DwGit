package com.smartisanos.voice.engine;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.smartisanos.voice.R;
import com.smartisanos.voice.util.ApplicationUtil;
import com.smartisanos.voice.util.ContactsUtil;
import com.smartisanos.voice.util.DataLoadUtil;
import com.smartisanos.voice.util.FileUtils;
import com.smartisanos.voice.util.LogUtils;
import com.smartisanos.voice.util.MediaUtil;
import com.smartisanos.voice.util.StringUtils;
import com.smartisanos.voice.util.VoiceConstant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smartisanos.app.voiceassistant.RecognizeResult;

public class GrammarManager {

    public interface LexiconUpdateResult {
        void onResult(String lexicon, int resultCode);
    }

    public interface LexiconUpdater {
        void onUpdate(String name, String content, LexiconUpdateResult cbk);
    }

    static final LogUtils log = LogUtils.getInstance(GrammarManager.class);

    public static final String LEXICON_HOT_WORD = "<userword>";
    public static final String LEXICON_APP = "<app>";
    public static final String LEXICON_APP_EXT = "<appext>";
    public static final String LEXICON_CONTACT = "<contact>";
    public static final String LEXICON_MUSIC = "<music>";
    public static final String LEXICON_YELLOW = "<yellow>";
    public static final String LEXICON_COMMAND = "<command>";

    public static final String SCENE_SEARCH = "search";

    private static List<Grammar> sPreloadGrammars;

    private final String GRAMMAR_DIR;
    private final Context mContext;
    private final LexiconUpdater mLexiconUpdater;
    private final Handler mGrammarHandler;
    private boolean mInited = false;

    private final List<Grammar> mGrammars = new ArrayList<Grammar>();
    private final HashMap<String, UserGrammar> mUserGrammars = new HashMap<String, UserGrammar>();

    private final ArrayDeque<Task> mTasks = new ArrayDeque<Task>();
    private Task mActive;

    public GrammarManager(Context context, LexiconUpdater updater) {
        if (context == null || updater == null) {
            throw new RuntimeException("GrammarManager invalid params!");
        }

        GRAMMAR_DIR = context.getDir("grammars", Context.MODE_PRIVATE).getAbsolutePath();
        mContext = context;
        mLexiconUpdater = updater;
        HandlerThread grammarThread = new HandlerThread("grammar manager");
        grammarThread.start();
        mGrammarHandler = new GrammarHandler(grammarThread.getLooper());
        mGrammarHandler.obtainMessage(MSG_INIT).sendToTarget();
    }

    private void init() {
        parsePreloadGrammar();
        parseUserGrammar();
        if (hasUserGrammar()) {
            copyPreloadGrammarFiles();  // 拷贝预置语法文件到生成的用户语法文件所在的目录
            // 由于“GRAMMAR_DIR”是私有的，需要使用SharedProvider暴露给讯飞引擎，而SharedProvider
            // 是使用assets来打开语法文件的InputStream的，因此把“GRAMMAR_DIR”添加到assert path中
            mContext.getAssets().addAssetPath(GRAMMAR_DIR);
        }
        mGrammars.addAll(sPreloadGrammars);
        mGrammars.addAll(mUserGrammars.values());
        mInited = true;
        log.infoRelease("init finish!");
        updateLexicon(null, null); // update all lexicon
    }

    public boolean isInited() {
        return mInited;
    }

    public void destroy() {
        mGrammarHandler.removeCallbacksAndMessages(null);
        mGrammarHandler.getLooper().quitSafely();
        mInited = false;
    }

    public String[] getAllGrammarFiles() {
        String[] files = new String[mGrammars.size()];
        for (int i = 0; i < mGrammars.size(); i++) {
            files[i] = mGrammars.get(i).file;
        }
        log.infoRelease("getAllGrammarFiles: " + Arrays.toString(files));
        return files;
    }

    public String[] getAllLexicons() {
        HashSet<String> lexicons = new HashSet<String>();
        for (Grammar grammar : mGrammars) {
            lexicons.addAll(grammar.lexicons);
        }

        String[] result = new String[lexicons.size()];
        lexicons.toArray(result);
        log.infoRelease("getAllLexicons: " + lexicons);
        return result;
    }

    public boolean hasUserGrammar() {
        return !mUserGrammars.isEmpty();
    }

    public String getUserScene(String grammarName) {
        Grammar grammar = mUserGrammars.get(grammarName);
        return grammar != null ? grammar.scene : null;
    }

    public String[] getGrammarFiles(String lexicon) {
        ArrayList<String> files = new ArrayList<String>();

        for (Grammar grammar : mGrammars) {
            if (grammar.lexicons.contains(lexicon)) {
                files.add(grammar.file);
            }
        }

        String[] result = new String[files.size()];
        files.toArray(result);
        log.infoRelease(lexicon + " getGrammarFiles: " + Arrays.toString(result));
        return result;
    }

    public String[] getScenes(String lexicon) {
        ArrayList<String> scenes = new ArrayList<String>();

        for (Grammar grammar : mGrammars) {
            if (grammar.lexicons.contains(lexicon)) {
                scenes.add(grammar.scene);
            }
        }

        String[] result = new String[scenes.size()];
        scenes.toArray(result);
        log.infoRelease(lexicon + " getScenes: " + Arrays.toString(result));
        return result;
    }

    public static boolean isPreloadScene(String name) {
        for (Grammar grammar : sPreloadGrammars) {
            if (TextUtils.equals(grammar.scene, name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPreloadLexicon(String name) {
        HashSet<String> lexicons = new HashSet<String>();
        for (Grammar grammar : sPreloadGrammars) {
            lexicons.addAll(grammar.lexicons);
        }
        return lexicons.contains(name) || lexicons.contains("<"+name+">");
    }

    public static int getPreloadLexiconId(String lexicon) {
        int id = RecognizeResult.ID_INVALID;
        if (isPreloadLexicon(lexicon)) {
            if (LEXICON_APP.equals(lexicon)) {
                id = RecognizeResult.ID_APP;
            } else if (LEXICON_CONTACT.equals(lexicon)) {
                id = RecognizeResult.ID_CONTACT;
            } else if (LEXICON_MUSIC.equals(lexicon)) {
                id = RecognizeResult.ID_MUSIC;
            }
        }

        return id;
    }

    public static String getPreloadLexiconName(int id) {
        switch (id) {
            case RecognizeResult.ID_APP:
                return "app";
            case RecognizeResult.ID_CONTACT:
                return "contact";
            case RecognizeResult.ID_MUSIC:
                return "music";
        }

        return null;
    }

    public void updateLexicon(final String lexicon) {
        updateLexicon(lexicon, null);
    }

    public void updateLexicon(final String lexicon, List<String> words) {
        if (lexicon == null) {
            for (String lex : getAllLexicons()) {
                updateLexicon(lex);
            }
        } else if (LEXICON_HOT_WORD.equals(lexicon) || getGrammarFiles(lexicon).length > 0) {
            updateLexiconInternal(new Task(lexicon, words));
            if (LEXICON_APP.equals(lexicon) || LEXICON_CONTACT.equals(lexicon)) {
                updateLexicon(LEXICON_HOT_WORD);
            }

            if (LEXICON_APP.equals(lexicon)) {
                updateLexicon(LEXICON_APP_EXT);
            }
        }
    }

    private void updateLexiconInternal(Task task) {
        if (task == null) {
            return;
        }

        synchronized (mTasks) {
            if (mTasks.remove(task)) {
                log.infoRelease("updateLexiconInternal: update: " + task.lexicon);
                mTasks.offer(task);
            } else {
                mTasks.offer(task);
                log.infoRelease("updateLexiconInternal: put: " + task.lexicon);
            }

            if (mInited && mActive == null) {
                scheduleNext();
            }
        }
    }

    protected void scheduleNext() {
        synchronized (mTasks) {
            if ((mActive = mTasks.poll()) != null) {
                log.infoRelease("scheduleNext: " + mActive.lexicon);
                mGrammarHandler.post(mActive);
            }
        }
    }

    private static final int TIME_FAIL_INTERNAL = 5000;
    private static final int TIME_RESULT_TIMEOUT = 3000;

    private class Task implements Runnable {
        String lexicon;
        List<String> words;
        String content;
        int retry = 0;

        Runnable resultTimeoutCheck = new Runnable() {
            @Override
            public void run() {
                handleResult(-2);
            }
        };

        LexiconUpdateResult updateCbk = new LexiconUpdateResult() {
            @Override
            public void onResult(String name, int resultCode) {
                if (TextUtils.equals(lexicon, name)) {
                    Message msg = mGrammarHandler.obtainMessage(MSG_ON_RESULT, Task.this);
                    msg.arg1 = resultCode;
                    msg.sendToTarget();
                } else {
                    log.e("onResult error: " + lexicon + " != " + name);
                }
            }
        };

        public Task(String lexicon, final List<String> words) {
            this.lexicon = lexicon;
            this.words = words;
        }

        @Override
        public void run() {
            loadContent();
            if (!TextUtils.isEmpty(content)) {
                log.i("Task run [" + lexicon + "] : " + content.substring(0, Math.min(200, content.length())));
                mLexiconUpdater.onUpdate(lexicon, content, updateCbk);
                mGrammarHandler.removeCallbacks(resultTimeoutCheck);
                mGrammarHandler.postDelayed(resultTimeoutCheck, TIME_RESULT_TIMEOUT);
            } else {
                log.e("Task run [" + lexicon + "] : empty content!!!");
                scheduleNext();
            }
        }

        public void handleResult(int resultCode) {
            log.infoRelease("handleResult: " + lexicon + " -> " + resultCode + ", retry = " + retry);
            mGrammarHandler.removeCallbacks(resultTimeoutCheck);
            boolean finished = resultCode == 0;
            if (!finished) {
                if (retry < 5) {
                    ++retry;
                    mGrammarHandler.removeCallbacks(this);
                    mGrammarHandler.postDelayed(this, TIME_FAIL_INTERNAL);
                } else {
                    log.e("handleResult: " + lexicon + " update failed!!!");
                    finished = true;
                }
            }

            if (finished) {
                scheduleNext();
            }
        }

        private void loadContent() {
            if (content != null) {
                return;
            }

            if (words == null) {
                words = loadWords(lexicon);
            } else {
                words = DataLoadUtil.normalizeKeys(words);
            }

            if (LEXICON_HOT_WORD.equals(lexicon)) {
                if (words == null) {
                    words = new ArrayList<String>();
                }
                // always add the app & contact.
                List<String> tmp;
                if ((tmp = loadWords(LEXICON_APP)) != null) {
                    words.addAll(tmp);
                }

                if ((tmp = loadWords(LEXICON_CONTACT)) != null) {
                    words.addAll(tmp);
                }

                if ((tmp = loadWords(LEXICON_COMMAND)) != null) {
                    words.addAll(tmp);
                }

                content = toHotWordJson(words);
            } else {
                content = toContentString(words);
            }
            log.infoRelease("loadContent: " + lexicon + ", words size = " + (words != null ? words.size() : 0));
            words = null;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Task)) {
                return false;
            }

            return TextUtils.equals(lexicon, ((Task) obj).lexicon);
        }
    }

    public static final int MSG_INIT = 0;
    public static final int MSG_ON_RESULT = 1;
    private class GrammarHandler extends Handler {
        public GrammarHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    init();
                    break;
                case MSG_ON_RESULT:
                    ((Task) msg.obj).handleResult(msg.arg1);
                    break;
            }
        }
    }

    private List<String> loadWords(String lexicon) {
        List<String> words = null;
        if (LEXICON_APP.equals(lexicon)) {
            words = ApplicationUtil.getAppNameList(mContext);
        } else if (LEXICON_APP_EXT.equals(lexicon)) {
            words = new ArrayList<String>();
            ArrayList<String> names = ApplicationUtil.getAppNameList(mContext);

            if (names != null && names.size() > 0) {
                for (String name : names) {
                    words.addAll(ApplicationUtil.getCustomizedTitle(name));
                }
                words.addAll(ApplicationUtil.getAliasName(mContext, names));
            }
        } else if (LEXICON_CONTACT.equals(lexicon)) {
            words = ContactsUtil.getContactNameList(mContext);
        } else if (LEXICON_MUSIC.equals(lexicon)) {
            words = MediaUtil.getMediaNameList(mContext, true);
            String tempMusicWhiteList = null;
            try {
                byte[] content = StringUtils.readFileFromAssets(mContext, VoiceConstant.MUSIC_WHITELIST_FILE);
                tempMusicWhiteList = new String(content, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!TextUtils.isEmpty(tempMusicWhiteList)) {
                String[] temp = tempMusicWhiteList.split(",");
                for (int i = 0; i < temp.length; i++) {
                    if (words.size() > 0 && !words.contains(temp[i]) && !TextUtils.isEmpty(temp[i])) {
                        words.add(temp[i]);
                    }
                }
            }
        } else if (LEXICON_YELLOW.equals(lexicon)) {
            words = loadYellowPage();
        } else if (LEXICON_COMMAND.equals(lexicon)) {
            words = loadCommands();
        }

        words = DataLoadUtil.normalizeKeys(words);
        return words;
    }

    private List<String> loadYellowPage(){
        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        try {
            inputReader = new InputStreamReader(mContext.getResources().getAssets().open(VoiceConstant.YELLOW_DATA));
            bufReader = new BufferedReader(inputReader);
            String line = "";
            StringBuffer result = new StringBuffer();
            while ((line = bufReader.readLine()) != null) result.append(line);
            return Arrays.asList(result.toString().split("\\|"));
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            try {
                if (bufReader != null) {
                    bufReader.close();
                }
                if (inputReader != null) {
                    inputReader.close();
                }
            } catch (Exception e2) {
            }
        }
    }

    protected ArrayList<String> loadCommands() {
        HashSet<String> commands = new HashSet<String>();
        XmlResourceParser xmlParser = mContext.getResources().getXml(R.xml.trio_voice_commands);
        int event = 0;
        try {
            event = xmlParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tag = xmlParser.getName();
                        if (("value".equals(tag) || "item".equals(tag))) {
                            commands.add(xmlParser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                event = xmlParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>(commands);
    }

    private static String toContentString(List<String> words) {
        if (words != null && words.size() > 0) {
            return TextUtils.join("|", words);
        }

        return "";
    }

    private static String toHotWordJson(List<String> words) {
        String content = "";
        if (words != null && words.size() > 0) {
            try {
                JSONObject userwordObject = new JSONObject();
                userwordObject.put("name", "ContactList");
                userwordObject.put("words", new JSONArray(DataLoadUtil.normalizeKeys(words)));
                JSONArray groupArray = new JSONArray();
                groupArray.put(userwordObject);
                JSONObject rootObject = new JSONObject();
                rootObject.put("userword", groupArray);
                content = rootObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return content;
    }

    private void parsePreloadGrammar() {
        List<Grammar> grammars = new ArrayList<Grammar>();

        String[] files = null;
        try {
            files = mContext.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (files != null && files.length > 0) {
            Pattern pScene = Pattern.compile("!grammar (.+);");
            Pattern pLexicon = Pattern.compile("!slot (.+);");
            Pattern pEnd = Pattern.compile("!start .+;");
            for (int i = 0; i < files.length; i++) {
                String file = files[i];
                String scene = null;
                List<String> lexicons = new ArrayList<String>();
                if (!TextUtils.isEmpty(file) && file.endsWith(".mp3")) {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(mContext.getAssets().open(file)));
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            Matcher matcher = null;
                            if ((matcher = pScene.matcher(line)).matches()) {
                                scene = matcher.group(1);
                            } else if ((matcher = pLexicon.matcher(line)).matches()) {
                                lexicons.add(matcher.group(1));
                            } else if (pEnd.matcher(line).matches()) {
                                break;
                            }
                        }
                        reader.close();

                        if (!TextUtils.isEmpty(scene) && !lexicons.isEmpty()) {
                            grammars.add(new Grammar(file, scene, lexicons));
                            log.infoRelease(String.format("parsePreloadGrammar: add grammar(%s, %s, %s)", file, scene, lexicons.toString()));
                        }
                    } catch (IOException e) {
                        log.e("parsePreloadGrammar error! " + file, e);
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                log.e("parsePreloadGrammar error! " + file, e);
                            }
                        }
                    }
                }
            }
        }

        sPreloadGrammars = grammars;
    }

    private void copyPreloadGrammarFiles() {
        for (Grammar grammar : sPreloadGrammars) {
            try {
                String copied = GRAMMAR_DIR + File.separator + FileUtils.getFileNameWithoutExtension(grammar.file) + ".gmr";
                FileUtils.writeFile(copied, mContext.getAssets().open(grammar.file));
                grammar.file = copied;
            } catch (Exception e) {
                log.e("init: copy grammar file error : " + grammar.file, e);
            }
        }
    }

    private void parseUserGrammar() {
        XmlResourceParser xmlParser = mContext.getResources().getXml(R.xml.trio_voice_commands);
        int event = 0;
        try {
            boolean exclude = false;
            int commandId = 0;
            UserGrammar grammar = null;
            List<Command> commands = new ArrayList<Command>();
            Command command = null;
            List<String> alias = null;
            List<String> rules = new ArrayList<String>();
            String tag = null;
            event = xmlParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                tag = xmlParser.getName();
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if ("module".equals(tag)) {
                            rules.clear();
                            grammar = new UserGrammar(xmlParser.getAttributeValue(null, "name"));
                            commandId = 0; // reset the command id.
                        } else if ("rule".equals(tag)) {
                            String rule = xmlParser.nextText().trim();
                            if (!rule.isEmpty()) {
                                rules.add(rule);
                            }
                        } else if ("command".equals(tag)) {
                            alias = new ArrayList<String>();
                            command = new Command();

                            exclude = xmlParser.getAttributeBooleanValue(null, "exclude", false);
                            command.id = xmlParser.getAttributeIntValue(null, "id", -1);
                            if (command.id < 0) {
                                command.id = ++commandId;
                            }

                            command.notInBaseGroup = xmlParser.getAttributeBooleanValue(null, "notbase", false);

                            String ops = xmlParser.getAttributeValue(null, "groups");
                            if (!TextUtils.isEmpty(ops)) {
                                command.groups = Arrays.asList(ops.split(","));
                            }
                        } else if ("value".equals(tag)) {
                            command.value = xmlParser.nextText();
                        } else if ("item".equals(tag)) {
                            alias.add(xmlParser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("module".equals(tag)) {
                            if (grammar.createGrammarFile(GRAMMAR_DIR, rules, commands)) {
                                mUserGrammars.put(grammar.name, grammar);
                                log.infoRelease("parseUserGrammar: add grammar " + grammar.scene);
                            }
                            grammar = null;
                            commands.clear();
                        } else if ("command".equals(tag)) {
                            if (!exclude) {
                                command.alias = alias;
                                commands.add(command);
                            }
                        }
                        break;
                    default:
                        break;
                }
                event = xmlParser.next();
            }
        } catch (Exception e) {
            log.e("parseUserGrammar error: ", e);
        }
    }


    public static class Grammar {
        String file;
        String name;
        String scene;
        List<String> lexicons;

        public Grammar() {}

        public Grammar(String file, String scene, List<String> lexicons) {
            this.file = file;
            this.scene = scene;
            this.lexicons = lexicons != null ? lexicons : new ArrayList<String>();
        }
    }

    public static class UserGrammar extends Grammar {
        private static final String TAIL = ";\r\n";
        private static final String RULE_ROOT = "root";
        private static final String RULE_BASE = "base";

        public UserGrammar(String name) {
            lexicons = new ArrayList<String>();
            this.name = name;
            this.scene = getHashName(name);
        }

        public boolean createGrammarFile(String dirs, List<String> rules, List<Command> commands) {
            if (TextUtils.isEmpty(name)) {
                return false;
            }

            HashMap<String, List<Command>> groups = new HashMap<String, List<Command>>();
            Pattern gPattern = Pattern.compile("<([a-zA-Z0-9]+)>");
            for (int i = 0; i < rules.size(); i++) {
                List<String> gps = new ArrayList<String>();
                Matcher matcher = gPattern.matcher(rules.get(i));
                while (matcher.find()) {
                    String gp = matcher.group(0);
                    if (isPreloadLexicon(gp)) {
                        if (!lexicons.contains(gp)) {
                            lexicons.add(gp);
                        }
                    } else {
                        gps.add(matcher.group(1));
                        if (!groups.containsKey(matcher.group(1))) {
                            groups.put(matcher.group(1), new ArrayList<Command>());
                        }
                    }
                }

                String rule = rules.get(i);
                for (String gp : gps) {
                    rule = rule.replaceFirst("<"+gp+">", "<" + getIdentityName(gp) + ">");
                }
                rules.set(i, rule);
            }

            List<Command> baseRule = new ArrayList<Command>();
            for (Command item : commands) {
                if (!item.notInBaseGroup) {
                    baseRule.add(item);
                }
                if (item.groups != null) {
                    for (String group : item.groups) {
                        if (groups.containsKey(group)) {
                            groups.get(group).add(item);
                        }
                    }
                }
            }

            if (baseRule.size() > 0) {
                groups.put(RULE_BASE, baseRule);
            }

            StringBuffer buffer = new StringBuffer("#BNF+IAT 1.0 UTF-8" + TAIL);
            buffer.append("!grammar ").append(scene).append(TAIL);

            for (String slot : groups.keySet()) {
                buffer.append("!slot <").append(getIdentityName(slot)).append(">").append(TAIL);
            }

            for (String slot : lexicons) {
                buffer.append("!slot ").append(slot).append(TAIL);
            }

            buffer.append("!start <").append(RULE_ROOT).append(">").append(TAIL);

            buffer.append(toRootRule(rules.size(), groups.containsKey(RULE_BASE)));
            for (int i = 0; i < rules.size(); i++) {
                buffer.append("<").append("rule").append(i).append(">:").append(rules.get(i)).append(TAIL);
            }

            for (Map.Entry<String, List<Command>> entry : groups.entrySet()) {
                buffer.append("<").append(getIdentityName(entry.getKey())).append(">:")
                        .append(getGroupContent(entry.getValue())).append(TAIL);
            }

            for (String lexicon : lexicons) {
                buffer.append(lexicon).append(":词典占位").append(TAIL);
            }

            file = dirs + File.separator + name + ".gmr";
            try {
                return FileUtils.writeFile(file, buffer.toString(), false);
            } catch (Exception e) {
                log.e("createGrammarFile: write grammar file error!", e);
                return false;
            }
        }

        public String toRootRule(int opRuleSize, boolean hasBaseRule) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<").append(RULE_ROOT).append(">:");
            for (int i = 0; i < opRuleSize; i++) {
                buffer.append("<").append("rule").append(i).append(">").append("|");
            }
            if (hasBaseRule) {
                buffer.append("<").append(getIdentityName(RULE_BASE)).append(">").append("|");
            }
            if (buffer.charAt(buffer.length() - 1) == '|') {
                buffer.deleteCharAt(buffer.length() -1);
            } else {
                buffer.append("错误");
            }

            buffer.append(TAIL);
            return buffer.toString();
        }

        public String getGroupContent(List<Command> items) {
            StringBuffer buffer = new StringBuffer();
            if (items != null && items.size() > 0) {
                for (Command item : items) {
                    item.alias = DataLoadUtil.normalizeKeys(item.alias);
                    if (item.alias != null && item.alias.size() > 0) {
                        String idstr = "!id("+ item.getOffsetId() +")";
                        for (String name : item.alias) {
                            buffer.append("\"").append(name).append("\"").append(idstr).append("|");
                        }
                    }
                }
            }

            if (buffer.length() > 0) {
                buffer.deleteCharAt(buffer.length() - 1);
            } else {
                buffer.append("错误");
            }

            return buffer.toString();
        }

        String getIdentityName(String name) {
            return getHashName(scene + name);
        }

        /**
         * 得到一个标准名，避免用户自定义的名称超过讯飞语法文件定义的15长度限制。
         * @param name 原始名称
         * @return 返回"h"加上hash值的字符串表示，头部加上“h”是为了防止解析本地结果时，
         * 出现如“&lt;2323ba ...&gt;”因为“&lt;2”而导致解析出错。
         */
        static String getHashName(String name) {
            return "h" + Integer.toHexString(Objects.hash(name));
        }
    }

    public static class Command {
        public static final int ID_OFFSET = 1 << 16;

        int id;
        boolean notInBaseGroup;
        String value;
        List<String> alias;
        List<String> groups;

        public int getOffsetId() {
            return ID_OFFSET + id;
        }
    }

}
