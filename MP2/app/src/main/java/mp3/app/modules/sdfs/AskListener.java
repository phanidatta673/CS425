package mp3.app.modules.sdfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Global;
import mp3.app.messages.FileMsg;

public class AskListener implements Runnable {
    private static Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private ConcurrentMap<String, Path> sdfsMap = null;

    public AskListener(ConcurrentMap<String, Path> sdfsMap) {
        try {
            server = new ServerSocket(Constants.AskListenerPort);
        } catch (IOException e) {
            log.error("Error starting ask listener: {}", e);
        }
        this.sdfsMap = sdfsMap;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.info("Listening to ask requests...");
                Socket socket = server.accept();

                // Read ask request
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                int msgLength = in.readInt();
                byte[] msgBytes = in.readNBytes(msgLength);
                FileMsg msg = FileMsg.fromBytes(msgBytes);
                log.info("Got ask request from {}: {}",
                        Global.getVmIDFromHostName(socket.getInetAddress().getHostName()), msg);

                // Get that file
                File file = sdfsMap.get(msg.getNameVersionString()).toFile();
                byte[] fileContent = FileUtils.readFileToByteArray(file);
                log.info("Loaded file {}", msg.getName());

                // Response
                FileMsg response = new FileMsg(msg.getName(), fileContent.length, msg.getVersion());
                byte[] responseBytes = response.toBytes();
                out.writeInt(responseBytes.length);
                out.write(responseBytes);
                out.write(fileContent);
                log.info("File responded: {}", response);
            } catch (IOException e) {
                log.error("Error while dealing with client: {}", e);
            }
        }
    }

}
