package edu.uiuc425.boss.role.leader.sdfs;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SdfsTable {
    // Who has file f (latest) -> Collection
    // Who has file f, version v -> Collection
    // Latest version of f -> int
    // Add [file][version]
    // Remove id from [file][version]
    private static final Logger log = LogManager.getLogger();
    private final TreeBasedTable<String, Integer, Set<Integer>> table;

    public SdfsTable() {
        table = TreeBasedTable.create(
                Comparator.naturalOrder(),
                Collections.reverseOrder());
    }

    public synchronized List<Integer> whoHas(String fileName, int version) {
        if (version == 0) return new ArrayList<>();

        return new ArrayList<>(table.get(fileName, version));
    }

    public synchronized List<Integer> whoHas(String fileName) {
        Set<Integer> result = new HashSet<>();
        SortedMap<Integer, Set<Integer>> versionReplicasMap = table.row(fileName);
        for (var entry : versionReplicasMap.entrySet()) {
            result.addAll(entry.getValue());
        }
        return new ArrayList<>(result);
    }

    public synchronized Pair<Integer, List<Integer>> whoHasLatestOf(String fileName) {
        int latestVersion = latestVersionOf(fileName);
        return new ImmutablePair<>(latestVersion, whoHas(fileName, latestVersion));
    }

    public synchronized int latestVersionOf(String fileName) {
        SortedMap<Integer, Set<Integer>> versionReplicasMap = table.row(fileName);
        if (versionReplicasMap.size() != 0)
            try {
                return versionReplicasMap.firstKey();
            } catch (NoSuchElementException e) {
                log.info("{} is not in table: {}", fileName, e.getMessage());
            }
        return 0;
    }

    public synchronized void deleteReplicaFromFileVersion(String name, int version, int replica) {
        log.info("Deleting {} from {}:{} entry", name, version, replica);
        Set<Integer> set = table.get(name, version);
        if (set != null)
            set.remove(replica);
    }

    public synchronized void put(String fileName, int version, int replicaID) {
        if (!table.contains(fileName, version))
            table.put(fileName, version, new HashSet<>());
        table.get(fileName, version).add(replicaID);
    }

    public synchronized void removeReplica(String fileName, int replicaID) {
        SortedMap<Integer, Set<Integer>> row = table.row(fileName);
        for (var entry : row.entrySet()) {
            entry.getValue().remove(replicaID);
        }
    }

    public synchronized void clearReplicaRecord(int replicaID) {
        Set<Table.Cell<String, Integer, Set<Integer>>> set = table.cellSet();
        for (var cell : set) {
            if (cell.getValue().contains(replicaID)) {
                table.get(cell.getRowKey(), cell.getColumnKey()).remove(replicaID);
            }
        }
    }

    public synchronized List<Pair<String, Integer>> filesIn(int replicaID) {
        Set<Table.Cell<String, Integer, Set<Integer>>> set = table.cellSet();
        List<Pair<String, Integer>> ans = new LinkedList<>();
        for (var cell : set) {
            if (cell.getValue().contains(replicaID)) {
                ans.add(new ImmutablePair<>(cell.getRowKey(), cell.getColumnKey()));
            }
        }
        return ans;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n");
        SortedMap<String, Map<Integer, Set<Integer>>> rowMap = table.rowMap();
        for (Map.Entry<String, Map<Integer, Set<Integer>>> row : rowMap.entrySet()) {
            sb.append(String.format("File %s:\n", row.getKey()));
            for (var col : row.getValue().entrySet()) {
                sb.append(String.format("    version %d: %s\n", col.getKey(), col.getValue()));
            }
        }
        return sb.toString();
    }
}
