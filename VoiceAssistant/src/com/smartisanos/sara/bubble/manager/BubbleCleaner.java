package com.smartisanos.sara.bubble.manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;

import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum BubbleCleaner {
    INSTANCE;

    private static final String AUTO_CLEAR_UNUSED_TIME_KEY = "AUTO_CLEAR_UNUSED_TIME_KEY";
    private static final String AUTO_CLEAR_TODO_OVER_TIME_KEY = "AUTO_CLEAR_TODO_OVER_TIME_KEY";
    private static final long CLEAR_UNUSED_FILE_INTERVAL = 7 * 24 * 60 * 60 * 1000L;
    private static final long CLEAR_TODO_OVER_INTERVAL = 60 * 60 * 1000L;
    private final static long TRASH_FORCE_DELETE_TIME = 60 * 60 * 24 * 30 * 1000L;

    private HashSet<GlobalBubble> mPendingClearBubbles = new HashSet<GlobalBubble>();
    private final Object mBubbleClearLock = new Object();

    public void addPendingClearBubble(GlobalBubble globalBubble) {
        mPendingClearBubbles.add(globalBubble);
    }

    public boolean removePendingClearBubble(GlobalBubble globalBubble) {
        return mPendingClearBubbles.remove(globalBubble);
    }

    public void markPendingClearBubbleDelay(GlobalBubble globalBubble) {
        removePendingClearBubble(globalBubble);
    }

    public Object getLock() {
        return mBubbleClearLock;
    }

    public void clearBubbleFilesWhenDestroy(Context context, GlobalBubble globalBubble,
                                            final List<GlobalBubbleAttach> attaches) {
        boolean isContainsBubble = removePendingClearBubble(globalBubble);
        if (isContainsBubble && globalBubble.getUri() != null) {
            final String mp3Path = SaraUtils.formatContent2FilePath(context.getApplicationContext(),
                    globalBubble.getUri());
            long delay = 300;
            MutiTaskHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (mBubbleClearLock) {
                        if (mp3Path != null) {
                            try {
                                new File(mp3Path).delete();
                                new File(mp3Path + ".wave").delete();
                            } catch (Exception e) {
                                LogUtils.e(e.toString());
                            }
                        }

                        if (attaches != null) {
                            deleteAttachFile(attaches, false);
                        }
                    }
                }
            }, delay);
        }
    }

    void deleteAttachFile(final List<GlobalBubbleAttach> globalBubbleAttach) {
        deleteAttachFile(globalBubbleAttach, true);
    }

    void deleteAttachFile(final List<GlobalBubbleAttach> globalBubbleAttach, boolean isRunAsync) {
        if (globalBubbleAttach == null) {
            return;
        }
        final Runnable cleanRunnable = new Runnable() {
            public void run() {
                for (GlobalBubbleAttach item : globalBubbleAttach) {
                    Uri uri = item.getLocalUri();
                    if (item.getType() == GlobalBubbleAttach.TYPE_IMAGE && uri != null) {
                        FileUtils.deleteLocalFile(uri);
                    }
                }
            }
        };
        if (isRunAsync) {
            MutiTaskHandler.post(cleanRunnable);
        } else {
            cleanRunnable.run();
        }
    }

    public void autoClear() {
        final Context appContext = SaraApplication.getInstance();
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                clearOverTimeBubbles(appContext);
                clearUnusedFiles(appContext);
            }
        });
    }

    private void clearOverTimeBubbles(final Context appContext) {
        long lastTime = SharePrefUtil.getLong(appContext, AUTO_CLEAR_TODO_OVER_TIME_KEY, 0);
        long now = System.currentTimeMillis();
        if (now - lastTime > CLEAR_TODO_OVER_INTERVAL) {
            SharePrefUtil.savePref(appContext, AUTO_CLEAR_TODO_OVER_TIME_KEY, now);
            long deleteDeadline = System.currentTimeMillis() - TRASH_FORCE_DELETE_TIME;
            Bundle tmp = new Bundle();
            tmp.putString("type", SaraConstant.LIST_TYPE_REMOVED);
            tmp.putLong(SaraConstant.KEY_BUBBLE_BEFORE, deleteDeadline);
            ArrayList<Parcelable> lists = BubbleDataRepository.getGlobleBubbleList(tmp);
            if (lists != null) {
                ArrayList<Integer> bubbleIdList = new ArrayList<Integer>();
                for (int i = 0; i < lists.size(); i++) {
                    Parcelable parcelable = lists.get(i);
                    if (parcelable instanceof GlobalBubble) {
                        GlobalBubble bubble = (GlobalBubble) parcelable;
                        String bubblePath = SaraUtils.formatContent2FilePath(appContext, bubble.getUri());
                        if (!TextUtils.isEmpty(bubblePath)) {
                            new File(bubblePath).delete();
                            new File(bubblePath + ".wave").delete();
                        }
                        bubbleIdList.add(bubble.getId());
                    }

                }

                if (!bubbleIdList.isEmpty()) {
                    Bundle bunble = new Bundle();
                    int[] intArray = new int[bubbleIdList.size()];
                    for (int i = 0; i < intArray.length; i++) {
                        intArray[i] = bubbleIdList.get(i);
                    }
                    bunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, intArray);
                    bunble.putString(SaraConstant.KEY_DESTROY_TYPE, SaraConstant.DESTROY_TYPE_REMOVED);
                    BubbleDataRepository.destroyGlobleBubble(appContext, bunble);
                    Intent deleteNotify = new Intent();
                    deleteNotify.setAction(SaraConstant.ACTION_DELETE_GLOBAL_BUBBLE);
                    LocalBroadcastManager.getInstance(appContext).sendBroadcast(deleteNotify);
                }
            }
        }
    }

    private void clearUnusedFiles(final Context appContext) {
        long lastTime = SharePrefUtil.getLong(appContext, AUTO_CLEAR_UNUSED_TIME_KEY, 0);
        long now = System.currentTimeMillis();
        if (now - lastTime > CLEAR_UNUSED_FILE_INTERVAL) {
            final long clearTimeEnd = now - CLEAR_UNUSED_FILE_INTERVAL;
            SharePrefUtil.savePref(appContext, AUTO_CLEAR_UNUSED_TIME_KEY, now);

            // clear voice
            String wavRootDir = SaraUtils.getWaveTmpPath(appContext);
            File wavRootDirFile = new File(wavRootDir);
            if (wavRootDirFile.exists()) {
                List<Uri> earliestUris = BubbleDataRepository.getEarliestVoiceBubbleUri(80);
                if (earliestUris != null) {
                    final Set<Long> earliestTimes = new HashSet<Long>();
                    long earliestTimeMin = Long.MAX_VALUE;
                    long earliestTimeMax = Long.MIN_VALUE;

                    for (Uri earliestUri : earliestUris) {
                        String path = SaraUtils.formatContent2FilePath(appContext, earliestUri);
                        if (!TextUtils.isEmpty(path)) {
                            File voiceFile = new File(path);
                            long lastModified;
                            if (voiceFile.exists()) {
                                lastModified = voiceFile.lastModified();
                                if (lastModified < earliestTimeMin) {
                                    earliestTimeMin = lastModified;
                                }
                                if (lastModified > earliestTimeMax) {
                                    earliestTimeMax = lastModified;
                                }
                                earliestTimes.add(lastModified);
                            }
                            File waveFile = new File(path + ".wave");
                            if (waveFile.exists()) {
                                lastModified = waveFile.lastModified();
                                if (lastModified < earliestTimeMin) {
                                    earliestTimeMin = lastModified;
                                }
                                if (lastModified > earliestTimeMax) {
                                    earliestTimeMax = lastModified;
                                }
                                earliestTimes.add(lastModified);
                            }
                        }
                    }

                    if (!earliestTimes.isEmpty() && earliestTimeMin <= earliestTimeMax
                            && earliestTimeMin < clearTimeEnd) {
                        final long finalEarliestTimeMin = earliestTimeMin;
                        final long finalEarliestTimeMax = earliestTimeMax > clearTimeEnd ? clearTimeEnd : earliestTimeMax;
                        File[] clearFiles = wavRootDirFile.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                if (file.isDirectory()) {
                                    return false;
                                }
                                long lastModified = file.lastModified();
                                if (lastModified < finalEarliestTimeMin) {
                                    return true;
                                } else if (lastModified > finalEarliestTimeMin && lastModified < finalEarliestTimeMax) {
                                    return !earliestTimes.contains(lastModified);
                                }
                                return false;
                            }
                        });
                        if (clearFiles != null) {
                            for (File clearFile : clearFiles) {
                                if (clearFile.getPath().endsWith(".mp3")
                                        || clearFile.getPath().endsWith(".wave")
                                        || clearFile.getPath().endsWith(".pcm")) {
                                    FileUtils.deleteFile(clearFile);
                                }
                            }
                        }
                    }
                }
            }

            // clear attaches
            File attachRootDirFile = appContext.getExternalCacheDir();
            if (attachRootDirFile != null && attachRootDirFile.exists()) {
                File[] clearFileDirs = attachRootDirFile.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() && file.lastModified() < clearTimeEnd;
                    }
                });
                if (clearFileDirs != null) {
                    for (File clearDir : clearFileDirs) {
                        FileUtils.deleteFile(clearDir);
                    }
                }
            }
        }
    }
}
