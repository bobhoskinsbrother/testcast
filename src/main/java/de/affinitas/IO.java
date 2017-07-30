package de.affinitas;

import java.io.File;

public class IO {


    private final String directoryName;

    public IO(String directoryName) {
        this.directoryName = directoryName;
    }

    public String makeFileName(String fileName) {
        return makeFileName(fileName, 0);
    }

    public void makeDirectoryIfNotExists() {
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private String makeFileName(final String fileName, int version) {
        String reply = "";
        if (version == 0) {
            reply = directoryName + "/" + fileName + ".avi";
        } else {
            reply = directoryName + "/" + fileName + "(" + version + ").avi";
        }
        if (fileExists(fileName)) {
            return makeFileName(fileName, version + 1);
        }
        return reply;
    }

    boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }


}
