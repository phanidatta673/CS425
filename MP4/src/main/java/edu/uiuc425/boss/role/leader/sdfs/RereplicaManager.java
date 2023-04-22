package edu.uiuc425.boss.role.leader.sdfs;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.Memberlist;
import edu.uiuc425.boss.messages.*;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;

public class RereplicaManager implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private static final Random random = new Random();
    private final int target;
    private final Queue<Pair<String, Integer>> files;
    private final SdfsTable table;
    private final Memberlist memberlist = Memberlist.getInstance();

    public RereplicaManager(int target, Queue<Pair<String, Integer>> files, SdfsTable table) {
        this.target = target;
        this.files = files;
        this.table = table;
    }

    @Override
    public void run() {
        log.info("Starts rereplicating...");
        while (!files.isEmpty()) {
            Pair<String, Integer> p = files.peek();
            String name = p.getLeft();
            int version = p.getRight();
            Pair<Integer, Integer> replicants = pickTransferReplicants(name, version);
            log.trace("For file {}:{}, I pick from {} to {}",
                    name, version, replicants.getLeft(), replicants.getRight());

            try (Socket source = new Socket(
                    Global.getHostNameByRingID(replicants.getLeft()),
                    Constants.CommandListenerPort);
                 Socket dest = new Socket(
                         Global.getHostNameByRingID(replicants.getRight()),
                         Constants.CommandListenerPort)) {
                InputStream sourceIn = source.getInputStream();
                OutputStream sourceOut = source.getOutputStream();
                OutputStream destOut = dest.getOutputStream();

                writeGetTo(name, version, sourceOut);

                GetResponse resFromSource = Response.parseDelimitedFrom(sourceIn).getGetRes();
                long fileSize = resFromSource.getFileHeader().getSize();

                writePutTo(name, version, destOut);

                InOutStreamer streamer = new InOutStreamer(fileSize, sourceIn, destOut);
                if (!streamer.call())
                    throw new IOException("Streamer failed!");

                files.poll();
                table.deleteReplicaFromFileVersion(name, version, target);
                log.info("All files are rereplicated!");
            } catch (IOException e) {
                log.error("Error while rereplicating {}:{} : {}",
                        name, version, e.getMessage());
            }
        }
    }

    private Pair<Integer, Integer> pickTransferReplicants(String name, int version) {
        List<Integer> replicants = table.whoHas(name, version);
        Collections.shuffle(replicants);

        Set<Integer> aliveMembers = memberlist.getAliveSetView();

        int source = 0;
        int dest = 0;

        for (int member : aliveMembers) {
            if (replicants.contains(member)) {
                source = member;
            } else
                dest = member;

            if (source != 0 && dest != 0)
                break;
        }
        return new ImmutablePair<>(source, dest);
    }

    private void writeGetTo(String name, int version, OutputStream out) throws IOException {
        FileHeader header = FileHeader.newBuilder()
                .setName(name)
                .setVersion(version)
                .build();
        GetRequest getReq = GetRequest.newBuilder()
                .setFileHeader(header).build();
        Request req = Request.newBuilder().setGetReq(getReq).build();
        req.writeDelimitedTo(out);
    }

    private void writePutTo(String name, int version, OutputStream out) throws IOException {
        FileHeader header = FileHeader.newBuilder()
                .setName(name)
                .setVersion(version)
                .build();
        PutRequest putReq = PutRequest.newBuilder()
                .setFileHeader(header).build();
        Request req = Request.newBuilder().setPutReq(putReq).build();
        req.writeDelimitedTo(out);
    }
}
