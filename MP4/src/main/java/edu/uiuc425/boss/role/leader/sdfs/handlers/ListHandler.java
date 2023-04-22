package edu.uiuc425.boss.role.leader.sdfs.handlers;

import edu.uiuc425.boss.messages.ListRequest;
import edu.uiuc425.boss.messages.ListResponse;
import edu.uiuc425.boss.messages.Response;
import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class ListHandler extends Handler {
    private static final Logger log = LogManager.getLogger();

    private final ListRequest req;
    private List<Integer> replicants;

    public ListHandler(Socket client, SdfsTable table, ListRequest req) {
        super(client, table);
        this.req = req;
    }


    @Override
    public void run() {
        log.info("Started ListHandler...");
        String sdfsName = req.getFileHeader().getName();
        replicants = table.whoHas(sdfsName);
        try {
            OutputStream out = client.getOutputStream();
            buildACK().writeDelimitedTo(out);
            log.info("ListHandler Done...");
        } catch (IOException e) {
            log.error("Something wrong when writing response to client: {}", e.getMessage());
        }
    }

    @Override
    public Response buildACK() {
        ListResponse res = ListResponse.newBuilder()
                .addAllReplicants(replicants).build();
        return Response.newBuilder().setListRes(res).build();
    }

    @Override
    public Response buildNACK() {
        return null;
    }
}
