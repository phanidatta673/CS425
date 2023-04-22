import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private Socket socket;
    private ServerSocket server;

    public void start(int port) {
        try {
            server = new ServerSocket(port);
            System.out.printf("Server listening at %d\n", port);

            while (true) {
                Socket socket = server.accept();
                Thread thread = new ServerThread(socket);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }

    }
}