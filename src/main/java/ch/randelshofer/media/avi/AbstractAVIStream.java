/*
 * @(#)AbstractAVIStream.java  1.0  2011-03-15
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
import ch.randelshofer.media.Codec;
import ch.randelshofer.media.VideoFormat;
import ch.randelshofer.media.io.ImageOutputStreamAdapter;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This class is just a stub.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-15 Created.
 */
public abstract class AbstractAVIStream {
    /**
     * Underlying output stream.
     */
    protected ImageOutputStream out;
    /** The offset in the underlying ImageOutputStream.
     * Normally this is 0 unless the underlying stream already contained data
     * when it was passed to the constructor.
     */
    protected long streamOffset;
    /**
     * Built-in video formats.
     * /
    public static enum AVIVideoFormat {

        RAW("DIB ", true, false),//
        RLE("RLE ", false, true),//
        TECHSMITH_SCREEN_CAPTURE("tscc", false, true),//
        JPG("MJPG", true, true),//
        PNG("png ", true, true);
        protected String fourCC;
        protected boolean isCompressed;
        protected boolean allSamplesAreSyncSamples;

        AVIVideoFormat(String fourCC, boolean allSamplesAreSyncSamples, boolean isCompressed) {
            this.fourCC = fourCC;
            this.allSamplesAreSyncSamples = allSamplesAreSyncSamples;
            this.isCompressed = isCompressed;
        }
    }*/

    /**
     * Supported media types.
     */
    protected static enum MediaType {

        AUDIO("auds"),//
        MIDI("mids"),//
        TEXT("txts"),//
        VIDEO("vids")//
        ;
        protected String fccType;

        MediaType(String fourCC) {
            this.fccType = fourCC;
        }
    }
    /** The list of tracks in the file. */
    protected ArrayList<Track> tracks = new ArrayList<Track>();



    /** Gets the position relative to the beginning of the QuickTime stream.
     * <p>
     * Usually this value is equal to the stream position of the underlying
     * ImageOutputStream, but can be larger if the underlying stream already
     * contained data.
     *
     * @return The relative stream position.
     * @throws IOException
     */
    protected long getRelativeStreamPosition() throws IOException {
        return out.getStreamPosition() - streamOffset;
    }

    /** Seeks relative to the beginning of the QuickTime stream.
     * <p>
     * Usually this equal to seeking in the underlying ImageOutputStream, but
     * can be different if the underlying stream already contained data.
     *
     */
    protected void seekRelative(long newPosition) throws IOException {
        out.seek(newPosition + streamOffset);
    }
    /**
     * AVI stores media data in samples.
     * A sample is a single element in a sequence of time-ordered data.
     */
    protected static class Sample {

        String chunkType;
        /** Offset of the sample relative to the start of the AVI file.
         */
        long offset;
        /** Data length of the sample. */
        long length;
        /**
         * The duration of the sample in time scale units.
         */
        int duration;
        /** Whether the sample is a sync-sample. */
        boolean isSync;

        /**
         * Creates a new sample.
         * @param duration
         * @param offset
         * @param length
         */
        public Sample(String chunkId, int duration, long offset, long length, boolean isSync) {
            this.chunkType = chunkId;
            this.duration = duration;
            this.offset = offset;
            this.length = length;
            this.isSync = isSync;
        }
    }

    /** Represents a track. */
    protected abstract class Track {

        // Common metadata
        /** The media type of the track. */
        final MediaType mediaType;
        /**
         * The timeScale of the media in the track.
         * <p>
         * Used with frameRate to specify the time scale that this stream will use.
         * Dividing frameRate by timeScale gives the number of samples per second.
         * For video streams, this is the frame rate. For audio streams, this rate
         * corresponds to the time needed to play nBlockAlign bytes of audio, which
         * for PCM audio is just the sample rate.
         */
        protected long timeScale = 1;
        /**
         * The frameRate of the media in timeScale units.
         * <p>
         * @see timeScale
         */
        protected long frameRate = 30;
        /**
         * List of samples.
         */
        protected LinkedList<Sample> samples;
        /** Interval between sync samples (keyframes).
         * 0 = automatic.
         * 1 = write all samples as sync samples.
         * n = sync every n-th sample.
         */
        protected int syncInterval = 30;
        protected String twoCC;
        protected String fourCC;

        public Track(int trackIndex, MediaType mediaType, String fourCC) {
            this.mediaType = mediaType;
            twoCC = "00" + Integer.toString(trackIndex);
            twoCC = twoCC.substring(twoCC.length() - 2);
            this.fourCC = fourCC;
        }
        /**
         * This chunk holds the AVI Stream Header.
         */
        FixedSizeDataChunk strhChunk;
        /**
         * This chunk holds the AVI Stream Format Header.
         */
        FixedSizeDataChunk strfChunk;
    }

    protected class VideoTrack extends Track {
        // Video metadata

        /**
         * The video format.
         */
        protected VideoFormat videoFormat;
        /**
         * The video compression quality.
         */
        protected float videoQuality = 0.97f;

        /** Index color model for RAW_RGB4 and RAW_RGB8 formats. */
        protected IndexColorModel palette;
        protected IndexColorModel previousPalette;
        /** Previous frame for delta compression. */
        protected Object previousData;
        /** Video codec. */
        protected Codec codec;
        protected Buffer outputBuffer;
        protected Rectangle rcFrame;

        public VideoTrack(int trackIndex, String fourCC) {
            super(trackIndex, MediaType.VIDEO, fourCC);
        }
    }
    /**
     * Chunk base class.
     */
    protected abstract class Chunk {

        /**
         * The chunkType of the chunk. A String with the length of 4 characters.
         */
        protected String chunkType;
        /**
         * The offset of the chunk relative to the start of the
         * ImageOutputStream.
         */
        protected long offset;

        /**
         * Creates a new Chunk at the current position of the ImageOutputStream.
         * @param chunkType The chunkType of the chunk. A string with a length of 4 characters.
         */
        public Chunk(String chunkType) throws IOException {
            this.chunkType = chunkType;
            offset = getRelativeStreamPosition();
        }

        /**
         * Writes the chunk to the ImageOutputStream and disposes it.
         */
        public abstract void finish() throws IOException;

        /**
         * Returns the size of the chunk including the size of the chunk header.
         * @return The size of the chunk.
         */
        public abstract long size();
    }

    /**
     * A CompositeChunk contains an ordered list of Chunks.
     */
    protected class CompositeChunk extends Chunk {

        /**
         * The type of the composite. A String with the length of 4 characters.
         */
        protected String compositeType;
        protected LinkedList<Chunk> children;
        protected boolean finished;

        /**
         * Creates a new CompositeChunk at the current position of the
         * ImageOutputStream.
         * @param compositeType The type of the composite.
         * @param chunkType The type of the chunk.
         */
        public CompositeChunk(String compositeType, String chunkType) throws IOException {
            super(chunkType);
            this.compositeType = compositeType;
            //out.write
            out.writeLong(0); // make room for the chunk header
            out.writeInt(0); // make room for the chunk header
            children = new LinkedList<Chunk>();
        }

        public void add(Chunk child) throws IOException {
            if (children.size() > 0) {
                children.getLast().finish();
            }
            children.add(child);
        }

        /**
         * Writes the chunk and all its children to the ImageOutputStream
         * and disposes of all resources held by the chunk.
         * @throws IOException
         */
        @Override
        public void finish() throws IOException {
            if (!finished) {
                if (size() > 0xffffffffL) {
                    throw new IOException("CompositeChunk \"" + chunkType + "\" is too large: " + size());
                }

                long pointer = getRelativeStreamPosition();
                seekRelative(offset);

                DataChunkOutputStream headerData = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
                headerData.writeType(compositeType);
                headerData.writeUInt(size() - 8);
                headerData.writeType(chunkType);
                for (Chunk child : children) {
                    child.finish();
                }
                seekRelative(pointer);
                if (size() % 2 == 1) {
                    out.writeByte(0); // write pad byte
                }
                finished = true;
            }
        }

        @Override
        public long size() {
            long length = 12;
            for (Chunk child : children) {
                length += child.size() + child.size() % 2;
            }
            return length;
        }
    }

    /**
     * Data Chunk.
     */
    protected class DataChunk extends Chunk {

        protected DataChunkOutputStream data;
        protected boolean finished;

        /**
         * Creates a new DataChunk at the current position of the
         * ImageOutputStream.
         * @param chunkType The chunkType of the chunk.
         */
        public DataChunk(String name) throws IOException {
            super(name);
            out.writeLong(0); // make room for the chunk header
            data = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
        }

        public DataChunkOutputStream getOutputStream() {
            if (finished) {
                throw new IllegalStateException("DataChunk is finished");
            }
            return data;
        }

        /**
         * Returns the offset of this chunk to the beginning of the random access file
         * @return
         */
        public long getOffset() {
            return offset;
        }

        @Override
        public void finish() throws IOException {
            if (!finished) {
                long sizeBefore = size();

                if (size() > 0xffffffffL) {
                    throw new IOException("DataChunk \"" + chunkType + "\" is too large: " + size());
                }

                long pointer = getRelativeStreamPosition();
                seekRelative(offset);

                DataChunkOutputStream headerData = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
                headerData.writeType(chunkType);
                headerData.writeUInt(size() - 8);
                seekRelative(pointer);
                if (size() % 2 == 1) {
                    out.writeByte(0); // write pad byte
                }
                finished = true;
                long sizeAfter = size();
                if (sizeBefore != sizeAfter) {
                    System.err.println("size mismatch " + sizeBefore + "src/main" + sizeAfter);
                }
            }
        }

        @Override
        public long size() {
            return 8 + data.size();
        }
    }

    /**
     * A DataChunk with a fixed size.
     */
    protected class FixedSizeDataChunk extends Chunk {

        protected DataChunkOutputStream data;
        protected boolean finished;
        protected long fixedSize;

        /**
         * Creates a new DataChunk at the current position of the
         * ImageOutputStream.
         * @param chunkType The chunkType of the chunk.
         */
        public FixedSizeDataChunk(String chunkType, long fixedSize) throws IOException {
            super(chunkType);
            this.fixedSize = fixedSize;
            data = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
            data.writeType(chunkType);
            data.writeUInt(fixedSize);
            data.clearCount();

            // Fill fixed size with nulls
            byte[] buf = new byte[(int) Math.min(512, fixedSize)];
            long written = 0;
            while (written < fixedSize) {
                data.write(buf, 0, (int) Math.min(buf.length, fixedSize - written));
                written += Math.min(buf.length, fixedSize - written);
            }
            if (fixedSize % 2 == 1) {
                out.writeByte(0); // write pad byte
            }
            seekToStartOfData();
        }

        public DataChunkOutputStream getOutputStream() {
            /*if (finished) {
            throw new IllegalStateException("DataChunk is finished");
            }*/
            return data;
        }

        /**
         * Returns the offset of this chunk to the beginning of the random access file
         * @return
         */
        public long getOffset() {
            return offset;
        }

        public void seekToStartOfData() throws IOException {
            seekRelative(offset + 8);
            data.clearCount();
        }

        public void seekToEndOfChunk() throws IOException {
            seekRelative(offset + 8 + fixedSize + fixedSize % 2);
        }

        @Override
        public void finish() throws IOException {
            if (!finished) {
                finished = true;
            }
        }

        @Override
        public long size() {
            return 8 + fixedSize;
        }
    }
}
