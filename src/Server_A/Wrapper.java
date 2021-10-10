package Server_A;

import java.io.Serializable;

public class Wrapper implements Serializable {
    private int id;
    private int to;
    private int timestamp;
    private String msg;

    public Wrapper(int id, int to, int timestamp, String msg) {
        this.id = id;
        this.to = to;
        this.timestamp = timestamp;
        this.msg = msg;
    }

    public Wrapper(int id, int timestamp, String msg) {
        this(id, -1, timestamp, msg);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
