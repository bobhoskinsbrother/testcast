package de.affinitas.screencast.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static de.affinitas.screencast.server.Location.TOP_RIGHT;

public class PopUpConfiguration {

    private final Properties properties;
    private String textStyling;

    public PopUpConfiguration()  {
        properties = new Properties();
        try {
            properties.load(new FileInputStream("testcast.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int paddingAroundNotification() {
        return Integer.parseInt(properties.getProperty("padding_around_notification", "10"));
    }

    public int readSpeedPerWord() {
        return Integer.parseInt(properties.getProperty("read_speed_per_word", "600"));
    }

    public Location location() {
        String property = properties.getProperty("notification_location", "TOP_RIGHT");
        try {
            return Location.valueOf(property);
        } catch(IllegalArgumentException e) {
            return TOP_RIGHT;
        }
    }

    public int notificationWidth() {
        return Integer.parseInt(properties.getProperty("notification_width", "300"));
    }

    public int notificationHeight() {
        return Integer.parseInt(properties.getProperty("notification_height", "300"));
    }

    public String serverHost() {
        return properties.getProperty("server_host", "localhost");
    }

    public String textStyling() {
        String property = properties.getProperty("text_styling", "");
        return property;
    }
}