package uk.co.itstherules;

import java.io.File;

public class IO {


    private final String directoryName;

    public IO(String directoryName) {
        this.directoryName = directoryName;
    }

    public String makeImageName(String fileName) {
        return makeImageName(fileName, 0);
    }

    public String makeMovieName(String fileName) {
        return makeMovieName(fileName, 0);
    }

    public void makeDirectoryIfNotExists() {
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private String makeImageName(final String fileName, int version) {
        return makeNameWithExtension(fileName, ".jpg", version);
    }

    private String makeMovieName(final String fileName, int version) {
        return makeNameWithExtension(fileName, ".avi", version);

    }

    private String makeNameWithExtension(final String fileName, final String extension, int version) {
        String reply = "";
        if (version == 0) {
            reply = directoryName + "/" + fileName + extension;
        } else {
            reply = directoryName + "/" + fileName + "(" + version + ")" + extension;
        }
        if (fileExists(reply)) {
            return makeNameWithExtension(fileName, extension, version + 1);
        }
        return reply;
    }

    boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }


}
