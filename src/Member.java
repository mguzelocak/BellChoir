import javax.sound.sampled.SourceDataLine;

public class Member implements Runnable {
    private final Thread t;
    private final BellNote note;
    private boolean myTurn = false;
    private boolean running = true;
    private SourceDataLine line;

    public Member(BellNote note, SourceDataLine line) {
        this.note = note;
        this.line = line;
        t = new Thread(this, note.note.name());
        this.t.start();
    }

    public synchronized void giveTurn() {
        myTurn = true;
        notify();
    }

    public synchronized void stop() {
        running = false;
        myTurn = true;
        notify();
    }

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

    public void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}

