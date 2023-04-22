package mp3.app.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileMsg {
    private static Logger log = LogManager.getLogger();
    private final static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Expose
    private String name = null;

    @Expose
    private int size = 0;

    @Expose
    private int version = 0;

    public FileMsg(
            String fileName,
            int fileSize,
            int version) {
        this.name = fileName;
        this.size = fileSize;
        this.version = version;
    }

    public static FileMsg fromBytes(byte[] bytes) {
        String input = new String(bytes, 0, bytes.length);
        return gson.fromJson(input, FileMsg.class);
    }

    public String getNameVersionString() {
        return String.format("%s:%d", name, version);
    }

    public String getName() {
        return name;
    }

    public int getFileSize() {
        return size;
    }

    public int getVersion() {
        return version;
    }

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    @Override
    public String toString() {
        String result = "";
        result += String.format("File message: {sdfs: %s, size: %d, version: %d}",
                name, size, version);
        return result;
    }
}
