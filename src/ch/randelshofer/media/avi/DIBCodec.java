/*
 * @(#)DIBCodec.java  1.0  2011-03-12
 *
 * Copyright © 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.avi;

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
 * {@code DIBCodec} encodes a BufferedImage as a byte[] encoded as a
 * Microsoft Device Independent Bitmap (DIB).
 * <p/>
 * This codec does not encode the color palette of an image. This must be done
 * separately.
 * <p/>
 * The pixels of a frame are written row by row from bottom to top and from
 * the left to the right.
 * <p/>
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
 * @version 1.0 2011-03-12 Created.
 */
public class DIBCodec extends AbstractVideoCodec {

    @Override
    public Format setInputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            int depth = vf.getDepth();
            if (depth <= 4) {
                depth = 4;
            } else if (depth <= 8) {
                depth = 8;
            } else {
                depth = 24;
            }
            if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
                return super.setInputFormat(new VideoFormat(VideoFormat.IMAGE, vf.getDataClass(), vf.getWidth(), vf.getHeight(), depth));
            }
        }
        return super.setInputFormat(null);
    }

    @Override
    public Format setOutputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            int depth = vf.getDepth();
            if (depth <= 4) {
                depth = 4;
            } else if (depth <= 8) {
                depth = 8;
            } else {
                depth = 24;
            }
            return super.setOutputFormat(new VideoFormat(VideoFormat.AVI_DIB, byte[].class, vf.getWidth(), vf.getHeight(), depth));
        }
        return super.setOutputFormat(null);
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
                case 4: {
                    byte[] pixels = getIndexed8(in);
                    if (pixels == null) {
                        out.flags = Buffer.FLAG_DISCARD;
                        return;
                    }
                    writeKey4(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 8: {
                    byte[] pixels = getIndexed8(in);
                    if (pixels == null) {
                        out.flags = Buffer.FLAG_DISCARD;
                        return;
                    }
                    writeKey8(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 24: {
                    int[] pixels = getRGB24(in);
                    if (pixels == null) {
                        out.flags = Buffer.FLAG_DISCARD;
                        return;
                    }
                    writeKey24(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                default:
                    out.flags = Buffer.FLAG_DISCARD;
                    return;
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

    /**
     * Encodes a 4-bit key frame.
     *
     * @param temp           The output stream.
     * @param pixels         The image data.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey4(OutputStream out, byte[] pixels, int width, int height, int offset, int scanlineStride)
            throws IOException {

        byte[] bytes = new byte[width];
        for (int y = (height - 1) * scanlineStride; y >= 0; y -= scanlineStride) { // Upside down
            for (int x = offset, xx = 0, n = offset + width; x < n; x += 2, ++xx) {
                bytes[xx] = (byte) (((pixels[y + x] & 0xf) << 4) | (pixels[y + x + 1] & 0xf));
            }
            out.write(bytes);
        }

    }

    /**
     * Encodes an 8-bit key frame.
     *
     * @param temp           The output stream.
     * @param pixels         The image data.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey8(OutputStream out, byte[] pixels, int width, int height, int offset, int scanlineStride)
            throws IOException {

        for (int y = (height - 1) * scanlineStride; y >= 0; y -= scanlineStride) { // Upside down
            out.write(pixels, y + offset, width);
        }
    }

    /**
     * Encodes a 24-bit key frame.
     *
     * @param temp           The output stream.
     * @param data           The image data.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey24(OutputStream out, int[] pixels, int width, int height, int offset, int scanlineStride)
            throws IOException {
        int w3 = width * 3;
        byte[] bytes = new byte[w3]; // holds a scanline of raw image data with 3 channels of 8 bit data
        for (int xy = (height - 1) * scanlineStride + offset; xy >= offset; xy -= scanlineStride) { // Upside down
            for (int x = 0, xp = 0; x < w3; x += 3, ++xp) {
                int p = pixels[xy + xp];
                bytes[x] = (byte) (p); // Blue
                bytes[x + 1] = (byte) (p >> 8); // Green
                bytes[x + 2] = (byte) (p >> 16); // Red
            }
            out.write(bytes);
        }
    }
}
