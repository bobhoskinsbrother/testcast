package com.sqanta;

import ch.randelshofer.screenrecorder.ScreenRecorder;
import uk.co.itstherules.screennotification.Location;
import uk.co.itstherules.screennotification.TestCastClient;
import uk.co.itstherules.screennotification.TestCastConfiguration;
import uk.co.itstherules.screennotification.TestCastServer;

import java.io.File;

public class RecordScreen {

    public static void main(String[] args) throws Exception {
        TestCastConfiguration configuration = new TestCastConfiguration();
        TestCastServer server = new TestCastServer(configuration);
        server.start();
        final ScreenRecorder recorder = new ScreenRecorder(new File(configuration.videoSaveDirectory()));
        Runnable runnable = new Runnable() { @Override public void run() { recorder.start(); } };
        Thread thread = new Thread(runnable);
        thread.run();
        TestCastClient client = new TestCastClient(configuration);
        long lengthOfPause = client.sendMessage("Top Right");
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Center", Location.center);
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Bottom Left", Location.bottom_left);
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Top Left", Location.top_left);
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Bottom Right", Location.bottom_right);
        Thread.sleep(lengthOfPause);
        server.stop();
        recorder.stop();
    }

}