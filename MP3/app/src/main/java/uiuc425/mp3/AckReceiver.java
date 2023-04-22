package uiuc425.mp3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AckReceiver implements Runnable {
    private final static Logger log = LogManager.getLogger();
    private AtomicIntegerArray memberlist = null;;
    private AtomicLongArray lastSeenByMe = null;
    private Set<Integer> suspiciousSet = null;
    private DatagramSocket socket = null;
    private byte[] recvBuffer = new byte[Constants.UdpMessageMaxSize];

    public AckReceiver(AtomicIntegerArray memberlist, AtomicLongArray lastSeenByMe, Set<Integer> suspiciousSet) {
        this.memberlist = memberlist;
        this.lastSeenByMe = lastSeenByMe;
        this.suspiciousSet = new HashSet<>(suspiciousSet);
        try {
            socket = new DatagramSocket(Constants.ACKPort);
            socket.setSoTimeout(Constants.ReceiveAckTimeout);
        } catch (IOException e) {
            log.error("Error creating udp socket: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        log.trace("Receiving ack at port: {}", socket.getLocalPort());
        DatagramPacket receivePacket = new DatagramPacket(recvBuffer, recvBuffer.length);
        try {
            while (suspiciousSet.size() != 0) {
                socket.receive(receivePacket);
                int receivedAckID = Common.getVmIDFromHostName(receivePacket.getAddress().getHostName());
                log.trace("I see you {}!", receivedAckID);
                suspiciousSet.remove(receivedAckID);
                lastSeenByMe.set(receivedAckID, System.currentTimeMillis());
            }
        } catch (SocketTimeoutException e) {
            log.warn("Socket timeout: {}", e.getMessage());
            log.warn("Remaining suspects: {}", Arrays.toString(suspiciousSet.toArray()));
            for (int suspect : suspiciousSet) {
                log.trace("I didn't see you {}!", suspect);
                memberlist.set(suspect, 0);
                lastSeenByMe.set(suspect, System.currentTimeMillis());
            }
        } catch (IOException e) {
            log.error("Error receiving ACK: {}", e.getMessage());
        }
        socket.close();
    }
}
