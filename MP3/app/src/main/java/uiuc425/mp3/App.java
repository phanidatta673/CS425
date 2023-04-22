package uiuc425.mp3;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) {
        log.trace("App starting with args length: {}", args.length);
        boolean runAsIntroducer = false;
        if (args.length > 1) {
            log.fatal("Usage: \"../gradlew run\" to run as node and");
            log.fatal("       \"../gradlew run --args introducer\" to run as introducer");
            return;
        }
        if (args.length == 1 && args[0].equals("introducer")) {
            runAsIntroducer = true;
        }

        if (runAsIntroducer) {
            log.warn("Running as introducer...");
            new Node(runAsIntroducer).start();
        } else {
            log.warn("Running as normal node...");
            InetSocketAddress introducer = new InetSocketAddress(
                    Constants.IntroducerHostName,
                    Constants.JoinPort);
            new Node(introducer).start();
        }
    }
}
