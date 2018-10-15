package com.smartisanos.ideapills.sync;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.smartisanos.ideapills.IdeaPillsApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SyncFileLogger {
    private static final String LOG_NAME = "synclog.txt";
    private static final String LOG_NAME_ALTERNATIVE = "synclog_2.txt";
    private static final String TAG = "SyncFileLogger";

    private static final int MSG_WRITE = 0; // paired with a LogMessage
    private static final int MSG_CLEAR = 1;
    private static final int MSG_OPEN = 2;

    private final File file1;
    private final File file2;

    //512kb
    private long maxFileSize = 512 * 1024;
    private File mCurrentLogFile;
    private Writer mWriter;
    private String mTag;
    private String mApplicationTag;
    private Handler mSaveStoreHandler;


    public SyncFileLogger() throws IOException {
        this(IdeaPillsApp.getInstance().getCacheDir());
    }

    /**
     * Create a file for writing logs.
     *
     * @param logFolder the folder path where the logs are stored
     * @param tag       tag used for message without tag
     * @throws IOException
     */
    public SyncFileLogger(File logFolder, String tag) throws IOException {
        this(logFolder);
        this.mTag = tag;
    }

    /**
     * Create a file for writing logs.
     *
     * @param logFolder the folder path where the logs are stored
     * @throws IOException
     */
    @SuppressLint("HandlerLeak")
    public SyncFileLogger(File logFolder) throws IOException {
        if (logFolder == null) {
            throw new IOException("Path is null");
        }
        this.file1 = new File(logFolder, LOG_NAME);
        this.file2 = new File(logFolder, LOG_NAME_ALTERNATIVE);
        if (!logFolder.exists()) logFolder.mkdirs();

        if (!logFolder.isDirectory()) {
            Log.e(TAG, logFolder + " is not a folder");
            throw new IOException("Path is not a directory");
        }

        if (!logFolder.canWrite()) {
            Log.e(TAG, logFolder + " is not a writable");
            throw new IOException("Folder is not writable");
        }

        mCurrentLogFile = chooseFileToWrite();

        // Initializing the HandlerThread
        HandlerThread handlerThread = new HandlerThread("SyncFileLOgger", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        if (!handlerThread.isAlive()) {
            handlerThread.start();
            mSaveStoreHandler = new Handler(handlerThread.getLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_OPEN:
                            try {
                                closeWriter();
                            } catch (IOException ignored) {
                            }
                            openWriter();
                            break;

                        case MSG_WRITE:
                            try {
                                LogMessage logmsg = (LogMessage) msg.obj;
                                if (mWriter != null) {
                                    mWriter.append(logmsg.formatLogMsg());
                                    mWriter.flush();
                                }
                            } catch (OutOfMemoryError e) {
                                Log.e(TAG, e.getClass().getSimpleName() + " : " + e.getMessage());
                            } catch (IOException e) {
                                Log.e(TAG, e.getClass().getSimpleName() + " : " + e.getMessage());
                            }

                            verifyFileSize();
                            break;
                        case MSG_CLEAR:
                            try {
                                closeWriter();
                            } catch (IOException e) {
                                Log.e(TAG, e.getMessage(), e);
                            } finally {
                                file1.delete();
                                file2.delete();

                                mCurrentLogFile = file1;
                                openWriter();
                            }
                            break;

                    }
                }
            };
            mSaveStoreHandler.sendEmptyMessage(MSG_OPEN);
        }
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    private File chooseFileToWrite() {
        if (!file1.exists() && !file2.exists())
            return file1;

        if (file1.exists() && file1.length() < maxFileSize)
            return file1;

        return file2;
    }

    public void info(String msg) {
        write('i', msg);
    }

    public void error(String msg) {
        write('e', msg);
    }

    private void write(char lvl, String message) {
        String tag;
        if (mTag == null)
            tag = TAG;
        else
            tag = mTag;
        write(lvl, tag, message);
    }

    private void write(char lvl, String tag, String message) {
        write(lvl, tag, message, null);
    }

    protected void write(char lvl, String tag, String message, Throwable tr) {
        if (tag == null) {
            write(lvl, message);
            return;
        }

        Message htmsg = Message.obtain(mSaveStoreHandler, MSG_WRITE, new LogMessage(lvl, tag, getApplicationLocalTag(), Thread.currentThread().getName(), message, tr));

        mSaveStoreHandler.sendMessage(htmsg);
    }

    private static class LogMessage {
        private static SimpleDateFormat dateFormat; // must always be used in the same thread
        private static Date mDate;

        private final long now;
        private final char level;
        private final String tag;
        private final String appTag;
        private final String threadName;
        private final String msg;
        private final Throwable cause;
        private String date;

        LogMessage(char lvl, String tag, String appTag, String threadName, String msg, Throwable tr) {
            this.now = System.currentTimeMillis();
            this.level = lvl;
            this.tag = tag;
            this.appTag = appTag;
            this.threadName = threadName;
            this.msg = msg;
            this.cause = tr;

            if (msg == null) {
                Log.e(TAG, "No message");
            }
        }

        private void addHeader(final StringBuilder csv) {
            if (dateFormat == null)
                dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault());
            if (date == null) {
                if (null == mDate)
                    mDate = new Date();
                mDate.setTime(now);
                date = dateFormat.format(mDate);
            }

            csv.append(date);
            csv.append(',');
            csv.append(level);
            csv.append(',');
            csv.append(android.os.Process.myPid());
            csv.append(',');
            if (threadName != null)
                csv.append(threadName);
            csv.append(',');
            if (appTag != null)
                csv.append(appTag);
            csv.append(',');
            if (tag != null)
                csv.append(tag);
            csv.append(',');
        }

        private void addException(final StringBuilder csv, Throwable tr) {
            if (tr == null)
                return;
            final StringBuilder sb = new StringBuilder(256);
            sb.append(tr.getClass());
            sb.append(": ");
            sb.append(tr.getMessage());
            sb.append('\n');

            for (StackTraceElement trace : tr.getStackTrace()) {
                //addHeader(csv);
                sb.append(" at ");
                sb.append(trace.getClassName());
                sb.append('.');
                sb.append(trace.getMethodName());
                sb.append('(');
                sb.append(trace.getFileName());
                sb.append(':');
                sb.append(trace.getLineNumber());
                sb.append(')');
                sb.append('\n');
            }

            addException(sb, tr.getCause());
            csv.append(sb.toString().replace(';', '-').replace(',', '-').replace('"', '\''));
        }

        public CharSequence formatLogMsg() {
            final StringBuilder sb = new StringBuilder(256);
            addHeader(sb);
            if (msg != null) sb.append(msg);
            sb.append('\n');
            if (cause != null) {
                addHeader(sb);
                sb.append('"');
                addException(sb, cause);
                sb.append('"');
                sb.append('\n');
            }
            return sb;
        }
    }

    private String getApplicationLocalTag() {
        if (mApplicationTag == null) mApplicationTag = getApplicationTag();
        return mApplicationTag;
    }

    /**
     * remove all the current log entries and start from scratch
     */
    public void clear() {
        mSaveStoreHandler.sendEmptyMessage(MSG_CLEAR);
    }

    /**
     * a special tag to be added to the logs
     *
     * @return
     */
    public String getApplicationTag() {
        return "";
    }

    private void verifyFileSize() {
        if (mCurrentLogFile != null) {
            long size = mCurrentLogFile.length();
            if (size > maxFileSize) {
                try {
                    closeWriter();
                } catch (IOException e) {
                    Log.e(TAG, "Can't use file : " + mCurrentLogFile, e);
                } finally {
                    if (mCurrentLogFile == file2)
                        mCurrentLogFile = file1;
                    else
                        mCurrentLogFile = file2;

                    mCurrentLogFile.delete();

                    openWriter();
                }
            }
        }
    }

    private void openWriter() {
        if (mWriter == null)
            try {
                mWriter = new OutputStreamWriter(new FileOutputStream(mCurrentLogFile, true), "UTF-8");
            } catch (IOException e) {
                Log.e(TAG, "can't get a mWriter for " + mCurrentLogFile + " : " + e.getMessage());
            }
    }

    private void closeWriter() throws IOException {
        if (mWriter != null) {
            mWriter.close();
            mWriter = null;
        }
    }
}