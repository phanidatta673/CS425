package mp3.app.messages;

import java.net.DatagramPacket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Memberlist;

// TODO: store memberlistView instead of raw memberlist
public class PingMsg {
    @Expose
    public Memberlist memberlist = null;

    private static Logger log = LogManager.getLogger();
    private final static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public PingMsg(Memberlist memberlist) {
        this.memberlist = memberlist;
    }

    public static PingMsg fromPacket(DatagramPacket packet) {
        String input = new String(packet.getData(), 0, packet.getLength());
        // log.trace("Parsing string: {}", input);
        return gson.fromJson(input, PingMsg.class);
    }

    @Override
    public String toString() {
        return String.format("Ping: memberlist: %s\n", memberlist.toString());
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public byte[] toBytes() {
        return toJson().getBytes();
    }

}
