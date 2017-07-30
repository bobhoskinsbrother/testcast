/**
 * @(#)AVIWriter.java  1.0  2011-03-12
 *
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.avi;

import ch.randelshofer.media.Buffer;
import ch.randelshofer.media.MovieWriter;
import ch.randelshofer.media.VideoFormat;
import ch.randelshofer.media.jpeg.JPEGCodec;
import ch.randelshofer.media.png.PNGCodec;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

import static java.lang.Math.max;

/**
 * This class supports writing of images into an AVI 1.0 video file.
 * <p>
 * The images are written as video frames.
 * <p>
 * For convenience, this class has built-in encoders for video frames in the following
 * formats: "JPEG", "PNG", "RAW", "RLE" and "tscc".
 * Media data in other formats, including all audio data, must be encoded before
 * it can be written with {@code AVIWriter}.
 * Alternatively, you can plug in your own codec.
 * </ul>
 * All frames must have the same format.
 * When JPG is used each frame can have an individual encoding quality.
 * <p>
 * All frames in an AVI file must have the same duration. The duration can
 * be set by setting an appropriate pair of values using methods
 * {@link #setFrameRate} and {@link #setTimeScale}.
 * <p>
 * The length of an AVI 1.0 file is limited to 1 GB.
 * This class supports lengths of up to 4 GB, but such files may not work on
 * all players.
 * <p>
 * For detailed information about the AVI RIFF file format see:<br>
 * <a href="http://msdn.microsoft.com/en-us/library/ms779636.aspx">msdn.microsoft.com AVI RIFF</a><br>
 * <a href="http://www.microsoft.com/whdc/archive/fourcc.mspx">www.microsoft.com FOURCC for Video Compression</a><br>
 * <a href="http://www.saettler.com/RIFFMCI/riffmci.html">www.saettler.com RIFF</a><br>
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class AVIWriter extends AbstractAVIStream implements MovieWriter {

    public final static VideoFormat VIDEO_RAW = new VideoFormat(VideoFormat.AVI_DIB);
    public final static VideoFormat VIDEO_JPEG = new VideoFormat(VideoFormat.AVI_MJPG);
    public final static VideoFormat VIDEO_PNG = new VideoFormat(VideoFormat.QT_PNG);
    public final static VideoFormat VIDEO_SCREEN_CAPTURE = new VideoFormat(VideoFormat.AVI_TECHSMITH_SCREEN_CAPTURE);

    /**
     * The states of the movie output stream.
     */
    private static enum States {

        STARTED, FINISHED, CLOSED;
    }
    /**
     * The current state of the movie output stream.
     */
    private States state = States.FINISHED;
    /**
     * This chunk holds the whole AVI content.
     */
    private CompositeChunk aviChunk;
    /**
     * This chunk holds the movie frames.
     */
    private CompositeChunk moviChunk;
    /**
     * This chunk holds the AVI Main Header.
     */
    FixedSizeDataChunk avihChunk;

    /**
     * Creates a new AVI writer.
     *
     * @param file the output file
     */
    public AVIWriter(File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        this.out = new FileImageOutputStream(file);
        this.streamOffset = 0;
    }

    /**
     * Creates a new AVI writer.
     *
     * @param out the output stream.
     */
    public AVIWriter(ImageOutputStream out) throws IOException {
        this.out = out;
        this.streamOffset = out.getStreamPosition();
    }

    /** Adds a video track.
     *
     * @param format The AVI video format.
     * @param timeScale The media time scale.
     * @param frameRate The frame rate in media time scale units.
     * @param width The width of a video image. Must be greater than 0. Overrides
     * the width given in {@code format}.
     * @param height The height of a video image. Must be greater than 0.
     * Overrides the height given in {@code format}.
     * @param depth The number of bits per pixel. Must be greater than 0.
     * Overrides the depth given in {@code format}.
     * @param syncInterval Interval for sync-samples. 0=automatic. 1=all frames
     * are keyframes. Values larger than 1 specify that for every n-th frame
     * is a keyframe.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    public int addVideoTrack(VideoFormat format, long timeScale, long frameRate, int width, int height, int depth, int syncInterval) throws IOException {
        return addVideoTrack(format.getEncoding(), timeScale, frameRate, width, height, depth, syncInterval);
    }

    /** Adds a video track.
     *
     * @param format The AVI video format.
     * @param timeScale The media time scale.
     * @param frameRate The frame rate in media time scale units.
     * @param syncInterval Interval for sync-samples. 0=automatic. 1=all frames
     * are keyframes. Values larger than 1 specify that for every n-th frame
     * is a keyframe.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    public int addVideoTrack(VideoFormat format, long timeScale, long frameRate, int syncInterval) throws IOException {
        return addVideoTrack(format.getEncoding(), timeScale, frameRate, format.getWidth(), format.getHeight(), format.getDepth(), syncInterval);
    }

    /** Adds a video track.
     *
     * @param format The AVI video format.
     * @param timeScale The media time scale.
     * @param frameRate The frame rate in media time scale units.
     * @param syncInterval Interval for sync-samples. 0=automatic. 1=all frames
     * are keyframes. Values larger than 1 specify that for every n-th frame
     * is a keyframe.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    public int addVideoTrack(VideoFormat format, long timeScale, long frameRate, int width, int height) throws IOException {
        return addVideoTrack(format.getEncoding(), timeScale, frameRate, width, height, 24, 24);
    }

    /** Adds a video track.
     *
     * @param fourCC The four character code of the format.
     * @param timeScale The media time scale.
     * @param frameRate The frame rate in media time scale units.
     * @param width The width of a video image. Must be greater than 0.
     * @param height The height of a video image. Must be greater than 0.
     * @param depth The number of bits per pixel. Must be greater than 0.
     * @param syncInterval Interval for sync-samples. 0=automatic. 1=all frames
     * are keyframes. Values larger than 1 specify that for every n-th frame
     * is a keyframe.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    public int addVideoTrack(String fourCC, long timeScale, long frameRate, int width, int height, int depth, int syncInterval) throws IOException {
        VideoTrack vt = new VideoTrack(tracks.size(), fourCC);
        vt.videoFormat = new VideoFormat(fourCC, byte[].class, width, height, depth);
        vt.timeScale = timeScale;
        vt.frameRate = frameRate;
        vt.syncInterval = syncInterval;
        vt.rcFrame = new Rectangle(0, 0, width, height);

        vt.samples = new LinkedList<Sample>();

        if (vt.videoFormat.getDepth() == 4) {
            byte[] gray = new byte[16];
            for (int i = 0; i < gray.length; i++) {
                gray[i] = (byte) ((i << 4) | i);
            }
            vt.palette = new IndexColorModel(4, 16, gray, gray, gray);
        } else if (vt.videoFormat.getDepth() == 8) {
            byte[] gray = new byte[256];
            for (int i = 0; i < gray.length; i++) {
                gray[i] = (byte) i;
            }
            vt.palette = new IndexColorModel(8, 256, gray, gray, gray);
        }
        createCodec(vt);

        tracks.add(vt);
        return tracks.size() - 1;
    }

    /** Sets the global color palette. */
    public void setPalette(int track, IndexColorModel palette) {
        ((VideoTrack) tracks.get(track)).palette = palette;
    }

    /**
     * Sets the compression quality of the video track.
     * A value of 0 stands for "high compression is important" a value of
     * 1 for "high image quality is important".
     * <p>
     * Changing this value affects frames which are subsequently written
     * to the AVIOutputStream. Frames which have already been written
     * are not changed.
     * <p>
     * This value has only effect on videos encoded with JPG format.
     * <p>
     * The default value is 0.9.
     *
     * @param newValue
     */
    public void setCompressionQuality(int track, float newValue) {
        VideoTrack vt = (VideoTrack) tracks.get(track);
        vt.videoQuality = newValue;
        if (vt.codec != null) {
            vt.codec.setQuality(newValue);
        }
    }

    /**
     * Returns the video compression quality.
     *
     * @return video compression quality
     */
    public float getVideoCompressionQuality(int track) {
        return ((VideoTrack) tracks.get(track)).videoQuality;
    }

    /**
     * Sets the dimension of the video track.
     * <p>
     * You need to explicitly set the dimension, if you add all frames from
     * files or input streams.
     * <p>
     * If you add frames from buffered images, then AVIOutputStream
     * can determine the video dimension from the image width and height.
     *
     * @param width Must be greater than 0.
     * @param height Must be greater than 0.
     */
    public void setVideoDimension(int track, int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("width and height must be greater zero.");
        }
        VideoTrack vt = (VideoTrack) tracks.get(track);
        vt.videoFormat = new VideoFormat(vt.videoFormat.getEncoding(), byte[].class, width, height, vt.videoFormat.getDepth());
    }

    /**
     * Gets the dimension of the video track.
     * <p>
     * Returns null if the dimension is not known.
     */
    public Dimension getVideoDimension(int track) {
        VideoTrack vt = (VideoTrack) tracks.get(track);
        VideoFormat fmt = vt.videoFormat;
        return new Dimension(fmt.getWidth(), fmt.getHeight());
    }

    /**
     * Sets the state of the QuickTimeOutpuStream to started.
     * <p>
     * If the state is changed by this method, the prolog is
     * written.
     */
    private void ensureStarted() throws IOException {
        if (state != States.STARTED) {
            writeProlog();
            state = States.STARTED;
        }
    }

    /**
     * Encodes an image as a video frame and writes it into a video track.
     *
     * @param track The track index.
     * @param image The image of the video frame.
     * @param duration This parameter is ignored. An an AVI file, all frames
     * must have the same duration.
     *
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws if the duration is less than 1, or if the dimension of the frame
     * does not match the dimension of the video.
     * @throws UnsupportedOperationException if the {@code MovieWriter} does not have
     * a built-in encoder for this video format.
     * @throws IOException if writing the sample data failed.
     */
    @Override
    public void writeFrame(int track, BufferedImage image, long duration) throws IOException {
        ensureStarted();

        VideoTrack vt = (VideoTrack) tracks.get(track);
        if (vt.codec == null) {
            throw new UnsupportedOperationException("No codec for this video format.");
        }

        // The dimension of the image must match the dimension of the video track
        VideoFormat fmt = vt.videoFormat;
        if (fmt.getWidth() != image.getWidth() || fmt.getHeight() != image.getHeight()) {
            throw new IllegalArgumentException("Dimensions of image[" + vt.samples.size()
                    + "] (width=" + image.getWidth() + ", height=" + image.getHeight()
                    + ") differs from image[0] (width="
                    + fmt.getWidth() + ", height=" + fmt.getHeight());
        }

        // Encode palette data
        {
            DataChunk videoFrameChunk;
            long offset = getRelativeStreamPosition();
            switch (fmt.getDepth()) {
                case 4: {
                    IndexColorModel imgPalette = (IndexColorModel) image.getColorModel();
                    int[] imgRGBs = new int[16];
                    imgPalette.getRGBs(imgRGBs);
                    int[] previousRGBs = new int[16];
                    if (vt.previousPalette == null) {
                        vt.previousPalette = vt.palette;
                    }
                    vt.previousPalette.getRGBs(previousRGBs);
                    if (!Arrays.equals(imgRGBs, previousRGBs)) {
                        vt.previousPalette = imgPalette;
                        DataChunk paletteChangeChunk = new DataChunk(vt.twoCC + "pc");
                        /*
                        int first = imgPalette.getMapSize();
                        int last = -1;
                        for (int i = 0; i < 16; i++) {
                        if (previousRGBs[i] != imgRGBs[i] && i < first) {
                        first = i;
                        }
                        if (previousRGBs[i] != imgRGBs[i] && i > last) {
                        last = i;
                        }
                        }*/
                        int first = 0;
                        int last = imgPalette.getMapSize() - 1;
                        /*
                         * typedef struct {
                        BYTE         bFirstEntry;
                        BYTE         bNumEntries;
                        WORD         wFlags;
                        PALETTEENTRY peNew[];
                        } AVIPALCHANGE;
                         *
                         * typedef struct tagPALETTEENTRY {
                        BYTE peRed;
                        BYTE peGreen;
                        BYTE peBlue;
                        BYTE peFlags;
                        } PALETTEENTRY;
                         */
                        DataChunkOutputStream pOut = paletteChangeChunk.getOutputStream();
                        pOut.writeByte(first);//bFirstEntry
                        pOut.writeByte(last - first + 1);//bNumEntries
                        pOut.writeShort(0);//wFlags

                        for (int i = first; i <= last; i++) {
                            pOut.writeByte((imgRGBs[i] >>> 16) & 0xff); // red
                            pOut.writeByte((imgRGBs[i] >>> 8) & 0xff); // green
                            pOut.writeByte(imgRGBs[i] & 0xff); // blue
                            pOut.writeByte(0); // reserved*/
                        }

                        moviChunk.add(paletteChangeChunk);
                        paletteChangeChunk.finish();
                        long length = getRelativeStreamPosition() - offset;
                        vt.samples.add(new Sample(paletteChangeChunk.chunkType, 0, offset, length - 8, false));
                        offset = getRelativeStreamPosition();
                    }
                    break;
                }
                case 8: {
                    IndexColorModel imgPalette = (IndexColorModel) image.getColorModel();
                    int[] imgRGBs = new int[256];
                    imgPalette.getRGBs(imgRGBs);
                    int[] previousRGBs = new int[256];
                    if (vt.previousPalette == null) {
                        vt.previousPalette = vt.palette;
                    }
                    vt.previousPalette.getRGBs(previousRGBs);
                    if (!Arrays.equals(imgRGBs, previousRGBs)) {
                        vt.previousPalette = imgPalette;
                        DataChunk paletteChangeChunk = new DataChunk(vt.twoCC + "pc");
                        /*
                        int first = imgPalette.getMapSize();
                        int last = -1;
                        for (int i = 0; i < 16; i++) {
                        if (previousRGBs[i] != imgRGBs[i] && i < first) {
                        first = i;
                        }
                        if (previousRGBs[i] != imgRGBs[i] && i > last) {
                        last = i;
                        }
                        }*/
                        int first = 0;
                        int last = imgPalette.getMapSize() - 1;
                        /*
                         * typedef struct {
                        BYTE         bFirstEntry;
                        BYTE         bNumEntries;
                        WORD         wFlags;
                        PALETTEENTRY peNew[];
                        } AVIPALCHANGE;
                         *
                         * typedef struct tagPALETTEENTRY {
                        BYTE peRed;
                        BYTE peGreen;
                        BYTE peBlue;
                        BYTE peFlags;
                        } PALETTEENTRY;
                         */
                        DataChunkOutputStream pOut = paletteChangeChunk.getOutputStream();
                        pOut.writeByte(first);//bFirstEntry
                        pOut.writeByte(last - first + 1);//bNumEntries
                        pOut.writeShort(0);//wFlags

                        for (int i = first; i <= last; i++) {
                            pOut.writeByte((imgRGBs[i] >>> 16) & 0xff); // red
                            pOut.writeByte((imgRGBs[i] >>> 8) & 0xff); // green
                            pOut.writeByte(imgRGBs[i] & 0xff); // blue
                            pOut.writeByte(0); // reserved*/
                        }

                        moviChunk.add(paletteChangeChunk);
                        paletteChangeChunk.finish();
                        long length = getRelativeStreamPosition() - offset;
                        vt.samples.add(new Sample(paletteChangeChunk.chunkType, 0, offset, length - 8, false));
                        offset = getRelativeStreamPosition();
                    }
                    break;
                }
            }
        }

        // Encode pixel data
        {
            if (vt.outputBuffer == null) {
                vt.outputBuffer = new Buffer();
            }

            boolean isSync = vt.syncInterval == 0 ? false : vt.samples.size() % vt.syncInterval == 0;

            Buffer inputBuffer = new Buffer();
            inputBuffer.flags = (isSync) ? Buffer.FLAG_KEY_FRAME : 0;
            inputBuffer.data = image;
            vt.codec.process(inputBuffer, vt.outputBuffer);
            if (vt.outputBuffer.flags == Buffer.FLAG_DISCARD) {
                return;
            }

            isSync = (vt.outputBuffer.flags & Buffer.FLAG_KEY_FRAME) != 0;

            long offset = getRelativeStreamPosition();

            DataChunk videoFrameChunk = new DataChunk(
                    isSync ? vt.twoCC + "db" : vt.twoCC + "dc");
            moviChunk.add(videoFrameChunk);
            videoFrameChunk.getOutputStream().write((byte[]) vt.outputBuffer.data, vt.outputBuffer.offset, vt.outputBuffer.length);
            videoFrameChunk.finish();
            long length = getRelativeStreamPosition() - offset;

            vt.samples.add(new Sample(videoFrameChunk.chunkType, (int) vt.frameRate, offset, length - 8, isSync));
            if (getRelativeStreamPosition() > 1L << 32) {
                throw new IOException("AVI file is larger than 4 GB");
            }
        }
    }

    private void createCodec(VideoTrack vt) {
        VideoFormat fmt = vt.videoFormat;
        String enc = fmt.getEncoding();
        if (enc.equals(VideoFormat.AVI_MJPG)) {
            vt.codec = new JPEGCodec();
        } else if (enc.equals(VideoFormat.AVI_PNG)) {
            vt.codec = new PNGCodec();
        } else if (enc.equals(VideoFormat.AVI_DIB)) {
            vt.codec = new DIBCodec();
        } else if (enc.equals(VideoFormat.AVI_RLE)) {
            vt.codec = new RunLengthCodec();
        } else if (enc.equals(VideoFormat.AVI_TECHSMITH_SCREEN_CAPTURE)) {
            vt.codec = new TechSmithCodec();
        }

        vt.codec.setInputFormat(new VideoFormat(enc, BufferedImage.class, fmt.getWidth(), fmt.getHeight(), fmt.getDepth()));
        vt.codec.setOutputFormat(new VideoFormat(enc, byte[].class, fmt.getWidth(), fmt.getHeight(), fmt.getDepth()));
        vt.codec.setQuality(vt.videoQuality);
    }

    /**
     * Writes a frame from a file to the video track.
     * <p>
     * This method does not inspect the contents of the file.
     * For example, Its your responsibility to only add JPG files if you have
     * chosen the JPEG video format.
     * <p>
     * If you add all frames from files or from input streams, then you
     * have to explicitly set the dimension of the video track before you
     * call finish() or close().
     *
     * @param file The file which holds the image data.
     *
     * @throws IllegalStateException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeFrame(int track, File file) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            writeFrame(track, in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Writes a frame to the video track.
     * <p>
     * This method does not inspect the contents of the file.
     * For example, its your responsibility to only add JPG files if you have
     * chosen the JPEG video format.
     * <p>
     * If you add all frames from files or from input streams, then you
     * have to explicitly set the dimension of the video track before you
     * call finish() or close().
     *
     * @param in The input stream which holds the image data.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeFrame(int track, InputStream in) throws IOException {
        ensureStarted();

        VideoTrack vt = (VideoTrack) tracks.get(track);

        DataChunk videoFrameChunk = new DataChunk(
                vt.videoFormat.getEncoding().equals(VideoFormat.AVI_DIB) ? vt.twoCC + "db" : vt.twoCC + "dc");
        moviChunk.add(videoFrameChunk);
        OutputStream mdatOut = videoFrameChunk.getOutputStream();
        long offset = getRelativeStreamPosition();
        byte[] buf = new byte[512];
        int len;
        while ((len = in.read(buf)) != -1) {
            mdatOut.write(buf, 0, len);
        }
        long length = getRelativeStreamPosition() - offset;
        videoFrameChunk.finish();
        vt.samples.add(new Sample(videoFrameChunk.chunkType, (int) vt.frameRate, offset, length - 8, true));
        if (getRelativeStreamPosition() > 1L << 32) {
            throw new IOException("AVI file is larger than 4 GB");
        }
    }

    /**
     * Writes an already encoded sample from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @param duration The duration of the sample in media time scale units.
     * @param isSync Whether the sample is a sync sample (keyframe).
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    @Override
    public void writeSample(int track, byte[] data, int off, int len, long duration, boolean isSync) throws IOException {
        ensureStarted();
        Track t = tracks.get(track);
        DataChunk dc;
        if (t instanceof VideoTrack) {
            VideoTrack vt = (VideoTrack) t;
            dc = new DataChunk(
                    vt.videoFormat.getEncoding().equals(VideoFormat.AVI_DIB) ? vt.twoCC + "db" : vt.twoCC + "dc");
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
        moviChunk.add(dc);
        OutputStream mdatOut = dc.getOutputStream();
        long offset = getRelativeStreamPosition();
        mdatOut.write(data, off, len);
        long length = getRelativeStreamPosition() - offset;
        dc.finish();
        t.samples.add(new Sample(dc.chunkType, (int) t.frameRate, offset, length - 8, true));
        if (getRelativeStreamPosition() > 1L << 32) {
            throw new IOException("AVI file is larger than 4 GB");
        }
    }

    /**
     * Writes multiple samples from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the data. The
     * contents has to match the format and dimensions of the media in this
     * track.
     *
     * @param track The track index.
     * @param sampleCount The number of samples.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write. Must be dividable by sampleCount.
     * @param sampleDuration The duration of a sample. All samples must
     * have the same duration.
     * @param isSync Whether the samples are sync samples. All samples must
     * either be sync samples or non-sync samples.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    @Override
    public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration, boolean isSync) throws IOException {
        for (int i=0;i<sampleCount;i++) {
            writeSample(track, data, off, len/sampleCount, sampleDuration, isSync);
            off+=len/sampleCount;
        }
    }

    /**
     * Closes the movie file as well as the stream being filtered.
     *
     * @exception IOException if an I/O error has occurred
     */
    @Override
    public void close() throws IOException {
        if (state == States.STARTED) {
            finish();
        }
        if (state != States.CLOSED) {
            out.close();
            state = States.CLOSED;
        }
    }

    /**
     * Finishes writing the contents of the AVI output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     *
     * @exception IllegalStateException if the dimension of the video track
     * has not been specified or determined yet.
     * @exception IOException if an I/O exception has occurred
     */
    public void finish() throws IOException {
        ensureOpen();
        if (state != States.FINISHED) {
            for (Track tr : tracks) {
                if (tr instanceof VideoTrack) {
                    VideoTrack vt = (VideoTrack) tr;
                    VideoFormat fmt = vt.videoFormat;
                    if (fmt.getWidth() == -1 || fmt.getHeight() == -1) {
                        throw new IllegalStateException("image width and height must be specified");
                    }
                }
            }
            moviChunk.finish();
            writeEpilog();
            state = States.FINISHED;
        }
    }

    /**
     * Check to make sure that this stream has not been closed
     */
    private void ensureOpen() throws IOException {
        if (state == States.CLOSED) {
            throw new IOException("Stream closed");
        }
    }

    /** Returns false because AVI does not support variable frame rates. */
    @Override
    public boolean isVFRSupported() {
        return false;
    }

    /** Returns true if the limit for media samples has been reached.
     * If this limit is reached, no more samples should be added to the movie.
     * <p>
     * AVI 1.0 files have a file size limit of 2 GB. This method returns true
     * if a file size of 1.8 GB has been reached.
     */
    @Override
    public boolean isDataLimitReached() {
        try {
            return getRelativeStreamPosition() > (long) (1.8 * 1024 * 1024 * 1024);
        } catch (IOException ex) {
            return true;
        }
    }

    private void writeProlog() throws IOException {
        // The file has the following structure:
        //
        // .RIFF AVI
        // ..avih (AVI Header Chunk)
        // ..LIST strl
        // ...strh (Stream Header Chunk)
        // ...strf (Stream Format Chunk)
        // ..LIST movi
        // ...00dc (Compressed video data chunk in Track 00, repeated for each frame)
        // ..idx1 (List of video data chunks and their location in the file)

        // The RIFF AVI Chunk holds the complete movie
        aviChunk = new CompositeChunk("RIFF", "AVI ");
        CompositeChunk hdrlChunk = new CompositeChunk("LIST", "hdrl");

        // Write empty AVI Main Header Chunk - we fill the data in later
        aviChunk.add(hdrlChunk);
        avihChunk = new FixedSizeDataChunk("avih", 56);
        avihChunk.seekToEndOfChunk();
        hdrlChunk.add(avihChunk);

        CompositeChunk strlChunk = new CompositeChunk("LIST", "strl");
        hdrlChunk.add(strlChunk);

        // Write empty AVI Stream Header Chunk - we fill the data in later
        for (Track tr : tracks) {
            if (tr instanceof VideoTrack) {
                VideoTrack vt = (VideoTrack) tr;
                vt.strhChunk = new FixedSizeDataChunk("strh", 56);
                vt.strhChunk.seekToEndOfChunk();
                strlChunk.add(vt.strhChunk);
                vt.strfChunk = new FixedSizeDataChunk("strf", vt.palette == null ? 40 : 40 + vt.palette.getMapSize() * 4);
                vt.strfChunk.seekToEndOfChunk();
                strlChunk.add(vt.strfChunk);
            } else {
                throw new UnsupportedOperationException("Track type not implemented yet.");
            }
        }

        moviChunk = new CompositeChunk("LIST", "movi");
        aviChunk.add(moviChunk);


    }

    private void writeEpilog() throws IOException {
        // Write empty AVI Stream Header Chunk - we fill the data in later
        long largestBufferSize = 0;
        // Compute values
        long duration = 0;
        for (Track tr : tracks) {
            if (tr instanceof VideoTrack) {

                VideoTrack vt = (VideoTrack) tr;

                long trackDuration = 0;
                for (Sample s : vt.samples) {
                    trackDuration += s.duration;
                }
                duration = max(duration, trackDuration);
                for (Sample s : vt.samples) {
                    if (s.length > largestBufferSize) {
                        largestBufferSize = s.length;
                    }
                }
            }
        }


        DataChunkOutputStream d;

        /* Create Idx1 Chunk and write data
         * -------------
        typedef struct _avioldindex {
        FOURCC  fcc;
        DWORD   cb;
        struct _avioldindex_entry {
        DWORD   dwChunkId;
        DWORD   dwFlags;
        DWORD   dwOffset;
        DWORD   dwSize;
        } aIndex[];
        } AVIOLDINDEX;
         */
        DataChunk idx1Chunk = new DataChunk("idx1");
        aviChunk.add(idx1Chunk);
        d = idx1Chunk.getOutputStream();
        long moviListOffset = moviChunk.offset + 8;
        //moviListOffset = 0;
        for (Track tr : tracks) {
            if (tr instanceof VideoTrack) {

                VideoTrack vt = (VideoTrack) tr;
                for (Sample f : vt.samples) {

                    d.writeType(f.chunkType); // dwChunkId
                    // Specifies a FOURCC that identifies a stream in the AVI file. The
                    // FOURCC must have the form 'xxyy' where xx is the stream number and yy
                    // is a two-character code that identifies the contents of the stream:
                    //
                    // Two-character code   Description
                    //  db                  Uncompressed video frame
                    //  dc                  Compressed video frame
                    //  pc                  Palette change
                    //  wb                  Audio data

                    d.writeUInt((f.chunkType.endsWith("pc") ? 0x100 : 0x0)//
                            | (f.isSync ? 0x10 : 0x0)); // dwFlags
                    // Specifies a bitwise combination of zero or more of the following
                    // flags:
                    //
                    // Value    Name            Description
                    // 0x10     AVIIF_KEYFRAME  The data chunk is a key frame.
                    // 0x1      AVIIF_LIST      The data chunk is a 'rec ' list.
                    // 0x100    AVIIF_NO_TIME   The data chunk does not affect the timing of the
                    //                          stream. For example, this flag should be set for
                    //                          palette changes.

                    d.writeUInt(f.offset - moviListOffset); // dwOffset
                    // Specifies the location of the data chunk in the file. The value
                    // should be specified as an offset, in bytes, from the start of the
                    // 'movi' list; however, in some AVI files it is given as an offset from
                    // the start of the file.

                    d.writeUInt(f.length); // dwSize
                    // Specifies the size of the data chunk, in bytes.
                }
            } else {
                throw new UnsupportedOperationException("Track type not yet implemented.");
            }
        }
        idx1Chunk.finish();

        /* Write Data into AVI Main Header Chunk
         * -------------
         * The AVIMAINHEADER structure defines global information in an AVI file.
         * see http://msdn.microsoft.com/en-us/library/ms779632(VS.85).aspx
        typedef struct _avimainheader {
        FOURCC fcc;
        DWORD  cb;
        DWORD  dwMicroSecPerFrame;
        DWORD  dwMaxBytesPerSec;
        DWORD  dwPaddingGranularity;
        DWORD  dwFlags;
        DWORD  dwTotalFrames;
        DWORD  dwInitialFrames;
        DWORD  dwStreams;
        DWORD  dwSuggestedBufferSize;
        DWORD  dwWidth;
        DWORD  dwHeight;
        DWORD  dwReserved[4];
        } AVIMAINHEADER; */
        avihChunk.seekToStartOfData();
        d = avihChunk.getOutputStream();

        // FIXME compute dwMicroSecPerFrame properly!
        Track tt = tracks.get(0);

        d.writeUInt((1000000L * (long) tt.timeScale) / (long) tt.frameRate); // dwMicroSecPerFrame
        // Specifies the number of microseconds between frames.
        // This value indicates the overall timing for the file.

        d.writeUInt(0); // dwMaxBytesPerSec
        // Specifies the approximate maximum data rate of the file.
        // This value indicates the number of bytes per second the system
        // must handle to present an AVI sequence as specified by the other
        // parameters contained in the main header and stream header chunks.

        d.writeUInt(0); // dwPaddingGranularity
        // Specifies the alignment for data, in bytes. Pad the data to multiples
        // of this value.

        d.writeUInt(0x10 | 0x20); // dwFlags (0x10 == hasIndex, 0x20=mustUseIndex)
        // Contains a bitwise combination of zero or more of the following
        // flags:
        //
        // Value   Name         Description
        // 0x10    AVIF_HASINDEX Indicates the AVI file has an index.
        // 0x20    AVIF_MUSTUSEINDEX Indicates that application should use the
        //                      index, rather than the physical ordering of the
        //                      chunks in the file, to determine the order of
        //                      presentation of the data. For example, this flag
        //                      could be used to create a list of frames for
        //                      editing.
        // 0x100   AVIF_ISINTERLEAVED Indicates the AVI file is interleaved.
        // 0x1000  AVIF_WASCAPTUREFILE Indicates the AVI file is a specially
        //                      allocated file used for capturing real-time
        //                      video. Applications should warn the user before
        //                      writing over a file with this flag set because
        //                      the user probably defragmented this file.
        // 0x20000 AVIF_COPYRIGHTED Indicates the AVI file contains copyrighted
        //                      data and software. When this flag is used,
        //                      software should not permit the data to be
        //                      duplicated.

        long dwTotalFrames = 0;
        for (Track t : tracks) {
            dwTotalFrames += t.samples.size();
        }
        d.writeUInt(dwTotalFrames); // dwTotalFrames
        // Specifies the total number of frames of data in the file.

        d.writeUInt(0); // dwInitialFrames
        // Specifies the initial frame for interleaved files. Noninterleaved
        // files should specify zero. If you are creating interleaved files,
        // specify the number of frames in the file prior to the initial frame
        // of the AVI sequence in this member.
        // To give the audio driver enough audio to work with, the audio data in
        // an interleaved file must be skewed from the video data. Typically,
        // the audio data should be moved forward enough frames to allow
        // approximately 0.75 seconds of audio data to be preloaded. The
        // dwInitialRecords member should be set to the number of frames the
        // audio is skewed. Also set the same value for the dwInitialFrames
        // member of the AVISTREAMHEADER structure in the audio stream header

        d.writeUInt(1); // dwStreams
        // Specifies the number of streams in the file. For example, a file with
        // audio and video has two streams.

        d.writeUInt(largestBufferSize); // dwSuggestedBufferSize
        // Specifies the suggested buffer size for reading the file. Generally,
        // this size should be large enough to contain the largest chunk in the
        // file. If set to zero, or if it is too small, the playback software
        // will have to reallocate memory during playback, which will reduce
        // performance. For an interleaved file, the buffer size should be large
        // enough to read an entire record, and not just a chunk.
        {
            VideoTrack vt = null;
            for (Track t : tracks) {
                if (t instanceof VideoTrack) {
                    vt = (VideoTrack) t;
                    break;
                }
            }
            VideoFormat fmt = vt.videoFormat;
            d.writeUInt(vt == null ? 0 : fmt.getWidth()); // dwWidth
            // Specifies the width of the AVI file in pixels.

            d.writeUInt(vt == null ? 0 : fmt.getHeight()); // dwHeight
            // Specifies the height of the AVI file in pixels.
        }
        d.writeUInt(0); // dwReserved[0]
        d.writeUInt(0); // dwReserved[1]
        d.writeUInt(0); // dwReserved[2]
        d.writeUInt(0); // dwReserved[3]
        // Reserved. Set this array to zero.

        for (Track tr : tracks) {
            /* Write Data into AVI Stream Header Chunk
             * -------------
             * The AVISTREAMHEADER structure contains information about one stream
             * in an AVI file.
             * see http://msdn.microsoft.com/en-us/library/ms779638(VS.85).aspx
            typedef struct _avistreamheader {
            FOURCC fcc;
            DWORD  cb;
            FOURCC fccType;
            FOURCC fccHandler;
            DWORD  dwFlags;
            WORD   wPriority;
            WORD   wLanguage;
            DWORD  dwInitialFrames;
            DWORD  dwScale;
            DWORD  dwRate;
            DWORD  dwStart;
            DWORD  dwLength;
            DWORD  dwSuggestedBufferSize;
            DWORD  dwQuality;
            DWORD  dwSampleSize;
            struct {
            short int left;
            short int top;
            short int right;
            short int bottom;
            }  rcFrame;
            } AVISTREAMHEADER;
             */
            tr.strhChunk.seekToStartOfData();
            d = tr.strhChunk.getOutputStream();
            d.writeType(tr.mediaType.fccType); // fccType - vids for video stream
            // Contains a FOURCC that specifies the type of the data contained in
            // the stream. The following standard AVI values for video and audio are
            // defined:
            //
            // FOURCC   Description
            // 'auds'   Audio stream
            // 'mids'   MIDI stream
            // 'txts'   Text stream
            // 'vids'   Video stream
            d.writeType(tr.fourCC);
            // Optionally, contains a FOURCC that identifies a specific data
            // handler. The data handler is the preferred handler for the stream.
            // For audio and video streams, this specifies the codec for decoding
            // the stream.

            if (tr instanceof VideoTrack && ((VideoTrack) tr).videoFormat.getDepth() <= 8) {
                d.writeUInt(0x00010000); // dwFlags - AVISF_VIDEO_PALCHANGES
            } else {
                d.writeUInt(0); // dwFlags
            }

            // Contains any flags for the data stream. The bits in the high-order
            // word of these flags are specific to the type of data contained in the
            // stream. The following standard flags are defined:
            //
            // Value    Name        Description
            //          AVISF_DISABLED 0x00000001 Indicates this stream should not
            //                      be enabled by default.
            //          AVISF_VIDEO_PALCHANGES 0x00010000
            //                      Indicates this video stream contains
            //                      palette changes. This flag warns the playback
            //                      software that it will need to animate the
            //                      palette.

            d.writeUShort(0); // wPriority
            // Specifies priority of a stream type. For example, in a file with
            // multiple audio streams, the one with the highest priority might be
            // the default stream.

            d.writeUShort(0); // wLanguage
            // Language tag.

            d.writeUInt(0); // dwInitialFrames
            // Specifies how far audio data is skewed ahead of the video frames in
            // interleaved files. Typically, this is about 0.75 seconds. If you are
            // creating interleaved files, specify the number of frames in the file
            // prior to the initial frame of the AVI sequence in this member. For
            // more information, see the remarks for the dwInitialFrames member of
            // the AVIMAINHEADER structure.

            d.writeUInt(tr.timeScale); // dwScale
            // Used with dwRate to specify the time scale that this stream will use.
            // Dividing dwRate by dwScale gives the number of samples per second.
            // For video streams, this is the frame rate. For audio streams, this
            // rate corresponds to the time needed to play nBlockAlign bytes of
            // audio, which for PCM audio is the just the sample rate.

            d.writeUInt(tr.frameRate); // dwRate
            // See dwScale.

            d.writeUInt(0); // dwStart
            // Specifies the starting time for this stream. The units are defined by
            // the dwRate and dwScale members in the main file header. Usually, this
            // is zero, but it can specify a delay time for a stream that does not
            // start concurrently with the file.

            d.writeUInt(tr.samples.size()); // dwLength
            // Specifies the length of this stream. The units are defined by the
            // dwRate and dwScale members of the stream's header.

            long dwSuggestedBufferSize = 0;
            for (Sample s : tr.samples) {
                if (s.length > dwSuggestedBufferSize) {
                    dwSuggestedBufferSize = s.length;
                }
            }
            d.writeUInt(dwSuggestedBufferSize); // dwSuggestedBufferSize
            // Specifies how large a buffer should be used to read this stream.
            // Typically, this contains a value corresponding to the largest chunk
            // present in the stream. Using the correct buffer size makes playback
            // more efficient. Use zero if you do not know the correct buffer size.

            d.writeInt(-1); // dwQuality
            // Specifies an indicator of the quality of the data in the stream.
            // Quality is represented as a number between 0 and 10,000.
            // For compressed data, this typically represents the value of the
            // quality parameter passed to the compression software. If set to –1,
            // drivers use the default quality value.

            d.writeUInt(0); // dwSampleSize
            // Specifies the size of a single sample of data. This is set to zero
            // if the samples can vary in size. If this number is nonzero, then
            // multiple samples of data can be grouped into a single chunk within
            // the file. If it is zero, each sample of data (such as a video frame)
            // must be in a separate chunk. For video streams, this number is
            // typically zero, although it can be nonzero if all video frames are
            // the same size. For audio streams, this number should be the same as
            // the nBlockAlign member of the WAVEFORMATEX structure describing the
            // audio.

            d.writeUShort(tr instanceof VideoTrack ? ((VideoTrack) tr).rcFrame.x : 0); // rcFrame.left
            d.writeUShort(tr instanceof VideoTrack ? ((VideoTrack) tr).rcFrame.y : 0); // rcFrame.top
            d.writeUShort(tr instanceof VideoTrack ? ((VideoTrack) tr).rcFrame.x + ((VideoTrack) tr).rcFrame.width : 0); // rcFrame.right
            d.writeUShort(tr instanceof VideoTrack ? ((VideoTrack) tr).rcFrame.y + ((VideoTrack) tr).rcFrame.height : 0); // rcFrame.bottom
            // Specifies the destination rectangle for a text or video stream within
            // the movie rectangle specified by the dwWidth and dwHeight members of
            // the AVI main header structure. The rcFrame member is typically used
            // in support of multiple video streams. Set this rectangle to the
            // coordinates corresponding to the movie rectangle to update the whole
            // movie rectangle. Units for this member are pixels. The upper-left
            // corner of the destination rectangle is relative to the upper-left
            // corner of the movie rectangle.

            {
                VideoTrack vt = (VideoTrack) tr;

                /* Write BITMAPINFOHEADR Data into AVI Stream Format Chunk
                /* -------------
                 * see http://msdn.microsoft.com/en-us/library/ms779712(VS.85).aspx
                typedef struct tagBITMAPINFOHEADER {
                DWORD  biSize;
                LONG   biWidth;
                LONG   biHeight;
                WORD   biPlanes;
                WORD   biBitCount;
                DWORD  biCompression;
                DWORD  biSizeImage;
                LONG   biXPelsPerMeter;
                LONG   biYPelsPerMeter;
                DWORD  biClrUsed;
                DWORD  biClrImportant;
                } BITMAPINFOHEADER;
                 */
                tr.strfChunk.seekToStartOfData();
                d = tr.strfChunk.getOutputStream();
                d.writeUInt(40); // biSize
                // Specifies the number of bytes required by the structure. This value
                // does not include the size of the color table or the size of the color
                // masks, if they are appended to the end of structure.

                d.writeInt(vt.videoFormat.getWidth()); // biWidth
                // Specifies the width of the bitmap, in pixels.

                d.writeInt(vt.videoFormat.getHeight()); // biHeight
                // Specifies the height of the bitmap, in pixels.
                //
                // For uncompressed RGB bitmaps, if biHeight is positive, the bitmap is
                // a bottom-up DIB with the origin at the lower left corner. If biHeight
                // is negative, the bitmap is a top-down DIB with the origin at the
                // upper left corner.
                // For YUV bitmaps, the bitmap is always top-down, regardless of the
                // sign of biHeight. Decoders should offer YUV formats with postive
                // biHeight, but for backward compatibility they should accept YUV
                // formats with either positive or negative biHeight.
                // For compressed formats, biHeight must be positive, regardless of
                // image orientation.

                d.writeShort(1); // biPlanes
                // Specifies the number of planes for the target device. This value must
                // be set to 1.

                d.writeShort(vt.videoFormat.getDepth()); // biBitCount
                // Specifies the number of bits per pixel (bpp).  For uncompressed
                // formats, this value is the average number of bits per pixel. For
                // compressed formats, this value is the implied bit depth of the
                // uncompressed image, after the image has been decoded.

                String enc = vt.videoFormat.getEncoding();
                if (enc.equals(VideoFormat.AVI_DIB)) {
                    d.writeInt(0); // biCompression - BI_RGB for uncompressed RGB
                } else if (enc.equals(VideoFormat.AVI_RLE)) {
                    if (vt.videoFormat.getDepth() == 8) {
                        d.writeInt(1); // biCompression - BI_RLE8
                    } else if (vt.videoFormat.getDepth() == 4) {
                        d.writeInt(2); // biCompression - BI_RLE4
                    } else {
                        throw new UnsupportedOperationException("RLE only supports 4-bit and 8-bit images");
                    }
                } else {
                    d.writeType(vt.videoFormat.getEncoding()); // biCompression
                }
                // For compressed video and YUV formats, this member is a FOURCC code,
                // specified as a DWORD in little-endian order. For example, YUYV video
                // has the FOURCC 'VYUY' or 0x56595559. For more information, see FOURCC
                // Codes.
                //
                // For uncompressed RGB formats, the following values are possible:
                //
                // Value        Description
                // BI_RGB       0x00000000 Uncompressed RGB.
                // BI_BITFIELDS 0x00000003 Uncompressed RGB with color masks.
                //                         Valid for 16-bpp and 32-bpp bitmaps.
                //
                // Note that BI_JPG and BI_PNG are not valid video formats.
                //
                // For 16-bpp bitmaps, if biCompression equals BI_RGB, the format is
                // always RGB 555. If biCompression equals BI_BITFIELDS, the format is
                // either RGB 555 or RGB 565. Use the subtype GUID in the AM_MEDIA_TYPE
                // structure to determine the specific RGB type.

                if (enc.equals(VideoFormat.AVI_DIB)) {
                    d.writeInt(0); // biSizeImage
                } else {
                    VideoFormat fmt = vt.videoFormat;
                    if (fmt.getDepth() == 4) {
                        d.writeInt(fmt.getWidth() * fmt.getHeight() / 2); // biSizeImage
                    } else {
                        int bytesPerPixel = Math.max(1, fmt.getDepth() / 8);
                        d.writeInt(fmt.getWidth() * fmt.getHeight() * bytesPerPixel); // biSizeImage
                    }
                }

                // Specifies the size, in bytes, of the image. This can be set to 0 for
                // uncompressed RGB bitmaps.

                d.writeInt(0); // biXPelsPerMeter
                // Specifies the horizontal resolution, in pixels per meter, of the
                // target device for the bitmap.

                d.writeInt(0); // biYPelsPerMeter
                // Specifies the vertical resolution, in pixels per meter, of the target
                // device for the bitmap.

                d.writeInt(vt.palette == null ? 0 : vt.palette.getMapSize()); // biClrUsed
                // Specifies the number of color indices in the color table that are
                // actually used by the bitmap.

                d.writeInt(0); // biClrImportant
                // Specifies the number of color indices that are considered important
                // for displaying the bitmap. If this value is zero, all colors are
                // important.

                if (vt.palette != null) {
                    for (int i = 0, n = vt.palette.getMapSize(); i < n; ++i) {
                        /*
                         * typedef struct tagRGBQUAD {
                        BYTE rgbBlue;
                        BYTE rgbGreen;
                        BYTE rgbRed;
                        BYTE rgbReserved; // This member is reserved and must be zero.
                        } RGBQUAD;
                         */
                        d.write(vt.palette.getBlue(i));
                        d.write(vt.palette.getGreen(i));
                        d.write(vt.palette.getRed(i));
                        d.write(0);
                    }
                }
            }
        }
        // -----------------
        aviChunk.finish();
    }
}
