package mp2.app;

import java.net.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp2.app.Messages.MessageType;
import mp2.app.Messages.PingMessage;

public class Pinger implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private final int currentHostNumber = Common.getSelfVmID();
    private AtomicIntegerArray memberList = null;
    private AtomicLongArray lastSeenByMe = null;

    public Pinger(AtomicIntegerArray memberList, AtomicLongArray lastSeenByMe) {
        this.memberList = memberList;
        this.lastSeenByMe = lastSeenByMe;
    }

    @Override
    public void run() {
        while (true) {
            long timestamp = new Date().getTime();
            PingMessage ping = new PingMessage(MessageType.PING, timestamp, memberList, lastSeenByMe);
            sendPingMessage(ping);
            try {
                Thread.sleep(Constants.PingInterval);
            } catch (InterruptedException e) {
                log.error("Thread sleep interupted: {}", e.getMessage());
            }
        }
    }

    public void sendPingMessage(PingMessage ping) {
        Set<Integer> hostIDs = new HashSet<>();
        hostIDs = getNeighbors(hostIDs, 1, 2);
        hostIDs = getNeighbors(hostIDs, -1, 2);
        if (hostIDs.size() > 0)
            new Thread(new AckReceiver(memberList, lastSeenByMe, hostIDs)).start();
        for (int hostID : hostIDs) {
            String hostName = String.format("fa22-cs425-50%02d.cs.illinois.edu", hostID);
            InetSocketAddress host = new InetSocketAddress(hostName, Constants.ReceivePort);
            log.trace("Pinging {}", host);
            ping.sendTo(host);
        }
    }

    private Set<Integer> getNeighbors(Set<Integer> hostIDs, int step, int n) {
        int counter = 0;
        for (int ptr = currentHostNumber + step; ptr != currentHostNumber && counter < n; ptr += step) {
            if (ptr > 10)
                ptr -= 10;
            else if (ptr < 1)
                ptr += 10;

            if (ptr == currentHostNumber)
                break;

            if (memberList.get(ptr) == 1) {
                hostIDs.add(ptr);
                counter++;
            }
        }
        return hostIDs;
    }
}
