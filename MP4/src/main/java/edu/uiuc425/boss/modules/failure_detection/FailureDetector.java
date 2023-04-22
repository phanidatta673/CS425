package edu.uiuc425.boss.modules.failure_detection;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Global;
import edu.uiuc425.boss.Member;
import edu.uiuc425.boss.Memberlist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FailureDetector implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Integer, Future<?>> pingers = new HashMap<>();
    private final Memberlist memberlist = Memberlist.getInstance();

    @Override
    public void run() {
        executor.submit(new Ponger(Constants.ReceivePingPort, (ping) -> {
            memberlist.onPing(ping);
            return null;
        }));
        while (!Thread.currentThread().isInterrupted()) {
            final Set<Integer> children = getChildren(Constants.MaxFailures + 1);
            Set<Integer> originalPingersSet = new HashSet<>(pingers.keySet());
            // Remove pingers who aren't my children
            for (Integer id : originalPingersSet) {
                if (!children.contains(id)) {
                    pingers.get(id).cancel(true);
                    log.warn("Removing pinger {}...", id);
                    pingers.remove(id);
                }
            }
            // Start pinging children who weren't in my pinger list
            for (Integer child : children) {
                if (!pingers.containsKey(child)) {
                    log.warn("Adding pinger {}...", child);
                    pingers.put(child, executor.submit(new Pinger(
                            child,
                            (pong) -> {
                                memberlist.onPong(child, pong);
                                return null;
                            }, () -> {
                        log.error("Machine {} seems to be dead.", child);
                        memberlist.markDead(child);
                    })));
                }
            }
        }
        log.error("Failure detector is down!");
    }

    private Set<Integer> getChildren(int n) {
        List<Member> list = memberlist.getListView();
        Set<Integer> children = new HashSet<>();
        for (int i = Global.getSelfRingID() + 1; children.size() < n; i++) {
            if (i > 10)
                i -= 10;
            if (i == Global.getSelfRingID())
                break;
            if (list.get(i).getIsAlive())
                children.add(i);
        }
        return children;
    }
}
