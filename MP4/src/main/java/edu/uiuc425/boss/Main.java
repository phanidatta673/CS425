package edu.uiuc425.boss;

import edu.uiuc425.boss.role.Node;
import edu.uiuc425.boss.role.leader.Leader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger log = LogManager.getLogger();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        log.info("Boss started with args {}", Arrays.asList(args));
        if (args.length > 1) {
            log.fatal("Usage: './gradlew run' to run as node and");
            log.fatal("       './gradlew run --args leader' to run as leader");
            return;
        }
        if (args.length == 1) {
            if (args[0].equals("leader")) {
                executor.submit(new Leader());
            } else {
                log.fatal("Usage: './gradlew run' to run as node and");
                log.fatal("       './gradlew run --args leader' to run as leader");
                return;
            }
        }
        new Node().mainLoop();
    }
}
