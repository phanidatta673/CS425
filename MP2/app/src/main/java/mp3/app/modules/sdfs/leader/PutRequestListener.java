package mp3.app.modules.sdfs.leader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Global;
import mp3.app.Memberlist;
import mp3.app.MyPoolExecutor;
import mp3.app.messages.AckMsg;
import mp3.app.messages.FileMsg;

public class PutRequestListener implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private Memberlist memberlist = null;
    private ConcurrentMap<String, Set<Integer>> lookup = null;

    public PutRequestListener(Memberlist memberlist, ConcurrentMap<String, Set<Integer>> lookup) {
        try {
            server = new ServerSocket(Constants.PutRequestListenerPort);
        } catch (IOException e) {
        }
        this.memberlist = memberlist;
        this.lookup = lookup;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.info("Waiting for PUT request...");
                Socket client = server.accept();
                MyPoolExecutor.submitTask(new Handler(client));
            } catch (IOException e) {
                log.error("Error reading from socket: {}", e);
            }
        }
    }

    private class Handler implements Runnable {
        private Socket putter = null;

        public Handler(Socket socket) {
            this.putter = socket;
        }

        @Override
        public void run() {
            try {
                // Read files
                DataInputStream in = new DataInputStream(putter.getInputStream());
                log.trace("size in stream: {}", in.available());
                int length = in.readInt();
                byte[] bytes = in.readNBytes(length);
                FileMsg msg = FileMsg.fromBytes(bytes);
                log.info("Got put request from {}: {}",
                        Global.getVmIDFromHostName(putter.getInetAddress().getHostName()), msg);
                byte[] fileContent = in.readNBytes(msg.getFileSize());
                log.info("msglen: {}, filelen: {}", length, msg.getFileSize());

                int version = incrementAndGetVersion(msg.getName());
                FileMsg outMsg = new FileMsg(msg.getName(), msg.getFileSize(), version);
                Map<Integer, Future<?>> replicas = getReplicaIDs();
                for (int replica : replicas.keySet()) {
                    replicas.replace(replica, MyPoolExecutor.submitTask(() -> {
                        try {
                            Socket replicaSocket = new Socket(
                                    Global.getHostNameByID(replica),
                                    Constants.StoreListenerPort);

                            DataInputStream replicatIn = new DataInputStream(replicaSocket.getInputStream());
                            DataOutputStream replicaOut = new DataOutputStream(replicaSocket.getOutputStream());
                            byte[] outMsgBytes = outMsg.toBytes();
                            replicaOut.writeInt(outMsgBytes.length);
                            replicaOut.write(outMsgBytes);
                            replicaOut.write(fileContent);

                            int ackLength = replicatIn.readInt();
                            replicatIn.readNBytes(ackLength);
                            log.info("Received replica ACK from {}", replica);
                        } catch (IOException e) {
                            log.error("Error writing to replica machine {}: {}", replica, e);
                        }
                    }));
                }
                log.info("==================PUT replicas: {}", replicas.keySet());
                for (int key : replicas.keySet()) {
                    try {
                        replicas.get(key).get();
                        lookup.compute(outMsg.getNameVersionString(), (nameVersion, integers) -> {
                            if (integers == null)
                                integers = new HashSet<Integer>();
                            integers.add(key);
                            return integers;
                        });
                        log.info("Lookup table: {}", lookup.keySet());
                    } catch (Exception e) {
                        log.error("Error waiting for ack from {}: {}", key, e);
                        return;
                    }
                }

                // Ack back to putter
                DataOutputStream out = new DataOutputStream(putter.getOutputStream());
                AckMsg ackMsg = new AckMsg();
                byte[] ackBytes = ackMsg.toBytes();
                out.writeInt(ackBytes.length);
                out.write(ackBytes);
            } catch (Exception e) {
                log.error("Error handling put request: {}", e);
            }
        }

    }

    private int incrementAndGetVersion(String key) {
        int maxVersionInLookup = 0;
        synchronized (lookup) {
            Set<String> keys = new HashSet<>(lookup.keySet());
            for (String nameVersion : keys) {
                Pair<String, Integer> pair = Global.splitNameVersionString(nameVersion);
                if (key.equals(pair.getLeft()))
                    maxVersionInLookup = Math.max(maxVersionInLookup, pair.getRight());
            }
        }
        return maxVersionInLookup + 1;
    }

    private Map<Integer, Future<?>> getReplicaIDs() {
        Map<Integer, Future<?>> result = new HashMap<>();
        List<Integer> alives = new LinkedList<>();
        alives.addAll(memberlist.getAlive());
        List<Integer> replicas = randomPick(alives, Constants.NumberOfReplicas);
        replicas.forEach((replica) -> {
            result.put(replica, null);
        });
        return result;
    }

    private List<Integer> randomPick(List<Integer> list, int n) {
        Random rand = new Random();
        List<Integer> newList = new LinkedList<>();

        for (int i = 0; i < n && list.size() > 0; i++) {
            int randomIndex = rand.nextInt(list.size());
            newList.add(list.get(randomIndex));
            list.remove(randomIndex);
        }
        return newList;
    }
}
