package edu.uiuc425.boss.role.normalnode;

import edu.uiuc425.boss.messages.Request;
import edu.uiuc425.boss.modules.sdfs.MessageListener;
import edu.uiuc425.boss.role.normalnode.daemons.DeleteDaemon;
import edu.uiuc425.boss.role.normalnode.daemons.GetDaemon;
import edu.uiuc425.boss.role.normalnode.daemons.PutDaemon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandListener extends MessageListener {
    private static final Logger log = LogManager.getLogger();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final SdfsStorage storage;

    public CommandListener(int port, SdfsStorage storage) {
        super(port);
        this.storage = storage;
    }

    @Override
    protected final void dispatch(Socket client) {
        try {
            Request command = Request.parseDelimitedFrom(client.getInputStream());
            if (command.hasPutReq()) {
                executor.submit(new PutDaemon(client, storage, command.getPutReq()));
            } else if (command.hasGetReq()) {
                executor.submit(new GetDaemon(client, storage, command.getGetReq()));
            } else if (command.hasDeleteReq()) {
                executor.submit(new DeleteDaemon(client, storage, command.getDeleteReq()));
            }
        } catch (IOException e) {
            log.error("Error command listener dispatch: {}", e.getMessage());
        }
    }
}
