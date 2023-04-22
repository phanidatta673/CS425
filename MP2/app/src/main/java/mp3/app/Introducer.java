package mp3.app;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.messages.PingMsg;

public class Introducer implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private ServerSocket server = null;
    private Memberlist memberlist = null;
    private ConcurrentMap<String, Set<Integer>> lookup = null;

    Introducer(Memberlist memberlist, ConcurrentMap<String, Set<Integer>> lookup) {
        this.memberlist = memberlist;
        this.lookup = lookup;
        try {
            server = new ServerSocket(Constants.JoinPort);
        } catch (IOException e) {
            log.fatal("Server cannot be constructed: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        log.info("Introducer starts listening at port {}", server.getLocalPort());
        while (true) {
            try {
                Socket client = server.accept();
                new Thread(new JoinHandler(client)).start();
            } catch (IOException e) {
                log.error("Error while accepting clients: {}", e.getStackTrace().toString());
            }
        }
    }

    private class JoinHandler implements Runnable {
        private Socket client = null;

        public JoinHandler(Socket client) {
            this.client = client;
            try {
                client.setSoTimeout(1000);
            } catch (IOException e) {
            }
        }

        @Override
        public void run() {
            long age = readAgeFromClient();
            updateMemberList(age);
            updateLookup();
            respond();
        }

        private long readAgeFromClient() {
            try {
                DataInputStream in = new DataInputStream(client.getInputStream());
                return in.readLong();
            } catch (IOException e) {
                log.error("Error reading age of client: {}", e);
            }
            return -1;
        }

        private void updateMemberList(long age) {
            String hostName = client.getInetAddress().getHostName();
            log.info("{} is joining with age {}!", hostName, age);
            int vmID = Global.getVmIDFromHostName(hostName);
            memberlist.refresh(vmID, age);
        }

        private void updateLookup() {
            String hostName = client.getInetAddress().getHostName();
            int vmID = Global.getVmIDFromHostName(hostName);

            Set<String> keys = new HashSet<>(lookup.keySet());
            for (String key : keys) {
                lookup.compute(key, (k, set) -> {
                    if (set == null)
                        return null;
                    if (set.contains(vmID))
                        set.remove(vmID);
                    if (set.size() == 0)
                        return null;
                    return set;
                });
            }
        }

        private void respond() {
            PingMsg msg = new PingMsg(memberlist);
            try {
                DataOutputStream os = new DataOutputStream(client.getOutputStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] bytes = msg.toBytes();
                baos.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
                baos.write(bytes);
                os.write(baos.toByteArray());
            } catch (IOException e) {
                log.error("Error writing to client: {}", e);
            }
        }
    }
}
