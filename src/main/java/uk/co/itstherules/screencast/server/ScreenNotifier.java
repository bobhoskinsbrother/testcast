package uk.co.itstherules.screencast.server;

import com.sun.awt.AWTUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ScreenNotifier {

    private final int readSpeedPerWord;
    private final int paddingAroundNotification;
    private final JLabel label;
    private final JWindow window;
    private final String textStyling;
    private final Dimension screenDimension;
    private final NotifierConfiguration configuration;
    private final JPanel panel;

    public ScreenNotifier(NotifierConfiguration configuration) {
        this.configuration = configuration;
        readSpeedPerWord = configuration.readSpeedPerWord();
        textStyling = configuration.textStyling();
        paddingAroundNotification = configuration.paddingAroundNotification();
        screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        window = new JWindow();
        window.setAlwaysOnTop(true);


        Dimension notifierDimension = new Dimension(configuration.notificationWidth(), configuration.notificationHeight());

        AWTUtilities.setWindowOpacity(window, 0.9f);
        Container container = window.getContentPane();
        container.setLayout(null);
        panel = makePanel(notifierDimension);
        container.add(panel);
        label = makeLabel(notifierDimension);
        panel.add(label);
        setDimensionAndLocation(configuration.location(), notifierDimension);
        window.pack();
    }


    public long showMessage(String text, Location location) {
        Dimension dimension = new Dimension(configuration.notificationWidth(), configuration.notificationHeight());
        setDimensionAndLocation(location, dimension);
        show();
        final long timeout = timeoutFor(text);
        label.setText(htmlDecorated(text));
        makeWorker(timeout).execute();
        return timeout;
    }

    public long showImage(byte[] decodedImage, Location location) {
        try {
            Image image = ImageIO.read(new ByteArrayInputStream(decodedImage));
            int height = image.getHeight(null);
            int width = image.getWidth(null);
            Dimension dimension = new Dimension(width, height);
            setDimensionAndLocation(location, dimension);
            show();
            final long timeout = 5000L;
            label.setText("");
            label.setIcon(new ImageIcon(image));
            makeWorker(timeout).execute();
            return timeout;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDimensionAndLocation(Location location, Dimension dimension) {
        setNotifierDimension(dimension);
        setLocationAt(location, dimension);
    }

    private void setNotifierDimension(Dimension dimension) {
        setComponentSize(window, dimension);
        setComponentSize(panel, dimension);
        setComponentSize(label, dimension);
    }

    private Point findLocation(Location location, Dimension notifierDimension) {
        switch (location) {
            case TOP_RIGHT: return topRightOfTheScreen(screenDimension, notifierDimension);
            case CENTER: return centerOfTheScreen(screenDimension, notifierDimension);
            case TOP_LEFT: return topLeftOfTheScreen();
            case BOTTOM_RIGHT: return bottomRightOfTheScreen(screenDimension, notifierDimension);
            case BOTTOM_LEFT: return bottomLeftOfTheScreen(screenDimension, notifierDimension);
            default: return topRightOfTheScreen(screenDimension, notifierDimension);
        }
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

    private SwingWorker<Void, Void> makeWorker(long timeout) {
        return new SwingWorker<Void, Void>() {
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
    }

    private String htmlDecorated(String text) {
        return  "<html>" +
                "<body style=\"text-align: center; width: 100%; padding: 10px; "+textStyling+"\">" +
                    text +
                "</body>" +
                "</html>";
    }

    private void setLocationAt(Location location, Dimension dimension) {
        window.setLocationRelativeTo(null);
        window.setLocation(findLocation(location, dimension));
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
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        Font verdana = new Font("Trebuchet MS", Font.BOLD, 14);
        label.setFont(verdana);
        label.setForeground(Color.BLACK);
        return label;
    }

    private JPanel makePanel(Dimension dimension) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setForeground(Color.BLACK);
        panel.setLayout(null);
        return panel;
    }

    private void setComponentSize(Component component, Dimension dimension) {
        component.setSize(dimension);
        component.setPreferredSize(dimension);
        component.setBounds(0, 0, dimension.width, dimension.height);
        component.setMaximumSize(dimension);
    }


}