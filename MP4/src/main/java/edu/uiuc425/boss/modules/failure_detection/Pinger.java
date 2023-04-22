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
import java.net.*;
import java.util.List;
import java.util.function.Function;

public class Pinger implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private final int target;
    private final DatagramPacket sendPkt;
    private final DatagramPacket recvPkt;
    private final Function<Pong, Void> onPong;
    private final Runnable onDead;
    private final DatagramSocket udp;

    public Pinger(int target,
                  Function<Pong, Void> onPong,
                  Runnable onDead) {
        this.target = target;
        this.onPong = onPong;
        this.onDead = onDead;

        SocketAddress remote = new InetSocketAddress(
                Global.getHostNameByRingID(target),
                Constants.ReceivePingPort);
        byte[] sendBuf = new byte[Constants.MaxPingSize];
        byte[] recvBuf = new byte[Constants.MaxPongSize];
        sendPkt = new DatagramPacket(sendBuf, sendBuf.length, remote);
        recvPkt = new DatagramPacket(recvBuf, recvBuf.length);

        DatagramSocket tmpUdp = null;
        try {
            tmpUdp = new DatagramSocket();
            tmpUdp.setSoTimeout(Constants.MaxPongWaitTime);
        } catch (IOException e) {
            log.error("Error constructing udp socket to {}: {}", target, e);
        }
        udp = tmpUdp;
    }

    @Override
    public void run() {
        int timeoutCounter = 0;

        while (!Thread.currentThread().isInterrupted() && timeoutCounter < Constants.MaxPongTimeouts) {
            Pong pong = pingTo(constructPing());
            if (pong != null) {
                timeoutCounter = 0;
                onPong.apply(pong);
            } else {
                timeoutCounter++;
                log.warn("Machine {} hasn't responded for {} pings", target, timeoutCounter);
            }

            try {
                Thread.sleep(Constants.PingInterval);
            } catch (InterruptedException e) {
                log.error("Thread sleep interrupted: {}", e.toString());
                break;
            }
        }
        if (timeoutCounter >= Constants.MaxPongTimeouts) {
            onDead.run();
        }
        log.warn("Shutting down pinger to {}", target);
    }

    private Pong pingTo(Ping pingMsg) {
        sendPkt.setData(pingMsg.toByteArray());
        Pong pong = null;
        try {
            udp.send(sendPkt);
            udp.receive(recvPkt);
            ByteString bs = ByteString.copyFrom(recvPkt.getData(), 0, recvPkt.getLength());
            pong = Pong.parseFrom(bs);
        } catch (SocketTimeoutException e) {
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing received packet from {}: {}", target, e);
        } catch (IOException e) {
            log.error("IOException when pinging {}: {}", target, e);
        }
        return pong;
    }

    private Ping constructPing() {
        List<Member> members = Memberlist.getInstance().getListView();
        var builder = Ping.newBuilder().addAllMember(members);
        return builder.build();
    }
}
