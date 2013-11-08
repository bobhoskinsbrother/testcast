package uk.co.itstherules.screennotification;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class TestCastServer {

    private HttpServer server;
    private PopUpNotifier notifier;

    public TestCastServer(TestCastConfiguration configuration) {
        try {
            notifier = new PopUpNotifier(configuration);
            server = HttpServer.create(new InetSocketAddress(9998), 0);
            server.createContext("/notify", notifier);
            server.setExecutor(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws IOException {
        server.start();
    }

    public void stop() throws IOException {
        server.stop(0);
        notifier = null;
    }

}
