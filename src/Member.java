import javax.sound.sampled.SourceDataLine;

/**
 * File:Member
 * Date:04/04/2025
 * <p>
 * Description:
 * Represents a single instrument/note-playing thread.
 * Each Member thread waits for its "turn" to play its assigned BellNote.
 * </p>
 *
 * @author mguzelocak
 */
public class Member implements Runnable {
    private final Thread t;

    /**
     * The BellNote this thread will play.
     */
    private final BellNote note;

    /**
     * controller for deciding the turn
     */
    private boolean myTurn = false;

    /**
     * checking  if the thread is running
     */
    private boolean running = true;

    /**
     * Audio line to write sound to.
     */
    private SourceDataLine line;

    /**
     * Constructs a Member responsible for playing a specific BellNote.
     *
     * @param note The BellNote this thread will play.
     * @param line Audio line to write sound to.
     */
    public Member(BellNote note, SourceDataLine line) {
        this.note = note;
        this.line = line;
        t = new Thread(this, note.note.name());
        this.t.start();
    }

    /**
     * Signals the thread to play its note.
     */
    public synchronized void giveTurn() {
        myTurn = true;
        notify();
    }

    /**
     * Signals the thread to stop running.
     */
    public synchronized void stop() {
        running = false;
        myTurn = true;
        notify();
    }

    /**
     * Main loop that waits for its turn to play.
     */
    public synchronized void run() {
        while (running) {
            try {
                while (!myTurn) {
                    wait();
                }
                playNote(line, note);
                myTurn = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Plays the note on the provided audio line.
     *
     * @param line The audio output.
     * @param bn   The BellNote to play.
     */
    public void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}

