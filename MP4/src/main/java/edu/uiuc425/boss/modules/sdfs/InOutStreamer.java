package edu.uiuc425.boss.modules.sdfs;

import edu.uiuc425.boss.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;

public class InOutStreamer implements Callable<Boolean> {
    private static final Logger log = LogManager.getLogger();
    private final InputStream in;
    private final List<OutputStream> outs;
    private final long fileSize;

    public InOutStreamer(long fileSize, InputStream in, List<OutputStream> outs) {
        this.in = in;
        this.outs = outs;
        this.fileSize = fileSize;
    }

    public InOutStreamer(long fileSize, InputStream in, OutputStream out) {
        this(fileSize, in, List.of(out));
    }

    // TODO: Change to runnable and throw error while error
    @Override
    public Boolean call() {
        long count = 0;
        byte[] buffer = new byte[Constants.ChunkSize];
        long start = System.currentTimeMillis();
        try {
            while (count < fileSize) {
                int len = Constants.ChunkSize;
                if (fileSize - count < Constants.ChunkSize)
                    len = (int) (fileSize - count);

                int chunkSize = in.read(buffer, 0, len);
                count += chunkSize;

                if (count % (chunkSize * 1024L) == 0)
                    log.info("Writing chunk size {}, size written (inclusive): {}mb", chunkSize, count / 1024L);

                for (OutputStream out : outs) {
                    out.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            log.error("File transferer IO error: {}", e.getMessage());
            return false;
        }
        long end = System.currentTimeMillis();
        log.trace("Total of {} bytes are written. Cost time {} seconds", count, (end - start) / 1000.0);
        return true;
    }
}
