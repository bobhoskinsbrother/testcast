/*
 * @(#)RawCodec.java  1.0  2011-03-15
 * 
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.quicktime;

import ch.randelshofer.media.AbstractVideoCodec;
import ch.randelshofer.media.Buffer;
import ch.randelshofer.media.Format;
import ch.randelshofer.media.VideoFormat;
import ch.randelshofer.media.io.SeekableByteArrayOutputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@code RawCodec} encodes a BufferedImage as a byte[] array.
 * <p>
 * This codec does not encode the color palette of an image. This must be done
 * separately.
 * <p>
 * The pixels of a frame are written row by row from top to bottom and from
 * the left to the right.
 * <p>
 * Supported input formats:
 * <ul>
 * {@code VideoFormat} with {@code BufferedImage.class}, any width, any height,
 * depth=4.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code VideoFormat} with {@code byte[].class}, same width and height as input
 * format, depth=4.
 * </ul>
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-15 Created.
 */
public class RawCodec extends AbstractVideoCodec {

    @Override
    public Format setInputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            int depth=vf.getDepth();
            if (depth<=8)depth=8;
            else if (depth<=16)depth=16;
            else if (depth<=24)depth=24;
            else depth=32;
            if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
                return super.setInputFormat(new VideoFormat(VideoFormat.IMAGE,vf.getDataClass(), vf.getWidth(), vf.getHeight(), depth));
            }
        }
        return super.setInputFormat(null);
    }

    @Override
    public Format setOutputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            int depth=vf.getDepth();
            if (depth<=8)depth=8;
            else if (depth<=16)depth=16;
            else if (depth<=24)depth=24;
            else depth=32;
            return super.setOutputFormat(new VideoFormat(VideoFormat.QT_RAW,
                        VideoFormat.QT_RAW_COMPRESSOR_NAME,byte[].class, vf.getWidth(), vf.getHeight(), depth));
        }
        return super.setOutputFormat(null);
    }

    /** Encodes an 8-bit key frame.
     *
     * @param temp The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            out.write(data, xy, width);
        }
    }
    /** Encodes a 24-bit key frame.
     *
     * @param temp The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey16(OutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        byte[] bytes = new byte[width * 2]; // holds a scanline of raw image data with 3 channels of 32 bit data
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            for (int x=0,i=0;x<width;x++,i+=2) {
                int pixel=data[xy+x];
                bytes[i]=(byte)(pixel>>8);
                bytes[i+1]=(byte)(pixel);
            }
            out.write(bytes, 0, bytes.length);
        }
    }
    /** Encodes a 24-bit key frame.
     *
     * @param temp The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey24(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        byte[] bytes = new byte[width * 3]; // holds a scanline of raw image data with 3 channels of 32 bit data
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            for (int x=0,i=0;x<width;x++,i+=3) {
                int pixel=data[xy+x];
                bytes[i]=(byte)(pixel>>16);
                bytes[i+1]=(byte)(pixel>>8);
                bytes[i+2]=(byte)(pixel);
            }
            out.write(bytes, 0, bytes.length);
        }
    }
    /** Encodes a 24-bit key frame.
     *
     * @param temp The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey32(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        byte[] bytes = new byte[width * 4]; // holds a scanline of raw image data with 3 channels of 32 bit data
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            for (int x=0,i=0;x<width;x++,i+=4) {
                int pixel=data[xy+x];
                bytes[i]=(byte)(pixel>>24);
                bytes[i+1]=(byte)(pixel>>16);
                bytes[i+2]=(byte)(pixel>>8);
                bytes[i+3]=(byte)(pixel);
            }
            out.write(bytes, 0, bytes.length);
        }
    }

    /** Encodes a 24-bit key frame.
     *
     * @param temp The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey24(OutputStream out, BufferedImage image)
            throws IOException {

        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = image.getRaster();
        int[] rgb = new int[width * 3]; // holds a scanline of raw image data with 3 channels of 32 bit data
        byte[] bytes = new byte[width * 3]; // holds a scanline of raw image data with 3 channels of 8 bit data
        for (int y = 0; y < height; y++) {
            // Note: Method getPixels is very slow as it does sample conversions for us
            rgb = raster.getPixels(0, y, width, 1, rgb);
            for (int k = 0, n = width * 3; k < n; k++) {
                bytes[k] = (byte) rgb[k];
            }
            out.write(bytes);
        }
    }

    @Override
    public void process(Buffer in, Buffer out) {
        if ((in.flags & Buffer.FLAG_DISCARD) != 0) {
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }

        SeekableByteArrayOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new SeekableByteArrayOutputStream((byte[]) out.data);
        } else {
            tmp = new SeekableByteArrayOutputStream();
        }
            VideoFormat vf = (VideoFormat) outputFormat;

        // Handle sub-image
        Rectangle r;
        int scanlineStride;
        if (in.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) in.data;
            WritableRaster raster = image.getRaster();
            scanlineStride = raster.getSampleModel().getWidth();
            r = raster.getBounds();
            r.x -= raster.getSampleModelTranslateX();
            r.y -= raster.getSampleModelTranslateY();
        } else {
            r = new Rectangle(0, 0, vf.getWidth(), vf.getHeight());
            scanlineStride = vf.getWidth();
        }

        try {
            switch (vf.getDepth()) {
                case 8: {
                    writeKey8(tmp, getIndexed8(in), r.width,r.height,r.x+r.y*scanlineStride,scanlineStride);
                    break;
                }
                case 16: {
                    writeKey16(tmp, getRGB15(in), r.width,r.height,r.x+r.y*scanlineStride,scanlineStride);
                    break;
                }
                case 24: {
                    writeKey24(tmp, getRGB24(in), r.width,r.height,r.x+r.y*scanlineStride,scanlineStride);
                    break;
                }
                case 32: {
                    writeKey24(tmp, getARGB32(in), r.width,r.height,r.x+r.y*scanlineStride,scanlineStride);
                    break;
                }
                default: {
                    out.flags = Buffer.FLAG_DISCARD;
                    return;
                }
            }

            out.flags = Buffer.FLAG_KEY_FRAME;
            out.data = tmp.getBuffer();
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }
    }
}
