package edu.uiuc425.boss;

import edu.uiuc425.boss.modules.failure_detection.Ping;
import edu.uiuc425.boss.modules.failure_detection.Pong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Memberlist {
    private static final Logger log = LogManager.getLogger();
    private static final Memberlist INSTANCE = new Memberlist();
    private final List<Member> members = new ArrayList<>();

    private Memberlist() {
        for (int i = 0; i <= Constants.MaxMachines; i++) {
            if (i == Global.getSelfRingID()) {
                members.add(Member.newBuilder()
                        .setIsAlive(true)
                        .setAge(Global.getAge())
                        .setLastAppeared(System.currentTimeMillis()).build());
            } else {
                members.add(Member.getDefaultInstance());
            }
        }
    }

    public static Memberlist getInstance() {
        return INSTANCE;
    }

    public synchronized void initialList(List<Member> list) {
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            if (i == Global.getSelfRingID()) {
                set(i, Member.newBuilder(members.get(i))
                        .setLastAppeared(System.currentTimeMillis())
                        .build());
            } else {
                set(i, list.get(i));
            }
        }
    }

    public synchronized void onPing(Ping ping) {
        List<Member> listView = new ArrayList<>(ping.getMemberList());
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            Member my = members.get(i);
            Member other = listView.get(i);

            if (i == Global.getSelfRingID()) {
                set(i, Member.newBuilder(my)
                        .setLastAppeared(System.currentTimeMillis())
                        .build());
            } else if (other.getAge() > my.getAge()) {
                set(i, other);
            } else if (other.getAge() == my.getAge()) {
                if (other.getLastAppeared() > my.getLastAppeared()) {
                    set(i, other);
                } else if (other.getLastAppeared() == my.getLastAppeared()) {
                    if (!other.getIsAlive() && my.getIsAlive())
                        set(i, other);
                }
            }
        }
    }

    public synchronized void onPong(int idx, Pong pong) {
        Member my = members.get(idx);
        Member other = pong.getMember();
        if (other.getAge() > my.getAge())
            set(idx, other);
        else if (other.getAge() == my.getAge() && other.getLastAppeared() > my.getLastAppeared())
            set(idx, other);
    }

    public synchronized void markDead(int idx) {
        set(idx, Member.newBuilder(members.get(idx))
                .setIsAlive(false)
                .build());
    }

    public synchronized void set(int idx, Member rhs) {
        members.set(idx, rhs);
    }

    public synchronized Member get(int idx) {
        return Member.newBuilder(members.get(idx)).build();
    }

    public synchronized List<Member> getListView() {
        return new ArrayList<>(members);
    }

    public synchronized Set<Integer> getAliveSetView() {
        Set<Integer> view = new HashSet<>();
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            Member member = members.get(i);
            if (member.getIsAlive())
                view.add(i);
        }
        return view;
    }

    @Override
    public String toString() {
        List<Member> list = getListView();
        StringBuilder builder = new StringBuilder("\n");
        for (int i = 1; i <= Constants.MaxMachines; i++) {
            Member member = list.get(i);
            builder.append(String.format("    %02d: isAlive: %b, age: %d, lastAppeared: %s\n",
                    i,
                    member.getIsAlive(),
                    member.getAge(),
                    member.getLastAppeared()));
        }
        return builder.toString();
    }
}
