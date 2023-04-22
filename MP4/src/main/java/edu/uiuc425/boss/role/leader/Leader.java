package edu.uiuc425.boss.role.leader;

import edu.uiuc425.boss.role.leader.sdfs.Coordinator;
import edu.uiuc425.boss.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Leader implements Runnable {
    private static final Logger log = LogManager.getLogger();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Leader() {
    }

    @Override
    public void run() {
        executor.submit(new Introducer(Constants.IntroducerPort));
        executor.submit(new Coordinator(Constants.CoordinatorPort));
        log.info("Leader is started!");
    }

}
