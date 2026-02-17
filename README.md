# Music Player

A JavaFX desktop music player application with a dark-themed UI, playlist management, and full playback controls.

## Features

- Play, pause, stop, and seek through audio tracks
- Previous/next navigation with auto-advance to the next track
- Add individual audio files or entire folders to the playlist
- Displays track metadata (title, artist, album)
- Interactive seek bar with elapsed/total time display
- Volume slider and mute toggle
- Dark theme with cyan accent highlighting

## Supported Formats

MP3, WAV, AAC, AIFF

## Requirements

- Java 21
- Maven 3.x

## Run

```bash
mvn javafx:run
```

## Build

```bash
mvn clean package
```

## License

MIT — see [LICENSE](LICENSE) for details.

## Project Structure

```
src/main/java/com/musicplayer/
├── App.java              # Entry point
├── MusicPlayerApp.java   # UI and controls
├── AudioEngine.java      # Playback engine
└── Song.java             # Song data model
```
