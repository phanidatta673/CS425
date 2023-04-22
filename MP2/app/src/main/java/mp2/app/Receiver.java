package mp2.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp2.app.Messages.AckMessage;
import mp2.app.Messages.PingMessage;

public class Receiver implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private AtomicIntegerArray memberlist = null;
    private DatagramSocket udpSocket = null;
    private AtomicLongArray lastSeenByMe = null;

    public Receiver(AtomicIntegerArray memberlist, AtomicLongArray lastSeenByMe) {
        this.memberlist = memberlist;
        this.lastSeenByMe = lastSeenByMe;
        try {
            udpSocket = new DatagramSocket(Constants.ReceivePort);
        } catch (IOException e) {
            log.error("Cannot construct udp socket: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        log.info("Receiver starts running at port {}", udpSocket.getLocalPort());
        while (true) {
            try {
                byte[] recvBuffer = new byte[Constants.UdpMessageMaxSize];
                DatagramPacket receivePacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                udpSocket.receive(receivePacket);

                log.trace("Got something! Length is: {}", receivePacket.getLength());
                PingMessage pingMessage = Common.parsePingMessageFromPacket(receivePacket);
                log.trace("Setting memberlist:");
                log.trace("Original: {}", Common.atomArrayToString(memberlist));
                log.trace("Input: {}", Common.atomArrayToString(pingMessage.memberlist));
                synchronized (lastSeenByMe) {
                    synchronized (pingMessage.lastSeens) {
                        for (int i = 0; i < memberlist.length(); i++) {
                            if (lastSeenByMe.get(i) > pingMessage.lastSeens.get(i)) {
                                memberlist.set(i, memberlist.get(i));
                            } else {
                                memberlist.set(i, pingMessage.memberlist.get(i));
                            }
                        }
                    }
                }
                log.trace("After: {}", Common.atomArrayToString(memberlist));
                new AckMessage().sendTo(receivePacket.getAddress(), Constants.ACKPort);
                log.trace("After receiver iteration, memberlist: {}", Common.atomArrayToString(memberlist));
            } catch (IOException e) {
                log.error("Something wrong when receiving: {}", e.getMessage());
            }
        }
    }
}
