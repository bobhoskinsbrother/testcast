/*
 * @(#)MovieWriter.java  1.0  2011-03-12
 * 
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Defines an API for objects which can write video and audio data into an
 * output stream.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public interface MovieWriter {

    /**
     * Encodes an image as a video frame and writes it into a video track.
     *
     * @param track The track index.
     * @param image The image of the video frame.
     * @param duration The duration of the video frame in media time scale units.
     *              If the writer does not support variable frame rate,
     *              the duration is ignored.
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws if the duration is less than 1, or if the dimension of the frame
     * does not match the dimension of the video.
     * @throws UnsupportedOperationException if the {@code MovieWriter} does not have
     * a built-in encoder for this video format.
     * @throws IOException if writing the sample data failed.
     */
    public void writeFrame(int track, BufferedImage image, long duration) throws IOException;

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
    public void writeSample(int track, byte[] data, int off, int len, long duration, boolean isSync) throws IOException;

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
    public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration, boolean isSync) throws IOException;

    /** Closes the writer. */
    public void close() throws IOException;

    /** Returns true if the writer supports variable frame rates. */
    public boolean isVFRSupported();

    /** Returns true if the limit for media data has been reached.
     * If this limit is reached, no more samples should be added to the movie.
     * <p>
     * This limit is imposed by data structures of the movie file
     * which will overflow if more samples are added to the movie.
     */
    public boolean isDataLimitReached();
}
