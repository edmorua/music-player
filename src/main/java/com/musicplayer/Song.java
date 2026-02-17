package com.musicplayer;

import javafx.util.Duration;

import java.io.File;

public class Song {

    private final String filePath;
    private String title;
    private String artist;
    private String album;
    private Duration duration;

    public Song(String filePath) {
        this.filePath = filePath;
        this.title = new File(filePath).getName().replaceFirst("\\.[^.]+$", "");
        this.artist = "Unknown Artist";
        this.album = "Unknown Album";
        this.duration = Duration.ZERO;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        if (artist != null && !artist.equals("Unknown Artist")) {
            return artist + " - " + title;
        }
        return title;
    }
}
