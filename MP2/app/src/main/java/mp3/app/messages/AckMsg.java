package mp3.app.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class AckMsg {
    @Expose
    private String content = "ACK";

    private final static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public String toString() {
        return "ACK";
    }

    public static AckMsg fromBytes(byte[] bytes) {
        String input = new String(bytes, 0, bytes.length);
        return gson.fromJson(input, AckMsg.class);
    }

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

}
