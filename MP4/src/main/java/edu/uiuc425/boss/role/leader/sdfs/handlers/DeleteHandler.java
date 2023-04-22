package edu.uiuc425.boss.role.leader.sdfs.handlers;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.messages.*;
import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteHandler extends Handler {
    private static final Logger log = LogManager.getLogger();
    private final DeleteRequest req;

    public DeleteHandler(Socket client, SdfsTable table, DeleteRequest req) {
        super(client, table);
        this.req = req;
    }

    @Override
    public void run() {
        log.info("Started DeleteHandler...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Integer> replicants = table.whoHas(req.getFileHeader().getName());
                Map<Integer, Socket> sockets = new HashMap<>();

                // Open sockets to replicant
                for (int replicant : replicants) {
                    Socket socket = new Socket(Global.getHostNameByRingID(replicant), Constants.CommandListenerPort);
                    Request requestToReplicant = Request.newBuilder().setDeleteReq(req).build();
                    requestToReplicant.writeDelimitedTo(socket.getOutputStream());
                    sockets.put(replicant, socket);
                }

                for (int replicant : replicants) {
                    Socket socket = sockets.get(replicant);
                    DeleteResponse res = Response.parseDelimitedFrom(socket.getInputStream()).getDeleteRes();
                    if (res.getState() == State.ACK)
                        table.removeReplica(req.getFileHeader().getName(), replicant);
                    else
                        log.error("Received NACK from DeleteDaemon. Not removing the entry!");
                }

                buildACK().writeDelimitedTo(client.getOutputStream());
                log.info("DeleteHandler done!");
                break;
            } catch (IOException e) {
                log.error("DeleteHandler IO error. Retrying... : {}", e.getMessage());
            }
        }
    }

    @Override
    public Response buildACK() {
        DeleteResponse res = DeleteResponse.newBuilder()
                .setState(State.ACK).build();
        return Response.newBuilder().setDeleteRes(res).build();
    }

    @Override
    public Response buildNACK() {
        return null;
    }
}
