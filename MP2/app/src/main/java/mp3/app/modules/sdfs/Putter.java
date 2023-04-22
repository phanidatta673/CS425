package mp3.app.modules.sdfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.messages.AckMsg;
import mp3.app.messages.FileMsg;

public class Putter implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private String sdfsName = null;
    private String localName = null;

    public Putter(String sdfsName, String localName) {
        this.sdfsName = sdfsName;
        this.localName = localName;
    }

    @Override
    public void run() {
        File file = new File(localName);
        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(file.toPath());
        } catch (FileNotFoundException e) {
            log.error("File [{}] is not found: {}", localName, e);
            return;
        } catch (IOException e) {
            log.error("Error while reading file [{}]: {}", localName, e);
            return;
        }

        try {
            Socket socket = new Socket(
                    Constants.LeaderHostName,
                    Constants.PutRequestListenerPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileMsg msg = new FileMsg(sdfsName, fileContent.length, 0);
            byte[] msgBytes = msg.toBytes();
            out.writeInt(msgBytes.length);
            out.write(msgBytes);
            out.write(fileContent);
            log.info("Done transfering PUT to coordinator. Waiting for ack...");

            // Wait for ack
            int ackMsgLength = in.readInt();
            byte[] ackMsgBytes = in.readNBytes(ackMsgLength);
            AckMsg ack = AckMsg.fromBytes(ackMsgBytes);
            log.info("PUT is acked by coordinator");
            socket.close();
        } catch (IOException e) {
            log.error("Error connecting to server: {}", e);
        }
    }
}
