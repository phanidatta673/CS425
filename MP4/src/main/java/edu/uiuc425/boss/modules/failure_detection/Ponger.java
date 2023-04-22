package edu.uiuc425.boss.modules.failure_detection;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.Member;
import edu.uiuc425.boss.Memberlist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.function.Function;

public class Ponger implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private final Function<Ping, Void> onPing;
    private final DatagramPacket sendPkt;
    private final DatagramPacket recvPkt;
    private DatagramSocket udp = null;

    public Ponger(int port, Function<Ping, Void> onPing) {
        this.onPing = onPing;
        byte[] sendBuf = new byte[Constants.MaxPongSize];
        byte[] recvBuf = new byte[Constants.MaxPingSize];
        sendPkt = new DatagramPacket(sendBuf, sendBuf.length);
        recvPkt = new DatagramPacket(recvBuf, recvBuf.length);

        try {
            udp = new DatagramSocket(port);
            udp.setSoTimeout(Constants.PongTimeout);
        } catch (IOException e) {
            log.error("Error while creating ponger socket: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                waitForPing();
                ByteString bs = ByteString.copyFrom(recvPkt.getData(), 0, recvPkt.getLength());
                Ping ping = Ping.parseFrom(bs);
                if (ping != null) {
                    onPing.apply(ping);
                    InetSocketAddress remote = new InetSocketAddress(
                            recvPkt.getAddress(),
                            recvPkt.getPort()
                    );
                    pongTo(remote);
                }
            } catch (InvalidProtocolBufferException e) {
                log.error("Error parsing ping: {}", e.getMessage());
            } catch (SocketTimeoutException e) {
                log.error("Ponger didn't receive any pings for {} seconds. " +
                        "I think I'm dead:(", Constants.PongTimeout / 1000);
                break;
            }
        }
    }

    private void waitForPing() throws SocketTimeoutException {
        try {
            udp.receive(recvPkt);
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error while waiting for ping: {}", e.getMessage());
        }
    }

    private void pongTo(InetSocketAddress remote) {
        try {
            Member member = Memberlist.getInstance().get(Global.getSelfRingID());
            Pong pong = Pong.newBuilder()
                    .setMember(member)
                    .build();
            sendPkt.setData(pong.toByteArray());
            sendPkt.setSocketAddress(remote);
            udp.send(sendPkt);
        } catch (IOException e) {
            log.error("Error while ponging back to {}",
                    Global.getRingIDFromHostName(remote.getHostName()));
        }
    }
}
