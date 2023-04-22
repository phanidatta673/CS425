package mp3.app;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import mp3.app.modules.sdfs.leader.Coordinator;

public class Leader implements Runnable {

    private Thread introducer = null;
    private Thread coordinator = null;
    private ConcurrentMap<String, Set<Integer>> lookup = new ConcurrentHashMap<>();

    public Leader(Memberlist memberlist) {
        introducer = new Thread(new Introducer(memberlist, lookup));
        coordinator = new Thread(new Coordinator(memberlist, lookup));
    }

    @Override
    public void run() {
        introducer.start();
        coordinator.start();
    }
}
