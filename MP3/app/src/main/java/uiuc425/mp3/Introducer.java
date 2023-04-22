package uiuc425.mp3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uiuc425.mp3.Messages.*;

public class Introducer implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private ServerSocket server = null;

    private AtomicIntegerArray memberlist = null;
    private AtomicLongArray lastSeenByMe = null;

    Introducer(AtomicIntegerArray memberlist, AtomicLongArray lastSeenByMe) {
        this.memberlist = memberlist;
        this.lastSeenByMe = lastSeenByMe;
        try {
            server = new ServerSocket(Constants.JoinPort);
        } catch (IOException e) {
            log.fatal("Server cannot be constructed: {}", e.getMessage());
        }
    }

    // TODO: Move to separate
    private class JoinHandler implements Runnable {
        private Socket client = null;

        public JoinHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            updateMemberList();
            writePingMessageToClient();
        }

        private void updateMemberList() {
            String hostName = client.getInetAddress().getHostName();
            log.info("{} is joining!", hostName);
            int vmID = Common.getVmIDFromHostName(hostName);

            memberlist.set(vmID, 1);
            lastSeenByMe.set(vmID, System.currentTimeMillis());
        }

        private void writePingMessageToClient() {
            InetSocketAddress addr = new InetSocketAddress(
                    client.getInetAddress(),
                    Constants.ReceivePort);
            PingMessage msg = new PingMessage(
                    MessageType.PING,
                    System.currentTimeMillis(),
                    memberlist,
                    lastSeenByMe);
            msg.sendTo(addr);
        }
    }

    @Override
    public void run() {
        log.info("Introducer starts listening at port {}", server.getLocalPort());
        while (true) {
            try {
                Socket client = server.accept();
                new Thread(new JoinHandler(client)).start();
            } catch (IOException e) {
                log.error("Error while accepting clients: {}", e.getStackTrace().toString());
            }
        }
    }
}
