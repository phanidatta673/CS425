package mp3.app.messages;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ListMsg {
    private static Logger log = LogManager.getLogger();
    private final static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Expose
    private List<String> hosts = null;

    public ListMsg(
            List<String> hosts) {
        this.hosts = new LinkedList<>(hosts);
    }

    public static ListMsg fromBytes(byte[] bytes) {
        String input = new String(bytes, 0, bytes.length);
        return gson.fromJson(input, ListMsg.class);
    }

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    public List<String> getList() {
        return new LinkedList<>(hosts);
    }

    @Override
    public String toString() {
        String result = "";
        result += String.format("ListMsg [{}]", hosts);
        return result;
    }
}
