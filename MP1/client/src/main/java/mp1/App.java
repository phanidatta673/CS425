/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package mp1;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrypoint for the Client. Will identify self as the client and other
 * machines as servers.
 */
public class App {
    private static int port = 7777;

    /**
     * Main function for the client. Adds to the hosts list if the host name
     * isn't the same to self. Queries only to the hosts in the list after that.
     * 
     * @param args Command line arguments. (if any)
     */
    public static void main(String[] args) {

        String currentHostName = getCurrentHostName();
        Log.sayF("Current host name: %s\n", currentHostName);

        List<InetSocketAddress> hosts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String hostName = String.format("fa22-cs425-50%02d.cs.illinois.edu", i);
            Log.sayF("Host added: %s\n", hostName);
            hosts.add(new InetSocketAddress(hostName, port));
        }
        Log.say("=================");

        new Client(hosts).start();
    }

    /**
     * Get the current host name.
     * 
     * @return Current host name.
     */
    private static String getCurrentHostName() {
        String currentHostName = null;
        try {
            currentHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Log.sayF("Error when getting host name: %s\n", e);
        }
        return currentHostName;
    }
}
