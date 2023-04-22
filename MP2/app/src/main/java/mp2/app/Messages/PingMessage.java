package mp2.app.Messages;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PingMessage {
    @Expose
    public MessageType type = MessageType.PING;
    @Expose
    public long timestamp = 0;
    @Expose
    public AtomicIntegerArray memberlist = null;
    @Expose
    public AtomicLongArray lastSeens = null;

    protected static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    protected static Logger log = LogManager.getLogger();
    protected DatagramSocket udpOut = null;

    private PingMessage() {
        try {
            udpOut = new DatagramSocket();
        } catch (IOException e) {
        }
    }

    public PingMessage(
            MessageType type,
            long timestamp,
            AtomicIntegerArray memberlist,
            AtomicLongArray lastSeens) {
        this();
        this.type = type;
        this.timestamp = timestamp;
        this.memberlist = memberlist;
        this.lastSeens = lastSeens;
    }

    public String getExplaination() {
        String result = "\n";
        result += String.format("type: %s\n", type);
        result += String.format("timestamp: %s\n", new Date(timestamp));
        result += "memberlist:\n";
        for (int i = 0; i < memberlist.length(); i++) {
            result += String.format("%s: last seen at ", memberlist.get(i));
            if (lastSeens.get(i) > 0)
                result += new Date(lastSeens.get(i)) + "\n";
        }
        result += "\n";
        return result;
    }

    protected MessageType getType() {
        return type;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public void sendTo(InetSocketAddress addr) {
        sendTo(addr.getAddress(), addr.getPort());
    }

    public void sendTo(InetAddress ipAddress, int port) {
        String json = toJson();

        DatagramPacket sendPacket = new DatagramPacket(
                json.getBytes(),
                json.length(),
                ipAddress,
                port);

        log.trace("Sending {} to {}:{}", getType(), ipAddress, port);
        log.trace("Content: {}", json);
        try {
            udpOut.send(sendPacket);
        } catch (IOException e) {
            log.error("Cannot send message: {}", e.getMessage());
        }
    }
}
