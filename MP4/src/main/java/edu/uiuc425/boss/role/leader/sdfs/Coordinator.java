package edu.uiuc425.boss.role.leader.sdfs;

import edu.uiuc425.boss.messages.Request;
import edu.uiuc425.boss.modules.sdfs.MessageListener;
import edu.uiuc425.boss.role.leader.sdfs.handlers.DeleteHandler;
import edu.uiuc425.boss.role.leader.sdfs.handlers.GetHandler;
import edu.uiuc425.boss.role.leader.sdfs.handlers.ListHandler;
import edu.uiuc425.boss.role.leader.sdfs.handlers.PutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Coordinator extends MessageListener {
    private static final Logger log = LogManager.getLogger();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final SdfsTable table = new SdfsTable();

    public Coordinator(int port) {
        super(port);
        executor.submit(new RereplicaChecker(table));
    }

    @Override
    protected final void dispatch(Socket client) {
        try {
            Request request = Request.parseDelimitedFrom(client.getInputStream());
            if (request.hasPutReq()) {
                executor.submit(new PutHandler(client, table, request.getPutReq()));
            } else if (request.hasGetReq()) {
                executor.submit(new GetHandler(client, table, request.getGetReq()));
            } else if (request.hasDeleteReq()) {
                executor.submit(new DeleteHandler(client, table, request.getDeleteReq()));
            } else if (request.hasListReq()) {
                executor.submit(new ListHandler(client, table, request.getListReq()));
            }
        } catch (IOException e) {
            log.error("Error coordinator dispatch: {}", e.getMessage());
        }
    }
}
