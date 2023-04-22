package mp3.app.modules.sdfs;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.messages.FileMsg;

public class Getter implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private String sdfsName = null;
    private String localName = null;
    private int lastN = 0;

    public Getter(String sdfsName, String localName, int lastN) {
        this.sdfsName = sdfsName;
        this.localName = localName;
        this.lastN = lastN;
    }

    public Getter(String sdfsName, String localName) {
        this(sdfsName, localName, 1);
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(
                    Constants.LeaderHostName,
                    Constants.GetRequestListenerPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileMsg msg = new FileMsg(sdfsName, 0, lastN);
            byte[] msgBytes = msg.toBytes();
            out.writeInt(msgBytes.length);
            out.write(msgBytes);
            log.info("Sent GET last {} to coordinator. Waiting for response...", lastN);

            List<byte[]> contentLists = new LinkedList<>();
            for (int i = 0; i < lastN; i++) {
                // Wait for ack
                int resultMsgLength = in.readInt();
                byte[] resultMsgBytes = in.readNBytes(resultMsgLength);
                FileMsg resultMsg = FileMsg.fromBytes(resultMsgBytes);

                // No such file exists
                if (resultMsg.getVersion() < 1) {
                    log.error("Version of file {} does not exist", resultMsg.getName());
                } else {
                    byte[] fileContent = in.readNBytes(resultMsg.getFileSize());
                    contentLists.add(fileContent);
                    log.info("Got file {}", resultMsg.getNameVersionString());
                }
            }
            File file = saveAll(contentLists);
            log.info("GET last {} saved at {}", lastN, file.toPath());
            socket.close();
        } catch (IOException e) {
            log.error("Error connecting to server: {}", e);
        }
    }

    private File saveAll(List<byte[]> contentList) {
        File saveFile = getSavePath(localName).toFile();
        if (saveFile.isFile()) {
            saveFile.delete();
        }
        saveFile.getParentFile().mkdirs();
        try (FileOutputStream output = new FileOutputStream(saveFile, true)) {
            for (int i = 0; i < contentList.size(); i++) {
                byte[] content = contentList.get(i);
                output.write(content);

                // Add delimiters if not last
                if (i != contentList.size() - 1) {
                    String delimiter = "";
                    for (int j = 0; j < 15; j++) {
                        delimiter += "*\n";
                    }
                    output.write(delimiter.getBytes());
                }
            }
        } catch (IOException e) {
            log.error("Error while writing to file: {}", e);
        }
        return saveFile;
    }

    private Path getSavePath(String name) {
        Path savePath = Paths.get(Constants.saved.toString(), name);
        savePath = savePath.toAbsolutePath();
        log.trace("Absolute path: {}", savePath);
        return savePath;
    }
}
