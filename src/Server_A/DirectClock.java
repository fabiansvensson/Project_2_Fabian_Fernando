package Server_A;

public class DirectClock {
    public int[] clock;
    int myId;

    public DirectClock(int numProc) {
        clock = new int[numProc];
        for(int i = 0; i < numProc; i++) clock[i] = 0;
    }
    public void setId(int id) {
        myId = id;
        clock[myId] = 1;
    }
    public int getValue(int i) {
        return clock[i];
    }
    public void tick() {
        System.out.println("myId: " + myId + " clock[myId] = " + clock[myId]);
        clock[myId]++;
    }
    public void sendAction() {
        tick();
    }
    public void receieveAction(int sender, int sentValue) {
        clock[sender] = Math.max(clock[sender], sentValue);
        clock[myId] = Math.max(clock[myId], sentValue) + 1;
    }
}
