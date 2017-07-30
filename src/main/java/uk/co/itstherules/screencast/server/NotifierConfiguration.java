package uk.co.itstherules.screencast.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static uk.co.itstherules.screencast.server.Location.TOP_RIGHT;

public class NotifierConfiguration {

    private final Properties properties;
    private String textStyling;

    public NotifierConfiguration()  {
        properties = new Properties();
        try {
            InputStream inputStream = NotifierConfiguration.class.getClassLoader().getResourceAsStream("testcast.properties");
            properties.load(inputStream);
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