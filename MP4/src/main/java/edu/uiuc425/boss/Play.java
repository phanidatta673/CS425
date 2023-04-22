package edu.uiuc425.boss;

import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Play {

    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) {
        (new Player()).run();
    }

    public static class Player implements Runnable {

        private final SdfsTable table = new SdfsTable();

        @Override
        public void run() {
            table.put("file10", 1, 1);
            table.put("file10", 1, 1);
            table.put("file10", 2, 4);
            table.put("file10", 2, 3);
            table.put("file20", 1, 2);
            log.info("latest of file10: {}", table.latestVersionOf("file10"));
            table.put("file30", 1, 3);
            log.info(table);
        }
    }
}
