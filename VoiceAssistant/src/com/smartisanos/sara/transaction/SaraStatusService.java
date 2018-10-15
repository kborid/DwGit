
package com.smartisanos.sara.transaction;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.DiscCacheUtils;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;
import com.smartisanos.sara.util.SaraConstant;

public class SaraStatusService extends IntentService {
    public static final String ALBUM_ID = "album_id";
    public SaraStatusService() {
        super(SaraStatusService.class.getName());
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (SaraConstant.ACTION_MUSIC_ALBUME_ART_CHANGE.equals(action)) {
            handleMusicAlbumeChange(intent);
        }
    }

    private void handleMusicAlbumeChange(Intent intent) {
        boolean notify = false;
        long albumId = intent.getLongExtra(ALBUM_ID, 0);
        Cursor cursor = getContentResolver().query(
                Uri.parse(SaraConstant.SMARTISAN_MUSIC_ALBUM_URI),
                new String[] {
                    MediaStore.Audio.Media._ID
                }, MediaStore.Audio.Media.ALBUM_ID + " = ? ", new String[] {
                    String.valueOf(albumId)
                }, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    notify = true;
                    long id = cursor.getLong(0);
                    StringBuilder cacheKey = new StringBuilder(SaraConstant.MUSIC_PIC_URI)
                            .append(id).append(SaraConstant.MUSIC_PIC_PATH);
                    removeFromCache(cacheKey.toString());
                }
            } finally {
                cursor.close();
            }
        }

        if (notify) {
            Intent notifyIntent = new Intent(SaraConstant.IMAGE_LOADER_CACHE_CHANGE)
                    .setPackage(getPackageName());
            sendBroadcast(notifyIntent);
        }
    }

    private void removeFromCache(String cacheKey) {
        MemoryCacheUtils.removeFromCache(cacheKey, ImageLoader.getInstance().getMemoryCache());
        DiscCacheUtils.removeFromCache(cacheKey, ImageLoader.getInstance().getDiskCache());
    }
}
