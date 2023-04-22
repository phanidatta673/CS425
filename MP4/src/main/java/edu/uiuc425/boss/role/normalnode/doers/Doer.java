package edu.uiuc425.boss.role.normalnode.doers;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.messages.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public abstract class Doer implements Runnable {
    private static final Logger log = LogManager.getLogger();
    protected Socket socket;

    protected void connectToServer() throws IOException {
        socket = new Socket(Constants.LeaderHostName, Constants.CoordinatorPort);
    }

    @Override
    public abstract void run();

    protected void sendRequest() throws IOException {
        if (socket == null)
            connectToServer();
        buildRequest().writeDelimitedTo(socket.getOutputStream());
    }

    protected abstract Request buildRequest();
}
