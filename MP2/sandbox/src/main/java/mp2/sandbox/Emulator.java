package mp2.sandbox;

import java.net.ServerSocket;
import java.util.Scanner;

public class Emulator {
    Scanner scanner = new Scanner(System.in);

    public void start() {
        try {
            ServerSocket ssk = new ServerSocket(10000);
            Thread t = new Thread(new Server(ssk));
            t.start();
            System.out.println("Waiting for input");
            scanner.next();
            try {
                ssk.close();
            } catch (Exception e) {
                System.out.println("Emulator: " + e.getMessage());
            }
        } catch (Exception e) {
        }
    }

    private class Server implements Runnable {
        private ServerSocket ssk = null;

        public Server(ServerSocket ssk) {
            this.ssk = ssk;
        }

        @Override
        public void run() {
            try {
                System.out.println("Server starts running");
                ssk.accept();
            } catch (Exception e) {
                System.out.println("Server: " + e.getMessage());
            }
        }
    }
}
