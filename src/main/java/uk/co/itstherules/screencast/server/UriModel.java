package uk.co.itstherules.screencast.server;


import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UriModel {

    private final String[] path;
    private final Map<String, List<String>> params;

    public UriModel(HttpExchange exchange) {
        String uri = exchange.getRequestURI().toASCIIString();
        path = uri.split("/");
        params = params(exchange);
    }

    public String getServiceName() {
        return path[1];
    }

    public Location getLocation() {
        return Location.valueOf(params.get("location").get(0));
    }

    public String getMessage() {
        return params.get("message").get(0);
    }

    public String getImage() {
        return params.get("image").get(0);
    }

    private Map<String, List<String>> params(HttpExchange exchange) {
        Map<String, List<String>> params = new HashMap<>();

        String encoding = "UTF8";
        String querystring = getQuerystring(exchange, encoding);

        String nameValuePairs[] = querystring.split("[&]");
        for (String nameValuePair : nameValuePairs) {
            int equalIndex = nameValuePair.indexOf('=');
            String name;
            String value;
            if (equalIndex < 0) {
                name = decode(encoding, nameValuePair);
                value = "";
            } else {
                name = decode(encoding, nameValuePair.substring(0, equalIndex));
                value = decode(encoding, nameValuePair.substring(equalIndex + 1));
            }
            List<String> list = params.computeIfAbsent(name, val -> new ArrayList<>());
            list.add(value);
        }
        return params;
    }

    private String decode(String encoding, String nameValuePair) {
        try {
            return URLDecoder.decode(nameValuePair, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getQuerystring(HttpExchange exchange, String encoding) {
        String querystring = "";

        InputStream in = exchange.getRequestBody();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte buf[] = new byte[4096];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            querystring = new String(out.toByteArray(), encoding);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return querystring;
    }

}
