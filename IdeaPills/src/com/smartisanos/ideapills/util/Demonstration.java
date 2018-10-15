package com.smartisanos.ideapills.util;

import android.content.Context;
import android.content.res.AssetManager;

import com.smartisanos.ideapills.data.BubbleDB;
import com.smartisanos.ideapills.entity.BubbleItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.service.onestep.GlobalBubble;

public class Demonstration {
    private static final LOG log = LOG.getInstance(Demonstration.class);

    public static final boolean ENABLE = false;

    public static void initDemoDataIfNeeded(Context context) {
        if (!ENABLE) {
            return;
        }
        byte[] data = null;
        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open("DemonstrationRes");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int count = -1;
            byte[] buf = new byte[1024];
            while((count = is.read(buf)) > 0) {
                baos.write(buf, 0, count);
            }
            is.close();
            baos.flush();
            data = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (data == null) {
            return;
        }
        List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
        try {
            JSONArray array = new JSONArray(new String(data));
            int length = array.length();
            for (int i = 0; i < length; i++) {
                JSONObject object = array.getJSONObject(i);
                if (object != null) {
                    int color = object.optInt("color");
                    String text = object.optString("text");
                    GlobalBubble bubble = new GlobalBubble();
                    BubbleItem item = new BubbleItem(bubble);
                    item.setColor(color);
                    item.setText(text);
                    item.setType(BubbleItem.TYPE_TEXT);
                    item.setTimeStamp(System.currentTimeMillis());
                    item.setWeight(length - i);
                    bubbleItems.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bubbleItems.size() > 0) {
            for (BubbleItem item : bubbleItems) {
                if (item == null) {
                    continue;
                }
                if (item.getId() <= 0) {
                    BubbleDB.insert(item);
                }
            }
        }
    }
}
