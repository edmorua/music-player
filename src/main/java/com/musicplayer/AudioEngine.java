package com.musicplayer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

public class AudioEngine {

    private MediaPlayer mediaPlayer;

    private final ObjectProperty<Duration> currentTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final ObjectProperty<Duration> totalDuration = new SimpleObjectProperty<>(Duration.ZERO);
    private final ObjectProperty<MediaPlayer.Status> status = new SimpleObjectProperty<>(MediaPlayer.Status.UNKNOWN);

    private double volume = 0.5;
    private boolean muted = false;

    private Runnable onEndOfMedia;
    private java.util.function.BiConsumer<Song, Media> onMetadataAvailable;

    public void play(Song song) {
        disposeCurrentPlayer();

        File file = new File(song.getFilePath());
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setVolume(muted ? 0.0 : volume);

        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) ->
                currentTime.set(newVal));

        mediaPlayer.setOnReady(() -> {
            totalDuration.set(media.getDuration());
            song.setDuration(media.getDuration());
            extractMetadata(song, media);
            mediaPlayer.play();
        });

        mediaPlayer.statusProperty().addListener((obs, oldVal, newVal) ->
                status.set(newVal));

        mediaPlayer.setOnEndOfMedia(() -> {
            status.set(MediaPlayer.Status.STOPPED);
            if (onEndOfMedia != null) {
                onEndOfMedia.run();
            }
        });

        mediaPlayer.setOnError(() -> {
            System.err.println("Media error: " + mediaPlayer.getError().getMessage());
            status.set(MediaPlayer.Status.STOPPED);
        });
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            mediaPlayer.play();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            currentTime.set(Duration.ZERO);
        }
    }

    public void seek(Duration target) {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN) {
            mediaPlayer.seek(target);
        }
    }

    public void setVolume(double value) {
        this.volume = Math.max(0.0, Math.min(1.0, value));
        if (mediaPlayer != null && !muted) {
            mediaPlayer.setVolume(this.volume);
        }
    }

    public double getVolume() {
        return volume;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(muted ? 0.0 : volume);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime;
    }

    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDuration;
    }

    public ReadOnlyObjectProperty<MediaPlayer.Status> statusProperty() {
        return status;
    }

    public void setOnEndOfMedia(Runnable handler) {
        this.onEndOfMedia = handler;
    }

    public void setOnMetadataAvailable(java.util.function.BiConsumer<Song, Media> handler) {
        this.onMetadataAvailable = handler;
    }

    private void extractMetadata(Song song, Media media) {
        media.getMetadata().addListener((javafx.collections.MapChangeListener<String, Object>) change -> {
            if (change.wasAdded()) {
                applyMetadataEntry(song, change.getKey(), change.getValueAdded());
                if (onMetadataAvailable != null) {
                    onMetadataAvailable.accept(song, media);
                }
            }
        });
        // Also read any metadata already available
        for (var entry : media.getMetadata().entrySet()) {
            applyMetadataEntry(song, entry.getKey(), entry.getValue());
        }
        if (!media.getMetadata().isEmpty() && onMetadataAvailable != null) {
            onMetadataAvailable.accept(song, media);
        }
    }

    private void applyMetadataEntry(Song song, String key, Object value) {
        if (value == null) return;
        switch (key) {
            case "title" -> song.setTitle(value.toString());
            case "artist" -> song.setArtist(value.toString());
            case "album" -> song.setAlbum(value.toString());
        }
    }

    private void disposeCurrentPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            currentTime.set(Duration.ZERO);
            totalDuration.set(Duration.ZERO);
        }
    }

    public void dispose() {
        disposeCurrentPlayer();
    }
}
