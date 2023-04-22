package mp3.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.messages.PingMsg;
import mp3.app.modules.faliureDetection.FailureDetector;
import mp3.app.modules.sdfs.AskListener;
import mp3.app.modules.sdfs.Deleter;
import mp3.app.modules.sdfs.Getter;
import mp3.app.modules.sdfs.Lister;
import mp3.app.modules.sdfs.Putter;
import mp3.app.modules.sdfs.RemoveListener;
import mp3.app.modules.sdfs.StoreListener;

public class Node {
    private static final Logger log = LogManager.getLogger();

    private Scanner scanner = new Scanner(System.in);
    private boolean runAsLeader = false;
    private InetSocketAddress leaderAddr = null;

    private Memberlist memberlist = new Memberlist(Global.getSelfVmID(), Global.getAge());
    private ConcurrentMap<String, Path> sdfsMap = new ConcurrentHashMap<>();

    private final Set<String> failureDetectionCommands = new HashSet<>(Arrays.asList(
            "l", "list_mem", "list_self", "j", "join", "leave"));

    private final Set<String> sdfsCommands = new HashSet<>(Arrays.asList(
            "put", "get", "delete", "store", "ls", "get-versions"));

    private boolean keepgoing = true;

    private Node() {
    }

    public Node(boolean runAsLeader) {
        this();
        this.runAsLeader = runAsLeader;
    }

    public Node(InetSocketAddress leaderAddr) {
        this();
        this.leaderAddr = leaderAddr;
    }

    public void start() {
        MyPoolExecutor.submitTask(new FailureDetector(memberlist));
        if (runAsLeader) {
            MyPoolExecutor.submitTask(new Leader(memberlist));
        }

        MyPoolExecutor.submitTask(new StoreListener(sdfsMap));
        MyPoolExecutor.submitTask(new AskListener(sdfsMap));
        MyPoolExecutor.submitTask(new RemoveListener(sdfsMap));
        mainLoop();
    }

    private void mainLoop() {
        while (keepgoing) {
            log.info("Please enter the command: (choices are: {}, {}):",
                    failureDetectionCommands, sdfsCommands);
            String command = scanner.next();
            if (failureDetectionCommands.contains(command))
                handleFailureDetectionCommands(command);
            else if (sdfsCommands.contains(command)) {
                handleSDFSCommands(command);
            } else {
                log.error("Invalid command: choose one of {}, {}", failureDetectionCommands, sdfsCommands);
            }
        }
        log.info("Server finished");
    }

    private void joinParty() {
        if (runAsLeader) {
            log.warn("I'm already an leader! No need to join!");
            return;
        }
        try {
            Socket socket = new Socket(
                    leaderAddr.getAddress(),
                    leaderAddr.getPort());

            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            os.writeLong(Global.getAge());

            DataInputStream in = new DataInputStream(socket.getInputStream());
            int length = in.readInt();
            byte[] bytes = in.readNBytes(length);
            DatagramPacket tmpPacket = new DatagramPacket(bytes, bytes.length);
            PingMsg msg = PingMsg.fromPacket(tmpPacket);

            memberlist.initialize(msg.memberlist);

            socket.close();
        } catch (Exception e) {
            log.fatal("Cannot join the party. Is the leader running? {}", e.getMessage());
        }
    }

    private void handleFailureDetectionCommands(String command) {
        if (command.equals("l") || command.equals("list_mem")) {
            boolean[] isAlive = memberlist.getReadonlyAliveList();
            log.info("memberlist: {}", memberlist);
            for (int i = 1; i <= Constants.MaxMachines; i++) {
                if (isAlive[i])
                    log.info("  Member {},", i);
            }
        } else if (command.equals("list_self")) {
            log.info("I am {}", Global.getSelfVmID());
        } else if (command.equals("j") || command.equals("join")) {
            joinParty();
        } else if (command.equals("leave")) {
            MyPoolExecutor.shutdownNow();
            log.info("All threads joined");
            keepgoing = false;
        }
    }

    private void handleSDFSCommands(String command) {
        if (command.equals("put")) {
            String sdfsFileName = scanner.next();
            String localFileName = scanner.next();
            MyPoolExecutor.submitTask(new Putter(sdfsFileName, localFileName));

        } else if (command.equals("get")) {
            String sdfsFileName = scanner.next();
            String localFileName = scanner.next();
            MyPoolExecutor.submitTask(new Getter(sdfsFileName, localFileName));

        } else if (command.equals("delete")) {
            String sdfsFileName = scanner.next();
            MyPoolExecutor.submitTask(new Deleter(sdfsFileName));

        } else if (command.equals("ls")) {
            String sdfsFileName = scanner.next();
            MyPoolExecutor.submitTask(new Lister(sdfsFileName));

        } else if (command.equals("store")) {
            Set<String> set = new HashSet<>(sdfsMap.keySet());
            log.info("I've got these files: {}", set);

        } else if (command.equals("get-versions")) {
            String sdfsFileName = scanner.next();
            int lastN = scanner.nextInt();
            String localFileName = scanner.next();
            MyPoolExecutor.submitTask(new Getter(sdfsFileName, localFileName, lastN));
        }
    }
}
