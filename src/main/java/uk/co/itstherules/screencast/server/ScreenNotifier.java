package uk.co.itstherules.screencast.server;

import com.sun.awt.AWTUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static uk.co.itstherules.screencast.server.Location.*;

public class ScreenNotifier {

    private final int readSpeedPerWord;
    private final int paddingAroundNotification;
    private final JLabel label;
    private final JWindow window;
    private final Map<Location, Point> locations;
    private final Dimension notifierDimension;
    private final String textStyling;

    public ScreenNotifier(PopUpConfiguration configuration) {
        readSpeedPerWord = configuration.readSpeedPerWord();
        textStyling = configuration.textStyling();
        paddingAroundNotification = configuration.paddingAroundNotification();
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        notifierDimension = new Dimension(configuration.notificationWidth(), configuration.notificationHeight());
        window = new JWindow();
        window.setAlwaysOnTop(true);

        AWTUtilities.setWindowOpacity(window, 0.9f);

        Container container = window.getContentPane();
        container.setLayout(null);

        JPanel panel = makePanel(notifierDimension);
        container.add(panel);

        label = makeLabel(notifierDimension);
        panel.add(label);

        locations = makeLocations(screenDimension, notifierDimension);

        window.setSize(notifierDimension);
        window.setPreferredSize(notifierDimension);
        setLocationAt(configuration.location());
        window.pack();
    }

    private Map<Location, Point> makeLocations(Dimension screenDimension, Dimension notifierDimension) {
        Map<Location, Point> map = new HashMap<>(Location.values().length);
        map.put(TOP_RIGHT, topRightOfTheScreen(screenDimension, notifierDimension));
        map.put(TOP_LEFT, topLeftOfTheScreen());
        map.put(BOTTOM_RIGHT, bottomRightOfTheScreen(screenDimension, notifierDimension));
        map.put(BOTTOM_LEFT, bottomLeftOfTheScreen(screenDimension, notifierDimension));
        map.put(CENTER, centerOfTheScreen(screenDimension, notifierDimension));
        return map;
    }

    private Point topLeftOfTheScreen() {
        return new Point(paddingAroundNotification, paddingAroundNotification);
    }

    private Point topRightOfTheScreen(Dimension screenDimension, Dimension notifierDimension) {
        int x = screenDimension.width - notifierDimension.width - paddingAroundNotification;
        int y = paddingAroundNotification;
        return new Point(x, y);
    }

    private Point bottomRightOfTheScreen(Dimension screenDimension, Dimension notifierDimension) {
        int x = screenDimension.width - notifierDimension.width - paddingAroundNotification;
        int y = screenDimension.height - notifierDimension.height - paddingAroundNotification;
        return new Point(x, y);
    }

    private Point bottomLeftOfTheScreen(Dimension screenDimension, Dimension notifierDimension) {
        int x = paddingAroundNotification;
        int y = screenDimension.height - notifierDimension.height - paddingAroundNotification;
        return new Point(x, y);
    }

    private Point centerOfTheScreen(Dimension screenDimension, Dimension notifierDimension) {
        int x = (screenDimension.width / 2) - (notifierDimension.width / 2);
        int y = (screenDimension.height / 2) - (notifierDimension.height / 2);
        return new Point(x, y);
    }

    public long showMessage(String text, Location location) {
        setLocationAt(location);
        show();
        final long timeout = timeoutFor(text);
        label.setText(htmlDecorated(text));

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    hide();
                }
                hide();
                return null;
            }
        };

        worker.execute();
        return timeout;
    }

    private String htmlDecorated(String text) {
        return  "<html>" +
                "<body style=\"text-align: center; width: 100%; padding: 10px; "+textStyling+"\">" +
                    text +
                "</body>" +
                "</html>";
    }

    private void setLocationAt(Location location) {
        window.setLocationRelativeTo(null);
        window.setLocation(locations.get(location));
    }

    private long timeoutFor(String text) {
        int words = text.split(" ").length + 1;
        return words * readSpeedPerWord;
    }

    private void show() {
        window.setVisible(true);
    }

    private void hide() {
        window.setVisible(false);
    }

    private JLabel makeLabel(Dimension dimension) {
        JLabel label = new JLabel();
        label.setBounds(0, 0, dimension.width, dimension.height);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setMaximumSize(notifierDimension);
        Font verdana = new Font("Trebuchet MS", Font.BOLD, 14);
        label.setFont(verdana);
        label.setForeground(Color.BLACK);
        return label;
    }

    private JPanel makePanel(Dimension dimension) {
        JPanel panel = new JPanel();
        panel.setSize(dimension);
        panel.setPreferredSize(dimension);
        panel.setBackground(Color.WHITE);
        panel.setBounds(0, 0, dimension.width, dimension.height);
        panel.setForeground(Color.BLACK);
        panel.setLayout(null);
        return panel;
    }

}