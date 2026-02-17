package com.musicplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MusicPlayerApp extends Application {

    private final AudioEngine audioEngine = new AudioEngine();
    private final ObservableList<Song> playlist = FXCollections.observableArrayList();
    private ListView<Song> playlistView;

    private Label titleLabel;
    private Label artistLabel;
    private Label albumLabel;
    private Label elapsedLabel;
    private Label totalLabel;
    private Slider seekSlider;
    private Slider volumeSlider;
    private Button playPauseButton;
    private Button muteButton;

    private int currentIndex = -1;
    private boolean seekSliderDragging = false;

    private static final String[] AUDIO_EXTENSIONS = {".mp3", ".wav", ".aac", ".aiff"};

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildTrackInfoBar());
        root.setCenter(buildPlaylistPanel());
        root.setBottom(buildControlsPanel());

        setupAudioEngineBindings();

        Scene scene = new Scene(root, 700, 500);
        scene.getStylesheets().add("data:text/css," + getStylesheet());
        stage.setTitle("Music Player");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> audioEngine.dispose());
        stage.show();
    }

    private VBox buildTrackInfoBar() {
        titleLabel = new Label("No track loaded");
        titleLabel.getStyleClass().add("track-title");

        artistLabel = new Label("");
        artistLabel.getStyleClass().add("track-detail");

        albumLabel = new Label("");
        albumLabel.getStyleClass().add("track-detail");

        HBox fileButtons = new HBox(8);
        fileButtons.setAlignment(Pos.CENTER_RIGHT);

        Button addFilesBtn = new Button("Add Files");
        addFilesBtn.setOnAction(e -> addFiles());

        Button addFolderBtn = new Button("Add Folder");
        addFolderBtn.setOnAction(e -> addFolder());

        fileButtons.getChildren().addAll(addFilesBtn, addFolderBtn);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fileButtons, Priority.ALWAYS);

        VBox trackInfo = new VBox(2, titleLabel, artistLabel, albumLabel);
        HBox.setHgrow(trackInfo, Priority.ALWAYS);

        topRow.getChildren().addAll(trackInfo, fileButtons);

        VBox topBar = new VBox(8, topRow);
        topBar.setPadding(new Insets(12));
        topBar.getStyleClass().add("track-info-bar");

        return topBar;
    }

    private VBox buildPlaylistPanel() {
        playlistView = new ListView<>(playlist);
        playlistView.getStyleClass().add("playlist-view");
        playlistView.setPlaceholder(new Label("Add files to get started"));

        playlistView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setText(null);
                    getStyleClass().remove("playing-cell");
                } else {
                    setText(song.toString());
                    getStyleClass().remove("playing-cell");
                    if (getIndex() == currentIndex) {
                        if (!getStyleClass().contains("playing-cell")) {
                            getStyleClass().add("playing-cell");
                        }
                    }
                }
            }
        });

        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int index = playlistView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    playTrack(index);
                }
            }
        });

        VBox wrapper = new VBox(playlistView);
        VBox.setVgrow(playlistView, Priority.ALWAYS);
        wrapper.setPadding(new Insets(0, 12, 0, 12));

        return wrapper;
    }

    private VBox buildControlsPanel() {
        // Seek bar row
        elapsedLabel = new Label("0:00");
        elapsedLabel.getStyleClass().add("time-label");
        elapsedLabel.setMinWidth(50);

        totalLabel = new Label("0:00");
        totalLabel.getStyleClass().add("time-label");
        totalLabel.setMinWidth(50);

        seekSlider = new Slider(0, 1, 0);
        seekSlider.getStyleClass().add("seek-slider");
        HBox.setHgrow(seekSlider, Priority.ALWAYS);

        seekSlider.setOnMousePressed(e -> seekSliderDragging = true);
        seekSlider.setOnMouseReleased(e -> {
            seekSliderDragging = false;
            Duration total = audioEngine.totalDurationProperty().get();
            if (total != null && total.greaterThan(Duration.ZERO)) {
                audioEngine.seek(total.multiply(seekSlider.getValue()));
            }
        });

        HBox seekRow = new HBox(8, elapsedLabel, seekSlider, totalLabel);
        seekRow.setAlignment(Pos.CENTER);

        // Transport controls
        Button prevButton = new Button("\u23EE");
        prevButton.getStyleClass().add("transport-btn");
        prevButton.setOnAction(e -> previousTrack());

        Button stopButton = new Button("\u23F9");
        stopButton.getStyleClass().add("transport-btn");
        stopButton.setOnAction(e -> {
            audioEngine.stop();
            playPauseButton.setText("\u25B6");
        });

        playPauseButton = new Button("\u25B6");
        playPauseButton.getStyleClass().addAll("transport-btn", "play-btn");
        playPauseButton.setOnAction(e -> togglePlayPause());

        Button nextButton = new Button("\u23ED");
        nextButton.getStyleClass().add("transport-btn");
        nextButton.setOnAction(e -> nextTrack());

        HBox transportBox = new HBox(12, prevButton, stopButton, playPauseButton, nextButton);
        transportBox.setAlignment(Pos.CENTER);

        // Volume controls
        muteButton = new Button("\uD83D\uDD0A");
        muteButton.getStyleClass().add("transport-btn");
        muteButton.setOnAction(e -> toggleMute());

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                audioEngine.setVolume(newVal.doubleValue()));

        HBox volumeBox = new HBox(6, muteButton, volumeSlider);
        volumeBox.setAlignment(Pos.CENTER_RIGHT);

        // Combine transport and volume in one row
        HBox controlsRow = new HBox();
        controlsRow.setAlignment(Pos.CENTER);

        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        controlsRow.getChildren().addAll(spacerLeft, transportBox, spacerRight, volumeBox);

        VBox controlsPanel = new VBox(8, seekRow, controlsRow);
        controlsPanel.setPadding(new Insets(12));
        controlsPanel.getStyleClass().add("controls-panel");

        return controlsPanel;
    }

    private void setupAudioEngineBindings() {
        audioEngine.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!seekSliderDragging && newVal != null) {
                Duration total = audioEngine.totalDurationProperty().get();
                if (total != null && total.greaterThan(Duration.ZERO)) {
                    seekSlider.setValue(newVal.toMillis() / total.toMillis());
                }
                elapsedLabel.setText(formatDuration(newVal));
            }
        });

        audioEngine.totalDurationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                totalLabel.setText(formatDuration(newVal));
            }
        });

        audioEngine.statusProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == MediaPlayer.Status.PLAYING) {
                playPauseButton.setText("\u23F8");
            } else {
                playPauseButton.setText("\u25B6");
            }
        });

        audioEngine.setOnEndOfMedia(this::nextTrack);

        audioEngine.setOnMetadataAvailable((song, media) ->
                Platform.runLater(() -> updateTrackInfoDisplay(song)));
    }

    private void togglePlayPause() {
        MediaPlayer.Status st = audioEngine.statusProperty().get();
        if (st == MediaPlayer.Status.PLAYING) {
            audioEngine.pause();
        } else if (st == MediaPlayer.Status.PAUSED) {
            audioEngine.resume();
        } else if (!playlist.isEmpty()) {
            if (currentIndex < 0) {
                playTrack(0);
            } else {
                playTrack(currentIndex);
            }
        }
    }

    private void playTrack(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        Song song = playlist.get(index);
        audioEngine.play(song);
        updateTrackInfoDisplay(song);
        playlistView.getSelectionModel().select(index);
        playlistView.refresh();
    }

    private void nextTrack() {
        if (playlist.isEmpty()) return;
        int next = currentIndex + 1;
        if (next >= playlist.size()) {
            next = 0;
        }
        playTrack(next);
    }

    private void previousTrack() {
        if (playlist.isEmpty()) return;
        int prev = currentIndex - 1;
        if (prev < 0) {
            prev = playlist.size() - 1;
        }
        playTrack(prev);
    }

    private void updateTrackInfoDisplay(Song song) {
        titleLabel.setText(song.getTitle());
        artistLabel.setText(song.getArtist());
        albumLabel.setText(song.getAlbum());
    }

    private void toggleMute() {
        boolean nowMuted = !audioEngine.isMuted();
        audioEngine.setMuted(nowMuted);
        muteButton.setText(nowMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A");
    }

    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add Audio Files");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac", "*.aiff"));
        List<File> files = chooser.showOpenMultipleDialog(playlistView.getScene().getWindow());
        if (files != null) {
            for (File file : files) {
                playlist.add(new Song(file.getAbsolutePath()));
            }
        }
    }

    private void addFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Add Folder");
        File dir = chooser.showDialog(playlistView.getScene().getWindow());
        if (dir != null) {
            addAudioFilesFromDirectory(dir);
        }
    }

    private void addAudioFilesFromDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File file : files) {
            if (file.isFile() && isAudioFile(file.getName())) {
                playlist.add(new Song(file.getAbsolutePath()));
            }
        }
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private String formatDuration(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) return "0:00";
        int totalSeconds = (int) Math.floor(d.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String getStylesheet() {
        return String.join("",
                ".root-pane { -fx-background-color: %232b2b2b; }",

                ".track-info-bar { -fx-background-color: %23333333; }",

                ".track-title { -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %23e0e0e0; }",

                ".track-detail { -fx-font-size: 12px; -fx-text-fill: %23aaaaaa; }",

                ".playlist-view { -fx-background-color: %23252525; -fx-control-inner-background: %23252525; }",

                ".playlist-view .list-cell { -fx-background-color: %23252525; -fx-text-fill: %23cccccc; -fx-padding: 6 12; }",

                ".playlist-view .list-cell:selected { -fx-background-color: %23444444; -fx-text-fill: %23ffffff; }",

                ".playlist-view .list-cell:hover { -fx-background-color: %233a3a3a; }",

                ".playing-cell { -fx-text-fill: %2300ccff !important; -fx-font-weight: bold; }",

                ".controls-panel { -fx-background-color: %23333333; }",

                ".transport-btn { -fx-font-size: 16px; -fx-background-color: %23444444; -fx-text-fill: %23e0e0e0; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand; }",

                ".transport-btn:hover { -fx-background-color: %23555555; }",

                ".play-btn { -fx-font-size: 20px; -fx-min-width: 48; -fx-min-height: 48; -fx-background-color: %230088cc; }",

                ".play-btn:hover { -fx-background-color: %2300aaee; }",

                ".time-label { -fx-text-fill: %23aaaaaa; -fx-font-size: 12px; }",

                ".seek-slider .track { -fx-background-color: %23555555; }",

                ".seek-slider .thumb { -fx-background-color: %2300ccff; }",

                ".label { -fx-text-fill: %23cccccc; }",

                ".button { -fx-background-color: %23444444; -fx-text-fill: %23e0e0e0; -fx-background-radius: 4; -fx-cursor: hand; }",

                ".button:hover { -fx-background-color: %23555555; }"
        );
    }
}
