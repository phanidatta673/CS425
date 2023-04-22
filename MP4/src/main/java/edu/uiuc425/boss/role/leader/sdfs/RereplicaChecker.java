package edu.uiuc425.boss.role.leader.sdfs;

import edu.uiuc425.boss.Constants;
import edu.uiuc425.boss.Member;
import edu.uiuc425.boss.Memberlist;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RereplicaChecker implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private final Memberlist memberlist = Memberlist.getInstance();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final SdfsTable table;
    private final Set<Integer> pendingRereplicates = new HashSet<>();

    public RereplicaChecker(SdfsTable table) {
        this.table = table;
    }

    @Override
    public void run() {
        log.info("RereplicaChecker starts!");
        while (!Thread.currentThread().isInterrupted()) {
            List<Member> view = memberlist.getListView();
            for (int i = 1; i <= Constants.MaxMachines; i++) {
                Queue<Pair<String, Integer>> files = new LinkedList<>(table.filesIn(i));
                if (!view.get(i).getIsAlive() && files.size() != 0 && !pendingRereplicates.contains(i)) {
                    log.warn("Rereplicating {} in machine {}", files, i);
                    executor.submit(new RereplicaManager(i, files, table));
                    pendingRereplicates.add(i);
                }
            }
        }
    }
}
