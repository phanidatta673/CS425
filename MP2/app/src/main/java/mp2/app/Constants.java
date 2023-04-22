package mp2.app;

public class Constants {
    public final static String IntroducerHostName = "fa22-cs425-5001.cs.illinois.edu";
    public final static int JoinPort = 10000;
    public final static int ReceivePort = 10001;
    public final static int ACKPort = 10002;

    public final static int MaxMessageLength = 1024;
    public final static int UdpMessageMaxSize = 1024;

    public final static int PingInterval = 1500;
    public final static int ReceiveAckTimeout = 400;
    public final static int PrintIntervalInMilliseconds = 1000;
}
