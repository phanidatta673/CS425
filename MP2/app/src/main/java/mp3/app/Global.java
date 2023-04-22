package mp3.app;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.messages.PingMsg;

public class Global {
    private static final Logger log = LogManager.getLogger();
    protected static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static final long age = System.currentTimeMillis();

    public static Pair<String, Integer> splitNameVersionString(String s) {
        log.info("Spliting string: {}", s);
        List<String> tokens = new LinkedList<>(Arrays.asList(s.split(":")));
        log.info("Tokens: {}", tokens);
        int version = Integer.valueOf(tokens.get(tokens.size() - 1));

        tokens.remove(tokens.size() - 1);
        String name = String.join(":", tokens);

        return new MutablePair<String, Integer>(name, version);
    }

    public static long getAge() {
        return age;
    }

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

    public static String getHostNameByID(int id) {
        return String.format("fa22-cs425-50%02d.cs.illinois.edu", id);
    }

    // TODO: generic
    public static PingMsg parsePingMessageFromBytes(byte[] buffer) {
        String input = new String(buffer, 0, buffer.length);
        log.trace("Parsing string: {}", input);
        return gson.fromJson(input, PingMsg.class);
    }
}
