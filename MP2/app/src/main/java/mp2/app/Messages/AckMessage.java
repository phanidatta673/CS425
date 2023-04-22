package mp2.app.Messages;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AckMessage {
    @Expose
    public MessageType type = MessageType.ACK;

    protected static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    protected static final Logger log = LogManager.getLogger();
    protected DatagramSocket udpOut = null;

    public AckMessage() {
        try {
            udpOut = new DatagramSocket();
        } catch (IOException e) {
        }
    }

    public String getExplaination() {
        return "Acknowledgement";
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