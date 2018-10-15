
package com.smartisanos.sara.entity;

import java.util.ArrayList;
import java.util.List;
import com.smartisanos.sara.service.MusicState;
import com.smartisanos.sara.util.SaraConstant;
import smartisanos.app.voiceassistant.MediaStruct;

public class MusicModel implements IModel {

    public static final int TYPE_MUISC = 0;
    public static final int TYPE_ALBUM = 1;
    public static final int TYPE_ARTIST = 2;
    public static final int TYPE_MUISC_ITEM = 3;
    public static final int TYPE_ALBUM_ITEM = 4;
    public static final int TYPE_ARTIST_ITEM = 5;

    public int type;
    public int index;

    public long audioId;
    public boolean playing;

    public MediaStruct struct;

    public static List<MusicModel> createModel(List<MediaStruct> structs) {

        List<MusicModel> models = new ArrayList<MusicModel>();
        if (structs == null){
            return models;
        }
        MusicModel model = null;
        for (MediaStruct struct : structs) {
            switch (struct.mFlagType) {
                case SaraConstant.DISPLAY_AS_ALBUM:
                    model = new MusicModel();
                    model.type = TYPE_ALBUM;
                    model.struct = struct;
                    models.add(model);
                    for (int index = 0; index < struct.mTitle.size(); index++) {
                        model = new MusicModel();
                        model.index = index;
                        model.struct = struct;
                        model.type = TYPE_ALBUM_ITEM;
                        models.add(model);
                    }
                    break;
                case SaraConstant.DISPLAY_AS_ARTIST:
                    model = new MusicModel();
                    model.type = TYPE_ARTIST;
                    model.struct = struct;
                    models.add(model);
                    for (int index = 0; index < struct.mTitle.size(); index++) {
                        model = new MusicModel();
                        model.index = index;
                        model.struct = struct;
                        model.type = TYPE_ARTIST_ITEM;
                        models.add(model);
                    }
                    break;
                case SaraConstant.DISPLAY_AS_MUISC:
                    model = new MusicModel();
                    model.type = TYPE_MUISC;
                    model.struct = struct;
                    models.add(model);
                    for (int index = 0; index < struct.mTitle.size(); index++) {
                        model = new MusicModel();
                        model.index = index;
                        model.struct = struct;
                        model.type = TYPE_MUISC_ITEM;
                        models.add(model);
                    }
                    break;
            }
        }
        return models;
    }

    public void setMusicState(MusicState musicState) {
        this.audioId = musicState.getAudioId();
        if (musicState.isPlaying()) {
            switch (type) {
                case TYPE_ALBUM:
                    playing = struct.mAlbumId == musicState.getAlbumId();
                    break;
                case TYPE_ARTIST:
                    playing = struct.mArtistId == musicState.getArtistId();
                    break;
                case TYPE_MUISC:
                case TYPE_MUISC_ITEM:
                case TYPE_ALBUM_ITEM:
                case TYPE_ARTIST_ITEM:
                    playing = struct.mId.get(index) == audioId;
                    break;
            }
        } else {
            playing = false;
        }
    }

    @Override
    public String toString() {
        return "MusicModel [type=" + type + ", index=" + index + ", audioId=" + audioId + ", playing="
                + playing + ", struct=" + struct + "]";
    }

}
