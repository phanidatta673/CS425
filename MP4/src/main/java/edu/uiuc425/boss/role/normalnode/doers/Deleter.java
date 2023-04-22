package edu.uiuc425.boss.role.normalnode.doers;

import edu.uiuc425.boss.messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Deleter extends Doer {
    private static final Logger log = LogManager.getLogger();
    private final String sdfsName;

    public Deleter(String sdfsName) {
        this.sdfsName = sdfsName;
    }

    @Override
    public void run() {
        log.trace("Running deleter...");
        try {
            connectToServer();
            sendRequest();

            DeleteResponse res = Response.parseDelimitedFrom(socket.getInputStream())
                    .getDeleteRes();
            if (res.getState() == State.NACK)
                log.error("Delete file failed!");
            log.trace("Delete {} succeed!", sdfsName);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    protected Request buildRequest() {
        DeleteRequest req = DeleteRequest.newBuilder()
                .setFileHeader(FileHeader.newBuilder()
                        .setName(sdfsName)
                        .setVersion(0)).build();
        return Request.newBuilder().setDeleteReq(req).build();
    }
}
