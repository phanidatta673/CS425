package edu.uiuc425.boss.role.leader.sdfs.handlers;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.Member;
import edu.uiuc425.boss.Memberlist;
import edu.uiuc425.boss.messages.*;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class PutHandler extends Handler {
    private static final Logger log = LogManager.getLogger();

    private final PutRequest req;

    public PutHandler(Socket client, SdfsTable table, PutRequest req) {
        super(client, table);
        this.req = req;
    }

    @Override
    public void run() {
        log.info("Started PutHandler...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Integer> replicants = pickReplicas(Constants.MaxFailures + 1);
                Map<Integer, Socket> sockets = new HashMap<>();
                int latestVersionOfFile = table.latestVersionOf(req.getFileHeader().getName());

                // Set version
                PutRequest putCmd = PutRequest.newBuilder(req)
                        .setFileHeader(FileHeader.newBuilder(req.getFileHeader())
                                .setVersion(latestVersionOfFile + 1))
                        .build();

                // Open sockets to replicant
                List<OutputStream> outs = new LinkedList<>();
                for (int replicant : replicants) {
                    Socket socket = new Socket(Global.getHostNameByRingID(replicant), Constants.CommandListenerPort);
                    outs.add(socket.getOutputStream());
                    sockets.put(replicant, socket);
                }

                // Send file to replicant
                InputStream in = client.getInputStream();
                for (OutputStream out : outs) {
                    Request cmd = Request.newBuilder().setPutReq(putCmd).build();
                    cmd.writeDelimitedTo(out);
                }

                InOutStreamer ft = new InOutStreamer(putCmd.getFileHeader().getSize(), in, outs);
                if (!ft.call()) {
                    log.error("Something bad happen when transferring replica");
                    PutResponse.newBuilder()
                            .setState(State.ACK).build()
                            .writeDelimitedTo(client.getOutputStream());
                    return;
                }

                FileHeader header = putCmd.getFileHeader();
                for (int id : replicants)
                    table.put(header.getName(), header.getVersion(), id);

                PutResponse.newBuilder()
                        .setState(State.ACK).build()
                        .writeDelimitedTo(client.getOutputStream());
                log.info("Put {}:{} to {}!",
                        header.getName(),
                        header.getVersion(),
                        replicants);
                break;
            } catch (IOException e) {
                log.error("PutHandler IO error. Retrying... : {}", e.getMessage());
            }
        }
    }

    @Override
    public Response buildACK() {
        return null;
    }

    @Override
    public Response buildNACK() {
        return null;
    }

    private List<Integer> pickReplicas(int n) {
        List<Member> memberlistView = Memberlist.getInstance().getListView();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            if (memberlistView.get(i).getIsAlive())
                candidates.add(i);
        }
        Collections.shuffle(candidates);
        return candidates.stream()
                .limit(n).collect(Collectors.toList());
    }
}
