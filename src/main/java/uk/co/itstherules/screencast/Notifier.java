package uk.co.itstherules.screencast;

import uk.co.itstherules.screencast.client.NotifierClient;
import uk.co.itstherules.screencast.server.NotifierConfiguration;

public final class Notifier {

    private static final NotifierClient CLIENT;

    static {
        CLIENT = new NotifierClient(new NotifierConfiguration());
    }

    private Notifier() {}

    public static void notify(String message) {
        long waitTimeInMillis = CLIENT.sendMessage(message);
        try {
            Thread.sleep(waitTimeInMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void splashScreen(String imagePath) {
        try {
            long waitTimeInMillis = CLIENT.sendImage(imagePath);
            Thread.sleep(waitTimeInMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
