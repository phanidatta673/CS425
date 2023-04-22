package mp3.app.modules.faliureDetection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Constants;
import mp3.app.Memberlist;
import mp3.app.MyPoolExecutor;

public class FailureDetector implements Runnable {
    private static final Logger log = LogManager.getLogger();

    private Memberlist memberlist = null;
    private Map<Integer, Future<?>> pingers = new HashMap<>();

    public FailureDetector(Memberlist memberlist) {
        this.memberlist = memberlist;
    }

    @Override
    public void run() {
        new Thread(new Ponger(memberlist, (ping) -> {
            log.trace("Got ping: {}", ping);
            memberlist.onPing(ping);
            return null;
        })).start();

        while (true) {
            Set<Integer> children = memberlist.getChildren(Constants.MaxFailures + 1);

            log.trace("Current pingers: {}", pingers.keySet());
            log.trace("Adding new children {}=====================================================", children);
            for (int i = 1; i <= Constants.MaxMachines; i++) {
                if (children.contains(i) && !pingers.containsKey(i)) {
                    Pinger pinger = new Pinger(i, memberlist, (pong) -> {
                        memberlist.onPong(pong);
                        return null;
                    }, (timeoutID) -> {
                        if (pingers.containsKey(timeoutID)) // Should still be working!
                            memberlist.markDead(timeoutID);

                        return null;
                    });
                    pingers.put(i, MyPoolExecutor.submitTask(pinger));
                    log.info("Added pinger {}, pingers -> {}", i, pingers.keySet());
                } else if (!children.contains(i) && pingers.containsKey(i)) {
                    pingers.get(i).cancel(true);
                    pingers.remove(i);
                    log.info("Removed pinger {}, pingers -> {}", i, pingers.keySet());
                }
            }

            log.trace("Should children: {}", children);
            log.trace("Real children: {}", pingers.keySet());

            if (log.getLevel() == Level.TRACE) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    log.error("Thread sleep interupted: {}", e.getMessage());
                }
            }
        }
    }
}