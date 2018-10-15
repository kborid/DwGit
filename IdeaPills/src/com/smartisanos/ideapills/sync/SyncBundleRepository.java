package com.smartisanos.ideapills.sync;


import android.annotation.NonNull;
import android.os.AsyncTask;
import android.os.Bundle;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.common.util.UIHandler;

public class SyncBundleRepository {
    public static <T> void requestAsync(final String method, Bundle params, final WrapRequestListener<T> wrapRequestListener) {
        new AsyncTask<Bundle, Object, Object>() {

            @Override
            protected void onPreExecute() {
                if (wrapRequestListener != null) {
                    wrapRequestListener.onRequestStart();
                }
            }

            @Override
            protected Object doInBackground(Bundle... params) {
                if (wrapRequestListener != null && wrapRequestListener.mIsCanceled) {
                    return new DataException("canceled", -1);
                }
                try {
                    return requestSync(method, params[0]);
                } catch (Exception e) {
                    //ignore
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (wrapRequestListener == null) {
                    return;
                }
                if (result instanceof Bundle) {
                    try {
                        T response = wrapRequestListener.handleResponse((Bundle) result);
                        if (response == null) {
                            throw new DataException(-1);
                        }
                        wrapRequestListener.onResponse(response);
                    } catch (DataException e) {
                        wrapRequestListener.onError(e);
                    }
                } else {
                    if (result instanceof Exception) {
                        wrapRequestListener.onError(new DataException((Exception) result));
                    } else {
                        wrapRequestListener.onError(new DataException(-1));
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    private static Bundle requestSync(String method, Bundle params) {
        return SyncManager.callCloudSync(IdeaPillsApp.getInstance(), method, null, params);
    }

    protected abstract static class WrapRequestListener<T> extends RequestListener<T> {

        protected RequestListener<T> mRequestListener;

        public WrapRequestListener(RequestListener<T> requestListener) {
            mRequestListener = requestListener;
        }

        @Override
        public void cancel(boolean isIgnoreResult) {
            mIsCanceled = true;
            mIsCancelWithOutNotify = isIgnoreResult;
        }

        public abstract T handleResponse(Bundle bundle) throws DataException;

        @Override
        public void onRequestStart() {
            if (mRequestListener != null) {
                mRequestListener.onRequestStart();
            }
        }

        @Override
        public void onResponse(T response) {
            if (mRequestListener != null) {
                if (mIsCanceled) {
                    if (!mIsCancelWithOutNotify) {
                        mRequestListener.onError(new DataException("canceled", -1));
                    }
                } else {
                    mRequestListener.onResponse(response);
                }
            }
        }

        @Override
        public void onError(DataException e) {
            if (mIsCanceled && mIsCancelWithOutNotify) {
                return;
            }
            if (mRequestListener != null) {
                mRequestListener.onError(e);
            }
        }
    }

    protected abstract static class WrapRequestRetryListener<T> extends WrapRequestListener<T> {

        private int mReqCount = 0;
        private int mRetryCount = 3;
        private long mDelayMills = 2000;
        private  boolean mNeedRetry = false;

        public WrapRequestRetryListener(RequestListener<T> requestListener, int retryCount, long delayMills) {
            super(requestListener);
            mRetryCount = retryCount;
            mDelayMills = delayMills;
        }

        public abstract T handleResponse(Bundle bundle) throws DataException;

        public abstract void handleRetry();

        @Override
        public void onRequestStart() {
            if (mReqCount == 0 && mRequestListener != null) {
                mRequestListener.onRequestStart();
            }
            mReqCount++;
            mNeedRetry = false;
        }

        @Override
        public void onResponse(T response) {
            if (mIsCanceled) {
                if (!mIsCancelWithOutNotify) {
                    mRequestListener.onError(new DataException("canceled", -1));
                }
            } else {
                if (!mNeedRetry || reachRetryLimit()) {
                    if (mRequestListener != null) {
                        mRequestListener.onResponse(response);
                    }
                } else {
                    UIHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            handleRetry();
                        }
                    }, mDelayMills);
                }
            }
        }

        protected boolean reachRetryLimit() {
            return mReqCount >= mRetryCount;
        }

        protected void setResponseNeedRetry(boolean needRetry) {
            mNeedRetry = needRetry;
        }

        @Override
        public void onError(DataException e) {
            if (mRequestListener != null) {
                mRequestListener.onError(e);
            }
        }
    }

    public abstract static class RequestListener<T>{
        protected volatile boolean mIsCanceled;
        protected volatile boolean mIsCancelWithOutNotify;

        public abstract void onRequestStart();

        public abstract void onResponse(@NonNull T response);

        public abstract void onError(@NonNull DataException e);

        public void cancel(boolean isIgnoreResult) {
            mIsCanceled = true;
            mIsCancelWithOutNotify = isIgnoreResult;
        }
    }

    public static class DataException extends Exception {

        public DataException(String message, int status) {
            super(message);
            this.status = status;
        }

        public DataException(int status) {
            super();
            this.status = status;
        }

        public DataException(Throwable cause) {
            super(cause);
            if (cause instanceof DataException) {
                this.status = ((DataException) cause).status;
            } else {
                this.status = -1;
            }
        }

        public int status;
    }
}
