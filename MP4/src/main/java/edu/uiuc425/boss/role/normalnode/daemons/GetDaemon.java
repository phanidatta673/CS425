package edu.uiuc425.boss.role.normalnode.daemons;

import edu.uiuc425.boss.messages.FileHeader;
import edu.uiuc425.boss.messages.GetRequest;
import edu.uiuc425.boss.messages.GetResponse;
import edu.uiuc425.boss.messages.Response;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import edu.uiuc425.boss.role.normalnode.SdfsStorage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;

import static edu.uiuc425.boss.messages.State.ACK;

public class GetDaemon extends Daemon {
    private static final Logger log = LogManager.getLogger();
    private final GetRequest cmd;
    private long fileSize = 0;

    public GetDaemon(Socket socket, SdfsStorage storage, GetRequest cmd) {
        super(socket, storage);
        this.cmd = cmd;
    }

    @Override
    public void run() {
        try {
            File file = storage.get(
                    cmd.getFileHeader().getName(),
                    cmd.getFileHeader().getVersion());
            fileSize = getFileSize(file);
            InputStream in = FileUtils.openInputStream(file);
            OutputStream out = socket.getOutputStream();

            buildACK().writeDelimitedTo(out);

            InOutStreamer fs = new InOutStreamer(fileSize, in, out);
            if (!fs.call()) {
                log.error("GetWorker response failed!");
                return;
            }
            log.info("GetWorker Done!");
        } catch (IOException e) {
            log.error("Error while reading from leader or writing to file: {}", e.getMessage());
        }
    }

    @Override
    protected Response buildACK() {
        FileHeader head = FileHeader.newBuilder(cmd.getFileHeader())
                .setSize(fileSize).build();
        GetResponse res = GetResponse.newBuilder()
                .setFileHeader(head)
                .setState(ACK).build();
        return Response.newBuilder().setGetRes(res).build();
    }

    @Override
    protected Response buildNACK() {
        return null;
    }

    private long getFileSize(File file) {
        long ans = -1;
        try {
            ans = Files.size(file.toPath());
        } catch (IOException e) {
            log.error("File [{}] might not be found", file.toPath());
        }
        return ans;
    }

}
