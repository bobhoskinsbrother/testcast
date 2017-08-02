package uk.co.itstherules.screencast;

import uk.co.itstherules.screencast.client.NotifierClient;
import uk.co.itstherules.screencast.server.NotifierConfiguration;

public final class Notifier {

    private static final NotifierClient CLIENT;

    static {
        CLIENT = new NotifierClient(new NotifierConfiguration());
    }

    private Notifier() {}

    public static void notify(final String message) {
        waitUsing(() -> CLIENT.sendMessage(message));
    }

    public static void splashScreen(final String imagePath) {
        waitUsing(() -> CLIENT.sendImage(imagePath));
    }

    private static void waitUsing(TimeSupplier supplier) {
        long waitTimeInMillis = supplier.supply();
        try {
            Thread.sleep(waitTimeInMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private interface TimeSupplier {
        long supply();
    }
}
