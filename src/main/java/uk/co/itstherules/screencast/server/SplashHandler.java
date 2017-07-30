package uk.co.itstherules.screencast.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

public class SplashHandler implements HttpHandler {

    private final ScreenNotifier notifier;
    private static final String SERVICE_NAME = "splash";

    SplashHandler(NotifierConfiguration configuration) {
        notifier = new ScreenNotifier(configuration);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        UriModel uriModel = new UriModel(exchange);
        if(SERVICE_NAME.equals(uriModel.getServiceName())) {
            String imageString = uriModel.getImage();
            byte[] decodedImage = Base64.getDecoder().decode(imageString);
            long timeToDisplay = notifier.showImage(decodedImage, Location.CENTER);
            exchange.getResponseHeaders().add("X-TESTCAST-SPLASH-DISPLAY-TIME", String.valueOf(timeToDisplay));
            exchange.sendResponseHeaders(200, imageString.length());
            OutputStream os = exchange.getResponseBody();
            os.write(imageString.getBytes("UTF8"));
            os.close();
        }
    }

}
