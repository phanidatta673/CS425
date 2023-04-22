package uiuc425.mp3;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uiuc425.mp3.Messages.*;

public class Common {
    private static final Logger log = LogManager.getLogger();
    protected static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static int getSelfVmID() {
        try {
            return getVmIDFromHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            log.error("Cannot get self hostname: {}", e.getMessage());
        }
        return -1;
    }

    public static int getVmIDFromHostName(String hostName) {
        // fa22-cs425-5002.cs.illinois.edu
        String fiveThousandString = hostName.substring(12, 15);
        int ans = Integer.valueOf(fiveThousandString.substring(1));
        return ans;
    }

    // TODO: generic
    public static PingMessage parsePingMessageFromPacket(DatagramPacket packet) {
        String input = new String(packet.getData(), 0, packet.getLength());
        log.trace("Parsing string: {}", input);
        return gson.fromJson(input, PingMessage.class);
    }

    public static String toJson(AckMessage ack) {
        return gson.toJson(ack);
    }

    public static String atomArrayToString(AtomicIntegerArray arr) {
        String ans = "";
        for (int i = 0; i < arr.length(); i++)
            ans += arr.get(i) + ", ";
        return ans;
    }

    public static String booleanArrayToString(boolean[] bools) {
        if (bools == null)
            return "";

        String ans = "";
        for (boolean bool : bools)
            ans += bool + ", ";
        return ans;
    }
}
