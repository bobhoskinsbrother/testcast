package uk.co.itstherules.screencast.client;

import uk.co.itstherules.screencast.server.Location;
import uk.co.itstherules.screencast.server.NotifierConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

public final class NotifierClient {

    private final NotifierConfiguration configuration;

    public NotifierClient(NotifierConfiguration configuration) {
        this.configuration = configuration;
    }

    public long sendMessage(String message) {
        return sendMessage(message, configuration.location());
    }

    public long sendMessage(String message, Location location) {
        String urlString = "http://" + configuration.serverHost() + ":9998/notify";
        try {
            URL url = new URL(urlString);
            String urlParameters = "location="+location.name()+"&message=" + encodedMessage(message);
            return post(url, urlParameters, "X-TESTCAST-NOTIFIER-DISPLAY-TIME");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long sendImage(String imagePath) {
        InputStream inputStream = NotifierClient.class.getClassLoader().getResourceAsStream(imagePath);
        String encodedImage = encodeImageFromInputStream(inputStream);
        String urlString = "http://" + configuration.serverHost() + ":9998/splash";
        try {
            return post(new URL(urlString), "image=" + encodedMessage(encodedImage), "X-TESTCAST-SPLASH-DISPLAY-TIME");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodedMessage(String message) {
        try {
            return URLEncoder.encode(message, "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private long post(URL url, String urlParameters, String headerValue) {
        try {
            HttpURLConnection connection = null;
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            DataOutputStream sender = new DataOutputStream(connection.getOutputStream());
            sender.writeBytes(urlParameters);
            sender.flush();
            sender.close();
            String header = connection.getHeaderField(headerValue);
            return Long.parseLong(header);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeImageFromInputStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int read;
            byte[] data = new byte[16384];
            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
            buffer.flush();
            byte[] imageBytes = buffer.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
