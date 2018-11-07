package com.kborid.smart.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kborid.smart.R;
import com.kborid.smart.util.ScreenUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PictureAdapter extends BaseAdapter {

    public final static int TYPE_UNIVERSAL = 1;
    public final static int TYPE_PICASSO = 2;
    public final static int TYPE_GLIDE = 3;

    private List<String> mList = new ArrayList<>();
    private Context context;
    private int mWidth, mHeight;
    private int mTypeLoader;

    public PictureAdapter(Context context, int type) {
        this.context = context;
        this.mTypeLoader = type;
        mWidth = ScreenUtils.mScreenWidth / 3;
        mHeight = (int) ((float) mWidth / 3 * 4);
        initData();
    }

    private void initData() {
        String url1 = "https://t1.ituba.cc/mm8/tupai/201511/195/%1$d.jpg";
        String url2 = "https://t1.ituba.cc/mm8/tupai/201511/136/%1$d.jpg";
        String url3 = "https://t1.ituba.cc/mm8/tupai/201511/120/%1$d.jpg";
        String url4 = "https://t1.ituba.cc/mm8/tupai/201511/105/%1$d.jpg";
        String url5 = "https://t1.ituba.cc/mm8/tupai/201511/109/%1$d.jpg";
        String url6 = "https://t1.ituba.cc/mm8/tupai/201511/116/%1$d.jpg";
        for (int i = 1; i <= 15; i++) {
            mList.add(String.format(url1, i));
            mList.add(String.format(url2, i));
            mList.add(String.format(url3, i));
            mList.add(String.format(url4, i));
            mList.add(String.format(url5, i));
            mList.add(String.format(url6, i));
        }
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MyViewHolder viewHolder;
        if (null == convertView) {
            viewHolder = new MyViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.lv_image_item, null);
            viewHolder.iv = (ImageView) convertView.findViewById(R.id.iv);
            viewHolder.iv.getLayoutParams().height = mHeight;
            viewHolder.iv.setLayoutParams(viewHolder.iv.getLayoutParams());
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MyViewHolder) convertView.getTag();
        }

        final String url = mList.get(position);

        switch (mTypeLoader) {
            case TYPE_UNIVERSAL:
                ImageLoader.getInstance().displayImage(url, viewHolder.iv);
                break;
            case TYPE_PICASSO:
                Picasso.with(context).load(url).into(viewHolder.iv);
                break;
            case TYPE_GLIDE:
                Glide.with(context).load(url).into(viewHolder.iv);
                break;
        }
        return convertView;
    }

    class MyViewHolder {
        ImageView iv;
    }
}
