package edu.uiuc425.boss.role.normalnode.doers;

import edu.uiuc425.boss.messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Lister extends Doer {
    private static final Logger log = LogManager.getLogger();
    private final String sdfsName;

    public Lister(String sdfsName) throws IOException {
        this.sdfsName = sdfsName;
    }

    @Override
    public void run() {
        Request req = buildRequest();
        try {
            connectToServer();
            sendRequest();

            InputStream in = socket.getInputStream();
            ListResponse res = Response.parseDelimitedFrom(in).getListRes();
            List<Integer> replicants = res.getReplicantsList();

            log.info("Replicant with file [{}]: {}", sdfsName, replicants);
        } catch (IOException e) {
            log.error("Something wrong when communicating with coordinator: {}", e.getMessage());
        }
    }

    @Override
    protected Request buildRequest() {
        ListRequest listRequest = ListRequest.newBuilder()
                .setFileHeader(FileHeader.newBuilder()
                        .setName(sdfsName).build()).build();
        return Request.newBuilder().setListReq(listRequest).build();
    }
}
