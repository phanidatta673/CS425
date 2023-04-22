package mp1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server class listens to the given port and creates ServerThread
 * for each given connection.
 */
public class Server {
    private ServerSocket server = null;

    /**
     * Starts the server on the given port.
     * 
     * @param port The port that server listens to.
     */
    public void start(int port) {
        server = createServerSocket(port);
        Log.sayF("Server listening at port %d\n", port);
        Log.say("==============================");
        while (true) {
            try {
                Socket client = server.accept();
                new ServerThread(client).start();
            } catch (IOException e) {
                Log.sayF("ServerSocket.accept error: %s\n", e);
            }
        }
    }

    /**
     * Creates the server socket listening on the given port and return.
     * 
     * @param port Port that server listens to.
     * @return
     */
    private ServerSocket createServerSocket(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
        }
        return serverSocket;
    }
}
