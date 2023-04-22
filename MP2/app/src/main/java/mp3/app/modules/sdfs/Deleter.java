package mp3.app.modules.sdfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.messages.AckMsg;
import mp3.app.messages.FileMsg;

public class Deleter implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private String sdfsName = null;

    public Deleter(String sdfsName) {
        this.sdfsName = sdfsName;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(
                    Constants.LeaderHostName,
                    Constants.DeleteRequestListenerPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileMsg msg = new FileMsg(sdfsName, 0, 0);
            byte[] msgBytes = msg.toBytes();
            out.writeInt(msgBytes.length);
            out.write(msgBytes);
            log.info("Done sending DELETE to coordinator. Waiting for ack...");

            // Wait for ack
            int ackMsgLength = in.readInt();
            byte[] ackMsgBytes = in.readNBytes(ackMsgLength);
            AckMsg ack = AckMsg.fromBytes(ackMsgBytes);
            log.info("DELETE is acked by coordinator");
            socket.close();
        } catch (IOException e) {
            log.error("Error connecting to server: {}", e);
        }
    }
}
