package mp1;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * ServerThread is the class that execute and responds to the request made
 * by the client.
 */
public class ServerThread extends Thread {

    private Socket client = null;

    public ServerThread(Socket client) {
        this.client = client;
    }

    /**
     * Overriding the Thread::run() function so that this function will be
     * ran when Thread.start() is called.
     * Basically gets the grep command from the client, and executes it, then
     * send back the results and close the socket after that.
     */
    @Override
    public void run() {
        Log.sayF("Server accepted %s:%d\n", client.getInetAddress(), client.getPort());
        String grepCmd = getGrepCmdFromClient();
        String result = executeGrep(grepCmd);
        sendBackResult(result);
        closeSocket();
        Log.say("Finished dealing with this client");
        Log.say("==============================");
    }

    /**
     * Read the string from the client input stream and return.
     * 
     * @return String read from the client.
     */
    private String getGrepCmdFromClient() {
        String grepCmd = null;
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            grepCmd = in.readUTF();
            Log.sayF("received command: %s\n", grepCmd);
        } catch (IOException e) {
            Log.sayF("Error while receiving command: %s\n", e);
        }
        return grepCmd;
    }

    /**
     * Executes the grep command using /bin/sh through the Process class
     * , then read and parse the output of it.
     * 
     * @param grepCmd The command to be executed.
     * @return The results of the command in string.
     */
    private String executeGrep(String grepCmd) {
        String result = new String();
        try {
            Log.sayF("Trying to run grep command: %s\n", grepCmd);
            Process process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", grepCmd });
            OutputStream output = process.getOutputStream();
            output.close();

            InputStream resultStream = process.getInputStream();

            List<String> results = IOUtils.readLines(resultStream, "UTF-8");
            for (int i = 0; i < results.size(); i++) {
                String str = results.get(i);
                result += str + '\n';
            }
            System.out.println(result);
        } catch (IOException e) {
            Log.sayF("Error while greping the log file: %s\n", e);
        }
        return result;
    }

    /**
     * Sending back the results to the client.
     * 
     * @param result The output to be sent to the client.
     */
    private void sendBackResult(String result) {
        Log.sayF("Trying to send back %s...\n", result);
        try {
            InputStream resultStream = new ByteArrayInputStream(result.getBytes());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            int count = 0;
            byte[] bytes = new byte[8192];
            while ((count = resultStream.read(bytes)) > 0) {
                Log.sayF("Writing %d bytes\n", count);
                out.write(bytes, 0, count);
            }
        } catch (IOException e) {
            Log.sayF("Error while sending result: %s\n", e);
        }
    }

    /**
     * Closes the socket safely.
     */
    private void closeSocket() {
        try {
            client.close();
        } catch (IOException e) {
            Log.sayF("Error closing client socket: %s\n", e);
        }
    }

}
