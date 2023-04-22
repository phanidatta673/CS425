package mp3.app.modules.sdfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Global;
import mp3.app.messages.AckMsg;
import mp3.app.messages.FileMsg;

public class RemoveListener implements Runnable {
    private static Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private ConcurrentMap<String, Path> sdfsMap = null;

    public RemoveListener(ConcurrentMap<String, Path> sdfsMap) {
        try {
            server = new ServerSocket(Constants.RemoveListernerPort);
        } catch (IOException e) {
            log.error("Error starting remove listener: {}", e);
        }
        this.sdfsMap = sdfsMap;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = server.accept();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                int msgLength = in.readInt();
                byte[] msgBytes = in.readNBytes(msgLength);
                FileMsg msg = FileMsg.fromBytes(msgBytes);
                log.info("Got remove request from {}: {}",
                        Global.getVmIDFromHostName(socket.getInetAddress().getHostName()), msg);

                remove(msg.getName());
                log.info("{} removed successfully. Acking back...", msg.getName());

                // Send ack back
                AckMsg ack = new AckMsg();
                byte[] ackByte = ack.toBytes();
                out.writeInt(ackByte.length);
                out.write(ackByte);
            } catch (IOException e) {
                log.error("Error while dealing with client: {}", e);
            }
        }
    }

    private void remove(String name) {
        Set<String> versionNames = new HashSet<>(sdfsMap.keySet());
        for (String versionName : versionNames) {
            try {
                Pair<String, Integer> pair = Global.splitNameVersionString(versionName);
                if (pair.getLeft().equals(name)) {
                    Path path = sdfsMap.get(versionName);
                    Files.delete(path);
                    sdfsMap.remove(versionName);
                    log.info("Removed {} at path: {}", versionName, path);
                }
            } catch (IOException e) {
                log.error("Error removing file: {}", e);
            }
        }
    }
}
