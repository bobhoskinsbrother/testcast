package uk.co.itstherules.screencast.client;

import uk.co.itstherules.screencast.server.Location;
import uk.co.itstherules.screencast.server.PopUpConfiguration;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public final class PopUpClient {

    private final PopUpConfiguration configuration;

    public PopUpClient(PopUpConfiguration configuration) {
        this.configuration = configuration;
    }

    public long sendMessage(String message) {
        return sendMessage(message, configuration.location());
    }

    public long sendMessage(String message, Location location) {
        long reply = -1;
        try {
            URL url = new URL("http://"+configuration.serverHost()+":9998/notify/" + location.name() + "/" + URLEncoder.encode(message, "UTF8"));
            URLConnection connection = url.openConnection();
            String header = connection.getHeaderField("X-TESTCAST-NOTIFIER-DISPLAY-TIME");
            reply = Long.parseLong(header);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return reply;
    }

}
