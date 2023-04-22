package mp1;

import java.net.InetSocketAddress;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Client class that dispatches each work to each Querier instance.
 */
public class Client {

    private List<Querier> queriers = null;

    private Scanner scanner = new Scanner(System.in);

    /**
     * Constructor of the Client class. Also constructs queriers
     * 
     * @param hosts The hosts that the client should query.
     */
    public Client(List<InetSocketAddress> hosts) {
        queriers = new LinkedList<>();
        for (InetSocketAddress host : hosts) {
            Querier querier = new Querier(host);
            queriers.add(querier);
        }
    }

    /**
     * Reads the command from stdin and do the query.
     */
    public void start() {
        Log.sayF("Please enter the grep command: ");
        String cmd = scanner.nextLine();
        doQuery(cmd);
    }

    /**
     * Checks the availability of each querier and assigns them to execute
     * the given query.
     * 
     * @param cmd The command to be sent to the servers.
     * @return Add result of each querier to the Client::results list.
     */
    public List<String> doQuery(String cmd) {
        List<String> results = new ArrayList<>();
        for (Querier querier : queriers) {
            if (querier.getAvailability())
                querier.query(cmd);
        }
        long totalCostTime = 0;
        int sum = 0;
        for (Querier querier : queriers) {
            if (querier.getAvailability()) {
                String result = querier.getResult();
                results.add(result);

                long lastCostTime = querier.getLastCostTime();
                totalCostTime = Math.max(lastCostTime, totalCostTime);
                sum += parseInt(result);
                Log.sayF("Result of %s is:\n%s", querier.getHostAndPort(), result);
                Log.sayF("Cost time: %d ms\n", lastCostTime);
                Log.say("================================");
            }
        }

        Log.sayF("Answer is: %d.\n", sum);
        Log.sayF("Total cost time: %d ms.\n", totalCostTime);
        return results;
    }

    public int parseInt(String s) {
        int ans = 0;
        try {
            ans = Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
        return ans;
    }
}
