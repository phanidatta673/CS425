package edu.uiuc425.boss.role;

import edu.uiuc425.boss.*;
import edu.uiuc425.boss.modules.failure_detection.FailureDetector;
import edu.uiuc425.boss.modules.failure_detection.Ping;
import edu.uiuc425.boss.role.normalnode.CommandListener;
import edu.uiuc425.boss.role.normalnode.SdfsStorage;
import edu.uiuc425.boss.role.normalnode.doers.Deleter;
import edu.uiuc425.boss.role.normalnode.doers.Getter;
import edu.uiuc425.boss.role.normalnode.doers.Lister;
import edu.uiuc425.boss.role.normalnode.doers.Putter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {
    private static final Logger log = LogManager.getLogger();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Scanner scanner = new Scanner(System.in);
    private static final String[] availableCommands = new String[]{
            "l", "list_mem",
            "j", "join",
            "p", "put",
            "g", "get",
            "gv", "get-versions",
            "d", "delete",
            "s", "store",
            "list_self",
            "leave",
            "ls",
    };
    private final SdfsStorage storage = new SdfsStorage();
    private boolean isJoined = false;

    public Node() {
    }


    public void mainLoop() {
        while (true) {
            try {
                log.info("Please enter the command: (choices are: {})", Arrays.asList(availableCommands));
                String command = scanner.next();
                if (!Arrays.asList(availableCommands).contains(command)) {
                    log.error("Invalid command!");
                    continue;
                }
                switch (command) {
                    case "join":
                    case "j":
                        if (isJoined)
                            log.error("I've already joined the party!");
                        else
                            joinParty();
                        break;

                    case "list_mem":
                    case "l":
                        log.info("Membership list: {}", Memberlist.getInstance());
                        break;

                    case "put":
                    case "p": {
                        String localName = scanner.next();
                        String sdfsName = scanner.next();
                        executor.submit(new Putter(localName, sdfsName));
                        break;
                    }

                    case "get":
                    case "g": {
                        String sdfsName = scanner.next();
                        String localName = scanner.next();
                        executor.submit(new Getter(sdfsName, localName));
                        break;
                    }

                    case "get-versions":
                    case "gv": {
                        String sdfsName = scanner.next();
                        int numVersions = scanner.nextInt();
                        String localName = scanner.next();
                        executor.submit(new Getter(sdfsName, numVersions, localName));
                        break;
                    }

                    case "delete":
                    case "d": {
                        String sdfsName = scanner.next();
                        executor.submit(new Deleter(sdfsName));
                        break;
                    }

                    case "store":
                    case "s":
                        log.info("Storage: {}", storage);
                        break;

                    case "ls": {
                        String sdfsName = scanner.next();
                        executor.submit(new Lister(sdfsName));
                        break;

                    }
                }
            } catch (Exception e) {
                log.error("Error while doing that command. Below is the stacktrace...");
                e.printStackTrace();
            }
        }
    }

    private void joinParty() {
        try (Socket socket = new Socket(Constants.LeaderHostName, Constants.IntroducerPort)) {
            JoinForm form = JoinForm.newBuilder()
                    .setAge(Global.getAge())
                    .build();
            form.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();
            Ping respond = Ping.parseDelimitedFrom(socket.getInputStream());
            List<Member> list = respond.getMemberList();
            if (list.size() > 0) {
                isJoined = true;
                log.info("I'm joined!");
                Memberlist.getInstance().initialList(list);
                executor.submit(new FailureDetector());
                executor.submit(new CommandListener(Constants.CommandListenerPort, storage));
            } else {
                log.error("I cannot join the party :(");
            }
        } catch (IOException e) {
            log.error("IO error while joining party: {}", e.getMessage());
        }
    }
}
