package edu.uiuc425.boss;

import java.io.File;

public class Constants {
    public static final String LeaderHostName = "fa22-cs425-5001.cs.illinois.edu";
    public static final int MaxMachines = 10;
    public static final int MaxFailures = 3;

    public static final int ServerTimeout = 60 * 60 * 1000; // one hour

    public static final int IntroducerPort = 10000;
    public static final int ReceivePingPort = 10001;
    public static final int CoordinatorPort = 10009;
    public static final int CommandListenerPort = 10011;

    public static final int MaxPingSize = 256;
    public static final int MaxPongSize = 128;
    public static final int ChunkSize = 1024;

    public static final int PingInterval = 800;

    // Regard self as dead if after PongTimeout
    public static final int PongTimeout = 1000 * 6000; // 6000 seconds

    public static final int MaxPongWaitTime = 1000;
    public static final int MaxPongTimeouts = 5;


    public static final File storage = new File("runtime/storage/").getAbsoluteFile();
    public static final File saved = new File("runtime/saved/").getAbsoluteFile();
}
