/*
 * @(#)RunLengthCodec.java  1.0  2011-03-12
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
import ch.randelshofer.media.io.ByteArrayImageOutputStream;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import static java.lang.Math.min;

/**
 * {@code RunLengthCodec} encodes a BufferedImage as a byte[] array.
 * <p>
 * Supported input formats:
 * <ul>
 * {@code VideoFormat} with {@code BufferedImage.class}, any width, any height,
 * depth=8.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code VideoFormat} with {@code byte[].class}, same width and height as input
 * format, depth=8.
 * </ul>
 * The codec supports lossless delta- and key-frame encoding of images with 8
 * bits per pixel.
 * <p>
 * The codec does not encode the color palette of an image. This must be done
 * separately.
 * <p>
 * A frame is compressed line by line from bottom to top.
 * <p>
 * Each line of a frame is compressed individually. A line consists of two-byte
 * op-codes optionally followed by data. The end of the line is marked with
 * the EOL op-code.
 * <p>
 * The following op-codes are supported:
 * <ul>
 * <li>{@code 0x00 0x00}
 * <br>Marks the end of a line.</li>
 *
 * <li>{@code  0x00 0x01}
 * <br>Marks the end of the bitmap.</li>
 *
 * <li>{@code 0x00 0x02 x y}
 * <br> Marks a delta (skip). {@code x} and {@code y}
 * indicate the horizontal and vertical offset from the current position.
 * {@code x} and {@code y} are unsigned 8-bit values.</li>
 *
 * <li>{@code 0x00 n data{n} 0x00?}
 * <br> Marks a literal run. {@code n}
 * gives the number of data bytes that follow. {@code n} must be between 3 and
 * 255. If n is odd, a pad byte with the value 0x00 must be added.
 * </li>
 * <li>{@code n data}
 * <br> Marks a repetition. {@code n}
 * gives the number of times the data byte is repeated. {@code n} must be
 * between 1 and 255.
 * </li>
 * </ul>
 * Example:
 * <pre>
 * Compressed data         Expanded data
 *
 * 03 04                   04 04 04
 * 05 06                   06 06 06 06 06
 * 00 03 45 56 67 00       45 56 67
 * 02 78                   78 78
 * 00 02 05 01             Move 5 right and 1 down
 * 02 78                   78 78
 * 00 00                   End of line
 * 09 1E                   1E 1E 1E 1E 1E 1E 1E 1E 1E
 * 00 01                   End of RLE bitmap
 * </pre>
 *
 * References:<br/>
 * <a href="http://wiki.multimedia.cx/index.php?title=Microsoft_RLE">http://wiki.multimedia.cx/index.php?title=Microsoft_RLE</a><br>
 *
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class RunLengthCodec extends AbstractVideoCodec {

    private byte[] previousPixels;

    @Override
    public Format setInputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
                return super.setInputFormat(new VideoFormat(VideoFormat.IMAGE,vf.getDataClass(), vf.getWidth(), vf.getHeight(), 8));
            }
        }
        return super.setInputFormat(null);
    }

    @Override
    public Format setOutputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            return super.setOutputFormat(new VideoFormat(VideoFormat.AVI_RLE,byte[].class, vf.getWidth(), vf.getHeight(), 8));
        }
        return super.setOutputFormat(null);
    }

    @Override
    public void process(Buffer in, Buffer out) {
        if ((in.flags & Buffer.FLAG_DISCARD) != 0) {
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }
        ByteArrayImageOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new ByteArrayImageOutputStream((byte[]) out.data);
        } else {
            tmp = new ByteArrayImageOutputStream();
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
        int offset=r.x + r.y * scanlineStride;

        try {
            byte[] pixels = getIndexed8(in);
            if (pixels == null) {
                throw new UnsupportedOperationException("Can not process buffer " + in);
            }

            if ((in.flags & Buffer.FLAG_KEY_FRAME) != 0 ||//
                    previousPixels == null) {

                writeKey8(tmp, pixels, r.width, r.height, offset, scanlineStride);
                out.flags = Buffer.FLAG_KEY_FRAME;
            } else {
                writeDelta8(tmp, pixels, previousPixels, r.width, r.height, offset, scanlineStride);
                out.flags = 0;
            }
            out.data = tmp.getBuffer();
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
            //
            if (previousPixels == null) {
                previousPixels = pixels.clone();
            } else {
                System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
            }
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }
    }

    /** Encodes an 8-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride) throws IOException {
        ByteArrayImageOutputStream buf = new ByteArrayImageOutputStream(data.length);
        writeKey8(buf, data, width, height, offset, scanlineStride);
        buf.toOutputStream(out);
    }

    /** Encodes an 8-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey8(ImageOutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        int ymax = offset + height * scanlineStride;
        int upsideDown = ymax - scanlineStride + offset;

        // Encode each scanline separately
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = upsideDown - y;
            int xymax = xy + width;

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine repeat count
                byte v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 255; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;
                if (repeatCount < 3) {
                    literalCount++;
                    if (literalCount == 254) {
                        out.write(0);
                        out.write(literalCount); // Literal OP-code
                        out.write(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        if (literalCount < 3) {
                            for (; literalCount > 0; --literalCount) {
                                out.write(1); // Repeat OP-code
                                out.write(data[xy - literalCount]);
                            }
                        } else {
                            out.write(0);
                            out.write(literalCount); // Literal OP-code
                            out.write(data, xy - literalCount, literalCount);
                            if (literalCount % 2 == 1) {
                                out.write(0); // pad byte
                            }
                            literalCount = 0;
                        }
                    }
                    out.write(repeatCount); // Repeat OP-code
                    out.write(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                if (literalCount < 3) {
                    for (; literalCount > 0; --literalCount) {
                        out.write(1); // Repeat OP-code
                        out.write(data[xy - literalCount]);
                    }
                } else {
                    out.write(0);
                    out.write(literalCount);
                    out.write(data, xy - literalCount, literalCount);
                    if (literalCount % 2 == 1) {
                        out.write(0); // pad byte
                    }
                }
                literalCount = 0;
            }

            out.write(0);
            out.write(0x0000);// End of line
        }
        out.write(0);
        out.write(0x0001);// End of bitmap
    }

    /** Encodes an 8-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta8(OutputStream out, byte[] data, byte[] prev,int width, int height, int offset, int scanlineStride) throws IOException {
        ByteArrayImageOutputStream buf = new ByteArrayImageOutputStream(data.length);
        writeDelta8(buf, data, prev,width, height, offset, scanlineStride);
        buf.toOutputStream(out);
    }
    /** Encodes an 8-bit delta frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta8(ImageOutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {

        out.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        int ymax = offset + height * scanlineStride;
        int upsideDown = ymax - scanlineStride + offset;

        // Encode each scanline
        int verticalOffset = 0;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = upsideDown - y;
            int xymax = xy + width;

            // determine skip count
            int skipCount = 0;
            for (; xy < xymax; ++xy, ++skipCount) {
                if (data[xy] != prev[xy]) {
                    break;
                }
            }
            if (skipCount == width) {
                // => the entire line can be skipped
                ++verticalOffset;
                continue;
            }

            while (verticalOffset > 0 || skipCount > 0) {
                if (verticalOffset == 1 && skipCount == 0) {
                    out.write(0x00);
                    out.write(0x00); // End of line OP-code
                    verticalOffset = 0;
                } else {
                    out.write(0x00);
                    out.write(0x02); // Skip OP-code
                    out.write(min(255, skipCount)); // horizontal offset
                    out.write(min(255, verticalOffset)); // vertical offset
                    skipCount -= min(255, skipCount);
                    verticalOffset -= min(255, verticalOffset);
                }
            }


            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine skip count
                for (skipCount = 0; xy < xymax; ++xy, ++skipCount) {
                    if (data[xy] != prev[xy]) {
                        break;
                    }
                }
                xy -= skipCount;

                // determine repeat count
                byte v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 255; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (skipCount < 4 && xy + skipCount < xymax && repeatCount < 3) {
                    literalCount++;
                } else {
                    while (literalCount > 0) {
                        if (literalCount < 3) {
                            out.write(1); // Repeat OP-code
                            out.write(data[xy - literalCount]);
                            literalCount--;
                        } else {
                            int literalRun = min(254, literalCount);
                            out.write(0);
                            out.write(literalRun); // Literal OP-code
                            out.write(data, xy - literalCount, literalRun);
                            if (literalRun % 2 == 1) {
                                out.write(0); // pad byte
                            }
                            literalCount -= literalRun;
                        }
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 0) {
                            out.write(0);
                            out.write(0x0002); // Skip OP-code
                            out.write(min(255, skipCount));
                            out.write(0);
                            xy += min(255, skipCount);
                            skipCount -= min(255, skipCount);
                        }
                        xy -= 1;
                    } else {
                        out.write(repeatCount); // Repeat OP-code
                        out.write(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            while (literalCount > 0) {
                if (literalCount < 3) {
                    out.write(1); // Repeat OP-code
                    out.write(data[xy - literalCount]);
                    literalCount--;
                } else {
                    int literalRun = min(254, literalCount);
                    out.write(0);
                    out.write(literalRun); // Literal OP-code
                    out.write(data, xy - literalCount, literalRun);
                    if (literalRun % 2 == 1) {
                        out.write(0); // pad byte
                    }
                    literalCount -= literalRun;
                }
            }

            out.write(0);
            out.write(0x0000); // End of line OP-code
        }

        out.write(0);
        out.write(0x0001);// End of bitmap
    }
}
