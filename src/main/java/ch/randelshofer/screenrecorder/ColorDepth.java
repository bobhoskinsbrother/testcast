package ch.randelshofer.screenrecorder;

public enum ColorDepth {

    DOZENS(8), THOUSANDS(16), MILLIONS(24);
    private int value;

    ColorDepth(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }
}
