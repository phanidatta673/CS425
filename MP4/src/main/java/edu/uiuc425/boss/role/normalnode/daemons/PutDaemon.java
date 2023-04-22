package edu.uiuc425.boss.role.normalnode.daemons;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.messages.PutRequest;
import edu.uiuc425.boss.messages.Response;
import edu.uiuc425.boss.modules.sdfs.InOutStreamer;
import edu.uiuc425.boss.role.normalnode.SdfsStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class PutDaemon extends Daemon {
    private static final Logger log = LogManager.getLogger();
    private final PutRequest cmd;

    public PutDaemon(Socket socket, SdfsStorage storage, PutRequest cmd) {
        super(socket, storage);
        this.cmd = cmd;
    }

    @Override
    public void run() {
        log.info("Started PutDaemon...");
        try {
            File file = getStoreFile(cmd.getFileHeader().getName(), cmd.getFileHeader().getVersion());
            FileOutputStream out = new FileOutputStream(file, true);
            InputStream in = socket.getInputStream();

            InOutStreamer fs = new InOutStreamer(cmd.getFileHeader().getSize(), in, out);
            if (!fs.call()) {
                log.error("FileStreamer failed!");
                return;
            }
            storage.put(
                    cmd.getFileHeader().getName(),
                    cmd.getFileHeader().getVersion(),
                    file);
            log.info("PutDaemon put [{}] version [{}] to {}!",
                    cmd.getFileHeader().getName(),
                    cmd.getFileHeader().getVersion(),
                    file.getPath());
        } catch (IOException e) {
            log.error("Error while reading from leader or writing to file: {}", e.getMessage());
        }
    }

    @Override
    protected Response buildACK() {
        return null;
    }

    @Override
    protected Response buildNACK() {
        return null;
    }

    private File getStoreFile(String name, int version) throws IOException {
        File file = new File(Constants.storage, String.format("%s:%d", name, version));
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }
}
