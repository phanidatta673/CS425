package edu.uiuc425.boss.role.normalnode.doers;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.messages.*;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class Getter extends Doer {
    private static final Logger log = LogManager.getLogger();
    private final String localName;
    private final int numVersions;
    private final String sdfsName;

    public Getter(String sdfsName, int numVersions, String localName) {
        this.localName = localName;
        this.numVersions = numVersions;
        this.sdfsName = sdfsName;
    }

    public Getter(String sdfsName, String localName) {
        this(sdfsName, 1, localName);
    }

    @Override
    public void run() {
        log.info("Started Getter...");
        try {
            connectToServer();
            sendRequest();

            for (int i = 0; i < numVersions; i++) {
                GetResponse res = Response.parseDelimitedFrom(socket.getInputStream())
                        .getGetRes();
                FileHeader head = res.getFileHeader();
                if (head.getVersion() == 0) {
                    log.error("This version of file {} doesn't exist in SDFS!", sdfsName);
                    return;
                }

                File localFile = (numVersions > 1) ?
                        getStoreFile(localName, head.getVersion())
                        : getStoreFile(localName);

                InOutStreamer ft = new InOutStreamer(
                        head.getSize(),
                        socket.getInputStream(),
                        FileUtils.openOutputStream(localFile));

                if (!ft.call()) {
                    log.error("Receive file failed!");
                    return;
                }
                log.info("File [{}] saved to {}", sdfsName, localFile.toString());
            }
            log.info("GET succeeds with {} versions of [{}].", numVersions, sdfsName);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    protected Request buildRequest() {
        GetRequest req = GetRequest.newBuilder()
                .setFileHeader(FileHeader.newBuilder()
                        .setName(sdfsName)
                        .setVersion(numVersions)).build();
        return Request.newBuilder().setGetReq(req).build();
    }

    private File getStoreFile(String name, int version) throws IOException {
        File file = new File(Constants.saved, String.format("%s:%d", name, version));
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }

    private File getStoreFile(String name) throws IOException {
        File file = new File(Constants.saved, name);
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }
}
