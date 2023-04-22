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

public class DeleteRequestListener implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private Memberlist memberlist = null;
    private ConcurrentMap<String, Set<Integer>> lookup = null;

    public DeleteRequestListener(Memberlist memberlist, ConcurrentMap<String, Set<Integer>> lookup) {
        try {
            server = new ServerSocket(Constants.DeleteRequestListenerPort);
        } catch (IOException e) {
        }
        this.memberlist = memberlist;
        this.lookup = lookup;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.info("Waiting for DELETE request...");
                Socket client = server.accept();
                MyPoolExecutor.submitTask(new Handler(client));
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
                // Read request
                DataInputStream in = new DataInputStream(client.getInputStream());
                log.info("size in stream: {}", in.available());
                int length = in.readInt();
                byte[] bytes = in.readNBytes(length);
                FileMsg msg = FileMsg.fromBytes(bytes);
                log.info("Got delete request from {}: {}",
                        Global.getVmIDFromHostName(client.getInetAddress().getHostName()), msg);

                List<Task> tasks = getRemoveTasks(msg.getName());

                for (Task task : tasks) {
                    task.future.get();
                    lookup.compute(task.versionNameString, (key, set) -> {
                        if (set == null)
                            return null;

                        set.remove(task.machineID);
                        return (set.size() == 0) ? null : set;
                    });
                }

                log.info("Delete done. Acking back to client...");
                // Ack back to client
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                AckMsg ackMsg = new AckMsg();
                byte[] ackBytes = ackMsg.toBytes();
                out.writeInt(ackBytes.length);
                out.write(ackBytes);
            } catch (Exception e) {
                log.error("Error handling put request: {}", e);
            }
        }

    }

    private List<Task> getRemoveTasks(String name) {
        List<Task> tasks = new LinkedList<>();
        Set<String> versionNames = new HashSet<>(lookup.keySet());
        for (String versionName : versionNames) {
            Pair<String, Integer> pair = Global.splitNameVersionString(versionName);
            if (pair.getLeft().equals(name)) {
                Set<Integer> replicas = new HashSet<>(lookup.get(versionName));
                log.info("Replica with {}: {}", versionName, replicas);
                for (int replica : replicas) {
                    if (memberlist.getAlive().contains(replica)) {
                        Task task = new Task(versionName, replica, MyPoolExecutor.submitTask(() -> {
                            try {
                                Socket socket = new Socket(
                                        Global.getHostNameByID(replica),
                                        Constants.RemoveListernerPort);
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                FileMsg msg = new FileMsg(name, 0, pair.getRight());
                                byte[] msgBytes = msg.toBytes();
                                out.writeInt(msgBytes.length);
                                out.write(msgBytes);
                                log.info("Sent REMOVE {} to replica {}", msg.getNameVersionString(), replica);

                                DataInputStream in = new DataInputStream(socket.getInputStream());
                                int ackLength = in.readInt();
                                byte[] ackBytes = in.readNBytes(ackLength);
                                AckMsg ack = AckMsg.fromBytes(ackBytes);
                                log.info("Got ACK deleting {} from {}", versionName, replica);
                                socket.close();
                            } catch (IOException e) {
                                log.error("Error assigning tasks of replica {}", replica);
                            }
                        }));
                        tasks.add(task);
                    }
                }
            }
        }
        return tasks;
    }

    private class Task {
        public String versionNameString = null;
        public int machineID = 0;
        public Future<?> future = null;

        public Task(String versionNameString, int machineID, Future<?> future) {
            this.versionNameString = versionNameString;
            this.machineID = machineID;
            this.future = future;
        }
    }
}
