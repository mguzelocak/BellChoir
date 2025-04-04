import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

public class Conductor implements Runnable {
    private Tone tone;
    private final Map<BellNote, Member> members = new HashMap<>();
    private final AudioFormat af;
    private final List<BellNote> songs;

    public Conductor(List<BellNote> songs, AudioFormat af) {
        this.songs = songs;
        this.af = af;
    }

    @Override
    public void run() {
//        int silenceLength = Note.SAMPLE_RATE / 20;
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            getMembers(songs, line);
            line.open();
            line.start();

            for (BellNote note : songs) {
                Member member = members.get(note);
                if (member != null) {
                    member.giveTurn();
                    try {
                        Thread.sleep(note.length.timeMs() );
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            line.drain();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopMembers() {
        for (Member member : members.values()) {
            member.stop();
        }
    }

    private void getMembers(List<BellNote> song, SourceDataLine line) {
        for (BellNote note : song) {
            if (!members.containsKey(note)) {
                members.put(note, new Member(note, line));
            }
        }
    }
}
