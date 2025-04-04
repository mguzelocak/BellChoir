import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Tone {

public static void main(String[] args) throws Exception {
    if (args.length < 1) {
        System.err.println("Error: No song file is provided");
        return;
    }

    String fileName = args[0];


    final AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
    Tone t = new Tone(af);

    List<BellNote> song = readSong(fileName);

    Conductor conductor = new Conductor(song, af);

    if (song.isEmpty()) {
        System.err.println(fileName + " not found.");
    } else {
        System.out.println("Playing downloaded song from file...");
        conductor.run();
        conductor.stopMembers();
//        t.playSong(song);
    }
}

    private final AudioFormat af;

    Tone(AudioFormat af) {
        this.af = af;
    }

    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn: song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }

    public void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        System.out.println("[Playing] " + bn.note + " for " + ms + "ms (bytes: " + length + ")");
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }

    public static List<BellNote> readSong(String filename) {
        List<BellNote> result = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNext()) {
                String noteStr = scanner.next();
                if (!scanner.hasNextInt()) {
                    System.err.println("Invalid format after the note: " + noteStr);
                    break;
                }
                int duration = scanner.nextInt();

                try {
                    Note note = Note.valueOf(noteStr);
                    NoteLength length;

                    switch (duration) {
                        case 1: length = NoteLength.WHOLE; break;
                        case 2: length = NoteLength.HALF; break;
                        case 4: length = NoteLength.QUARTER; break;
                        case 8: length = NoteLength.EIGTH; break;
                        default:
                            System.err.println("Unaccepted duration: " + duration);
                            continue;
                    }

                    result.add(new BellNote(note, length));
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid note name: " + noteStr);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
        }

        return result;
    }
}

class BellNote {
    final Note note;
    final NoteLength length;

    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BellNote)) return false;
        BellNote other = (BellNote) obj;
        return note == other.note && length == other.length;
    }

    @Override
    public int hashCode() {
        return note.hashCode() * 31 + length.hashCode();
    }
}

enum NoteLength {
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGTH(0.125f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }
}

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

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 1;

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0d;
    private final double MAX_VOLUME = 127.0d;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];


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
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }

    }

    public byte[] sample() {
        return sinSample;
    }
}
