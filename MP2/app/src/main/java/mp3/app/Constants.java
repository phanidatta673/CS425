package mp3.app;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public final static String LeaderHostName = "fa22-cs425-5001.cs.illinois.edu";
    public final static int MaxMachines = 10;
    public final static int MaxFailures = 3;
    public final static int NumberOfReplicas = MaxFailures + 1;

    public final static int ServerTimeout = 60 * 60 * 1000; // one hour

    public final static int JoinPort = 10000;
    public final static int ReceivePingPort = 10001;
    public final static int PutRequestListenerPort = 10002;
    public final static int StoreListenerPort = 10003;
    public final static int GetRequestListenerPort = 10004;
    public final static int AskListenerPort = 10005;
    public final static int DeleteRequestListenerPort = 10006;
    public final static int RemoveListernerPort = 10007;
    public final static int ListRequestListenerPort = 10008;

    public final static int MaxPingSize = 512;
    public final static int MaxPongSize = 128;

    public final static int PingInterval = 800;
    public final static int PongTimeout = 1000 * 60 * 60; // one hour
    public final static int PrintInterval = 1000;

    public final static int MaxPongWaitTime = 700;
    public final static int MaxPongTimeouts = 5;

    public final static Path storage = Paths.get("storage/");
    public final static Path saved = Paths.get("saved/");
}
