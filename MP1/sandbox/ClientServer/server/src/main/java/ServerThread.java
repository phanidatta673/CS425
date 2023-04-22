import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket client = null;
    public ServerThread(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.printf("Server accepted a client: %s:%d\n", client.getInetAddress(), client.getPort());
        try {
            File file = new File("server/logs/hi.log");
            InputStream in = new FileInputStream(file);
            OutputStream out = client.getOutputStream();
            int count = 0;
            byte[] bytes = new byte[8192];
            while ((count = in.read(bytes)) > 0) {
                out.write(bytes, 0, count);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("Finished my job");
    }
}
