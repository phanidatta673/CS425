package mp3.app.messages;

import java.net.DatagramPacket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PongMsg {
    @Expose
    public int id = -1;

    @Expose
    public int counter = -1;

    @Expose
    public long age = -1;

    protected static final Logger log = LogManager.getLogger();
    protected static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public PongMsg(int id, int counter, long age) {
        this.id = id;
        this.counter = counter;
        this.age = age;
    }

    @Override
    public String toString() {
        return String.format("Pong from %d(%d): %d", id, age, counter);
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public byte[] toBytes() {
        return toJson().getBytes();
    }

    public static PongMsg fromPacket(DatagramPacket packet) {
        String input = new String(packet.getData(), 0, packet.getLength());
        // log.trace("Parsing string: {}", input);
        return gson.fromJson(input, PongMsg.class);
    }
}