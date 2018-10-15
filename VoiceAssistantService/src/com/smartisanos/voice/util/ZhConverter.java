package com.smartisanos.voice.util;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ZhConverter {
    private static ZhConverter sConverter;
    private Properties charMap = new Properties();

    public static ZhConverter getInstance(Context context) {
            if (sConverter == null) {
                synchronized(ZhConverter.class) {
                    if (sConverter == null) {
                        sConverter = new ZhConverter(context);
                    }
                }
            }
            return sConverter;
    }

    private ZhConverter(Context context) {
        InputStream is = null;
        try {
            is = context.getApplicationContext().getAssets().open("zh2Hans_smartisan.properties");
            charMap.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
        }
    }

    public static String convert(Context context, String text) {
        ZhConverter instance = getInstance(context);
        return instance.convert(text);
    }

    public String convert(String in) {
        StringBuilder outString = new StringBuilder();

        for (int i = 0; i < in.length(); i++) {

            char c = in.charAt(i);
            String key = "" + c;

            if (charMap.containsKey(key)) {
                outString.append(charMap.get(key));
            } else {
                outString.append(key);
            }
        }

        return outString.toString();
    }
}
