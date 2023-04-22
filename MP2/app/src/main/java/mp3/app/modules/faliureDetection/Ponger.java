package mp3.app.modules.faliureDetection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Global;
import mp3.app.Constants;
import mp3.app.Memberlist;
import mp3.app.messages.PingMsg;
import mp3.app.messages.PongMsg;

public class Ponger implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private DatagramSocket udp = null;
    private Function<PingMsg, Boolean> onPing = null;
    private Memberlist memberlist = null;

    public Ponger(Memberlist memberlist, Function<PingMsg, Boolean> onPing) {
        try {
            udp = new DatagramSocket(Constants.ReceivePingPort);
            udp.setSoTimeout(Constants.PongTimeout);
        } catch (IOException e) {
            log.error("Cannot listen to port {}!", Constants.ReceivePingPort);
        }

        this.memberlist = memberlist;
        this.onPing = onPing;
    }

    @Override
    public void run() {
        log.info("Ponger starts listening at port {}", udp.getLocalPort());
        byte[] recvBuffer = new byte[Constants.MaxPingSize];
        while (true) {
            try {
                DatagramPacket inPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                udp.receive(inPacket);

                int self = Global.getSelfVmID();
                int other = Global.getVmIDFromHostName(inPacket.getAddress().getHostName());

                PingMsg ping = PingMsg.fromPacket(inPacket);
                log.trace("Got ping from {}: {}", other, ping);
                onPing.apply(ping);

                PongMsg pong = new PongMsg(Global.getSelfVmID(), memberlist.getAndIncrementCounter(self),
                        Global.getAge());
                log.trace("Ponging {}: {}", Global.getVmIDFromHostName(inPacket.getAddress().getHostName()), pong);

                byte[] pongBytes = pong.toBytes();
                DatagramPacket outPacket = new DatagramPacket(
                        pongBytes,
                        pongBytes.length,
                        inPacket.getAddress(),
                        inPacket.getPort());

                udp.send(outPacket);
            } catch (IOException e) {
                log.error("Something wrong when receiving: {}", e.getMessage());
            }
        }
    }
}
