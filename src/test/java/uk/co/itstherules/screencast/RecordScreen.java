package uk.co.itstherules.screencast;

import uk.co.itstherules.screencast.client.PopUpClient;
import uk.co.itstherules.screencast.server.Location;
import uk.co.itstherules.screencast.server.PopUpConfiguration;
import uk.co.itstherules.screencast.server.PopUpServer;

public class RecordScreen {

    public static void main(String... args) throws Exception {
        PopUpConfiguration configuration = new PopUpConfiguration();
        PopUpServer server = new PopUpServer(configuration);
        server.start();
        PopUpClient client = new PopUpClient(configuration);
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