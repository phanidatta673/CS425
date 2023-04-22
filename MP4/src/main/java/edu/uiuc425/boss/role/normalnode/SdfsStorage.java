package edu.uiuc425.boss.role.normalnode;

import com.google.common.collect.TreeBasedTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public class SdfsStorage {
    private static final Logger log = LogManager.getLogger();
    private final TreeBasedTable<String, Integer, File> table;

    public SdfsStorage() {
        table = TreeBasedTable.create(
                Comparator.naturalOrder(),
                Collections.reverseOrder());
    }

    public synchronized void put(String fileName, int version, File file) {
        table.put(fileName, version, file);
    }

    public synchronized File get(String fileName, int version) {
        return table.get(fileName, version);
    }

    public synchronized List<File> get(String fileName) {
        List<File> ans = new LinkedList<>();
        var row = table.row(fileName);
        for (File f : row.values()) {
            ans.add(new File(f.toURI()));
        }
        return ans;
    }

    public synchronized void delete(String fileName) {
        table.row(fileName).clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n");
        SortedMap<String, Map<Integer, File>> rowMap = table.rowMap();
        for (Map.Entry<String, Map<Integer, File>> row : rowMap.entrySet()) {
            sb.append(String.format("File %s:\n", row.getKey()));
            for (var col : row.getValue().entrySet()) {
                sb.append(String.format("    version %d: %s\n", col.getKey(), col.getValue()));
            }
        }
        return sb.toString();
    }
}
