package mp3.app.modules.sdfs.leader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Global;
import mp3.app.Memberlist;
import mp3.app.messages.FileMsg;

public class GetRequestListener implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private Memberlist memberlist = null;
    private ConcurrentMap<String, Set<Integer>> lookup = null;

    public GetRequestListener(Memberlist memberlist, ConcurrentMap<String, Set<Integer>> lookup) {
        try {
            server = new ServerSocket(Constants.GetRequestListenerPort);
        } catch (IOException e) {
        }
        this.memberlist = memberlist;
        this.lookup = lookup;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.info("Waiting for GET request...");
                Socket client = server.accept();
                new Handler(client).run();
            } catch (IOException e) {
                log.error("Error reading from socket: {}", e);
            }
        }
    }

    private class Handler implements Runnable {
        private Socket client = null;
        private Socket replicaSocket = null;

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
                log.info("Got get request from {}: {}",
                        Global.getVmIDFromHostName(client.getInetAddress().getHostName()), msg);

                // Me -> Replica
                int lastN = msg.getVersion();
                int latestVersion = getLatestVersionOf(msg.getName());
                for (int i = 0, version = latestVersion; i < lastN; i++) {
                    FileMsg resMsg = null;
                    byte[] content = null;
                    FileMsg askMsg = new FileMsg(msg.getName(), 0, version);
                    int replica = pickOneReplicaWithNameVersion(askMsg.getName(), askMsg.getVersion());
                    if (replica == 0) {
                        log.info("Cannot find {} in system", askMsg.getNameVersionString());
                        resMsg = new FileMsg(msg.getName(), 0, 0);
                        content = new byte[0];
                    } else {
                        log.info("Chosen replica: {}", replica);
                        replicaSocket = new Socket(Global.getHostNameByID(replica), Constants.AskListenerPort);
                        DataOutputStream replicaOut = new DataOutputStream(replicaSocket.getOutputStream());
                        DataInputStream replicaIn = new DataInputStream(replicaSocket.getInputStream());
                        byte[] askMsgBytes = askMsg.toBytes();
                        replicaOut.writeInt(askMsgBytes.length);
                        replicaOut.write(askMsgBytes);
                        log.info("Written ask to replica {}: {}", replica, askMsg);

                        // Replica -> Me
                        int resLength = replicaIn.readInt();
                        byte[] resBytes = replicaIn.readNBytes(resLength);
                        resMsg = FileMsg.fromBytes(resBytes);
                        content = replicaIn.readNBytes(resMsg.getFileSize());
                        log.info("Got file from replica {}", replica);
                    }
                    // Me -> Client
                    byte[] resBytes = resMsg.toBytes();
                    out.writeInt(resBytes.length);
                    out.write(resBytes);
                    out.write(content);
                    version--;
                }
            } catch (Exception e) {
                log.error("Error handling GET request: {}", e);
            }
        }

    }

    private int getLatestVersionOf(String name) {
        Set<String> versionNameStrings = new HashSet<>(lookup.keySet());
        int maxVersion = 0;
        for (String versionNameString : versionNameStrings) {
            Pair<String, Integer> pair = Global.splitNameVersionString(versionNameString);
            if (pair.getLeft().equals(name)) {
                maxVersion = Math.max(maxVersion, pair.getRight());
            }
        }
        return maxVersion;
    }

    private int pickOneReplicaWithNameVersion(String name, int version) {
        Set<String> versionNameStrings = new HashSet<>(lookup.keySet());
        for (String versionNameString : versionNameStrings) {
            Pair<String, Integer> pair = Global.splitNameVersionString(versionNameString);
            if (pair.getLeft().equals(name) && pair.getRight().equals(version)) {
                Set<Integer> replicas = new HashSet<>(lookup.get(versionNameString));
                for (int replicaID : replicas) {
                    if (memberlist.getAlive().contains(replicaID))
                        return replicaID;
                }
            }
        }
        return 0;
    }
}
