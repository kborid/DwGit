package com.smartisanos.sara.bullet.contact.holder;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.LruCache;
import android.view.View;

import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bullet.widget.AvatarImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import com.smartisanos.sara.R;

/**
 * Created by sukai on 18-4-10.
 */

public class PhoneContactPhotoManager implements Handler.Callback {
    private static final String TAG = "PhoneContactPhotoManager";
    private static final String LOADER_THREAD_NAME = "PhoneContactPhotoManager";
    //message flag for loading avatar
    private static final int MESSAGE_REQUEST_LOADING = 1;
    //message flag for loaded avatar
    private static final int MESSAGE_PHOTOS_LOADED = 2;

    //Cache size for {@link #mBitmapCache} for devices with "large" RAM.
    private static final int BITMAP_CACHE_SIZE = 36864 * 48;//1728k
    private final float cacheSizeAdjustment = 0.5f;
    private final int bitmapCacheSize = (int) (cacheSizeAdjustment * BITMAP_CACHE_SIZE);
    //cache for loading phone contact avatar request
    private final ConcurrentHashMap<Long, Request> mPendingRequests = new ConcurrentHashMap<>();
    //cache for phone contact avatar bitmap
    private final LruCache<Object, Bitmap> mBitmapHolderCache = new LruCache<Object, Bitmap>(bitmapCacheSize);

    private static PhoneContactPhotoManager mManager = null;
    //main UI handler
    private final Handler mMainThreadHandler = new Handler(this);
    //thread for query phone contact avatar
    private LoaderThread mLoaderThread;

    public static PhoneContactPhotoManager getInstance() {
        if (mManager == null) {
            mManager = new PhoneContactPhotoManager();
        }
        return mManager;
    }

    private PhoneContactPhotoManager(){

    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_REQUEST_LOADING:
                ensureLoaderThread();
                mLoaderThread.requestLoading((Request) msg.obj);
                return true;
            case MESSAGE_PHOTOS_LOADED:
                processLoadedImages((Request) msg.obj);
                return true;
        }
        return false;
    }

    public void loadPhotoByIdOrUri(View view, long contactId, String name) {
        setViewTag(view, contactId);

        if (mBitmapHolderCache.get(contactId) != null) {
            ((AvatarImageView) view).setAvatarBitmap(mBitmapHolderCache.get(contactId));
        } else if (mPendingRequests.contains(contactId)) {
            // do nothing
        } else {
            Request request = new Request(view, contactId, name);
            mPendingRequests.put(contactId, request);
            requestLoading(request);
        }
    }

    private void requestLoading(Request request) {
        Message message = mMainThreadHandler.obtainMessage(MESSAGE_REQUEST_LOADING);
        message.obj = request;
        message.what = MESSAGE_REQUEST_LOADING;
        mMainThreadHandler.sendMessage(message);
    }

    public void ensureLoaderThread() {
        if (mLoaderThread == null) {
            mLoaderThread = new LoaderThread();
            mLoaderThread.start();
        }
    }

    private void processLoadedImages(Request request) {
        long key = request.getContactId();
        View view = request.getView();
        mPendingRequests.remove(request.getContactId(), request);

        if (getViewTag(view) != key) {
            return;
        }

        if (request.isLoadName()) {
            ((AvatarImageView) view).setName(request.getName());
            //remove request from requests cache

        } else {
            Bitmap data = mBitmapHolderCache.get(key);

            ((AvatarImageView) view).setmUseDefaultAvatar(true);
            ((AvatarImageView) view).setAvatarBitmap(data);
            //remove request from requests cache
        }
    }

    private void cacheAvatarData(long key, Bitmap data) {
        if (mBitmapHolderCache.get(key) == null) {
            mBitmapHolderCache.put(key, data);
        }
    }

    private void setViewTag(View targetView, long contactId) {
        targetView.setTag(R.id.contact_loader_view_tag_id, contactId);
    }

    private Long getViewTag(View targetView) {
        return (Long) targetView.getTag(R.id.contact_loader_view_tag_id);
    }

    private class LoaderThread extends HandlerThread implements Handler.Callback {
        private static final int MESSAGE_LOAD_PHOTOS = 1;

        private Handler mLoaderThreadHandler;

        public LoaderThread() {
            super(LOADER_THREAD_NAME);
        }

        public void ensureHandler() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
        }

        public void requestLoading(Request request) {
            ensureHandler();

            Message message = mLoaderThreadHandler.obtainMessage(MESSAGE_LOAD_PHOTOS);
            message.what = MESSAGE_LOAD_PHOTOS;
            message.obj = request;

            mLoaderThreadHandler.sendMessage(message);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LOAD_PHOTOS:
                    loadPhotosInBackground((Request) msg.obj);
                    break;
            }

            return true;
        }

        private void loadPhotosInBackground(Request request) {
            InputStream input = null;
            Bitmap photo;
            View view = request.getView();
            ContentResolver resolver = view.getContext().getContentResolver();
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                    request.getContactId());

            try {
                SaraApplication.getInstance().grantUriPermission(SaraApplication.getInstance().getPackageName(), uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
                photo = BitmapFactory.decodeStream(input);

                if (input == null) {
                    request.setLoadName(true);

                } else {
                    cacheAvatarData(request.getContactId(), photo);
                }

                Message message = mMainThreadHandler.obtainMessage(MESSAGE_PHOTOS_LOADED);
                message.obj = request;
                message.what = MESSAGE_PHOTOS_LOADED;

                mMainThreadHandler.sendMessage(message);
            } catch (OutOfMemoryError error) {
                error.printStackTrace();
                Log.e(TAG, "outofmemoryerror", error);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * class for a query request
     */
    private class Request {
        private View view;
        private String name;
        private long contactId;
        private boolean loadName;

        public Request(View view, long contactId, String name) {
            this.view = view;
            this.name = name;
            this.contactId = contactId;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getContactId() {
            return contactId;
        }

        public void setContactId(long contactId) {
            this.contactId = contactId;
        }

        public boolean isLoadName() {
            return loadName;
        }

        public void setLoadName(boolean loadName) {
            this.loadName = loadName;
        }
    }
}
