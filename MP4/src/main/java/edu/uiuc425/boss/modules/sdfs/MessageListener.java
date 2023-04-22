package edu.uiuc425.boss.modules.sdfs;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class MessageListener implements Runnable {
    protected final Logger log = LogManager.getLogger();
    private final ServerSocket server;


    protected MessageListener(int port) {
        ServerSocket tmp = null;
        try {
            tmp = new ServerSocket(port);
        } catch (IOException e) {
            log.error("Error while creating the server socket: {}", e.getMessage());
        }
        server = tmp;
    }

    @Override
    public final void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket client = server.accept();
                dispatch(client);
            } catch (IOException e) {
                log.error("MessageListener error while dealing with message: {}", e.getMessage());
            }
        }
    }

    protected abstract void dispatch(Socket client) throws IOException;
}