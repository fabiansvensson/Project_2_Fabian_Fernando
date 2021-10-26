package Server_B;

public class LamportClock {
    int c;

    public LamportClock() {
        this.c = 1;
    }

    public int getValue() {
        return c;
    }

    public void tick() {
        c = c + 1;
    }

    public void sendAction() {
        c = c + 1;
    }

    public void receiveAction(int src, int sentValue) {
        c = Math.max(c, sentValue) + 1;
    }

}
