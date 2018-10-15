
package com.smartisanos.sara.bubble;

import java.util.ArrayList;
import java.util.List;
import smartisanos.app.numberassistant.YellowPageResult;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolder;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolderApp;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolderContact;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolderMusic;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolderYellowPage;
import com.smartisanos.sara.entity.AppModel;
import com.smartisanos.sara.entity.ContactModel;
import com.smartisanos.sara.entity.IModel;
import com.smartisanos.sara.entity.MusicModel;
import com.smartisanos.sara.entity.YellowPageModel;
import com.smartisanos.sara.service.MusicServiceManager;
import com.smartisanos.sara.R;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.MediaStruct;
public class VoiceSearchResultAdapter extends BaseAdapter implements  MusicServiceManager.Callback,ViewHolderMusic.OnMusicClickListener{
    public static final int TYPE_CONTACT = 0;
    public static final int TYPE_YELLOWPAGE = 1;
    public static final int TYPE_APP = 2;
    public static final int TYPE_MUSIC = 3;
    private static final int ITEM_TYPE_COUNT = 4;
        public static final int DEFAULT_LENS = 5;
        public int mArtistShowPlay = -1;
        public int mAlbumShowPlay = -1;
        public int mCurTypeIndex = -1;
        private int mClickList = 0;
        private int mClickButton = 1;
        private Context mContext;
        public boolean mEnablePlaying;
        protected boolean isFirstPlay;
        private LayoutInflater mInflater;
        private int mCurPlayPosition = -1;
        private boolean mIsPlaying = false;
        private Resources mResource;

        private List<ContactModel> mContactModel = new ArrayList<ContactModel>();
        private List<YellowPageModel> mYellowPageModel = new ArrayList<YellowPageModel>();
        private List<AppModel> mAppModel = new ArrayList<AppModel>();
        private List<MusicModel> mMusicModel = new ArrayList<MusicModel>();
        private MusicServiceManager mMusicServiceManager;
    private ViewHolderApp.IAppStartListener mIAppStartListener;
        public VoiceSearchResultAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
            mMusicServiceManager = MusicServiceManager.from(mContext);
        }
        public void destroy() {
            MusicServiceManager manager = MusicServiceManager.from(mContext);
            manager.unBindMusicService();
            manager.removeCallback(this);
            MusicServiceManager.remove(mContext);
        }

        public void playMusic(int position, int showPlay, int typeIndex) {
            mCurPlayPosition = position;
            mAlbumShowPlay = showPlay;
            mArtistShowPlay = showPlay;
            mIsPlaying = true;
            mCurTypeIndex = typeIndex;
            if (isFirstPlay) {
                mEnablePlaying = true;
                isFirstPlay = false;
            }
        }
        @Override
        public int getItemViewType(int position) {
            int contactSize = mContactModel.size();
            int appSize = mAppModel.size();
            int yellowSize = mYellowPageModel.size();

            if (position < appSize) {
                return TYPE_APP;
            }
            position -= appSize;
            if (position < contactSize) {
                return TYPE_CONTACT;
            }
            position -= contactSize;
            if (position < yellowSize) {
                return TYPE_YELLOWPAGE;
            }
            return TYPE_MUSIC;
        }
        @Override
        public int getViewTypeCount() {
            return ITEM_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            int type = getItemViewType(position);

            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                switch (type) {
                    case TYPE_CONTACT:
                        convertView = mInflater.inflate(R.layout.contact_list_item, parent, false);
                        holder = new ViewHolderContact(mContext, convertView);
                        break;
                    case TYPE_YELLOWPAGE:
                        convertView = mInflater.inflate(R.layout.yellow_list_item, parent, false);
                        holder = new ViewHolderYellowPage(mContext, convertView);
                        break;
                    case TYPE_APP:
                        convertView = mInflater.inflate(R.layout.app_list_item, parent, false);
                        holder = new ViewHolderApp(mContext, convertView);
                        ((ViewHolderApp) holder).setAppStartListener(mIAppStartListener);
                        break;
                    case TYPE_MUSIC:
                        convertView = mInflater.inflate(R.layout.music_list_item, parent, false);
                        holder = new ViewHolderMusic(mContext, convertView);
                        ((ViewHolderMusic) holder).setOnMusicClickListener(this);
                        break;
                }
                convertView.setTag(holder);
            }

            IModel model = getItem(position);
            boolean showTitle = false;
            switch (type) {
                case TYPE_APP:
                    showTitle = position == 0;
                    break;
                case TYPE_CONTACT:
                    showTitle =  (position == mAppModel.size());;
                    break;
                case TYPE_YELLOWPAGE:
                    showTitle = (position == mContactModel.size() + mAppModel.size());
                    break;
                case TYPE_MUSIC:
                    showTitle = (position == mContactModel.size() + mYellowPageModel.size()
                            + mAppModel.size());
                    ((MusicModel) model).setMusicState(mMusicServiceManager.getMusicState());
                    break;
            }
            holder.bindView(model, showTitle);
            return convertView;
        }
        @Override
        public IModel getItem(int position) {
            int contactSize = mContactModel.size();
            int yellowSize = mYellowPageModel.size();
            int appSize = mAppModel.size();
            int musicSize = mMusicModel.size();

            if (position < appSize) {
                return mAppModel.get(position);
            }
            position -= appSize;
            if (position < contactSize) {
                return mContactModel.get(position);
            }
            position -= contactSize;
            if (position < yellowSize) {
                return mYellowPageModel.get(position);
            }
            position -= yellowSize;
            if (position < musicSize) {
                return mMusicModel.get(position);
            }
            return null;
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mContactModel.size() + mYellowPageModel.size() + mAppModel.size() + mMusicModel.size();
        }

    public void setData(List<ApplicationStruct> app,
            List<ContactStruct> contact, List<YellowPageResult> yellow,
            List<MediaStruct> music) {
        if (music != null && music.size() >0){
            MusicServiceManager manager = MusicServiceManager.from(mContext);
            manager.addCallback(this);
            manager.bindMusicService();
        }
        mAppModel = AppModel.createModel(app);
        mYellowPageModel = YellowPageModel.createModel(yellow);
        mContactModel = ContactModel.createModel(contact);
        mMusicModel = MusicModel.createModel(music);
        autoPlayMusic();

        notifyDataSetChanged();
    }

        private void refreshUI() {
            notifyDataSetChanged();
        }

        @Override
        public void onMusicServiceConnected() {
            autoPlayMusic();
        }

        @Override
        public void onMusicStateChange() {
            refreshUI();
        }

        private void autoPlayMusic() {
            if (mAppModel.isEmpty() && mYellowPageModel.isEmpty() && mContactModel.isEmpty()
                    && mMusicModel.size() > 0) {
                ArrayList<Long> allIds = new ArrayList<Long>();
                ArrayList<Long> musicList = mMusicModel.get(0).struct.mId;
                for (int i = 0; i < musicList.size(); i++) {
                    allIds.add(musicList.get(i));
                }
                mMusicServiceManager.playNewMusic(dealWithMusicIds(allIds), 0, SaraConstant.PLAYMODEL_REPEAT_ALL,true, false);
            }
        }

    @Override
    public void playOrPause(MusicModel model, int playMode) {
        boolean continuePlay;
        if (SaraConstant.PLAYMODEL_REPEAT_ALL == playMode) {
            // btn click
            continuePlay = isCurrentPlayInModel(model);
        } else {
            // item click
            long audioId = model.struct.mId.get(model.index);
            continuePlay = (audioId == mMusicServiceManager.getMusicState().getAudioId());
        }
        if (continuePlay) {
            mMusicServiceManager.continuePlayOrPause();
        } else {
            long[] musicIds = dealWithMusicIds(model.struct.mId);
            mMusicServiceManager.playNewMusic(musicIds, model.index, playMode,true, false);
        }
    }
    private long[] dealWithMusicIds(List<Long> ids) {
        long[] musicIds = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            musicIds[i] = ids.get(i);
        }
        return musicIds;
    }

    private boolean isCurrentPlayInModel(MusicModel model) {
        long playingAudioId = mMusicServiceManager.getMusicState().getAudioId();
        for (long audioId : model.struct.mId) {
            if (audioId == playingAudioId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void musicResultItemClick(MusicModel model, int playMode, boolean play, boolean onlyOpen) {
        boolean continuePlay = isCurrentPlayInModel(model);
        if (!continuePlay) {
            long[] musicIds = dealWithMusicIds(model.struct.mId);
            mMusicServiceManager.playNewMusic(musicIds, model.index, playMode, play, onlyOpen);
        } else if (!mMusicServiceManager.getMusicState().isPlaying() && play && !onlyOpen) {
            mMusicServiceManager.continuePlayOrPause();
        }

    }
    public boolean isNonEmpty(){
        if (mAppModel.size() != 0 || mContactModel.size() != 0  || mYellowPageModel.size() != 0
                || mMusicModel.size() != 0 ) {
            return true;
        }
        return false;
    }

    public boolean isViewVisibleByType(int type) {
        boolean visible = false;
        switch (type) {
        case TYPE_APP:
            visible = !mAppModel.isEmpty();
            break;
        case TYPE_MUSIC:
            visible = !mMusicModel.isEmpty();
            break;
        case TYPE_CONTACT:
            visible = !mContactModel.isEmpty() || !mYellowPageModel.isEmpty();
            break;
        default:
            break;
        }
        return visible;
    }

    public int getCurrentPosition(int type) {
        int position = 0;
        switch (type) {
            case TYPE_APP:
                position = 0;
                break;
            case TYPE_CONTACT:
                position = mAppModel.size();
                break;
            case TYPE_MUSIC:
                position = mContactModel.size() + mYellowPageModel.size() + mAppModel.size();
                break;
            default:
                break;
        }
        return position;
    }

    public void setAppStartListener(ViewHolderApp.IAppStartListener listener) {
        mIAppStartListener = listener;
    }
}
