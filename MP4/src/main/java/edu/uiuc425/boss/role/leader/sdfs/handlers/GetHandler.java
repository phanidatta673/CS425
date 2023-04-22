package edu.uiuc425.boss.role.leader.sdfs.handlers;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.messages.*;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class GetHandler extends Handler {
    private static final Logger log = LogManager.getLogger();
    private static final Random random = new Random();

    private final GetRequest req;

    public GetHandler(Socket client, SdfsTable table, GetRequest req) {
        super(client, table);
        this.req = req;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int numVersions = req.getFileHeader().getVersion();
                int latestVer = table.latestVersionOf(req.getFileHeader().getName());
                for (int i = 0; i < numVersions; i++) {
                    int version = latestVer - i;
                    List<Integer> replicants = table.whoHas(req.getFileHeader().getName(), version);
                    if (replicants.size() == 0) {
                        log.error("Nobody has this file!");
                        buildNACK().writeDelimitedTo(client.getOutputStream());
                        return;
                    }
                    int replicant = replicants.get(random.nextInt(replicants.size()));
                    Socket socket = new Socket(
                            Global.getHostNameByRingID(replicant),
                            Constants.CommandListenerPort);
                    log.info("Getting replica from replicant {}...", replicant);

                    // Getting replica from replicant
                    GetRequest getCmd = GetRequest.newBuilder(req)
                            .setFileHeader(FileHeader.newBuilder(req.getFileHeader())
                                    .setVersion(version)).build();
                    Request cmd = Request.newBuilder().setGetReq(getCmd).build();
                    cmd.writeDelimitedTo(socket.getOutputStream());

                    // Write the content back to client
                    Response res = Response.parseDelimitedFrom(socket.getInputStream());
                    res.writeDelimitedTo(client.getOutputStream());
                    long fileSize = res.getGetRes().getFileHeader().getSize();
                    InOutStreamer ios = new InOutStreamer(fileSize, socket.getInputStream(), client.getOutputStream());
                    if (!ios.call()) {
                        throw new IOException("My OP error");
                    }
                    socket.close();
                }
                log.info("GetHandler done!");
                break;
            } catch (IOException e) {
                log.error("GetHandler IO error. Retrying... : {}", e.getMessage());
            }
        }
    }

    @Override
    public Response buildACK() {
        return null;
    }

    @Override
    public Response buildNACK() {
        FileHeader resFile = FileHeader.newBuilder(req.getFileHeader())
                .setVersion(0)
                .setSize(0).build();
        GetResponse getResponse = GetResponse.newBuilder()
                .setState(State.NACK)
                .setFileHeader(resFile).build();
        return Response.newBuilder().setGetRes(getResponse).build();
    }
}
