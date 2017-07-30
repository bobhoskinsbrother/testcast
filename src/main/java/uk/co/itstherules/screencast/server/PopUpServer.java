package uk.co.itstherules.screencast.server;

import com.sun.net.httpserver.HttpServer;
import uk.co.itstherules.TestCastService;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class PopUpServer implements TestCastService {

    private HttpServer server;
    private PopUpNotifierHandler notifier;

    public PopUpServer(PopUpConfiguration configuration) {
        try {
            notifier = new PopUpNotifierHandler(configuration);
            server = HttpServer.create(new InetSocketAddress(9998), 0);
            server.createContext("/notify", notifier);
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
    }

}
