package mp3.app;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import com.google.gson.annotations.Expose;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mp3.app.messages.PingMsg;
import mp3.app.messages.PongMsg;

public class Memberlist {
    private static final Logger log = LogManager.getLogger();

    @Expose
    private AtomicIntegerArray isAlive = null;

    @Expose
    private AtomicIntegerArray maxPong = null;

    @Expose
    private AtomicLongArray age = null;

    private Memberlist() {
    }

    public Memberlist(int id, long newAge) {
        this();
        isAlive = new AtomicIntegerArray(Constants.MaxMachines + 1);
        maxPong = new AtomicIntegerArray(Constants.MaxMachines + 1);
        age = new AtomicLongArray(Constants.MaxMachines + 1);
        isAlive.set(id, 1);
        maxPong.set(id, 1);
        age.set(id, newAge);
    }

    public synchronized void initialize(Memberlist other) {
        synchronized (other) {
            for (int i = 1; i <= Constants.MaxMachines; i++) {
                if (i == Global.getSelfVmID())
                    continue;
                isAlive.set(i, other.isAlive.get(i));
                maxPong.set(i, other.maxPong.get(i));
                age.set(i, other.age.get(i));
            }
        }
    }

    public synchronized void onPong(PongMsg pong) {
        int id = pong.id;

        if (pong.age > age.get(id)) {
            isAlive.set(id, 1);
            maxPong.set(id, pong.counter);
            age.set(id, pong.age);
            // return true;
        } else if (pong.age == age.get(id)) {
            int beforeUpdated = maxPong.getAndUpdate(id, val -> Math.max(val, pong.counter));
            if (pong.counter > beforeUpdated) {
                isAlive.set(id, 1);
                // return true;
            }
        }
        // return false;
    }

    public synchronized void onPing(PingMsg ping) {
        update(ping.memberlist);
    }

    public synchronized void update(Memberlist other) {
        synchronized (other) {
            log.trace("Setting memberlist:");
            log.trace("     Origial: {}", toString());
            log.trace("     Input:   {}", other.toString());
            int selfID = Global.getSelfVmID();

            for (int i = 1; i <= Constants.MaxMachines; i++) {
                if (i != selfID) {
                    long myAge = age.get(i);
                    int myMaxPong = maxPong.get(i);
                    int otherAlive = other.isAlive.get(i);
                    long otherAge = other.age.get(i);
                    int otherMaxPong = other.maxPong.get(i);

                    if (otherAge > myAge) {
                        isAlive.set(i, otherAlive);
                        maxPong.set(i, otherMaxPong);
                        age.set(i, otherAge);

                    } else if (otherAge == myAge) {
                        isAlive.updateAndGet(i, myAlive -> {
                            if (myAlive == 1 && otherAlive == 0 && otherMaxPong >= myMaxPong)
                                return 0;
                            else if (myAlive == 0 && otherAlive == 1 && otherMaxPong > myMaxPong)
                                return 1;
                            return myAlive;
                        });
                        maxPong.updateAndGet(i, val -> Math.max(val, otherMaxPong));
                    }
                }
            }
        }
        log.trace("     After: {}", toString());
    }

    public synchronized int getAndIncrementCounter(int i) {
        int before = maxPong.getAndAdd(i, 1);
        return before;
    }

    public synchronized void markDead(int i) {
        log.info("Marking {} as dead!", i);
        isAlive.set(i, 0);
    }

    public synchronized void refresh(int i, long newAge) {
        isAlive.set(i, 1);
        maxPong.set(i, 0);
        age.set(i, newAge);
    }

    @Override
    public synchronized String toString() {
        String result = "\n";
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            result += String.format("(%s,%d), %d\n", Integer.toString(isAlive.get(i)), maxPong.get(i), age.get(i));
        }
        return result;
    }

    public synchronized boolean[] getReadonlyAliveList() {
        boolean[] aliveBool = new boolean[isAlive.length()];
        for (int i = 0; i < isAlive.length(); i++) {
            aliveBool[i] = isAlive.get(i) == 1;
        }
        return aliveBool;
    }

    public synchronized Set<Integer> getChildren(int nChildren) {
        int selfID = Global.getSelfVmID();
        Set<Integer> ans = new HashSet<>();
        for (int ptr = selfID + 1; ans.size() < nChildren; ptr++) {
            if (ptr > 10)
                ptr -= 10;

            if (ptr == selfID)
                break;

            if (isAlive.get(ptr) == 1) {
                ans.add(ptr);
            }
        }
        return ans;
    }

    public synchronized Set<Integer> getAlive() {
        Set<Integer> result = new HashSet<>();
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            if (isAlive.get(i) == 1)
                result.add(i);
        }
        return result;
    }
}
