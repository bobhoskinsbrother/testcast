package uk.co.itstherules.screennotification;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

class PopUpNotifier implements HttpHandler {

    private final ScreenNotifier notifier;
    private static final String SERVICE_NAME = "notify";

    PopUpNotifier(TestCastConfiguration configuration) {
        notifier = new ScreenNotifier(configuration);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().toASCIIString();
        UriModel uriModel = new UriModel(uri);
        if(SERVICE_NAME.equals(uriModel.getServiceName())) {
            String message = uriModel.getMessage();
            long timeToDisplay = notifier.showMessage(message, uriModel.getLocation());
            exchange.getResponseHeaders().add("X-TESTCAST-NOTIFIER-DISPLAY-TIME", String.valueOf(timeToDisplay));
            exchange.sendResponseHeaders(200, message.length());
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes("UTF8"));
            os.close();
        }
    }

}
