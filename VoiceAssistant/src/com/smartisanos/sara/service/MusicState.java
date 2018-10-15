package com.smartisanos.sara.service;

public class MusicState {
    private boolean isPlaying;
    private long audioId;
    private long albumId;
    private long artistId;

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public long getAudioId() {
        return audioId;
    }

    public void setAudioId(long audioId) {
        this.audioId = audioId;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public long getArtistId() {
        return artistId;
    }

    public void setArtistId(long artistId) {
        this.artistId = artistId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("isPlaying=").append(isPlaying).append(", audioId=")
                .append(audioId).append(", albumId=").append(albumId).append(", artistId=")
                .append(artistId);
        return sb.toString();
    }
}
