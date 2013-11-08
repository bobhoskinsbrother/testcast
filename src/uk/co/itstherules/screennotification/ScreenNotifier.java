package uk.co.itstherules.screennotification;

import com.sun.awt.AWTUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static uk.co.itstherules.screennotification.Location.*;

public class ScreenNotifier {

    private final int readSpeedPerWord;
    private final int paddingAroundNotification;
    private final JLabel label;
    private final JWindow window;
    private final Map<Location, Point> locations;

    public ScreenNotifier(TestCastConfiguration configuration) {
        readSpeedPerWord = configuration.readSpeedPerWord();
        paddingAroundNotification = configuration.paddingAroundNotification();
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension notifierDimension = new Dimension(configuration.notificationWidth(), configuration.notificationHeight());
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
        map.put(top_right, topRightOfTheScreen(screenDimension, notifierDimension));
        map.put(top_left, topLeftOfTheScreen());
        map.put(bottom_right, bottomRightOfTheScreen(screenDimension, notifierDimension));
        map.put(bottom_left, bottomLeftOfTheScreen(screenDimension, notifierDimension));
        map.put(center, centerOfTheScreen(screenDimension, notifierDimension));
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
        label.setText(text);

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

    private void setLocationAt(Location location) {
        window.setLocationRelativeTo(null);
        window.setLocation(locations.get(location));
    }

    public long showMessage(String text) {
        return showMessage(text, top_right);
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