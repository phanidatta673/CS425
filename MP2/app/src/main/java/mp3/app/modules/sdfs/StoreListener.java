package mp3.app.modules.sdfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Global;
import mp3.app.messages.AckMsg;
import mp3.app.messages.FileMsg;

public class StoreListener implements Runnable {
    private static Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private ConcurrentMap<String, Path> sdfsMap = null;

    public StoreListener(ConcurrentMap<String, Path> sdfsMap) {
        try {
            server = new ServerSocket(Constants.StoreListenerPort);
        } catch (IOException e) {
            log.error("Error starting store listener: {}", e);
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
                log.info("Got store request from {}: {}",
                        Global.getVmIDFromHostName(socket.getInetAddress().getHostName()), msg);
                byte[] fileContent = in.readNBytes(msg.getFileSize());

                Path storePath = store(msg.getNameVersionString(), fileContent);
                if (storePath != null) {
                    sdfsMap.put(msg.getNameVersionString(), storePath);
                    log.info("{} stored successfully to {}. Acking back...", msg.getNameVersionString(), storePath);

                    // Send ack back
                    AckMsg ack = new AckMsg();
                    byte[] ackByte = ack.toBytes();
                    out.writeInt(ackByte.length);
                    out.write(ackByte);
                }
            } catch (IOException e) {
                log.error("Error while dealing with client: {}", e);
            }
        }
    }

    private Path store(String name, byte[] content) {
        try {
            Path storePath = getStorePath(name);
            log.trace("Writing {} to path {}...", name, storePath);

            FileUtils.writeByteArrayToFile(storePath.toFile(), content);
            return storePath;
        } catch (IOException e) {
            log.error("Error writing to file: {}", e);
        }
        return null;
    }

    private Path getStorePath(String name) {
        Path storePath = Paths.get(Constants.storage.toString(), name);
        storePath = storePath.toAbsolutePath();
        log.trace("Absolute path: {}", storePath);
        return storePath;
    }
}
