package mp3.app.modules.faliureDetection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Global;
import mp3.app.Constants;
import mp3.app.Memberlist;
import mp3.app.messages.PingMsg;
import mp3.app.messages.PongMsg;

// TODO: tolerate ping/pong lost
public class Pinger implements Runnable {
    private static final Logger log = LogManager.getLogger();

    private Memberlist memberlist = null;

    private int targetID = -1;
    private InetSocketAddress target = null;
    private DatagramSocket udp = null;

    private Function<PongMsg, Void> onPong = null;
    private Function<Integer, Void> onDead = null;

    public Pinger(int targetID,
            Memberlist memberlist,
            Function<PongMsg, Void> onPong,
            Function<Integer, Void> onDead) {
        target = new InetSocketAddress(Global.getHostNameByID(targetID), Constants.ReceivePingPort);
        this.targetID = targetID;
        this.memberlist = memberlist;
        this.onPong = onPong;
        this.onDead = onDead;
        try {
            udp = new DatagramSocket();
            udp.setSoTimeout(Constants.MaxPongWaitTime);
        } catch (IOException e) {
            log.error("Cannot create udp socket: {}", e);
        }
    }

    @Override
    public void run() {
        byte[] recvBuffer = new byte[Constants.MaxPongSize];

        log.info("Start pinging to {}", targetID);
        int timeouts = 0;
        while (timeouts < Constants.MaxPongTimeouts
                && !Thread.currentThread().isInterrupted()) {

            PingMsg ping = new PingMsg(memberlist);
            byte[] bytes = ping.toBytes();

            DatagramPacket sendPacket = new DatagramPacket(
                    bytes,
                    bytes.length,
                    target.getAddress(),
                    target.getPort());

            try {
                log.trace("Pinging {}", targetID);
                udp.send(sendPacket);

                DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                udp.receive(recvPacket);

                PongMsg pong = PongMsg.fromPacket(recvPacket);
                log.trace("Got pong from {}: {}", targetID, pong);
                timeouts = 0;
                onPong.apply(pong);
            } catch (SocketTimeoutException e) {
                log.error("Timeout # {} for host {}: {}", ++timeouts, targetID, e);
            } catch (IOException e) {
                log.error("Error IO: {}", e);
                break;
            }

            // Sleep only if receiving pong
            try {
                Thread.sleep(Constants.PingInterval);
            } catch (InterruptedException e) {
                log.error("Thread sleep interupted: {}", e.getMessage());
            }
        }
        if (timeouts >= Constants.MaxPongTimeouts)
            onDead.apply(targetID);

        log.info("Pinging to {} stopped\n", targetID);
    }
}
