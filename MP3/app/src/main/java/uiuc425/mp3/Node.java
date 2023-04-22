package uiuc425.mp3;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Node {
    private static final Logger log = LogManager.getLogger();
    private boolean runAsIntroducer = false;
    private AtomicIntegerArray memberlist = new AtomicIntegerArray(11);
    private AtomicLongArray lastSeenByMe = new AtomicLongArray(11);
    private static InetSocketAddress introducerAddress = null;
    private Scanner scanner = new Scanner(System.in);
    private List<Thread> threads = new LinkedList<>();

    public Node(boolean runAsIntroducer) {
        this.runAsIntroducer = runAsIntroducer;
    }

    public Node(InetSocketAddress introducerAddress) {
        Node.introducerAddress = introducerAddress;
    }

    public void start() {
        int selfID = Common.getSelfVmID();
        lastSeenByMe.set(selfID, System.currentTimeMillis());

        Thread receiverThread = new Thread(new Receiver(memberlist, lastSeenByMe));
        receiverThread.start();
        threads.add(receiverThread);

        if (runAsIntroducer) {
            memberlist.set(selfID, 1);
            Thread introducerThread = new Thread(new Introducer(memberlist, lastSeenByMe));
            introducerThread.start();
            threads.add(introducerThread);
        }
        Thread pingerThread = new Thread(new Pinger(memberlist, lastSeenByMe));
        pingerThread.start();
        threads.add(pingerThread);
        mainLoop();
    }

    private void joinParty() {
        if (runAsIntroducer) {
            log.warn("I'm already an introducer! No need to join!");
            return;
        }
        try {
            // Since the introducer will introduce this node once the connection
            // has established, there's nothing else to do after that.
            Socket socket = new Socket(
                    introducerAddress.getAddress(),
                    introducerAddress.getPort());
            socket.close();
        } catch (Exception e) {
            log.fatal("Cannot join the party. Is the introducer running? {}", e.getMessage());
        }
    }

    private void mainLoop() {
        while (true) {
            log.info("Please enter the command: (choices are: list_mem, list_self, join, leave):");
            String command = scanner.nextLine();
            if (command.equals("list_mem")) {
                log.info("memberlist: {}", memberlist);
                for (int i = 1; i < memberlist.length(); i++) {
                    if (memberlist.get(i) == 1)
                        log.info("  Member {},", i);
                }
            } else if (command.equals("list_self")) {
                log.info("I am {}", Common.getSelfVmID());
            } else if (command.equals("join")) {
                joinParty();
            } else if (command.equals("leave")) {
                for (Thread t : threads) {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        log.error("Error joining thread {}: {}", t.getName(), e.getMessage());
                    }
                }
                log.info("All threads joined");
                break;
            } else {
                log.error("Invalid command: choose one of list_mem, list_self, join, leave");
            }
        }

        log.info("Server finished");
    }
}
