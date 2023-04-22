package edu.uiuc425.boss.role.normalnode.daemons;

import edu.uiuc425.boss.messages.DeleteRequest;
import edu.uiuc425.boss.messages.DeleteResponse;
import edu.uiuc425.boss.messages.Response;
import edu.uiuc425.boss.messages.State;
import edu.uiuc425.boss.role.normalnode.SdfsStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class DeleteDaemon extends Daemon {
    private static final Logger log = LogManager.getLogger();
    private final DeleteRequest cmd;

    public DeleteDaemon(Socket socket, SdfsStorage storage, DeleteRequest cmd) {
        super(socket, storage);
        this.cmd = cmd;
    }

    @Override
    public void run() {
        log.trace("DeleteDaemon started...");
        try {
            List<File> fileList = storage.get(cmd.getFileHeader().getName());
            for (File f : fileList)
                f.delete();

            storage.delete(cmd.getFileHeader().getName());

            buildACK().writeDelimitedTo(socket.getOutputStream());
            log.trace("DeleteDaemon done!");
        } catch (IOException e) {
            log.error("Error while reading from leader or deleting file: {}", e.getMessage());
        }
    }

    @Override
    protected Response buildACK() {
        DeleteResponse res = DeleteResponse.newBuilder()
                .setState(State.ACK).build();
        return Response.newBuilder().setDeleteRes(res).build();
    }

    @Override
    protected Response buildNACK() {
        return null;
    }
}
