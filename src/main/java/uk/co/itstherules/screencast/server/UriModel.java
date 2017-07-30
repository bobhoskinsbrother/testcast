package uk.co.itstherules.screencast.server;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UriModel {

    private final String[] path;

    public UriModel(String uri) {
        this.path = uri.split("/");
    }

    public String getServiceName() {
        return path[1];
    }

    public Location getLocation() {
        return Location.valueOf(path[2]);
    }

    public String getMessage() {
        try {
            return URLDecoder.decode(path[3], "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
