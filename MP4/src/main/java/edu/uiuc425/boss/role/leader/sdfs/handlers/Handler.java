package edu.uiuc425.boss.role.leader.sdfs.handlers;

import edu.uiuc425.boss.messages.Response;
import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;

import java.net.Socket;

public abstract class Handler implements Runnable {
    protected final Socket client;
    protected final SdfsTable table;

    protected Handler(Socket client, SdfsTable table) {
        this.client = client;
        this.table = table;
    }

    @Override
    public abstract void run();

    public abstract Response buildACK();

    public abstract Response buildNACK();
}
