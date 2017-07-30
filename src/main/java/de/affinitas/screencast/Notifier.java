package de.affinitas.screencast;

import de.affinitas.screencast.client.PopUpClient;
import de.affinitas.screencast.server.PopUpConfiguration;

public final class Notifier {

    private static final PopUpClient CLIENT;

    static {
        CLIENT = new PopUpClient(new PopUpConfiguration());
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

}
