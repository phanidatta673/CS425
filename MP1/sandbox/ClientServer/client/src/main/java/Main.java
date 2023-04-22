import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String host = "localhost";
    private static final int port = 5001;

    public static void main(String[] args) {
        try {
            OutputStream fileOut = new FileOutputStream("result.log");
            OutputStream consoleOut = new PrintStream(System.out);
            List<OutputStream> outs = new LinkedList<>();
            outs.add(fileOut);
            outs.add(consoleOut);

            Scanner scanner = new Scanner(System.in);

            Socket socket = new Socket(host, port);
            InputStream in = socket.getInputStream();
            DataOutputStream queryStream = new DataOutputStream(socket.getOutputStream());
            byte[] bytes = new byte[8192];
            int count = 0;
            System.out.printf("I am %s:%d\n", socket.getInetAddress(), socket.getPort());
            while (true) {
                System.out.print("Please enter your query (Enter st to stop): ");
                String query = scanner.nextLine();
                System.out.printf("Query is: %s\n", query);
                if (query.equals("st")) break;
                queryStream.writeUTF(query);

                while ((count = in.read(bytes)) > 0) {
                    for (OutputStream out : outs)
                        out.write(bytes, 0, count);
                }
            }

            for (OutputStream out : outs)
                out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(e);
        }

    }
}
