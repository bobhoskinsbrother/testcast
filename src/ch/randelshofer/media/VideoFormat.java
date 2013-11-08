/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.randelshofer.media;

/**
 * Defines a video format.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class VideoFormat extends Format {

    /**
     * The width of a video frame.
     */
    private final int width;
    /**
     * The height of a video frame.
     */
    private final int height;
    /**
     * The number of bits per pixel.
     */
    private final int depth;
    /**
     * The data class.
     */
    private final Class dataClass;
    /**
     * The encoding name.
     */
    private final String encoding;
    /**
     * The compressor name.
     */
    private final String compressorName;


    // Standard video encoding strings
    public static final String IMAGE = "image";

    /**
     * Cinepak format.
     */
    public static final String QT_CINEPAK = "cvid";
    /**
     * JPEG format.
     */
    public static final String QT_JPEG = "jpeg";
    public static final String QT_JPEG_COMPRESSOR_NAME = "Photo - JPEG";
    /**
     * PNG format.
     */
    public static final String QT_PNG = "png ";
    public static final String QT_PNG_COMPRESSOR_NAME = "PNG";
    /**
     * Animation format.
     */
    public static final String QT_ANIMATION = "rle ";
    public static final String QT_ANIMATION_COMPRESSOR_NAME = "Animation";
    /**
     * Raw format.
     */
    public static final String QT_RAW = "raw ";
    public static final String QT_RAW_COMPRESSOR_NAME = "NONE";

    // AVI Formats
    /**
     * Microsoft Device Independent Bitmap (DIB) format.
     */
    public static final String AVI_DIB = "DIB ";
    /**
     * Microsoft Run Length format.
     */
    public static final String AVI_RLE = "RLE ";
    /**
     * Techsmith Screen Capture format.
     */
    public static final String AVI_TECHSMITH_SCREEN_CAPTURE = "tscc";
    /**
     * JPEG format.
     */
    public static final String AVI_MJPG = "MJPG";
    /**
     * PNG format.
     */
    public static final String AVI_PNG = "png ";


    public VideoFormat(String encoding, Class dataClass, int width, int height, int depth) {
        this(encoding, encoding, dataClass, width, height, depth);
    }

    public VideoFormat(String encoding, String compressorName, Class dataClass, int width, int height, int depth) {
        this.encoding = encoding;
        this.compressorName = compressorName;
        this.dataClass = dataClass;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public VideoFormat(String encoding, String compressorName) {
        this(encoding, compressorName, null, -1, -1, -1);
    }

    public VideoFormat(String encoding) {
        this(encoding, encoding, null, -1, -1, -1);
    }

    public int getDepth() {
        return depth;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Class getDataClass() {
        return dataClass;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getCompressorName() {
        return compressorName;
    }

}
