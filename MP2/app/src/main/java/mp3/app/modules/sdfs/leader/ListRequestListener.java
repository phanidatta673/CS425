package mp3.app.modules.sdfs.leader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Global;
import mp3.app.Memberlist;
import mp3.app.messages.FileMsg;
import mp3.app.messages.ListMsg;

public class ListRequestListener implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private Memberlist memberlist = null;
    private ConcurrentMap<String, Set<Integer>> lookup = null;

    public ListRequestListener(Memberlist memberlist, ConcurrentMap<String, Set<Integer>> lookup) {
        try {
            server = new ServerSocket(Constants.ListRequestListenerPort);
        } catch (IOException e) {
        }
        this.memberlist = memberlist;
        this.lookup = lookup;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.info("Waiting for LIST request...");
                Socket client = server.accept();
                new Handler(client).run();
            } catch (IOException e) {
                log.error("Error reading from socket: {}", e);
            }
        }
    }

    private class Handler implements Runnable {
        private Socket client = null;

        public Handler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                // Client -> Me
                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                log.trace("size in stream: {}", in.available());

                int length = in.readInt();
                byte[] bytes = in.readNBytes(length);
                FileMsg msg = FileMsg.fromBytes(bytes);
                log.info("Got LIST request from {}: {}",
                        Global.getVmIDFromHostName(client.getInetAddress().getHostName()), msg);

                List<String> hosts = getHostsWithFile(msg.getName());
                ListMsg res = new ListMsg(hosts);
                byte[] resBytes = res.toBytes();

                // Me -> Client
                out.writeInt(resBytes.length);
                out.write(resBytes);
            } catch (Exception e) {
                log.error("Error handling LIST request: {}", e);
            }
        }

    }

    List<String> getHostsWithFile(String fileName) {
        Set<Integer> hostIDs = new HashSet<>();
        Set<String> keys = new HashSet<>(lookup.keySet());

        for (String key : keys) {
            Pair<String, Integer> pair = Global.splitNameVersionString(key);
            if (pair.getLeft().equals(fileName)) {
                hostIDs.addAll(lookup.get(key));
            }
        }

        Set<String> hosts = new HashSet<>();
        for (int hostID : hostIDs) {
            if (memberlist.getAlive().contains(hostID)) {
                hosts.add(Global.getHostNameByID(hostID));
            }
        }
        List<String> hostList = new LinkedList<>();
        hostList.addAll(hosts);
        log.info("Hosts with this file {}: {}", fileName, hosts);
        return hostList;
    }
}
