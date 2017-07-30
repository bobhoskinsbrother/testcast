package uk.co.itstherules.snapshot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SnapShotService {

    public SnapShotService() { }

    public void screen(String fileName) {
        try {
            BufferedImage image = createSnapshot();
            writeSnapshot(image, fileName);
        } catch (AWTException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSnapshot(BufferedImage i, String fileName) throws IOException {
        File outputfile = new File(fileName);
        ImageIO.write(i, "jpg", outputfile);
    }

    private BufferedImage createSnapshot() throws AWTException {
        Robot robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screen = new Rectangle(screenSize);
        return robot.createScreenCapture(screen);
    }

}
