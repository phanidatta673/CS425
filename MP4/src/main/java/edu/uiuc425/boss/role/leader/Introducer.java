package edu.uiuc425.boss.role.leader;

import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.JoinForm;
import edu.uiuc425.boss.Member;
import edu.uiuc425.boss.Memberlist;
import edu.uiuc425.boss.modules.failure_detection.Ping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Introducer implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private static final Memberlist memberlist = Memberlist.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ServerSocket server = null;

    public Introducer(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            log.error("Error while starting introducer: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        log.warn("Introducer starts running...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket client = server.accept();
                log.info("Accepting {}:{}", client.getInetAddress().getHostName(), client.getPort());
                executor.submit(new Handler(client));
            } catch (IOException e) {
                log.error("Error while accepting client: {}", e.getMessage());
            }
        }
    }

    private record Handler(Socket client) implements Runnable {
        @Override
        public void run() {
            try {
                int target = Global.getRingIDFromHostName(client.getInetAddress().getHostName());
                JoinForm form = JoinForm.parseDelimitedFrom(client.getInputStream());
                memberlist.set(target, Member.newBuilder()
                        .setIsAlive(true)
                        .setAge(form.getAge())
                        .setLastAppeared(form.getAge())
                        .build());
                Ping respond = Ping.newBuilder()
                        .addAllMember(memberlist.getListView())
                        .build();
                respond.writeDelimitedTo(client.getOutputStream());
                client.getOutputStream().flush();
            } catch (IOException e) {
                log.error("Error while dealing with joiner: {}", e.getMessage());
            }
        }
    }
}
