package mp1;

import java.net.InetSocketAddress;

/**
 * Querier that wraps up the host, the task to run, the thread to
 * run on, and the results.
 */
public class Querier {
    private InetSocketAddress host = null;
    private ClientRunnable task = null;
    private Thread t = null;
    private boolean isAvailable = true;

    public Querier(InetSocketAddress host) {
        this.host = host;
    }

    /**
     * Assign the command to a new ClientRunnable object if it's
     * available.
     * 
     * @param cmd The command to be sent to the server.
     */
    public void query(String cmd) {
        task = new ClientRunnable(host, cmd);
        isAvailable = task.getAvailability();
        if (isAvailable) {
            t = new Thread(task);
            t.start();
        }
    }

    /**
     * Gets the unique host and port combination to identify the server.
     * 
     * @return Combined string of the host and the port.
     */
    public String getHostAndPort() {
        String s = new String();
        s += host.getAddress();
        s += ":" + host.getPort();
        return s;
    }

    /**
     * Getter function for the availability.
     * 
     * @return If the querier is available or not
     */
    public boolean getAvailability() {
        return isAvailable;
    }

    /**
     * Joins the thread and returns the result back.
     * 
     * @return Returns the result of the query from the server.
     */
    public String getResult() {
        joinThread();
        return task.getResult();
    }

    public long getLastCostTime() {
        return task.getLastCostTime();
    }

    /**
     * Joins the thread. Just created this function to avoid try-catch
     * blocks and keep the Querier::getResult function clean.
     */
    private void joinThread() {
        try {
            t.join();
        } catch (InterruptedException e) {
            Log.sayF("%s:%d thread interrupted\n", host.getAddress(), host.getPort());
        }
    }
}
