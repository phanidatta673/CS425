package mp3.app.modules.sdfs.leader;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.Memberlist;
import mp3.app.MyPoolExecutor;

public class Coordinator implements Runnable {
    private Logger log = LogManager.getLogger();
    private Memberlist memberlist = null;
    private ConcurrentMap<String, Set<Integer>> lookup = null;

    public Coordinator(Memberlist memberlist, ConcurrentMap<String, Set<Integer>> lookup) {
        this.memberlist = memberlist;
        this.lookup = lookup;
    }

    @Override
    public void run() {
        MyPoolExecutor.submitTask(new PutRequestListener(memberlist, lookup));
        MyPoolExecutor.submitTask(new GetRequestListener(memberlist, lookup));
        MyPoolExecutor.submitTask(new DeleteRequestListener(memberlist, lookup));
        MyPoolExecutor.submitTask(new ListRequestListener(memberlist, lookup));
        log.info("Coordinator successfully ran!");
    }
}
