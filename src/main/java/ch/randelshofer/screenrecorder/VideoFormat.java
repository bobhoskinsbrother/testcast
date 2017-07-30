package ch.randelshofer.screenrecorder;

public enum VideoFormat {

    AVI("avi"), QUICKTIME("mov");

    private String fileExtension;

    VideoFormat(String extension) {
        fileExtension = extension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

}
