import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;

public class Member implements Runnable{
    private final List<Note> assignedNotes;
    private final SourceDataLine sourceDataLine;
    private final Object lock;
    private volatile BellNote currentNote;
    private volatile boolean shouldPlay = false;
    private final String name;


    public Member(List<Note> assignedNotes, SourceDataLine sourceDataLine, Object lock, String name) {
        this.assignedNotes = assignedNotes;
        this.sourceDataLine = sourceDataLine;
        this.lock = lock;
        this.name = name;
    }

    public boolean canPlay(Note note) {
        return assignedNotes.contains(note);
    }

    public void giveNote(BellNote note) {
        this.currentNote = note;
        this.shouldPlay = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void run() {}
}
