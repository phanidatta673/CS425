package edu.uiuc425.boss;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Global {
    private static final Logger log = LogManager.getLogger();
    private static final long age = System.currentTimeMillis();

    public static long getAge() {
        return age;
    }

    public static int getSelfRingID() {
        try {
            return getRingIDFromHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            log.error("Cannot get self hostname: {}", e.getMessage());
        }
        return -1;
    }

    public static int getRingIDFromHostName(String hostName) throws NumberFormatException {
        // fa22-cs425-5002.cs.illinois.edu
        String fiveThousandString = hostName.substring(12, 15);
        return Integer.parseInt(fiveThousandString.substring(1));
    }

    public static String getHostNameByRingID(int id) {
        return String.format("fa22-cs425-50%02d.cs.illinois.edu", id);
    }

    public static String getSelfHostName() {
        return getHostNameByRingID(Global.getSelfRingID());
    }
}
