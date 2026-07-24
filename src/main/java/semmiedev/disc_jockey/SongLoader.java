package semmiedev.disc_jockey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import semmiedev.disc_jockey.gui.SongListWidget;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class SongLoader {
    public static final ArrayList<Song> SONGS = new ArrayList<>();
    public static final ArrayList<SongFolder> FOLDERS = new ArrayList<>();
    public static final ArrayList<String> SONG_SUGGESTIONS = new ArrayList<>();
    public static volatile boolean loadingSongs;
    public static volatile boolean showToast;

    public static class SongFolder {
        public final String name;
        public final String path;
        public final ArrayList<Song> songs = new ArrayList<>();
        public final ArrayList<SongFolder> subFolders = new ArrayList<>();
        public SongListWidget.FolderEntry entry;

        public SongFolder(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    public static void loadSongs() {
        if (loadingSongs) return;
        new Thread(() -> {
            loadingSongs = true;
            SONGS.clear();
            FOLDERS.clear();
            SONG_SUGGESTIONS.clear();
            SONG_SUGGESTIONS.add("Songs are loading, please wait");

            loadFolder(Main.songsFolder, null);

            for (Song song : SONGS) SONG_SUGGESTIONS.add(song.displayName);
            Main.config.favorites.removeIf(favorite -> SongLoader.SONGS.stream().map(song -> song.fileName).noneMatch(favorite::equals));

            if (showToast && Minecraft.getInstance().font != null) SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Main.NAME, Component.translatable(Main.MOD_ID+".loading_done"));
            showToast = true;
            loadingSongs = false;
        }).start();
    }

    private static void loadFolder(File folder, SongFolder parentFolder) {
        if (!folder.isDirectory()) return;

        SongFolder songFolder = new SongFolder(folder.getName(), folder.getPath());

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadFolder(file, songFolder);
            } else {
                try {
                    String fileName = file.getName().toLowerCase();
                    if (((fileName.endsWith(".mid") || fileName.endsWith(".midi")) && !Main.config.enableExperimentalMIDI)) {
                        continue;
                    }
                    Song song = loadSong(file, false);
                    if (song != null) {
                        SONGS.add(song);
                        songFolder.songs.add(song);
                        if (parentFolder != null) {
                            song.folder = songFolder;
                        }
                    }
                } catch (Exception exception) {
                    Main.LOGGER.error("Unable to read or parse song {}", file.getName(), exception);
                }
            }
        }

        if (parentFolder == null) {
            FOLDERS.addAll(songFolder.subFolders);
        } else {
            parentFolder.subFolders.add(songFolder);
        }
    }

    public static Song loadSong(File file) throws IOException {
        return loadSong(file, true);
    }

    public static Song loadSong(File file, boolean loadNotes) throws IOException {
        if (file.isFile()) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".mid") || fileName.endsWith(".midi")) {
                if (!Main.config.enableExperimentalMIDI) {
                    return null;
                }
                try {
                    Song song = MidiLoader.loadFromMidi(file);
                    song.fileName = file.getName().replaceAll("[\\n\\r]", "");
                    song.filePath = file.getPath();
                    song.displayName = song.name.replaceAll("\\s", "").isEmpty() ? (song.fileName.replaceAll("(?i)\\.midi?$", "") + " [midi]") : song.name + " [midi]";
                    song.entry = new SongListWidget.SongEntry(song, SONGS.size());
                    song.entry.favorite = Main.config.favorites.contains(song.fileName);
                    song.searchableFileName = song.fileName.toLowerCase().replaceAll("\\s", "");
                    song.searchableName = song.name.toLowerCase().replaceAll("\\s", "");
                    return song;
                } catch (Exception e) {
                    throw new IOException("Failed to load MIDI file", e);
                }
            }

            BinaryReader reader = new BinaryReader(Files.newInputStream(file.toPath()));
            Song song = new Song();

            song.fileName = file.getName().replaceAll("[\\n\\r]", "");
            song.filePath = file.getPath();

            song.length = reader.readShort();

            boolean newFormat = song.length == 0;
            if (newFormat) {
                song.formatVersion = reader.readByte();
                song.vanillaInstrumentCount = reader.readByte();
                song.length = reader.readShort();
            }

            song.height = reader.readShort();
            song.name = reader.readString().replaceAll("[\\n\\r]", "");
            song.author = reader.readString().replaceAll("[\\n\\r]", "");
            song.originalAuthor = reader.readString().replaceAll("[\\n\\r]", "");
            song.description = reader.readString().replaceAll("[\\n\\r]", "");
            song.tempo = reader.readShort();
            song.autoSaving = reader.readByte();
            song.autoSavingDuration = reader.readByte();
            song.timeSignature = reader.readByte();
            song.minutesSpent = reader.readInt();
            song.leftClicks = reader.readInt();
            song.rightClicks = reader.readInt();
            song.blocksAdded = reader.readInt();
            song.blocksRemoved = reader.readInt();
            song.importFileName = reader.readString().replaceAll("[\\n\\r]", "");

            if (newFormat) {
                song.loop = reader.readByte();
                song.maxLoopCount = reader.readByte();
                song.loopStartTick = reader.readShort();
            }

            song.displayName = song.name.replaceAll("\\s", "").isEmpty() ? song.fileName : song.name+" ("+song.fileName+")";
            song.entry = new SongListWidget.SongEntry(song, SONGS.size());
            song.entry.favorite = Main.config.favorites.contains(song.fileName);
            song.searchableFileName = song.fileName.toLowerCase().replaceAll("\\s", "");
            song.searchableName = song.name.toLowerCase().replaceAll("\\s", "");

            if (!loadNotes) {
                return song;
            }

            short tick = -1;
            short jumps;
            ArrayList<Long> noteList = new ArrayList<>();
            HashSet<Note> uniqueSet = new HashSet<>();
            while ((jumps = reader.readShort()) != 0) {
                tick += jumps;
                short layer = -1;
                while ((jumps = reader.readShort()) != 0) {
                    layer += jumps;

                    byte instrumentId = reader.readByte();
                    byte noteId = (byte)(reader.readByte() - 33);

                    if (newFormat) {
                        reader.readByte();
                        reader.readByte();
                        reader.readShort();
                    }

                    if (noteId < 0) {
                        noteId = 0;
                    } else if (noteId > 24) {
                        noteId = 24;
                    }

                    Note note = new Note(Note.INSTRUMENTS[instrumentId], noteId);
                    if (uniqueSet.add(note)) {
                        song.uniqueNotes.add(note);
                    }

                    long noteLong = tick | layer << Note.LAYER_SHIFT | (long)instrumentId << Note.INSTRUMENT_SHIFT | (long)noteId << Note.NOTE_SHIFT;
                    noteList.add(noteLong);
                }
            }
            song.notes = noteList.stream().mapToLong(Long::longValue).toArray();

            return song;
        }
        return null;
    }

    public static void ensureSongLoaded(Song song) throws IOException {
        if (song.notes != null && song.notes.length > 0) {
            return;
        }
        File songFile = new File(song.filePath);
        if (!songFile.exists()) {
            throw new IOException("Song file not found: " + song.filePath);
        }
        Song fullSong = loadSong(songFile, true);
        if (fullSong == null) {
            throw new IOException("Failed to load song: " + song.fileName);
        }
        song.notes = fullSong.notes;
        song.uniqueNotes.clear();
        song.uniqueNotes.addAll(fullSong.uniqueNotes);
    }

    public static void sort() {
        SONGS.sort(Comparator.comparing(song -> song.displayName));
        FOLDERS.sort(Comparator.comparing(folder -> folder.name));
        for (SongFolder folder : FOLDERS) {
            folder.songs.sort(Comparator.comparing(song -> song.displayName));
            folder.subFolders.sort(Comparator.comparing(subFolder -> subFolder.name));
        }
    }
}