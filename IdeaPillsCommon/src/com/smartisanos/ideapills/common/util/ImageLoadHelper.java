package com.smartisanos.ideapills.common.util;

import android.util.LruCache;

public class ImageLoadHelper<K, V>{
    public abstract static class LoadTask<K, V> implements Runnable {
        LruCache<K, V> mCache;
        K mObj;
        V mTaget;
        public LoadTask(K obj) {
            mObj = obj;
        }
        public void run() {
            mTaget = doInBackground(mObj);
            if (mTaget != null) {
                mCache.put(mObj, mTaget);
            }
            UIHandler.post(new Runnable() {
                public void run() {
                    onLoadFinish(mObj, mTaget);
                }
            });
        }
        public abstract V doInBackground(K obj);
        public void onLoadFinish(K obj, V tar) {}
    }

    private LruCache<K, V> mCache = new LruCache<K, V>(50);
    private V mDefaultImage;

    public void setDefaultImage(V defaultImage) {
        this.mDefaultImage = defaultImage;
    }

    public V getDefaultImage() {
        return mDefaultImage;
    }

    public V getImage(K obj) {
        return mCache.get(obj);
    }

    public void postLoadTask(LoadTask<K, V> task) {
        task.mCache = mCache;
        MutiTaskHandler.post(task);
    }

    public void removeTask(LoadTask<K, V> task) {
        MutiTaskHandler.removeCallbacks(task);
    }

    public void clear() {
        mCache.evictAll();
    }
}
