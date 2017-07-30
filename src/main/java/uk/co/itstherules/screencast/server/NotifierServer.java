package uk.co.itstherules.screencast.server;

import com.sun.net.httpserver.HttpServer;
import uk.co.itstherules.TestCastService;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class NotifierServer implements TestCastService {

    private HttpServer server;
    private NotifierHandler notifier;
    private SplashHandler splash;

    public NotifierServer(NotifierConfiguration configuration) {
        try {
            notifier = new NotifierHandler(configuration);
            splash = new SplashHandler(configuration);
            server = HttpServer.create(new InetSocketAddress(9998), 0);
            server.createContext("/notify", notifier);
            server.createContext("/splash", splash);
            server.setExecutor(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop(0);
        notifier = null;
        splash = null;
    }

}
