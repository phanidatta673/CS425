package edu.uiuc425.boss.role.normalnode.daemons;

import edu.uiuc425.boss.messages.Response;
import edu.uiuc425.boss.role.normalnode.SdfsStorage;

import java.net.Socket;

public abstract class Daemon implements Runnable {
    protected final Socket socket;
    protected final SdfsStorage storage;

    protected Daemon(Socket socket, SdfsStorage storage) {
        this.socket = socket;
        this.storage = storage;
    }

    @Override
    public abstract void run();

    protected abstract Response buildACK();

    protected abstract Response buildNACK();
}
