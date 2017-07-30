package uk.co.itstherules.screencast;

import uk.co.itstherules.screencast.client.NotifierClient;
import uk.co.itstherules.screencast.server.Location;
import uk.co.itstherules.screencast.server.NotifierConfiguration;
import uk.co.itstherules.screencast.server.NotifierServer;

public class RecordScreen {

    public static void main(String... args) throws Exception {
        NotifierConfiguration configuration = new NotifierConfiguration();
        NotifierServer server = new NotifierServer(configuration);
        server.start();
        NotifierClient client = new NotifierClient(configuration);
        long lengthOfPause = client.sendMessage("Top Right");
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Center", Location.CENTER);
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Bottom Left", Location.BOTTOM_LEFT);
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Top Left", Location.TOP_LEFT);
        Thread.sleep(lengthOfPause);
        lengthOfPause = client.sendMessage("Bottom Right", Location.BOTTOM_RIGHT);
        Thread.sleep(lengthOfPause);
        server.stop();
    }

}