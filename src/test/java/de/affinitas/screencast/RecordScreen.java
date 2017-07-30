package de.affinitas.screencast;

import de.affinitas.screencast.client.PopUpClient;
import de.affinitas.screencast.server.Location;
import de.affinitas.screencast.server.PopUpConfiguration;
import de.affinitas.screencast.server.PopUpServer;

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