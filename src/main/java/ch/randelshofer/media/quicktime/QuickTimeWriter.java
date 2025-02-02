/**
 * @(#)QuickTimeWriter.java  1.3.5  2011-03-12
 *
 * Copyright (c) 2010-2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.quicktime;

import ch.randelshofer.media.Buffer;
import ch.randelshofer.media.MovieWriter;
import ch.randelshofer.media.VideoFormat;
import ch.randelshofer.media.io.ImageOutputStreamAdapter;
import ch.randelshofer.media.jpeg.JPEGCodec;
import ch.randelshofer.media.png.PNGCodec;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.sound.sampled.AudioFormat;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

import static java.lang.Math.max;

/**
 * Supports writing of time-based video and audio data into a QuickTime movie
 * file (.MOV) without the need of native code.
 * <p>
 * {@code QuickTimeWriter} works with tracks and samples. After creating a
 * {@code QuickTimeWriter} one or more video and audio tracks can be added to
 * it. Then samples can be written into the track(s). A sample is a single
 * element in a sequence of time-ordered data. For video data a sample typically
 * consists of a single video frame, for uncompressed stereo audio data a sample
 * contains one PCM impulse per channel. Samples of compressed media data may encompass larger time units.
 * <p>
 * Tracks support edit lists. An edit list specifies when to play which portion
 * of the media data at what speed. An empty edit can be used to insert an empty
 * time span, for example to offset a track from the start of the movie. Edits
 * can also be used to play the same portion of media data multiple times
 * without having it to store it more than once in the track.<br>
 * Moreover edit lists are useful for lossless cutting of media data at non-sync
 * frames. For example, MP3 layer III audio data can not be cut at arbitrary
 * frames, because audio data can be 'borrowed' from previous frames. An edit
 * list can be used to select the desired portion of the audio data, while the
 * track stores the media starting from the nearest sync frame.
 * <p>
 * Samples are stored in a QuickTime file in the same sequence as they are written.
 * In order to get optimal movie playback, the samples from different tracks
 * should be interleaved from time to time. An interleave should occur about twice
 * per second. Furthermore, to overcome any latencies in sound playback, at
 * least one second of sound data needs to be placed at the beginning of the
 * movie. So that the sound and video data is offset from each other in the file
 * by one second.
 * <p>
 * For convenience, this class has built-in encoders for video frames in the following
 * formats: RAW, ANIMATION, JPEG and PNG. Media data in other formats, including all audio
 * data, must be encoded before it can be written with {@code QuickTimeWriter}.
 * Alternatively, you can plug in your own codec.
 * <p>
 * <b>Example:</b> Writing 10 seconds of a movie with 640x480 pixel, 30 fps,
 * PNG-encoded video and 16-bit stereo, 44100 Hz, PCM-encoded audio.
 * <p>
 * <pre>
 * QuickTimeWriter w = new QuickTimeWriter(new File("mymovie.mov"));
 * w.addAudioTrack(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED), 44100, 2, 16, 2, 44100, true)); // audio in track 0
 * w.addVideoTrack(QuickTimeWriter.VIDEO_PNG, 30, 640, 480);  // video in track 1
 *
 * // calculate total movie duration in media time units for each track
 * long atmax = w.getMediaTimeScale(0) * 10;
 * long vtmax = w.getMediaTimeScale(1) * 10;
 *
 * // duration of a single sample
 * long asduration = 1;
 * long vsduration = 1;
 *
 * // half a second in media time units (we interleave twice per second)
 * long atstep = w.getMediaTimeScale(0) / 2;
 * long vtstep = w.getMediaTimeScale(1) / 2;
 *
 * // the time when the next interleave occurs in media time units
 * long atnext = w.getMediaTimeScale(0); // offset audio by 1 second
 * long vtnext = 0;
 *
 * // the current time in media time units
 * long atime = 0;
 * long vtime = 0;
 *
 * // create buffers
 * int asamplesize = 2 * 2; // 16-bit stereo * 2 channels
 * byte[] audio=new byte[atstep * asamplesize];
 * BufferedImage img=new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
 *
 * // main loop
 * while (atime &lt; atmax || vtime &lt; vtmax) {
 *      atnext = Math.min(atmax, atnext + atstep); // advance audio to next interleave time
 *      while (atime &lt; atnext) { // catch up with audio time
 *          int duration = (int) Math.min(audio.length / asamplesize, atmax - atime);
 *          ...fill in audio data for time "atime" and duration "duration" here...
 *          w.writeSamples(0, duration, audio, 0, duration * asamplesize, asduration);
 *          atime += duration;
 *      }
 *      vtnext = Math.min(vtmax, vtnext + vtstep); // advance video to next interleave time
 *      while (vtime &lt; vtnext) { // catch up with video time
 *          int duration = (int) Math.min(1, vtmax - vtime);
 *          ...fill in image data for time "vtime" and duration "duration" here...
 *          w.writeFrame(1, img, vsduration);
 *          vtime += duration;
 *      }
 * }
 * w.close();
 * </pre>
 * <p>
 * For information about the QuickTime file format see the
 * "QuickTime File Format Specification", Apple Inc. 2010-08-03. (qtff)
 * <a href="http://developer.apple.com/library/mac/documentation/QuickTime/QTFF/qtff.pdf/">
 * http://developer.apple.com/library/mac/documentation/QuickTime/QTFF/qtff.pdf
 * </a>
 *
 * @author Werner Randelshofer
 * @version 1.3.4 2011-03-12 Streamlines the code with {@code AVIWriter}.
 * <br>1.3.3 2011-01-17 Improves writing of compressed movie headers.
 * <br>1.3.2 2011-01-17 Fixes out of bounds exception when writing
 * sub-images with ANIMATION codec. Fixes writing of compressed movie headers.
 * <br>1.3.1 2011-01-09 Fixes broken RAW codec.
 * <br>1.3 2011-01-07 Improves robustness of API.
 *                    Adds method toWebOptimizedMovie().
 * <br>1.2.2 2011-01-07 Reduces file seeking with "ANIMATION" codec.
 * <br>1.2.1 2011-01-07 Fixed default syncInterval for "ANIMATION" video.
 * <br>1.2 2011-01-05 Adds support for "ANIMATION" encoded video.
 * <br>1.1 2011-01-04 Adds "depth" parameter to addVideoTrack method.
 * <br>1.0 2011-01-02 Adds support for edit lists. Adds support for MP3
 * audio format.
 * <br>0.1.1 2010-12-05 Updates the link to the QuickTime file format
 * specification.
 * <br>0.1 2010-09-30 Created.
 */
public class QuickTimeWriter extends AbstractQuickTimeStream implements MovieWriter {

    public final static VideoFormat VIDEO_RAW = new VideoFormat(VideoFormat.QT_RAW, VideoFormat.QT_RAW_COMPRESSOR_NAME);
    public final static VideoFormat VIDEO_ANIMATION = new VideoFormat(VideoFormat.QT_ANIMATION, VideoFormat.QT_ANIMATION_COMPRESSOR_NAME);
    public final static VideoFormat VIDEO_JPEG = new VideoFormat(VideoFormat.QT_JPEG, VideoFormat.QT_JPEG_COMPRESSOR_NAME);
    public final static VideoFormat VIDEO_PNG = new VideoFormat(VideoFormat.QT_PNG, VideoFormat.QT_PNG_COMPRESSOR_NAME);

    /**
     * Creates a new QuickTime writer.
     *
     * @param file the output file
     */
    public QuickTimeWriter(File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        this.out = new FileImageOutputStream(file);
        this.streamOffset = 0;
    }

    /**
     * Creates a new QuickTime writer.
     *
     * @param out the output stream.
     */
    public QuickTimeWriter(ImageOutputStream out) throws IOException {
        this.out = out;
        this.streamOffset = out.getStreamPosition();
    }

    /**
     * Sets the time scale for this movie, that is, the number of time units
     * that pass per second in its time coordinate system.
     * <p>
     * The default value is 600.
     *
     * @param timeScale
     */
    public void setMovieTimeScale(long timeScale) {
        if (timeScale < 1 || timeScale > (2L << 32)) {
            throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
        }
        this.movieTimeScale = timeScale;
    }

    /**
     * Returns the time scale of the movie.
     *
     * @return time scale
     * @see #setMovieTimeScale(long)
     */
    public long getMovieTimeScale() {
        return movieTimeScale;
    }

    /**
     * Returns the time scale of the media in a track.
     *
     * @param track Track index.
     * @return time scale
     * @see #setMovieTimeScale(long)
     */
    public long getMediaTimeScale(int track) {
        return tracks.get(track).mediaTimeScale;
    }

    /**
     * Returns the media duration of a track in the media's time scale.
     *
     * @param track Track index.
     * @return media duration
     */
    public long getMediaDuration(int track) {
        return tracks.get(track).mediaDuration;
    }

    /**
     * Returns the track duration in the movie's time scale without taking
     * the edit list into account.
     * <p>
     * The returned value is the media duration of the track in the movies's time
     * scale.
     *
     * @param track Track index.
     * @return unedited track duration
     */
    public long getUneditedTrackDuration(int track) {
        Track t = tracks.get(track);
        return t.mediaDuration * t.mediaTimeScale / movieTimeScale;
    }

    /**
     * Returns the track duration in the movie's time scale.
     * <p>
     * If the track has an edit-list, the track duration is the sum
     * of all edit durations.
     * <p>
     * If the track does not have an edit-list, then this method returns
     * the media duration of the track in the movie's time scale.
     *
     * @param track Track index.
     * @return track duration
     */
    public long getTrackDuration(int track) {
        return tracks.get(track).getTrackDuration(movieTimeScale);
    }

    /**
     * Returns the total duration of the movie in the movie's time scale.
     *
     * @return media duration
     */
    public long getMovieDuration() {
        long duration = 0;
        for (Track t : tracks) {
            duration = max(duration, t.getTrackDuration(movieTimeScale));
        }
        return duration;
    }

    /** Sets the color table for videos with indexed color models.
     *
     * @param track The track number.
     * @param icm IndexColorModel. Specify null to use the standard Macintosh
     * color table.
     */
    public void setVideoColorTable(int track, IndexColorModel icm) {
        VideoTrack t = (VideoTrack) tracks.get(track);
        t.videoColorTable = icm;
    }

    /** Gets the preferred color table for displaying the movie on devices that
     * support only 256 colors.
     *
     * @param track The track number.
     * @return The color table or null, if the video uses the standard Macintosh
     * color table.
     */
    public IndexColorModel getVideoColorTable(int track) {
        VideoTrack t = (VideoTrack) tracks.get(track);
        return t.videoColorTable;
    }

    /** Sets the edit list for the specified track.
     * <p>
     * In the absence of an edit list, the presentation of the track starts
     * immediately. An empty edit is used to offset the start time of a track.
     * <p>
     * @throws IllegalArgumentException If the edit list ends with an empty edit.
     */
    public void setEditList(int track, Edit[] editList) {
        if (editList != null && editList.length > 0 && editList[editList.length - 1].mediaTime == -1) {
            throw new IllegalArgumentException("Edit list must not end with empty edit.");
        }
        tracks.get(track).editList = editList;
    }

    /** Adds a video track.
     *
     * @param format The QuickTime video format.
     * @param timeScale The media time scale. This is typically the frame rate.
     * If the frame rate is not an integer fraction of a second, specify a
     * multiple of the frame rate and specify a correspondingly multiplied
     * duration when writing frames. For example, for a rate of 23.976 fps
     * specify a time scale of 23976 and multiply the duration of a video frame
     * by 1000.
     * @param width The width of a video image. Must be larger than 0.
     * @param height The height of a video image. Must be larger than 0.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    public int addVideoTrack(VideoFormat format, long timeScale, int width, int height) throws IOException {
        return addVideoTrack(format.getEncoding(), format.getCompressorName(), timeScale, width, height, 24, 30);
    }

    /** Adds a video track.
     *
     * @param format The QuickTime video format.
     * @param timeScale The media time scale. This is typically the frame rate.
     * If the frame rate is not an integer fraction of a second, specify a
     * multiple of the frame rate and specify a correspondingly multiplied
     * duration when writing frames. For example, for a rate of 23.976 fps
     * specify a time scale of 23976 and multiply the duration of a video frame
     * by 1000.
     * @param width The width of a video image. Must be larger than 0.
     * @param height The height of a video image. Must be larger than 0.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    public int addVideoTrack(VideoFormat format, long timeScale, int width, int height, int depth, int syncInterval) throws IOException {
        return addVideoTrack(format.getEncoding(), format.getCompressorName(), timeScale, width, height, depth, syncInterval);
    }

    /** Adds a video track.
     *
     * @param compressionType The QuickTime "image compression format" 4-Character code.
     * A list of supported 4-Character codes is given in qtff, table 3-1, page 96.
     * @param compressorName The QuickTime compressor name. Can be up to 32 characters long.
     * @param timeScale The media time scale between 1 and 2^32.
     * @param width The width of a video frame.
     * @param height The height of a video frame.
     * @param depth The number of bits per pixel.
     * @param syncInterval Interval for sync-samples. 0=automatic. 1=all frames
     * are keyframes. Values larger than 1 specify that for every n-th frame
     * is a keyframe. Apple's QuickTime will not work properly if there is
     * not at least one keyframe every second.
     *
     * @return Returns the track index.
     * 
     * @throws IllegalArgumentException if {@code width} or {@code height} is
     * smaller than 1, if the length of {@code compressionType} is not equal to 4, if
     * the length of the {@code compressorName} is not between 1 and 32,
     * if the tiimeScale is not between 1 and 2^32.
     */
    public int addVideoTrack(String compressionType, String compressorName, long timeScale, int width, int height, int depth, int syncInterval) throws IOException {
        ensureStarted();
        if (compressionType == null || compressionType.length() != 4) {
            throw new IllegalArgumentException("compressionType must be 4 characters long:" + compressionType);
        }
        if (compressorName == null || compressorName.length() < 1 || compressorName.length() > 32) {
            throw new IllegalArgumentException("compressorName must be between 1 and 32 characters long:" + compressionType);
        }
        if (timeScale < 1 || timeScale > (2L << 32)) {
            throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Width and height must be greater than 0, width:" + width + " height:" + height);
        }

        VideoTrack t = new VideoTrack();
        t.mediaCompressionType = compressionType;
        t.mediaCompressorName = compressorName;
        t.mediaTimeScale = timeScale;
        t.videoWidth = width;
        t.videoHeight = height;
        t.videoDepth = depth;
        t.syncInterval = syncInterval;
        t.videoFormat = new VideoFormat(compressionType, compressorName, byte[].class, width, height, depth);
        createCodec(t);
        tracks.add(t);
        return tracks.size() - 1;
    }

    private void createCodec(VideoTrack vt) {
        String enc = vt.videoFormat.getEncoding();
        if (enc.equals(VideoFormat.QT_JPEG)) {
            vt.codec = new JPEGCodec();
        } else if (enc.equals(VideoFormat.QT_PNG)) {
            vt.codec = new PNGCodec();
        } else if (enc.equals(VideoFormat.QT_RAW)) {
            vt.codec = new RawCodec();
        } else if (enc.equals(VideoFormat.QT_ANIMATION)) {
            vt.codec = new AnimationCodec();
        }
        vt.codec.setInputFormat(new VideoFormat(VideoFormat.IMAGE, BufferedImage.class, vt.videoWidth, vt.videoHeight, vt.videoDepth));
        vt.codec.setOutputFormat(new VideoFormat(vt.videoFormat.getEncoding(), vt.videoFormat.getCompressorName(), byte[].class, vt.videoWidth, vt.videoHeight, vt.videoDepth));
        vt.codec.setQuality(vt.videoQuality);
    }

    /** Adds an audio track, and configures it using an
     * {@code AudioFormat} object from the javax.sound API.
     * <p>
     * Use this method for writing audio data from an {@code AudioInputStream}
     * into a QuickTime Movie file.
     *
     * @param format The javax.sound audio format.
     * @return Returns the track index.
     */
    public int addAudioTrack(AudioFormat format) throws IOException {
        ensureStarted();
        String qtAudioFormat;
        double sampleRate = format.getSampleRate();
        long timeScale = (int) Math.floor(sampleRate);
        int sampleSizeInBits = format.getSampleSizeInBits();
        int numberOfChannels = format.getChannels();
        boolean bigEndian = format.isBigEndian();
        int frameDuration = (int) (format.getSampleRate() / format.getFrameRate());
        int frameSize = format.getFrameSize();
        boolean isCompressed = format.getProperty("vbr") != null && ((Boolean) format.getProperty("vbr")).booleanValue();

        if (format.getEncoding().equals(AudioFormat.Encoding.ALAW)) {
            qtAudioFormat = "alaw";
            if (sampleSizeInBits != 8) {
                throw new IllegalArgumentException("Sample size of 8 for ALAW required:" + sampleSizeInBits);
            }
        } else if (format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            switch (sampleSizeInBits) {
                case 16:
                    qtAudioFormat = (bigEndian) ? "twos" : "sowt";
                    break;
                case 24:
                    qtAudioFormat = "in24";
                    break;
                case 32:
                    qtAudioFormat = "in32";
                    break;
                default:
                    throw new IllegalArgumentException("Sample size of 16, 24 or 32 for PCM_SIGNED required:" + sampleSizeInBits);
            }
        } else if (format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            if (sampleSizeInBits != 8) {
                throw new IllegalArgumentException("Sample size of 8 PCM_UNSIGNED required:" + sampleSizeInBits);
            }
            qtAudioFormat = "raw ";
        } else if (format.getEncoding().equals(AudioFormat.Encoding.ULAW)) {
            if (sampleSizeInBits != 8) {
                throw new IllegalArgumentException("Sample size of 8 for ULAW required:" + sampleSizeInBits);
            }
            qtAudioFormat = "ulaw";
        } else if (format.getEncoding().toString().equals("MP3")) {
            qtAudioFormat = ".mp3";
        } else {
            qtAudioFormat = format.getEncoding().toString();
            if (qtAudioFormat.length() != 4) {
                throw new IllegalArgumentException("Unsupported encoding:" + format.getEncoding());
            }
        }

        return addAudioTrack(qtAudioFormat, timeScale, sampleRate,
                numberOfChannels, sampleSizeInBits,
                isCompressed, frameDuration, frameSize);
    }

    /** Adds an audio track.
     *
     * @param compressionType The QuickTime 4-character code.
     * A list of supported 4-Character codes is given in qtff, table 3-7, page 113.
     * @param timeScale The media time scale between 1 and 2^32.
     * @param sampleRate The sample rate. The integer portion must match the
     * {@code timeScale}.
     * @param numberOfChannels The number of channels: 1 for mono, 2 for stereo.
     * @param sampleSizeInBits The number of bits in a sample: 8 or 16.
     * @param isCompressed Whether the sound is compressed.
     * @param frameDuration The frame duration, expressed in the media’s
     *                   timescale, where the timescale is equal to the sample
     *                   rate. For uncompressed formats, this field is always 1.
     * @param frameSize  For uncompressed audio, the number of bytes in a
     *                   sample for a single channel (sampleSize divided by 8).
     *                   For compressed audio, the number of bytes in a frame.
     *
     * @throws IllegalArgumentException if the audioFormat is not 4 characters long,
     * if the time scale is not between 1 and 2^32, if the integer portion of the
     * sampleRate is not equal to the timeScale, if numberOfChannels is not 1 or 2.
     * @return Returns the track index.
     */
    public int addAudioTrack(String compressionType, //
            long timeScale, double sampleRate, //
            int numberOfChannels, int sampleSizeInBits, //
            boolean isCompressed, //
            int frameDuration, int frameSize) throws IOException {
        ensureStarted();
        if (compressionType == null || compressionType.length() != 4) {
            throw new IllegalArgumentException("audioFormat must be 4 characters long:" + compressionType);
        }
        if (timeScale < 1 || timeScale > (2L << 32)) {
            throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
        }
        if (timeScale != (int) Math.floor(sampleRate)) {
            throw new IllegalArgumentException("timeScale: " + timeScale + " must match integer portion of sampleRate: " + sampleRate);
        }
        if (numberOfChannels != 1 && numberOfChannels != 2) {
            throw new IllegalArgumentException("numberOfChannels must be 1 or 2: " + numberOfChannels);
        }
        if (sampleSizeInBits != 8 && sampleSizeInBits != 16) {
            throw new IllegalArgumentException("sampleSize must be 8 or 16: " + numberOfChannels);
        }

        AudioTrack t = new AudioTrack();
        t.mediaCompressionType = compressionType;
        t.mediaTimeScale = timeScale;
        t.soundSampleRate = sampleRate;
        t.soundCompressionId = isCompressed ? -2 : -1;
        t.soundNumberOfChannels = numberOfChannels;
        t.soundSampleSize = sampleSizeInBits;
        t.soundSamplesPerPacket = frameDuration;
        if (isCompressed) {
            t.soundBytesPerPacket = frameSize;
            t.soundBytesPerFrame = frameSize * numberOfChannels;
        } else {
            t.soundBytesPerPacket = frameSize / numberOfChannels;
            t.soundBytesPerFrame = frameSize;
        }
        t.soundBytesPerSample = sampleSizeInBits / 8;
        tracks.add(t);
        return tracks.size() - 1;
    }

    /**
     * Sets the compression quality of a  track.
     * <p>
     * A value of 0 stands for "high compression is important" a value of
     * 1 for "high image quality is important".
     * <p>
     * Changing this value affects the encoding of video frames which are
     * subsequently written into the track. Frames which have already been written
     * are not changed.
     * <p>
     * This value has no effect on videos encoded with lossless encoders
     * such as the PNG format.
     * <p>
     * The default value is 0.97.
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
     * Returns the compression quality of a track.
     *
     * @return compression quality
     */
    public float getCompressionQuality(int track) {
        return ((VideoTrack) tracks.get(track)).videoQuality;
    }

    /** Sets the sync interval for the specified video track. 
     * @param track The track number.
     * @param i Interval between sync samples (keyframes).
     * 0 = automatic.
     * 1 = write all samples as sync samples.
     * n = sync every n-th sample.
     */
    public void setSyncInterval(int track, int i) {
        ((VideoTrack) tracks.get(track)).syncInterval = i;
    }

    /** Gets the sync interval from the specified video track. */
    public int getSyncInterval(int track) {
        return ((VideoTrack) tracks.get(track)).syncInterval;
    }

    /**
     * Sets the state of the QuickTimeWriter to started.
     * <p>
     * If the state is changed by this method, the prolog is
     * written.
     */
    protected void ensureStarted() throws IOException {
        ensureOpen();
        if (state == States.FINISHED) {
            throw new IOException("Can not write into finished movie.");
        }
        if (state != States.STARTED) {
            creationTime = new Date();
            writeProlog();
            mdatAtom = new WideDataAtom("mdat");
            state = States.STARTED;
        }
    }

    /**
     * Encodes an image as a video frame and writes it into a video track.
     *
     * @param track The track index.
     * @param image The image of the video frame.
     * @param duration The duration of the video frame in media time scale units.
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws if the duration is less than 1, or if the dimension of the frame
     * does not match the dimension of the video.
     * @throws UnsupportedOperationException if the QuickTimeWriter does not have
     * a built-in codec for this video format.
     * @throws IOException if writing the sample data failed.
     */
    @Override
    public void writeFrame(int track, BufferedImage image, long duration) throws IOException {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be greater 0.");
        }
        VideoTrack vt = (VideoTrack) tracks.get(track); // throws index out of bounds exception if illegal track index
        if (vt.mediaType != MediaType.VIDEO) {
            throw new IllegalArgumentException("Track " + track + " is not a video track");
        }
        if (vt.codec == null) {
            throw new UnsupportedOperationException("No codec for this video format.");
        }
        ensureStarted();

        // Get the dimensions of the first image
        if (vt.videoWidth == -1) {
            vt.videoWidth = image.getWidth();
            vt.videoHeight = image.getHeight();
        } else {
            // The dimension of the image must match the dimension of the video track
            if (vt.videoWidth != image.getWidth() || vt.videoHeight != image.getHeight()) {
                throw new IllegalArgumentException("Dimensions of frame[" + tracks.get(track).getSampleCount()
                        + "] (width=" + image.getWidth() + ", height=" + image.getHeight()
                        + ") differs from video dimension (width="
                        + vt.videoWidth + ", height=" + vt.videoHeight + ") in track " + track + "");
            }
        }

        // Encode pixel data
        {

            if (vt.outputBuffer == null) {
                vt.outputBuffer = new Buffer();
            }

            boolean isSync = vt.syncInterval == 0 ? false : vt.sampleCount % vt.syncInterval == 0;

            Buffer inputBuffer = new Buffer();
            inputBuffer.flags = (isSync) ? Buffer.FLAG_KEY_FRAME : 0;
            inputBuffer.data = image;
            vt.codec.process(inputBuffer, vt.outputBuffer);
            if (vt.outputBuffer.flags == Buffer.FLAG_DISCARD) {
                return;
            }

            isSync = (vt.outputBuffer.flags & Buffer.FLAG_KEY_FRAME) != 0;

            long offset = getRelativeStreamPosition();
            OutputStream mdatOut = mdatAtom.getOutputStream();
            mdatOut.write((byte[]) vt.outputBuffer.data, vt.outputBuffer.offset, vt.outputBuffer.length);

            long length = getRelativeStreamPosition() - offset;
            vt.addSample(new Sample(duration, offset, length), 1, isSync);
        }
    }

    /**
     * Writes an already encoded sync sample from a file into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param file The file which holds the encoded data sample.
     * @param duration The duration of the sample in media time scale units.
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws IllegalArgumentException if the track does not support video,
     * if the duration is less than 1, or if the dimension of the frame does not
     * match the dimension of the video.
     * @throws IOException if writing the sample data failed.
     */
    public void writeSample(int track, File file, long duration) throws IOException {
        writeSample(track, file, duration, true);
    }

    /**
     * Writes an already encoded sample from a file into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param file The file which holds the encoded data sample.
     * @param duration The duration of the sample in media time scale units.
     * @param isSync whether the sample is a sync sample (key frame).
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws IllegalArgumentException if the track does not support video,
     * if the duration is less than 1, or if the dimension of the frame does not
     * match the dimension of the video.
     * @throws IOException if writing the sample data failed.
     */
    public void writeSample(int track, File file, long duration, boolean isSync) throws IOException {
        ensureStarted();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            writeSample(track, in, duration, isSync);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Writes an already encoded sync sample from an input stream into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param in The input stream which holds the encoded sample data.
     * @param duration The duration of the video frame in media time scale units.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeSample(int track, InputStream in, long duration) throws IOException {
        writeSample(track, in, duration, true);
    }

    /**
     * Writes an already encoded sample from an input stream into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param in The input stream which holds the encoded sample data.
     * @param duration The duration of the video frame in media time scale units.
     * @param isSync Whether the sample is a sync sample (keyframe).
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeSample(int track, InputStream in, long duration, boolean isSync) throws IOException {
        ensureStarted();
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be greater 0");
        }
        Track t = tracks.get(track); // throws index out of bounds exception if illegal track index
        ensureOpen();
        ensureStarted();
        long offset = getRelativeStreamPosition();
        OutputStream mdatOut = mdatAtom.getOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            mdatOut.write(buf, 0, len);
        }
        long length = getRelativeStreamPosition() - offset;
        t.addSample(new Sample(duration, offset, length), 1, isSync);
    }

    /**
     * Writes an already encoded sync sample from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param data The encoded sample data.
     * @param duration The duration of the sample in media time scale units.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeSample(int track, byte[] data, long duration) throws IOException {
        writeSample(track, data, 0, data.length, duration, true);
    }

    /**
     * Writes an already encoded sample from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param data The encoded sample data.
     * @param duration The duration of the sample in media time scale units.
     * @param isSync Whether the sample is a sync sample.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeSample(int track, byte[] data, long duration, boolean isSync) throws IOException {
        ensureStarted();
        writeSample(track, data, 0, data.length, duration, isSync);
    }

    /**
     * Writes an already encoded sync sample from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @param duration The duration of the sample in media time scale units.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeSample(int track, byte[] data, int off, int len, long duration) throws IOException {
        ensureStarted();
        writeSample(track, data, off, len, duration, true);
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
    public void writeSample(int track, byte[] data, int off, int len, long duration, boolean isSync) throws IOException {
        ensureStarted();
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be greater 0");
        }
        Track t = tracks.get(track); // throws index out of bounds exception if illegal track index
        ensureOpen();
        ensureStarted();
        long offset = getRelativeStreamPosition();
        OutputStream mdatOut = mdatAtom.getOutputStream();
        mdatOut.write(data, off, len);
        t.addSample(new Sample(duration, offset, len), 1, isSync);
    }

    /**
     * Writes multiple sync samples from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param sampleCount The number of samples.
     * @param data The encoded sample data. The length of data must be dividable
     * by sampleCount.
     * @param sampleDuration The duration of a sample. All samples must
     * have the same duration.
     *
     * @throws IllegalArgumentException if {@code sampleDuration} is less than 1
     * or if the length of {@code data} is not dividable by {@code sampleCount}.
     * @throws IOException if writing the chunk failed.
     */
    public void writeSamples(int track, int sampleCount, byte[] data, long sampleDuration) throws IOException {
        writeSamples(track, sampleCount, data, 0, data.length, sampleDuration, true);
    }

    /**
     * Writes multiple sync samples from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the samples. The contents
     * has to match the format and dimensions of the media in this track.
     *
     * @param track The track index.
     * @param sampleCount The number of samples.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write. Must be dividable by sampleCount.
     * @param sampleDuration The duration of a sample. All samples must
     * have the same duration.
     *
     * @throws IllegalArgumentException if the duration is less than 1.
     * @throws IOException if writing the image failed.
     */
    public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration) throws IOException {
        writeSamples(track, sampleCount, data, off, len, sampleDuration, true);
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
        ensureStarted();
        if (sampleDuration <= 0) {
            throw new IllegalArgumentException("sampleDuration must be greater 0, sampleDuration=" + sampleDuration);
        }
        if (sampleCount <= 0) {
            throw new IllegalArgumentException("sampleCount must be greater 0, sampleCount=" + sampleCount);
        }
        if (len % sampleCount != 0) {
            throw new IllegalArgumentException("len must be divisable by sampleCount len=" + len + " sampleCount=" + sampleCount);
        }
        Track t = tracks.get(track); // throws index out of bounds exception if illegal track index
        ensureOpen();
        ensureStarted();
        long offset = getRelativeStreamPosition();
        OutputStream mdatOut = mdatAtom.getOutputStream();
        mdatOut.write(data, off, len);


        int sampleLength = len / sampleCount;
        Sample first = new Sample(sampleDuration, offset, sampleLength);
        Sample last = new Sample(sampleDuration, offset + sampleLength * (sampleCount - 1), sampleLength);
        t.addChunk(new Chunk(first, last, sampleCount, 1), isSync);
    }

    /** Returns true because QuickTime supports variable frame rates. */
    @Override
    public boolean isVFRSupported() {
        return true;
    }

    /** Returns true if the limit for media samples has been reached.
     * If this limit is reached, no more samples should be added to the movie.
     * <p>
     * QuickTime files can be up to 64 TB long, but there are other values that
     * may overflow before this size is reached. This method returns true
     * when the files size exceeds 2^60 or when the media duration value of a
     * track exceeds 2^61.
     */
    @Override
    public boolean isDataLimitReached() {
        try {
            long maxMediaDuration = 0;
            for (Track t : tracks) {
                maxMediaDuration = max(t.mediaDuration, maxMediaDuration);
            }

            return getRelativeStreamPosition() > (long) (1L << 61) //
                    || maxMediaDuration > 1L << 61;
        } catch (IOException ex) {
            return true;
        }
    }

    /**
     * Closes the movie file as well as the stream being filtered.
     *
     * @exception IOException if an I/O error has occurred
     */
    @Override
    public void close() throws IOException {
        try {
            if (state == States.STARTED) {
                finish();
            }
        } finally {
            if (state != States.CLOSED) {
                out.close();
                state = States.CLOSED;
            }
        }
    }

    /**
     * Finishes writing the contents of the QuickTime output stream without closing
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
            for (int i = 0, n = tracks.size(); i < n; i++) {
            }
            mdatAtom.finish();
            writeEpilog();
            state = States.FINISHED;
            /*
            for (int i = 0, n = tracks.size(); i < n; i++) {
            if (tracks.get(i) instanceof VideoTrack) {
            VideoTrack t = (VideoTrack) tracks.get(i);
            t.videoWidth = t.videoHeight = -1;
            }
            }*/
        }
    }

    /**
     * Check to make sure that this stream has not been closed
     */
    protected void ensureOpen() throws IOException {
        if (state == States.CLOSED) {
            throw new IOException("Stream closed");
        }
    }

    /** Writes the stream prolog. */
    private void writeProlog() throws IOException {
        /* File type atom
         *
        typedef struct {
        magic brand;
        bcd4 versionYear;
        bcd2 versionMonth;
        bcd2 versionMinor;
        magic[4] compatibleBrands;
        } ftypAtom;
         */
        DataAtom ftypAtom = new DataAtom("ftyp");
        DataAtomOutputStream d = ftypAtom.getOutputStream();
        d.writeType("qt  "); // brand
        d.writeBCD4(2005); // versionYear
        d.writeBCD2(3); // versionMonth
        d.writeBCD2(0); // versionMinor
        d.writeType("qt  "); // compatibleBrands
        d.writeInt(0); // compatibleBrands (0 is used to denote no value)
        d.writeInt(0); // compatibleBrands (0 is used to denote no value)
        d.writeInt(0); // compatibleBrands (0 is used to denote no value)
        ftypAtom.finish();
    }

    private void writeEpilog() throws IOException {
        Date modificationTime = new Date();
        long duration = getMovieDuration();

        DataAtom leaf;

        /* Movie Atom ========= */
        moovAtom = new CompositeAtom("moov");

        /* Movie Header Atom -------------
         * The data contained in this atom defines characteristics of the entire
         * QuickTime movie, such as time scale and duration. It has an atom type
         * value of 'mvhd'.
         *
         * typedef struct {
        byte version;
        byte[3] flags;
        mactimestamp creationTime;
        mactimestamp modificationTime;
        int timeScale;
        int duration;
        int preferredRate;
        short preferredVolume;
        byte[10] reserved;
        int[9] matrix;
        int previewTime;
        int previewDuration;
        int posterTime;
        int selectionTime;
        int selectionDuration;
        int currentTime;
        int nextTrackId;
        } movieHeaderAtom;
         */
        leaf = new DataAtom("mvhd");
        moovAtom.add(leaf);
        DataAtomOutputStream d = leaf.getOutputStream();
        d.writeByte(0); // version
        // A 1-byte specification of the version of this movie header atom.

        d.writeByte(0); // flags[0]
        d.writeByte(0); // flags[1]
        d.writeByte(0); // flags[2]
        // Three bytes of space for future movie header flags.

        d.writeMacTimestamp(creationTime); // creationTime
        // A 32-bit integer that specifies the calendar date and time (in
        // seconds since midnight, January 1, 1904) when the movie atom was
        // created. It is strongly recommended that this value should be
        // specified using coordinated universal time (UTC).

        d.writeMacTimestamp(modificationTime); // modificationTime
        // A 32-bit integer that specifies the calendar date and time (in
        // seconds since midnight, January 1, 1904) when the movie atom was
        // changed. BooleanIt is strongly recommended that this value should be
        // specified using coordinated universal time (UTC).

        d.writeUInt(movieTimeScale); // timeScale
        // A time value that indicates the time scale for this movie—that is,
        // the number of time units that pass per second in its time coordinate
        // system. A time coordinate system that measures time in sixtieths of a
        // second, for example, has a time scale of 60.

        d.writeUInt(duration); // duration
        // A time value that indicates the duration of the movie in time scale
        // units. Note that this property is derived from the movie’s tracks.
        // The value of this field corresponds to the duration of the longest
        // track in the movie.

        d.writeFixed16D16(1d); // preferredRate
        // A 32-bit fixed-point number that specifies the rate at which to play
        // this movie. A value of 1.0 indicates normal rate.

        d.writeShort(256); // preferredVolume
        // A 16-bit fixed-point number that specifies how loud to play this
        // movie’s sound. A value of 1.0 indicates full volume.

        d.write(new byte[10]); // reserved;
        // Ten bytes reserved for use by Apple. Set to 0.

        d.writeFixed16D16(1f); // matrix[0]
        d.writeFixed16D16(0f); // matrix[1]
        d.writeFixed2D30(0f); // matrix[2]
        d.writeFixed16D16(0f); // matrix[3]
        d.writeFixed16D16(1f); // matrix[4]
        d.writeFixed2D30(0); // matrix[5]
        d.writeFixed16D16(0); // matrix[6]
        d.writeFixed16D16(0); // matrix[7]
        d.writeFixed2D30(1f); // matrix[8]
        // The matrix structure associated with this movie. A matrix shows how
        // to map points from one coordinate space into another. See “Matrices”
        // for a discussion of how display matrices are used in QuickTime:
        // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap4/chapter_5_section_4.html#//apple_ref/doc/uid/TP40000939-CH206-18737

        d.writeInt(0); // previewTime
        // The time value in the movie at which the preview begins.

        d.writeInt(0); // previewDuration
        // The duration of the movie preview in movie time scale units.

        d.writeInt(0); // posterTime
        // The time value of the time of the movie poster.

        d.writeInt(0); // selectionTime
        // The time value for the start time of the current selection.

        d.writeInt(0); // selectionDuration
        // The duration of the current selection in movie time scale units.

        d.writeInt(0); // currentTime;
        // The time value for current time position within the movie.

        d.writeUInt(tracks.size() + 1); // nextTrackId
        // A 32-bit integer that indicates a value to use for the track ID
        // number of the next track added to this movie. Note that 0 is not a
        // valid track ID value.

        for (int i = 0, n = tracks.size(); i < n; i++) {
            Track t = tracks.get(i);
            /* Track Atom ======== */
            t.writeTrackAtoms(i, moovAtom, modificationTime);
        }
        //
        moovAtom.finish();
    }

    /** Writes a version of the movie which is optimized for the web into the
     * specified output file.
     * <p>
     * This method finishes the movie and then copies its content into
     * the specified file. The web-optimized file starts with the movie header.
     *
     * @param outputFile The output file
     * @param compressHeader Whether the movie header shall be compressed.
     */
    public void toWebOptimizedMovie(File outputFile, boolean compressHeader) throws IOException {
        finish();
        long originalMdatOffset = mdatAtom.getOffset();
        CompositeAtom originalMoovAtom = moovAtom;
        mdatOffset = 0;

        ImageOutputStream originalOut = out;
        try {
            out = null;

            if (compressHeader) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int maxIteration = 5;
                long compressionHeadersSize = 40 + 8;
                long headerSize = 0;
                long freeSize = 0;
                while (true) {
                    mdatOffset = compressionHeadersSize + headerSize + freeSize;
                    buf.reset();
                    DeflaterOutputStream deflater = new DeflaterOutputStream(buf);
                    out = new MemoryCacheImageOutputStream(deflater);
                    writeEpilog();
                    out.close();
                    deflater.close();

                    if (buf.size() > headerSize + freeSize && --maxIteration > 0) {
                        if (headerSize != 0) {
                            freeSize = Math.max(freeSize, buf.size() - headerSize - freeSize);
                        }
                        headerSize = buf.size();
                    } else {
                        freeSize = headerSize + freeSize - buf.size();
                        headerSize = buf.size();
                        break;
                    }
                }

                if (maxIteration < 0 || buf.size() == 0) {
                    compressHeader = false;
                    System.err.println("WARNING QuickTimeWriter failed to compress header.");
                } else {
                    out = new FileImageOutputStream(outputFile);
                    writeProlog();

                    // 40 bytes compression headers
                    DataAtomOutputStream daos = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
                    daos.writeUInt(headerSize + 40);
                    daos.writeType("moov");

                    daos.writeUInt(headerSize + 32);
                    daos.writeType("cmov");

                    daos.writeUInt(12);
                    daos.writeType("dcom");
                    daos.writeType("zlib");

                    daos.writeUInt(headerSize + 12);
                    daos.writeType("cmvd");
                    daos.writeUInt(originalMoovAtom.size());

                    daos.write(buf.toByteArray());

                    // 8 bytes "free" atom + free data
                    daos.writeUInt(freeSize + 8);
                    daos.writeType("free");
                    for (int i = 0; i < freeSize; i++) {
                        daos.write(0);
                    }
                }

            }
            if (!compressHeader) {
                out = new FileImageOutputStream(outputFile);
                mdatOffset = moovAtom.size();
                writeProlog();
                writeEpilog();
            }


            byte[] buf = new byte[4096];
            originalOut.seek((originalMdatOffset));
            for (long count = 0, n = mdatAtom.size(); count < n;) {
                int read = originalOut.read(buf, 0, (int) Math.min(buf.length, n - count));
                out.write(buf, 0, read);
                count += read;
            }
            out.close();
        } finally {
            mdatOffset = 0;
            moovAtom = originalMoovAtom;
            out = originalOut;
        }
    }
}
