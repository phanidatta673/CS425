import edu.uiuc425.boss.role.leader.sdfs.SdfsTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LookupTableTest {
    private static final Logger log = LogManager.getLogger();

    @Test
    void testDuplicatePuts() {
        SdfsTable table = new SdfsTable();
        table.put("file1", 1, 1);
        table.put("file1", 1, 1);
        table.put("file1", 1, 1);
        assertEquals(1, table.whoHasLatestOf("file1").getRight().size());
        assertEquals(1, table.latestVersionOf("file1"));
        assertEquals(1, table.whoHas("file1").size());
    }

    @Test
    void testDuplicateVersions() {
        SdfsTable table = new SdfsTable();
        table.put("file1", 1, 1);
        table.put("file1", 1, 2);
        table.put("file1", 1, 3);
        assertEquals(3, table.whoHas("file1").size());
        assertEquals(3, table.whoHasLatestOf("file1").getRight().size());
    }

    @Test
    void testDuplicateReplicas() {
        SdfsTable table = new SdfsTable();
        table.put("file1", 1, 1);
        table.put("file1", 2, 1);
        table.put("file1", 2, 1);
        table.put("file1", 3, 1);
        table.put("file1", 4, 1);
        table.put("file1", 4, 1);
        table.put("file1", 4, 1);
        assertEquals(1, table.whoHas("file1").size());
        assertEquals(1, table.whoHasLatestOf("file1").getRight().size());
        assertEquals(1, table.whoHas("file1", 2).size());
        assertEquals(4, table.latestVersionOf("file1"));
    }

    @Test
    void testDifferentFiles() {
        SdfsTable table = new SdfsTable();
        table.put("file1", 1, 1);
        table.put("file1", 4, 5);
        table.put("file2", 1, 1);
        table.put("file2", 3, 2);
        table.put("file2", 5, 3);
        assertEquals(2, table.whoHas("file1").size());
        assertEquals(3, table.whoHas("file2").size());
        assertEquals(1, table.whoHasLatestOf("file1").getRight().size());
        assertEquals(1, table.whoHasLatestOf("file2").getRight().size());
    }

    @Test
    void testDifferentReplicas() {
        SdfsTable table = new SdfsTable();
        table.put("file1", 1, 1);
        table.put("file1", 1, 2);
        table.put("file1", 1, 3);
        table.put("file1", 2, 3);
        table.put("file1", 3, 1);
        table.put("file1", 3, 4);
        assertEquals(2, table.whoHasLatestOf("file1").getRight().size());
        assertEquals(4, table.whoHas("file1").size());
        assertEquals(3, table.whoHas("file1", 1).size());
        assertEquals(3, table.latestVersionOf("file1"));
    }

    @Test
    void testClearReplicaRecord() {
        SdfsTable table = new SdfsTable();
        table.put("file1", 1, 1);
        table.put("file1", 2, 1);
        table.put("file1", 1, 2);
        table.put("file1", 1, 3);
        assertEquals(3, table.whoHas("file1").size());
        table.clearReplicaRecord(2);
        assertEquals(2, table.whoHas("file1").size());

        table.put("file1", 2, 2);
        assertEquals(3, table.whoHas("file1").size());

        table.put("file1", 4, 6);
        table.put("file1", 3, 5);
        assertEquals(5, table.whoHas("file1").size());

        table.clearReplicaRecord(1);
        assertFalse(table.whoHas("file1").contains(1));
    }
}
