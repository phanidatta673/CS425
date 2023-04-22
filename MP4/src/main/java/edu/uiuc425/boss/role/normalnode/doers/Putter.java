package edu.uiuc425.boss.role.normalnode.doers;

import edu.uiuc425.boss.messages.*;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Putter extends Doer {
    private static final Logger log = LogManager.getLogger();
    private final File localFile;
    private final String sdfsName;
    private long fileSize = 0;

    public Putter(String localName, String sdfsName) {
        localFile = new File(localName).getAbsoluteFile();
        this.sdfsName = sdfsName;
    }

    @Override
    public void run() {
        log.info("Putter started...");
        try {
            fileSize = getFileSize(localFile);

            connectToServer();
            sendRequest();

            InOutStreamer ft = new InOutStreamer(
                    fileSize,
                    FileUtils.openInputStream(localFile),
                    socket.getOutputStream());

            if (!ft.call()) {
                log.error("File transferer failed!");
                return;
            }

            PutResponse res = Response.parseDelimitedFrom(socket.getInputStream())
                    .getPutRes();
            if (res.getState() == State.NACK) {
                log.error("File transfer: Leader said NACK :(");
                return;
            }
            log.info("Receive ACK from leader! Putter succeed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Request buildRequest() {
        PutRequest req = PutRequest.newBuilder()
                .setFileHeader(FileHeader.newBuilder()
                        .setName(sdfsName)
                        .setSize(fileSize)).build();
        return Request.newBuilder().setPutReq(req).build();
    }

    private long getFileSize(File file) throws IOException {
        long ans = 0;
        try {
            ans = Files.size(file.toPath());
        } catch (IOException e) {
            log.error("File [{}] might not be found", file.toPath());
            throw e;
        }
        return ans;
    }
}
