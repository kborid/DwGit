package com.smartisanos.sara.bubble.search.viewholder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.smartisanos.sara.R;
import com.smartisanos.sara.entity.IModel;
import com.smartisanos.sara.entity.MusicModel;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import smartisanos.app.voiceassistant.MediaStruct;

public class ViewHolderMusic extends ViewHolder implements OnClickListener, OnLongClickListener {

    View mSubLayout;
    View mMainLayout;
    View mIvPalyingIcon;

    TextView mTvType;
    TextView mTvInfo;
    TextView mTvTime;
    TextView mTvTitle;
    TextView mTitleView;
    TextView mTvSubTitle;

    ImageView mIvPlay;
    ImageView mIvIcon;
    private MusicModel model;
    private OnMusicClickListener mOnMusicClickListener;

    public interface OnMusicClickListener {
        void playOrPause(MusicModel model, int palyModel);
        void  musicResultItemClick(MusicModel model, int palyModel,boolean play, boolean onlyOpen);
    }

    public ViewHolderMusic(Context context, View v) {
        super(context, v);
        mTitleView = (TextView) v.findViewById(R.id.title_music);
        mIvIcon = (ImageView) v.findViewById(R.id.music_pic_title);
        mTvType = (TextView) v.findViewById(R.id.type);
        mTvTitle = (TextView) v.findViewById(R.id.music_title_name);
        mTvInfo = (TextView) v.findViewById(R.id.music_info);
        mIvPlay = (ImageView) v.findViewById(R.id.play_pause_btn);

        mIvPalyingIcon = v.findViewById(R.id.playing_icon);
        mMainLayout = v.findViewById(R.id.music_list_title);
        mSubLayout = v.findViewById(R.id.sub_music_list);
        mTvSubTitle = (TextView) v.findViewById(R.id.music_sub_title);
        mTvTime = (TextView) v.findViewById(R.id.music_time);
        mIvIcon.setOnClickListener(this);
        mIvPlay.setOnClickListener(this);
        mSubLayout.setOnClickListener(this);
        mMainLayout.setOnClickListener(this);

        mMainLayout.setOnLongClickListener(this);
        mSubLayout.setOnLongClickListener(this);
    }


    @Override
    public void bindView(IModel m, boolean showTitle) {
        model = (MusicModel) m;
        MediaStruct struct = model.struct;

        mTitleView.setVisibility(true == showTitle ? View.VISIBLE : View.GONE);

        int subItemSize = model.struct.mTitle.size();
        String type, title, info;
        switch (model.type) {
            case MusicModel.TYPE_MUISC:
                title = struct.mTitle.get(0);
                type = mResources.getString(R.string.music_title);
                info = struct.mAlbum;
                bindMainLayout(type, title, info);
                break;
            case MusicModel.TYPE_ALBUM:
                type = mResources.getString(R.string.music_album);
                title = struct.mAlbum;
                info = mResources.getQuantityString(R.plurals.music_artist, subItemSize, subItemSize, struct.mArtist);
                bindMainLayout(type, title, info);
                break;
            case MusicModel.TYPE_ARTIST:
                type = mResources.getString(R.string.music_artist);
                title = struct.mArtist;
                info = mResources.getQuantityString(R.plurals.music_total, subItemSize, subItemSize);
                bindMainLayout(type, title, info);
                break;
            case MusicModel.TYPE_MUISC_ITEM:
            case MusicModel.TYPE_ALBUM_ITEM:
            case MusicModel.TYPE_ARTIST_ITEM:
                mMainLayout.setVisibility(View.GONE);
                mSubLayout.setVisibility(View.VISIBLE);
                mTvSubTitle.setText(struct.mTitle.get(model.index));
                mTvTime.setText(SaraUtils.formatTime(struct.mTime.get(model.index)));
                mIvIcon.setContentDescription(struct.mTitle.get(model.index));
                mIvPlay.setContentDescription(mIvPlay.getResources().getString(model.playing ? R.string.read_pause : R.string.read_play));
                if (struct.mId.get(model.index) == model.audioId) {
                    mIvPalyingIcon.setBackgroundResource(model.playing ? R.drawable.ic_list_play : R.drawable.ic_list_pause);
                    mIvPalyingIcon.setVisibility(View.VISIBLE);
                } else {
                    mIvPalyingIcon.setVisibility(View.GONE);
                }
                break;
        }
    }


    public void setOnMusicClickListener(OnMusicClickListener onMusicClickListener) {
        this.mOnMusicClickListener = onMusicClickListener;
    }

    private void playOrPauseMusic(MusicModel model, int palyModel) {
        if (null != mOnMusicClickListener) {
            mOnMusicClickListener.playOrPause(model, palyModel);
        }
    }


    private void musicResultItemClick(MusicModel model2, int playmodelRepeatAll,boolean play, boolean onlyOpen) {
           if (null != mOnMusicClickListener) {
               mOnMusicClickListener.musicResultItemClick(model, playmodelRepeatAll,play, onlyOpen);
           }

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_pause_btn:

                playOrPauseMusic(model, SaraConstant.PLAYMODEL_REPEAT_ALL);
                mIvPlay.setBackgroundResource(model.playing ?
                        R.drawable.btn_music_play_bg : R.drawable.btn_music_pause_bg);
                mIvPlay.setContentDescription(model.playing ?
                        mContext.getResources().getString(R.string.read_play) : mContext.getResources().getString(R.string.read_pause));
                break;
            case R.id.sub_music_list:

                playOrPauseMusic(model, SaraConstant.PLAYMODEL_NONE);
                break;
            case R.id.music_list_title:
                handleClickItem();
                break;
            case R.id.music_pic_title:
                if (model.type == MusicModel.TYPE_MUISC){
                    musicResultItemClick(model, SaraConstant.PLAYMODEL_REPEAT_ALL,false, false);
                    handleClickItem();
                }
                break;
        }
    }



    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.music_list_title:
            case R.id.sub_music_list:
                break;

            default:
                break;
        }
        return false;
    }


    private void bindMainLayout(String type, String title, String info) {
        mSubLayout.setVisibility(View.GONE);
        mMainLayout.setVisibility(View.VISIBLE);
        ImageLoader.getInstance().displayImage(
         SaraConstant.MUSIC_PIC_URI + model.struct.mId.get(0) + SaraConstant.MUSIC_PIC_PATH, mIvIcon);

        mTvType.setText(type);
        mTvTitle.setText(title);
        mTvInfo.setText(info);
        mIvPlay.setBackgroundResource(model.playing ? R.drawable.btn_music_play_bg : R.drawable.btn_music_pause_bg);
        mIvPlay.setContentDescription(mIvPalyingIcon.getResources().getString(model.playing ? R.string.read_pause : R.string.read_play));
        mIvIcon.setContentDescription(type);
    }





    private void handleClickItem() {
        switch(model.type) {
            case MusicModel.TYPE_ALBUM:
                enterAlbumPage(model.struct.mAlbumId, model.struct.mAlbum, model.struct.mArtist);
                break;
            case MusicModel.TYPE_ARTIST:
                enterArtistPage(model.struct.mArtistId, model.struct.mArtist);
                break;
            case MusicModel.TYPE_MUISC:
                enterMusicPlayer();
                break;
        }
     }

    private void enterMusicPlayer() {
        musicResultItemClick(model, SaraConstant.PLAYMODEL_REPEAT_ALL, false, true);
        Intent musicIntent = new Intent(SaraConstant.ACTION_PLAYBACK_VIEWER);
        musicIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SaraUtils.startActivity(mContext, musicIntent);
    }

    private void enterArtistPage(long artistId, String artist) {
        Intent intent = new Intent(SaraConstant.ACTION_BROWSER_ARTIST_ALBUM);
        Bundle bundle = new Bundle();
        bundle.putLong(SaraConstant.ARTIST_ID, artistId);
        bundle.putString(SaraConstant.ARTIST_KEY, artist);
        intent.putExtra(SaraConstant.PARAM, bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SaraUtils.startActivity(mContext, intent);
    }

    private void enterAlbumPage(long albumId, String album, String artist) {
        Intent intent = new Intent(SaraConstant.ACTION_BROWSER_ALBUM);
        Bundle bundle = new Bundle();
        bundle.putLong(SaraConstant.ALBUM_ID, albumId);
        bundle.putString(SaraConstant.ALBUM_KEY, album);
        bundle.putString(SaraConstant.ARTIST_KEY, artist);
        intent.putExtra(SaraConstant.PARAM, bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SaraUtils.startActivity(mContext, intent);
    }
}
