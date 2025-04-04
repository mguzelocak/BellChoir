public class Player implements Runnable {
    private static final int NUM_TURNS = 5;

    private enum State {
       A,B,C;
    }
    State state;

    public static void main(String[] args) {
        // Create all the players, and give each a turn
        final int numStates = State.values().length;

        Player[] players = new Player[numStates];
        for (State s : State.values()) {
            players[s.ordinal()] = new Player(s);
        }

        for (int i = 0; i < NUM_TURNS; i++) {
            for (Player p : players) {
                p.giveTurn();
            }
        }
        for (Player p : players) {
            p.stopPlayer();
        }
    }

    private final State myJob;
    private final Thread t;
    private volatile boolean running;
    private boolean myTurn;
    private int turnCount;

    Player(State myJob) {
        this.myJob = myJob;
        turnCount = 1;
        t = new Thread(this, myJob.name());
        t.start();
    }

    public void stopPlayer() {
        running = false;
    }

    public void giveTurn() {
        synchronized (this) {
            if (myTurn) {
                throw new IllegalStateException("Attempt to give a turn to a player who's hasn't completed the current turn");
            }
            myTurn = true;
            notify();
            while (myTurn) {
                try {
                    wait();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public void run() {
        running = true;
        synchronized (this) {
            do {
                // Wait for my turn
                while (!myTurn) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {}
                }

                // My turn!
                doTurn();
                turnCount++;

                // Done, complete turn and wakeup the waiting process
                myTurn = false;
                notify();
            } while (running);
        }
    }

    private void doTurn() {
        System.out.println("Player[" + myJob.name() + "] taking turn " + turnCount);
    }
}
