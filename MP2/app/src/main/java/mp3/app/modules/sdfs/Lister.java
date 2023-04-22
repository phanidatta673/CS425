package mp3.app.modules.sdfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.messages.FileMsg;
import mp3.app.messages.ListMsg;

public class Lister implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private String sdfsName = null;

    public Lister(String sdfsName) {
        this.sdfsName = sdfsName;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(
                    Constants.LeaderHostName,
                    Constants.ListRequestListenerPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileMsg msg = new FileMsg(sdfsName, 0, 0);
            byte[] msgBytes = msg.toBytes();
            out.writeInt(msgBytes.length);
            out.write(msgBytes);
            log.info("Sent LIST to coordinator. Waiting for response...");

            // Wait for ack
            int resLength = in.readInt();
            byte[] resBytes = in.readNBytes(resLength);
            ListMsg result = ListMsg.fromBytes(resBytes);

            if (result.getList().size() == 0) {
                log.error("File {} does not exist", msg.getName());
            } else {
                log.info("LIST response: {}", result.getList());
            }

            socket.close();
        } catch (IOException e) {
            log.error("Error connecting to server: {}", e);
        }
    }
}
