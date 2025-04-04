import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Represents the musical duration of a note.
 * Used to compute playback length in milliseconds.
 */
enum NoteLength {
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGTH(0.125f);

    /**
     * A float fraction of a whole note
     */
    private final int timeMs;

    /**
     * Constructs the note length.
     *
     * @param length A float fraction of a whole note.
     */
    private NoteLength(float length) {
        timeMs = (int) (length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    /**
     * Gets duration in milliseconds.
     *
     * @return Duration in ms.
     */
    public int timeMs() {
        return timeMs;
    }
}

/**
 * Enum representing musical notes including a generated sample waveform for each.
 * The waveform is precomputed using a sine function for each note frequency.
 * REST must be the first value and represents silence.
 */
enum Note {
    // REST Must be the first 'Note'
    REST,
    A4,
    A4S,
    B4,
    C4,
    C4S,
    D4,
    D4S,
    E4,
    F4,
    F4S,
    G4,
    G4S,
    A5;

    /**
     * The sample rate for sound generation (approx. 48kHz).
     */
    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz


    /**
     * The measure length in seconds (used for waveform length).
     */
    public static final int MEASURE_LENGTH_SEC = 1;

    /**
     * Constant step used in waveform generation.
     */
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    /**
     * Frequency of reference note A4 (440 Hz).
     */
    private final double FREQUENCY_A_HZ = 440.0d;

    /**
     * Max amplitude value used for waveform volume.
     */
    private final double MAX_VOLUME = 127.0d;

    /**
     * The audio waveform for this note, generated as a sine wave.
     */
    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    /**
     * Constructs the Note and generates its waveform unless it is REST.
     */
    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            // Calculate the frequency!
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0d;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

            // Create sinusoidal data sample for the desired frequency
            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte) (Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }

    }

    /**
     * Returns the audio sample for this note.
     *
     * @return Byte array of the waveform.
     */
    public byte[] sample() {
        return sinSample;
    }
}

/**
 * File: Tone.java
 * Date:04/04/2025
 * <p>
 * Description: The main class responsible for reading a song file and playing it using multiple threads.
 * Each note is played using a separate Member thread via the Conductor.
 * </p>
 *
 * @author mguzelocak
 */
public class Tone {

    private final AudioFormat af;

    /**
     * Constructs a Tone object with a given audio format.
     *
     * @param af The audio format to use.
     */
    Tone(AudioFormat af) {
        this.af = af;
    }

    /**
     * Entry point for the program. Reads a song file and plays it.
     *
     * @param args Command line arguments; expects a path to a song file as the first argument.
     * @throws Exception on file read or audio line issues.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Error: No song file is provided");
            return;
        }

        String fileName = args[0];

        final AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);

        List<BellNote> song = readSong(fileName);

        Conductor conductor = new Conductor(song, af);

        if (song == null) {
            System.err.println("Song loading failed due to errors.");
        } else {
            System.out.println("Playing downloaded song from file...");
            conductor.run();
            conductor.stopMembers();
        }
    }

    /**
     * Reads a song file and converts it into a list of BellNotes.
     * Expected format per line: "NOTE DURATION", e.g., "C4 4"
     *
     * @param filename Path to the song text file.
     * @return List of BellNotes or null if the file contains errors.
     */
    public static List<BellNote> readSong(String filename) {
        List<BellNote> result = new ArrayList<>();
        boolean errorOccurred = false;

        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNext()) {
                String noteStr = scanner.next();

                if (!scanner.hasNextInt()) {
                    System.err.println("Invalid format after the note: " + noteStr);
                    errorOccurred = true;
                    break;
                }

                int duration = scanner.nextInt();
                Note note = null;
                try {
                    note = Note.valueOf(noteStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid note name: " + noteStr);
                    errorOccurred = true;
                    break;
                }

                NoteLength length = null;
                switch (duration) {
                    case 1:
                        length = NoteLength.WHOLE;
                        break;
                    case 2:
                        length = NoteLength.HALF;
                        break;
                    case 4:
                        length = NoteLength.QUARTER;
                        break;
                    case 8:
                        length = NoteLength.EIGTH;
                        break;
                    default:
                        System.err.println("Unaccepted duration: " + duration);
                        errorOccurred = true;
                        break;
                }

                if (note != null && length != null) {
                    result.add(new BellNote(note, length));
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
            return null;
        }

        if (result.isEmpty()) {
            System.err.println("No song loaded.");
        }

        if (errorOccurred || result.isEmpty()) {
            return null; // don't play if there's any error or nothing to play
        }

        return result;
    }

    /**
     * Plays a song sequentially using the provided list of BellNotes.
     *
     * @param song List of BellNote objects to play.
     * @throws LineUnavailableException if audio line cannot be opened.
     */
    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn : song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }

    /**
     * Plays a single BellNote using the audio line.
     *
     * @param line The audio line to write sound to.
     * @param bn   The BellNote to play.
     */
    public void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        System.out.println("[Playing] " + bn.note + " for " + ms + "ms (bytes: " + length + ")");
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}

/**
 * Represents a musical note and its duration to be played.
 */
class BellNote {
    /**
     * The musical note.
     */
    final Note note;

    /**
     * The duration of the note.
     */
    final NoteLength length;

    /**
     * Constructs a BellNote with given note and length.
     *
     * @param note   The musical note.
     * @param length The duration of the note.
     */
    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }

    /**
     * Checks whether this note is equal to another BellNote.
     *
     * @param obj The other object to compare to.
     * @return true if the notes and durations are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BellNote)) return false;
        BellNote other = (BellNote) obj;
        return note == other.note && length == other.length;
    }

    /**
     * Returns the hash code for this BellNote.
     *
     * @return hash code based on note and length.
     */
    @Override
    public int hashCode() {
        return note.hashCode() * 31 + length.hashCode();
    }
}
