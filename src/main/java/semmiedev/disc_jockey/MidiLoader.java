package semmiedev.disc_jockey;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.LongFunction;

/**
 * Experimental MIDI file loader for Disc Jockey.
 * Converts MIDI files to the Song format used by the mod.
 * Supports General MIDI instrument mapping and variable tempo.
 */
public class MidiLoader {

    // General MIDI program → NoteBlockInstrument mapping
    private static final Map<Integer, NoteBlockInstrument> INSTRUMENT_MAP = new HashMap<>();

    static {
        // Piano-like
        INSTRUMENT_MAP.put(0, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(1, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(2, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(3, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(4, NoteBlockInstrument.PLING);
        INSTRUMENT_MAP.put(5, NoteBlockInstrument.PLING);
        // Chromatic Percussion
        INSTRUMENT_MAP.put(8, NoteBlockInstrument.BELL);
        INSTRUMENT_MAP.put(9, NoteBlockInstrument.BELL);
        INSTRUMENT_MAP.put(10, NoteBlockInstrument.XYLOPHONE);
        INSTRUMENT_MAP.put(11, NoteBlockInstrument.CHIME);
        INSTRUMENT_MAP.put(12, NoteBlockInstrument.IRON_XYLOPHONE);
        INSTRUMENT_MAP.put(13, NoteBlockInstrument.IRON_XYLOPHONE);
        INSTRUMENT_MAP.put(14, NoteBlockInstrument.BELL);
        INSTRUMENT_MAP.put(15, NoteBlockInstrument.BELL);
        // Organ
        INSTRUMENT_MAP.put(16, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(17, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(18, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(19, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(20, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(21, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(22, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(23, NoteBlockInstrument.BIT);
        // Guitar
        INSTRUMENT_MAP.put(24, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(25, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(26, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(27, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(28, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(29, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(30, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(31, NoteBlockInstrument.GUITAR);
        // Bass
        INSTRUMENT_MAP.put(32, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(33, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(34, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(35, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(36, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(37, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(38, NoteBlockInstrument.BASS);
        INSTRUMENT_MAP.put(39, NoteBlockInstrument.BASS);
        // Strings
        INSTRUMENT_MAP.put(40, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(41, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(42, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(43, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(44, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(45, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(46, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(47, NoteBlockInstrument.HARP);
        // Ensemble
        INSTRUMENT_MAP.put(48, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(49, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(50, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(51, NoteBlockInstrument.FLUTE);
        // Brass
        INSTRUMENT_MAP.put(56, NoteBlockInstrument.TRUMPET);
        INSTRUMENT_MAP.put(57, NoteBlockInstrument.TRUMPET_EXPOSED);
        INSTRUMENT_MAP.put(58, NoteBlockInstrument.TRUMPET_WEATHERED);
        INSTRUMENT_MAP.put(59, NoteBlockInstrument.TRUMPET_EXPOSED);
        INSTRUMENT_MAP.put(60, NoteBlockInstrument.TRUMPET_OXIDIZED);
        INSTRUMENT_MAP.put(61, NoteBlockInstrument.TRUMPET);
        // Synth Brass
        INSTRUMENT_MAP.put(62, NoteBlockInstrument.TRUMPET);
        INSTRUMENT_MAP.put(63, NoteBlockInstrument.TRUMPET);
        // Reed
        INSTRUMENT_MAP.put(64, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(65, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(66, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(67, NoteBlockInstrument.FLUTE);
        // Pipe
        INSTRUMENT_MAP.put(68, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(69, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(70, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(71, NoteBlockInstrument.FLUTE);
        // Synth Lead
        INSTRUMENT_MAP.put(80, NoteBlockInstrument.PLING);
        INSTRUMENT_MAP.put(81, NoteBlockInstrument.PLING);
        INSTRUMENT_MAP.put(82, NoteBlockInstrument.PLING);
        INSTRUMENT_MAP.put(83, NoteBlockInstrument.PLING);
        INSTRUMENT_MAP.put(84, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(85, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(86, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(87, NoteBlockInstrument.BIT);
        // Synth Pad
        INSTRUMENT_MAP.put(88, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(89, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(90, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(91, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(92, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(93, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(94, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(95, NoteBlockInstrument.FLUTE);
        // Synth Effects
        INSTRUMENT_MAP.put(96, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(97, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(98, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(99, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(100, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(101, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(102, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(103, NoteBlockInstrument.BIT);
        // Ethnic / Misc
        INSTRUMENT_MAP.put(104, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(105, NoteBlockInstrument.BANJO);
        INSTRUMENT_MAP.put(106, NoteBlockInstrument.DIDGERIDOO);
        INSTRUMENT_MAP.put(107, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(108, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(109, NoteBlockInstrument.HARP);
        INSTRUMENT_MAP.put(110, NoteBlockInstrument.DIDGERIDOO);
        INSTRUMENT_MAP.put(111, NoteBlockInstrument.BANJO);
        // Percussive
        INSTRUMENT_MAP.put(112, NoteBlockInstrument.BASEDRUM);
        INSTRUMENT_MAP.put(113, NoteBlockInstrument.BASEDRUM);
        INSTRUMENT_MAP.put(114, NoteBlockInstrument.BASEDRUM);
        INSTRUMENT_MAP.put(115, NoteBlockInstrument.BASEDRUM);
        INSTRUMENT_MAP.put(116, NoteBlockInstrument.SNARE);
        INSTRUMENT_MAP.put(117, NoteBlockInstrument.DIDGERIDOO);
        INSTRUMENT_MAP.put(118, NoteBlockInstrument.HAT);
        INSTRUMENT_MAP.put(119, NoteBlockInstrument.SNARE);
        // Sound Effects
        INSTRUMENT_MAP.put(120, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(121, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(122, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(123, NoteBlockInstrument.BIT);
        INSTRUMENT_MAP.put(124, NoteBlockInstrument.GUITAR);
        INSTRUMENT_MAP.put(125, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(126, NoteBlockInstrument.FLUTE);
        INSTRUMENT_MAP.put(127, NoteBlockInstrument.FLUTE);
    }

    // Bank-based instrument mapping (bank << 8 | program)
    private static final Map<Integer, NoteBlockInstrument> BANKED_INSTRUMENT_MAP = new HashMap<>();

    static {
        // Copy GM bank 0
        for (var entry : INSTRUMENT_MAP.entrySet()) {
            BANKED_INSTRUMENT_MAP.put(entry.getKey(), entry.getValue());
        }
    }

    // Percussion pitch → NoteBlockInstrument mapping (GM standard)
    private static final Map<Integer, NoteBlockInstrument> PERCUSSION_MAP = new HashMap<>();

    static {
        PERCUSSION_MAP.put(35, NoteBlockInstrument.BASEDRUM); // Acoustic Bass Drum
        PERCUSSION_MAP.put(36, NoteBlockInstrument.BASEDRUM); // Bass Drum
        PERCUSSION_MAP.put(37, NoteBlockInstrument.SNARE);    // Side Stick (closest to snare rim)
        PERCUSSION_MAP.put(38, NoteBlockInstrument.SNARE);    // Acoustic Snare
        PERCUSSION_MAP.put(39, NoteBlockInstrument.SNARE);    // Hand Clap
        PERCUSSION_MAP.put(40, NoteBlockInstrument.SNARE);    // Electric Snare
        PERCUSSION_MAP.put(41, NoteBlockInstrument.BASEDRUM); // Low Floor Tom
        PERCUSSION_MAP.put(42, NoteBlockInstrument.HAT);      // Closed Hi-Hat
        PERCUSSION_MAP.put(43, NoteBlockInstrument.BASEDRUM); // High Floor Tom
        PERCUSSION_MAP.put(44, NoteBlockInstrument.HAT);      // Pedal Hi-Hat
        PERCUSSION_MAP.put(45, NoteBlockInstrument.BASEDRUM); // Low Tom
        PERCUSSION_MAP.put(46, NoteBlockInstrument.HAT);      // Open Hi-Hat
        PERCUSSION_MAP.put(47, NoteBlockInstrument.BASEDRUM); // Low-Mid Tom
        PERCUSSION_MAP.put(48, NoteBlockInstrument.BASEDRUM); // Hi-Mid Tom
        PERCUSSION_MAP.put(49, NoteBlockInstrument.BASEDRUM); // Crash Cymbal
        PERCUSSION_MAP.put(50, NoteBlockInstrument.BASEDRUM); // High Tom
        PERCUSSION_MAP.put(51, NoteBlockInstrument.BASEDRUM); // Ride Cymbal
        PERCUSSION_MAP.put(52, NoteBlockInstrument.BASEDRUM); // Chinese Cymbal
        PERCUSSION_MAP.put(53, NoteBlockInstrument.BASEDRUM); // Ride Bell
        PERCUSSION_MAP.put(54, NoteBlockInstrument.BASEDRUM); // Tambourine
        PERCUSSION_MAP.put(55, NoteBlockInstrument.BASEDRUM); // Splash Cymbal
        PERCUSSION_MAP.put(56, NoteBlockInstrument.COW_BELL); // Cowbell
        PERCUSSION_MAP.put(57, NoteBlockInstrument.BASEDRUM); // Crash Cymbal 2
        PERCUSSION_MAP.put(58, NoteBlockInstrument.BASEDRUM); // Vibraslap
        PERCUSSION_MAP.put(59, NoteBlockInstrument.BASEDRUM); // Ride Cymbal 2
        PERCUSSION_MAP.put(60, NoteBlockInstrument.BASEDRUM); // Hi Bongo
        PERCUSSION_MAP.put(61, NoteBlockInstrument.BASEDRUM); // Low Bongo
        PERCUSSION_MAP.put(62, NoteBlockInstrument.BASEDRUM); // Mute Hi Conga
        PERCUSSION_MAP.put(63, NoteBlockInstrument.BASEDRUM); // Open Hi Conga
        PERCUSSION_MAP.put(64, NoteBlockInstrument.BASEDRUM); // Low Conga
        PERCUSSION_MAP.put(65, NoteBlockInstrument.BASEDRUM); // High Timbale
        PERCUSSION_MAP.put(66, NoteBlockInstrument.BASEDRUM); // Low Timbale
        PERCUSSION_MAP.put(67, NoteBlockInstrument.BASEDRUM); // High Agogo
        PERCUSSION_MAP.put(68, NoteBlockInstrument.BASEDRUM); // Low Agogo
        PERCUSSION_MAP.put(69, NoteBlockInstrument.BASEDRUM); // Cabasa
        PERCUSSION_MAP.put(70, NoteBlockInstrument.BASEDRUM); // Maracas
        PERCUSSION_MAP.put(71, NoteBlockInstrument.BASEDRUM); // Short Whistle
        PERCUSSION_MAP.put(72, NoteBlockInstrument.BASEDRUM); // Long Whistle
        PERCUSSION_MAP.put(73, NoteBlockInstrument.BASEDRUM); // Short Guiro
        PERCUSSION_MAP.put(74, NoteBlockInstrument.BASEDRUM); // Long Guiro
        PERCUSSION_MAP.put(75, NoteBlockInstrument.BASEDRUM); // Claves
        PERCUSSION_MAP.put(76, NoteBlockInstrument.BASEDRUM); // Hi Wood Block
        PERCUSSION_MAP.put(77, NoteBlockInstrument.BASEDRUM); // Low Wood Block
        PERCUSSION_MAP.put(78, NoteBlockInstrument.BASEDRUM); // Mute Cuica
        PERCUSSION_MAP.put(79, NoteBlockInstrument.BASEDRUM); // Open Cuica
        PERCUSSION_MAP.put(80, NoteBlockInstrument.BASEDRUM); // Mute Triangle
        PERCUSSION_MAP.put(81, NoteBlockInstrument.BASEDRUM); // Open Triangle
    }

    public static final int PERCUSSION_CHANNEL = 9; // 0-indexed MIDI channel 10

    private record NoteData(long midiTick, int channel, int originalPitch, int velocity, int program, int bankMSB, int bankLSB) {}

    /**
     * Load a MIDI file and convert it to a Song object.
     */
    public static Song loadFromMidi(File midiFile) throws IOException {
        Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(midiFile);
        } catch (InvalidMidiDataException e) {
            throw new IOException("Invalid MIDI data", e);
        }

        if (sequence == null) {
            throw new IOException("Failed to read MIDI file (null sequence)");
        }

        float divisionType = sequence.getDivisionType();
        int resolution = sequence.getResolution();
        int ppq = (divisionType == Sequence.PPQ) ? resolution : resolution * 4;

        // Track tempo changes
        List<Song.TempoChange> tempoChanges = new ArrayList<>();
        List<NoteData> rawNotes = new ArrayList<>();
        int[] channelInstruments = new int[16];
        Arrays.fill(channelInstruments, -1);
        int[] channelBankMSB = new int[16];
        int[] channelBankLSB = new int[16];
        long maxMidiTick = 0;
        short tempo = 120; // default 120 BPM

        // First pass: collect tempo changes and note data
        for (Track track : sequence.getTracks()) {
            if (track == null) continue;
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                if (message instanceof MetaMessage metaMessage) {
                    if (metaMessage.getType() == 0x51) { // Set Tempo
                        byte[] data = metaMessage.getData();
                        if (data.length >= 3) {
                            int mspqn = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                            if (mspqn > 0) {
                                tempoChanges.add(new Song.TempoChange(event.getTick(), mspqn));
                            }
                        }
                    }
                } else if (message instanceof ShortMessage sm) {
                    int channel = sm.getChannel();
                    int command = sm.getCommand();
                    int data1 = sm.getData1();
                    int data2 = sm.getData2();

                    if (command == ShortMessage.CONTROL_CHANGE) {
                        if (data1 == 0) {
                            channelBankMSB[channel] = data2 & 0x7F;
                        } else if (data1 == 32) {
                            channelBankLSB[channel] = data2 & 0x7F;
                        }
                    } else if (command == ShortMessage.PROGRAM_CHANGE) {
                        channelInstruments[channel] = data1;
                    } else if (command == ShortMessage.NOTE_ON && data2 > 0) {
                        long midiTick = event.getTick();
                        if (midiTick > maxMidiTick) maxMidiTick = midiTick;
                        int program = channelInstruments[channel];
                        rawNotes.add(new NoteData(midiTick, channel, data1, data2, program, channelBankMSB[channel], channelBankLSB[channel]));
                    }
                }
            }
        }

        // If no notes, return empty song
        if (rawNotes.isEmpty()) {
            Song emptySong = new Song();
            String name = midiFile.getName().substring(0, midiFile.getName().lastIndexOf('.'));
            emptySong.fileName = midiFile.getName();
            emptySong.name = name;
            emptySong.displayName = name;
            emptySong.length = 0;
            emptySong.tempo = tempo;
            emptySong.notes = new long[0];
            return emptySong;
        }

        // Build tempo timeline
        tempoChanges.sort(Comparator.comparingLong(tc -> tc.midiTick));
        // Ensure first tempo change at tick 0
        if (tempoChanges.isEmpty() || tempoChanges.get(0).midiTick > 0) {
            tempoChanges.add(0, new Song.TempoChange(0, 500000)); // 120 BPM default
        }

        // Binary search for microseconds at a given tick
        LongFunction<Long> tickToMicros = (midiTick) -> {
            double micros = 0;
            long prevTick = 0;
            long prevMspqn = tempoChanges.get(0).mspqn;
            for (int i = 1; i < tempoChanges.size(); i++) {
                Song.TempoChange tc = tempoChanges.get(i);
                if (tc.midiTick >= midiTick) {
                    long deltaTicks = midiTick - prevTick;
                    micros += deltaTicks * prevMspqn / ppq;
                    return (long) micros;
                }
                long deltaTicks = tc.midiTick - prevTick;
                micros += deltaTicks * prevMspqn / ppq;
                prevTick = tc.midiTick;
                prevMspqn = tc.mspqn;
            }
            // After last tempo change
            long deltaTicks = midiTick - prevTick;
            micros += deltaTicks * prevMspqn / ppq;
            return (long) micros;
        };

        // Compute window-based octave offsets per channel (excluding percussion)
        double windowTicks = ppq * 2.0; // 2-beat windows
        int windowCount = (int) ((maxMidiTick / windowTicks) + 1);
        Map<Integer, Map<Integer, List<Integer>>> windowPitchMap = new HashMap<>();
        for (NoteData nd : rawNotes) {
            if (nd.channel() == PERCUSSION_CHANNEL) continue;
            int windowIdx = (int) (nd.midiTick() / windowTicks);
            windowPitchMap
                .computeIfAbsent(windowIdx, k -> new HashMap<>())
                .computeIfAbsent(nd.channel(), k -> new ArrayList<>())
                .add(nd.originalPitch());
        }

        // Compute best octave offset per window per channel
        int lowBound = 54; // F#3
        int highBound = 78; // F#5
        int targetCenter = 66; // middle of range (F#4)
        Map<Integer, Map<Integer, Integer>> windowChannelOffset = new HashMap<>();
        for (var windowEntry : windowPitchMap.entrySet()) {
            int windowIdx = windowEntry.getKey();
            Map<Integer, List<Integer>> channelMap = windowEntry.getValue();
            Map<Integer, Integer> channelOffset = new HashMap<>();
            for (var channelEntry : channelMap.entrySet()) {
                int channel = channelEntry.getKey();
                List<Integer> pitches = channelEntry.getValue();
                int minPitch = 127;
                int maxPitch = 0;
                double sum = 0;
                for (int p : pitches) {
                    if (p < minPitch) minPitch = p;
                    if (p > maxPitch) maxPitch = p;
                    sum += p;
                }
                double average = sum / pitches.size();
                int bestOffset = 0;
                int bestViolation = Integer.MAX_VALUE;
                double bestCenterDist = Double.MAX_VALUE;
                for (int oct = -4; oct <= 4; oct++) {
                    int offset = oct * 12;
                    int shiftedMin = minPitch + offset;
                    int shiftedMax = maxPitch + offset;
                    int violation = 0;
                    if (shiftedMin < lowBound) violation += lowBound - shiftedMin;
                    if (shiftedMax > highBound) violation += shiftedMax - highBound;
                    double shiftedAvg = average + offset;
                    double centerDist = Math.abs(shiftedAvg - targetCenter);
                    if (violation < bestViolation || (violation == bestViolation && centerDist < bestCenterDist)) {
                        bestViolation = violation;
                        bestCenterDist = centerDist;
                        bestOffset = offset;
                    }
                }
                channelOffset.put(channel, bestOffset);
            }
            windowChannelOffset.put(windowIdx, channelOffset);
        }

        // Second pass: convert notes to noteLongs with octave adjustment
        List<Long> noteLongs = new ArrayList<>();
        for (NoteData nd : rawNotes) {
            long midiTick = nd.midiTick();
            int channel = nd.channel();
            int originalPitch = nd.originalPitch();
            int program = nd.program();
            int bankMSB = nd.bankMSB();
            int bankLSB = nd.bankLSB();

            // Apply window-specific octave offset
            int adjustedPitch = originalPitch;
            if (channel != PERCUSSION_CHANNEL) {
                int windowIdx = (int) (midiTick / windowTicks);
                var channelOffsetMap = windowChannelOffset.get(windowIdx);
                if (channelOffsetMap != null) {
                    Integer offset = channelOffsetMap.get(channel);
                    if (offset != null) {
                        adjustedPitch = originalPitch + offset;
                        if (adjustedPitch < 0) adjustedPitch = 0;
                        if (adjustedPitch > 127) adjustedPitch = 127;
                    }
                }
            }

            // Convert adjusted pitch to Minecraft note ID
            int note = adjustedPitch;
            while (note < 54) note += 12;
            while (note > 78) note -= 12;
            int noteId = note - 54;

            NoteBlockInstrument instrument;
            if (channel == PERCUSSION_CHANNEL) {
                instrument = getPercussionInstrument(originalPitch);
            } else {
                instrument = getInstrumentForBank(bankMSB, bankLSB, program);
            }
            int instrumentId = instrument.ordinal();

            // Convert MIDI tick to song tick (50ms per tick) with variable tempo
            long microseconds = tickToMicros.apply(midiTick);
            double timeInMs = microseconds / 1000.0;
            int songTick = (int) Math.round(timeInMs / 50.0);

            short layer = 0;

            long noteLong = (long) songTick | (long) layer << Note.LAYER_SHIFT | (long) instrumentId << Note.INSTRUMENT_SHIFT | (long) noteId << Note.NOTE_SHIFT;
            noteLongs.add(noteLong);
        }

        // Sort notes by tick
        noteLongs.sort(Comparator.comparingInt(n -> (short)(long)n));

        String name = midiFile.getName().substring(0, midiFile.getName().lastIndexOf('.'));
        short length = noteLongs.isEmpty() ? 0 : (short) (int) (noteLongs.get(noteLongs.size() - 1) & 0xFFFF);

        // Recalculate tempo based on actual duration
        if (maxMidiTick > 0) {
            long totalMicroseconds = tickToMicros.apply(maxMidiTick);
            double totalSeconds = totalMicroseconds / 1_000_000.0;
            double ticksPerSecond = length / totalSeconds;
            tempo = (short) Math.round(ticksPerSecond * 100.0);
        }

        // Create Song instance
        Song song = new Song();
        song.fileName = midiFile.getName();
        song.name = name;
        song.displayName = name;
        song.length = length;
        song.midiPpq = ppq;
        song.tempoChanges = tempoChanges;
        song.tempo = tempo;

        song.notes = noteLongs.stream().mapToLong(Long::longValue).toArray();

        // Populate uniqueNotes
        Set<Note> seen = new HashSet<>();
        for (long noteLong : noteLongs) {
            byte instrumentId = (byte) (noteLong >> Note.INSTRUMENT_SHIFT);
            byte noteId = (byte) (noteLong >> Note.NOTE_SHIFT);
            Note note = new Note(Note.INSTRUMENTS[instrumentId], noteId);
            if (seen.add(note)) {
                song.uniqueNotes.add(note);
            }
        }

        return song;
    }

    private static NoteBlockInstrument getInstrumentForBank(int bankMSB, int bankLSB, int program) {
        int bankKey = (bankMSB << 8) | bankLSB;
        if (bankKey == 0) {
            return INSTRUMENT_MAP.getOrDefault(program, NoteBlockInstrument.HARP);
        }
        int combinedKey = (bankKey << 8) | program;
        return BANKED_INSTRUMENT_MAP.getOrDefault(combinedKey, NoteBlockInstrument.HARP);
    }

    private static NoteBlockInstrument getPercussionInstrument(int pitch) {
        return PERCUSSION_MAP.getOrDefault(pitch, NoteBlockInstrument.HAT);
    }
}
