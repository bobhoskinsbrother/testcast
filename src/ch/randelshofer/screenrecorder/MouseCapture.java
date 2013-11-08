package ch.randelshofer.screenrecorder;

import java.awt.*;

/**
 * Holds a mouse capture.
 */
class MouseCapture {

    private long time;
    private Point p;

    public long getTime() {
        return time;
    }

    public Point getP() {
        return p;
    }

    public MouseCapture(long time, Point p) {
        this.time = time;
        this.p = p;
    }
}
