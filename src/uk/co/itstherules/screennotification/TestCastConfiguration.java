package uk.co.itstherules.screennotification;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static uk.co.itstherules.screennotification.Location.*;

public class TestCastConfiguration {

    private final Properties properties;

    public TestCastConfiguration() throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream("testcast.properties"));
    }

    public int paddingAroundNotification() {
        return Integer.parseInt(properties.getProperty("padding_around_notification", "10"));
    }

    public int readSpeedPerWord() {
        return Integer.parseInt(properties.getProperty("read_speed_per_word", "600"));
    }

    public String videoSaveDirectory() {
        return properties.getProperty("video_save_directory", ".");
    }

    public Location location() {
        String property = properties.getProperty("notification_location", "top_right");
        try {
            return Location.valueOf(property);
        } catch(IllegalArgumentException e) {
            return top_right;
        }
    }

    public int notificationWidth() {
        return Integer.parseInt(properties.getProperty("notification_width", "300"));
    }

    public int notificationHeight() {
        return Integer.parseInt(properties.getProperty("notification_height", "300"));
    }

}