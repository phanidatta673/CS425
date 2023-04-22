package mp1;

/**
 * Wrapper for the System.out.print functions in case we have to
 * change the logging methods to something else such as Log4J.
 * 
 */
public class Log {
    public static void say(String s) {
        System.out.println(s);
    }

    public static void sayF(String fmt, Object... args) {
        System.out.printf(fmt, args);
    }
}
