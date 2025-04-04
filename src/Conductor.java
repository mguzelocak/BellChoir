import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File:Conductor.java
 * Date:04/04/2025
 * <p>
 * Description:
 * Coordinates the playback of multiple BellNote threads ("Members") like a conductor of a choir.
 * </p>
 *
 * @author mguzelocak
 */
public class Conductor implements Runnable {
    private final Map<BellNote, Member> members = new HashMap<>();
    /**
     * Audio format.
     */
    private final AudioFormat af;
    /**
     * List of BellNotes to play.
     */
    private final List<BellNote> songs;
    private Tone tone;

    /**
     * Constructs the Conductor with a song and audio format.
     *
     * @param songs List of BellNotes to play.
     * @param af    Audio format.
     */
    public Conductor(List<BellNote> songs, AudioFormat af) {
        this.songs = songs;
        this.af = af;
    }

    /**
     * Runs the playback loop using threads for each note.
     */
    @Override
    public void run() {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            getMembers(songs, line);
            line.open();
            line.start();

            for (BellNote note : songs) {
                Member member = members.get(note);
                if (member != null) {
                    member.giveTurn();
                    try {
                        Thread.sleep(note.length.timeMs());
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

    /**
     * Stops all member threads after playback.
     */
    public void stopMembers() {
        for (Member member : members.values()) {
            member.stop();
        }
    }

    /**
     * Initializes and maps notes to Member threads.
     *
     * @param song List of BellNotes.
     * @param line Audio line for playback.
     */
    private void getMembers(List<BellNote> song, SourceDataLine line) {
        for (BellNote note : song) {
            if (!members.containsKey(note)) {
                members.put(note, new Member(note, line));
            }
        }
    }
}
