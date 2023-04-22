import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] argv)
    {
        new Server().start(5001);
    }
}
