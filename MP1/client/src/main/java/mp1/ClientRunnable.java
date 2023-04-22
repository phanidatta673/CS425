package mp1;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Actual class that sends the command to the server and
 * reads the result.
 */
public class ClientRunnable implements Runnable {
    private String cmd = null;
    private Socket socket = null;
    private volatile long lastCostTime = 0;
    private volatile String result = null;

    /**
     * Constructor that connects to the given host.
     * 
     * @param host The server to be connected to.
     * @param cmd  The command to be sent.
     */
    public ClientRunnable(InetSocketAddress host, String cmd) {
        this.cmd = cmd;
        socket = connectToServer(host);
    }

    /**
     * Check if the socket is successfully connected.
     * 
     * @return Is the socket non-null.
     */
    public boolean getAvailability() {
        if (socket == null)
            return false;
        return true;
    }

    /**
     * Runs when the thread starts.
     * Sends the command to the server, read results from them,
     * then close the socket.
     */
    @Override
    public void run() {
        long start = System.nanoTime();
        sendCmdToServer(cmd);
        result = getResultFromServer();
        lastCostTime = (System.nanoTime() - start) / 1000000;
        closeSocket();
    }

    /**
     * Connects to a given host and returns the socket.
     */
    private static Socket connectToServer(InetSocketAddress host) {
        Socket socket = null;
        try {
            socket = new Socket(host.getAddress(), host.getPort());
        } catch (IOException e) {
            Log.sayF("Error while connecting to server: %s\n", e);
        }
        return socket;
    }

    /**
     * Writes the command to the socket output stream.
     * 
     * @param cmd Command to be sent.
     */
    private void sendCmdToServer(String cmd) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(cmd);
        } catch (IOException e) {
            Log.sayF("Error while sending command to %s: %s\n", socket.getInetAddress());
        }
    }

    /**
     * Reads and returns the result from the server.
     * 
     * @return Result from the server.
     */
    private String getResultFromServer() {
        String result = null;
        try {
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int count = 0;
            byte[] bytes = new byte[8192];

            while ((count = in.read(bytes)) > 0) {
                Log.sayF("Received %d bytes\n", count);
                out.write(bytes, 0, count);
            }
            byte[] outBytes = out.toByteArray();
            result = new String(outBytes);
        } catch (IOException e) {
            Log.sayF("Error while reading result of %s: %s\n", socket.getInetAddress(), e);
        }
        return result;
    }

    /**
     * Safely closes the socket.
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.sayF("Error closing socket %s: %s\n", socket.getInetAddress(), e);
        }
    }

    /**
     * Getter function for the result.
     * 
     * @return Result of the server query.
     */
    public String getResult() {
        return result;
    }

    /**
     * Get last cost time in milliseconds.
     * 
     * @return Last cost time in milliseconds
     */
    public long getLastCostTime() {
        return lastCostTime;
    }

}
