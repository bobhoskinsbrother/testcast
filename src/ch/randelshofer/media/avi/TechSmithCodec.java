/*
 * @(#)TechSmithCodec.java  1.0  2011-03-12
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
import ch.randelshofer.media.io.SeekableByteArrayOutputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;

import static java.lang.Math.min;

/**
 * {@code TechSmithCodec} (tscc) encodes a BufferedImage as a byte[] array.
 * <p/>
 * This codec does not encode the color palette of an image. This must be done
 * separately.
 * <p/>
 * Supported input formats:
 * <ul>
 * {@code VideoFormat} with {@code BufferedImage.class}, any width, any height,
 * depth=8,16 or 24.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code VideoFormat} with {@code byte[].class}, same width and height as input
 * format, depth=8,16 or 24.
 * </ul>
 * The codec supports lossless delta- and key-frame encoding of images with 8, 16 or
 * 24 bits per pixel.
 * <p/>
 * Compression of a frame is performed in two steps: In the first, step
 * a frame is compressed line by line from bottom to top. In the second step
 * the resulting data is compressed again using zlib compression.
 * <p/>
 * Apart from the second compression step and the support for 16- and 24-bit
 * data, this encoder is identical to the {@link MicrosoftRLEEncoder}.
 * <p/>
 * Each line of a frame is compressed individually. A line consists of two-byte
 * op-codes optionally followed by data. The end of the line is marked with
 * the EOL op-code.
 * <p/>
 * The following op-codes are supported:
 * <ul>
 * <li>{@code 0x00 0x00}
 * <br>Marks the end of a line.</li>
 * <p/>
 * <li>{@code 0x00 0x01}
 * <br>Marks the end of the bitmap.</li>
 * <p/>
 * <li>{@code 0x00 0x02 x y}
 * <br> Marks a delta (skip). {@code x} and {@code y}
 * indicate the horizontal and vertical offset from the current position.
 * {@code x} and {@code y} are unsigned 8-bit values.</li>
 * <p/>
 * <li>{@code 0x00 n pixel{n} 0x00?}
 * <br> Marks a literal run. {@code n}
 * gives the number of 8-, 16- or 24-bit pixels that follow.
 * {@code n} must be between 3 and 255.
 * If n is odd and 8-bit pixels are used, a pad byte with the value 0x00 must be
 * added.
 * </li>
 * <li>{@code n pixel}
 * <br> Marks a repetition. {@code n}
 * gives the number of times the given pixel is repeated. {@code n} must be
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
 * <p/>
 * References:<br/>
 * <a href="http://wiki.multimedia.cx/index.php?title=TechSmith_Screen_Capture_Codec"
 * >http://wiki.multimedia.cx/index.php?title=TechSmith_Screen_Capture_Codec</a><br>
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class TechSmithCodec extends AbstractVideoCodec {

    private ByteArrayImageOutputStream temp = new ByteArrayImageOutputStream(ByteOrder.LITTLE_ENDIAN);
    private Object previousPixels;

    @Override
    public Format setInputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
                return super.setInputFormat(new VideoFormat(VideoFormat.IMAGE, vf.getDataClass(), vf.getWidth(), vf.getHeight(), vf.getDepth()));
            }
        }
        return super.setInputFormat(null);
    }

    @Override
    public Format setOutputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            int depth = vf.getDepth();
            if (depth <= 8) {
                depth = 8;
            } else if (depth <= 16) {
                depth = 16;
            } else {
                depth = 24;
            }
            return super.setOutputFormat(new VideoFormat(VideoFormat.AVI_TECHSMITH_SCREEN_CAPTURE, byte[].class, vf.getWidth(), vf.getHeight(), depth));
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
        int offset = r.x + r.y * scanlineStride;

        try {
            switch (vf.getDepth()) {
                case 8: {
                    byte[] pixels = getIndexed8(in);
                    if (pixels == null) {
                        out.flags = Buffer.FLAG_DISCARD;
                        return;
                    }

                    if ((in.flags & Buffer.FLAG_KEY_FRAME) != 0 ||//
                            previousPixels == null) {

                        writeKey8(tmp, pixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
                        out.flags = Buffer.FLAG_KEY_FRAME;
                    } else {
                        writeDelta8(tmp, pixels, (byte[]) previousPixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
                        out.flags = 0;
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                case 16: {
                    short[] pixels = getRGB15(in);
                    if (pixels == null) {
                        out.flags = Buffer.FLAG_DISCARD;
                        return;
                    }

                    if ((in.flags & Buffer.FLAG_KEY_FRAME) != 0 ||//
                            previousPixels == null) {

                        writeKey16(tmp, pixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
                        out.flags = Buffer.FLAG_KEY_FRAME;
                    } else {
                        writeDelta16(tmp, pixels, (short[]) previousPixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
                        out.flags = 0;
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                case 24: {
                    int[] pixels = getRGB24(in);
                    if (pixels == null) {
                        out.flags = Buffer.FLAG_DISCARD;
                        return;
                    }

                    if ((in.flags & Buffer.FLAG_KEY_FRAME) != 0 ||//
                            previousPixels == null) {

                        writeKey24(tmp, pixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
                        out.flags = Buffer.FLAG_KEY_FRAME;
                    } else {
                        writeDelta24(tmp, pixels, (int[]) previousPixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
                        out.flags = 0;
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                default: {
                    out.flags = Buffer.FLAG_DISCARD;
                    return;
                }
            }

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
     * Encodes a 8-bit key frame.
     *
     * @param temp           The output stream. Must be set to Big-Endian.
     * @param data           The image data.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        temp.clear();
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
                        temp.write(0);
                        temp.write(literalCount); // Literal OP-code
                        temp.write(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        if (literalCount < 3) {
                            for (; literalCount > 0; --literalCount) {
                                temp.write(1); // Repeat OP-code
                                temp.write(data[xy - literalCount]);
                            }
                        } else {
                            temp.write(0);
                            temp.write(literalCount); // Literal OP-code
                            temp.write(data, xy - literalCount, literalCount);
                            if (literalCount % 2 == 1) {
                                temp.write(0); // pad byte
                            }
                            literalCount = 0;
                        }
                    }
                    temp.write(repeatCount); // Repeat OP-code
                    temp.write(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                if (literalCount < 3) {
                    for (; literalCount > 0; --literalCount) {
                        temp.write(1); // Repeat OP-code
                        temp.write(data[xy - literalCount]);
                    }
                } else {
                    temp.write(0);
                    temp.write(literalCount);
                    temp.write(data, xy - literalCount, literalCount);
                    if (literalCount % 2 == 1) {
                        temp.write(0); // pad byte
                    }
                }
                literalCount = 0;
            }

            temp.write(0);
            temp.write(0x0000);// End of line
        }
        temp.write(0);
        temp.write(0x0001);// End of bitmap
        //temp.toOutputStream(out);

        DeflaterOutputStream defl = new DeflaterOutputStream(out);
        temp.toOutputStream(defl);
        defl.finish();
    }

    /**
     * Encodes a 8-bit delta frame.
     *
     * @param temp           The output stream. Must be set to Big-Endian.
     * @param data           The image data.
     * @param prev           The image data of the previous frame.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta8(OutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {

        temp.clear();

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
                temp.write(0x00); // Escape code
                temp.write(0x02); // Skip OP-code
                temp.write(min(255, skipCount)); // horizontal offset
                temp.write(min(255, verticalOffset)); // vertical offset
                skipCount -= min(255, skipCount);
                verticalOffset -= min(255, verticalOffset);
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
                            temp.write(1); // Repeat OP-code
                            temp.write(data[xy - literalCount]);
                            literalCount--;
                        } else {
                            int literalRun = min(254, literalCount);
                            temp.write(0); // Escape code
                            temp.write(literalRun); // Literal OP-code
                            temp.write(data, xy - literalCount, literalRun);
                            if (literalRun % 2 == 1) {
                                temp.write(0); // pad byte
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
                            temp.write(0); // Escape code
                            temp.write(0x0002); // Skip OP-code
                            temp.write(min(255, skipCount));
                            temp.write(0);
                            xy += min(255, skipCount);
                            skipCount -= min(255, skipCount);
                        }
                        xy -= 1;
                    } else {
                        temp.write(repeatCount); // Repeat OP-code
                        temp.write(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            while (literalCount > 0) {
                if (literalCount < 3) {
                    temp.write(1); // Repeat OP-code
                    temp.write(data[xy - literalCount]);
                    literalCount--;
                } else {
                    int literalRun = min(254, literalCount);
                    temp.write(0);
                    temp.write(literalRun); // Literal OP-code
                    temp.write(data, xy - literalCount, literalRun);
                    if (literalRun % 2 == 1) {
                        temp.write(0); // pad byte
                    }
                    literalCount -= literalRun;
                }
            }

            temp.write(0); // Escape code
            temp.write(0x00); // End of line OP-code
        }
        temp.write(0); // Escape code
        temp.write(0x01);// End of bitmap


        if (temp.length() == 2) {
            temp.toOutputStream(out);
        } else {
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            temp.toOutputStream(defl);
            defl.finish();
        }
    }

    /**
     * Encodes a 16-bit key frame.
     *
     * @param temp           The output stream. Must be set to Big-Endian.
     * @param data           The image data.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey16(OutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        temp.clear();
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
                short v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 255; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;
                if (repeatCount < 3) {
                    literalCount++;
                    if (literalCount == 254) {
                        temp.write(0); // Escape code
                        temp.write(literalCount); // Literal OP-code
                        temp.writeShorts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        if (literalCount < 3) {
                            for (; literalCount > 0; --literalCount) {
                                temp.write(1); // Repeat OP-code
                                temp.writeShort(data[xy - literalCount]);
                            }
                        } else {
                            temp.write(0);
                            temp.write(literalCount); // Literal OP-code
                            temp.writeShorts(data, xy - literalCount, literalCount);
                            ///if (literalCount % 2 == 1) {
                            ///    temp.write(0); // pad byte
                            ///}
                            literalCount = 0;
                        }
                    }
                    temp.write(repeatCount); // Repeat OP-code
                    temp.writeShort(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                if (literalCount < 3) {
                    for (; literalCount > 0; --literalCount) {
                        temp.write(1); // Repeat OP-code
                        temp.writeShort(data[xy - literalCount]);
                    }
                } else {
                    temp.write(0);
                    temp.write(literalCount);
                    temp.writeShorts(data, xy - literalCount, literalCount);
                    ///if (literalCount % 2 == 1) {
                    ///    temp.write(0); // pad byte
                    ///}
                }
                literalCount = 0;
            }

            temp.write(0);
            temp.write(0x0000);// End of line
        }
        temp.write(0);
        temp.write(0x0001);// End of bitmap
        //temp.toOutputStream(out);

        DeflaterOutputStream defl = new DeflaterOutputStream(out);
        temp.toOutputStream(defl);
        defl.finish();
    }

    /**
     * Encodes a 16-bit delta frame.
     *
     * @param temp           The output stream. Must be set to Big-Endian.
     * @param data           The image data.
     * @param prev           The image data of the previous frame.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta16(OutputStream out, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {


        temp.clear();

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
                temp.write(0x00); // Escape code
                temp.write(0x02); // Skip OP-code
                temp.write(min(255, skipCount)); // horizontal offset
                temp.write(min(255, verticalOffset)); // vertical offset
                skipCount -= min(255, skipCount);
                verticalOffset -= min(255, verticalOffset);
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
                short v = data[xy];
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
                            temp.write(1); // Repeat OP-code
                            temp.writeShort(data[xy - literalCount]);
                            literalCount--;
                        } else {
                            int literalRun = min(254, literalCount);
                            temp.write(0); // Escape code
                            temp.write(literalRun); // Literal OP-code
                            temp.writeShorts(data, xy - literalCount, literalRun);
                            ///if (literalRun % 2 == 1) {
                            ///    temp.write(0); // pad byte
                            ///}
                            literalCount -= literalRun;
                        }
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 0) {
                            temp.write(0); // Escape code
                            temp.write(0x02); // Skip OP-code
                            temp.write(min(255, skipCount)); // horizontal skip
                            temp.write(0); // vertical skip
                            xy += min(255, skipCount);
                            skipCount -= min(255, skipCount);
                        }
                        xy -= 1;
                    } else {
                        temp.write(repeatCount); // Repeat OP-code
                        temp.writeShort(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            while (literalCount > 0) {
                if (literalCount < 3) {
                    temp.write(1); // Repeat OP-code
                    temp.writeShort(data[xy - literalCount]);
                    literalCount--;
                } else {
                    int literalRun = min(254, literalCount);
                    temp.write(0); // Escape code
                    temp.write(literalRun); // Literal OP-code
                    temp.writeShorts(data, xy - literalCount, literalRun);
                    ///if (literalRun % 2 == 1) {
                    ///    temp.write(0); // pad byte
                    ///}
                    literalCount -= literalRun;
                }
            }

            temp.write(0); // Escape code
            temp.write(0x00); // End of line OP-code
        }

        temp.write(0); // Escape code
        temp.write(0x01);// End of bitmap OP-code

        if (temp.length() == 2) {
            temp.toOutputStream(out);
        } else {
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            temp.toOutputStream(defl);
            defl.finish();
        }
    }

    /**
     * Encodes a 24-bit key frame.
     *
     * @param temp           The output stream. Must be set to Big-Endian.
     * @param data           The image data.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey24(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        temp.clear();
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
                int v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 255; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;
                if (repeatCount < 3) {
                    literalCount++;
                    if (literalCount == 254) {
                        temp.write(0);
                        temp.write(literalCount); // Literal OP-code
                        writeInts24LE(temp, data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        if (literalCount < 3) {
                            for (; literalCount > 0; --literalCount) {
                                temp.write(1); // Repeat OP-code
                                writeInt24LE(temp, data[xy - literalCount]);
                            }
                        } else {
                            temp.write(0);
                            temp.write(literalCount); // Literal OP-code
                            writeInts24LE(temp, data, xy - literalCount, literalCount);
                            ///if (literalCount % 2 == 1) {
                            ///    temp.write(0); // pad byte
                            ///}
                            literalCount = 0;
                        }
                    }
                    temp.write(repeatCount); // Repeat OP-code
                    writeInt24LE(temp, v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                if (literalCount < 3) {
                    for (; literalCount > 0; --literalCount) {
                        temp.write(1); // Repeat OP-code
                        writeInt24LE(temp, data[xy - literalCount]);
                    }
                } else {
                    temp.write(0);
                    temp.write(literalCount);
                    writeInts24LE(temp, data, xy - literalCount, literalCount);
                    ///if (literalCount % 2 == 1) {
                    ///    temp.write(0); // pad byte
                    ///}
                }
                literalCount = 0;
            }

            temp.write(0);
            temp.write(0x0000);// End of line
        }
        temp.write(0);
        temp.write(0x0001);// End of bitmap
        //temp.toOutputStream(out);

        DeflaterOutputStream defl = new DeflaterOutputStream(out);
        temp.toOutputStream(defl);
        defl.finish();
    }

    /**
     * Encodes a 24-bit delta frame.
     *
     * @param temp           The output stream. Must be set to Big-Endian.
     * @param data           The image data.
     * @param prev           The image data of the previous frame.
     * @param offset         The offset to the first pixel in the data array.
     * @param width          The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta24(OutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {

        temp.clear();

        int ymax = offset + height * scanlineStride;
        int upsideDown = ymax - scanlineStride + offset;

        // Encode each scanline
        int verticalOffset = 0;
        ScanlineLoop:
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
                temp.write(0x00); // Escape code
                temp.write(0x02); // Skip OP-code
                temp.write(min(255, skipCount)); // horizontal offset
                temp.write(min(255, verticalOffset)); // vertical offset
                skipCount -= min(255, skipCount);
                verticalOffset -= min(255, verticalOffset);
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
                int v = data[xy];
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
                            temp.write(1); // Repeat OP-code
                            writeInt24LE(temp, data[xy - literalCount]);
                            literalCount--;
                        } else {
                            int literalRun = min(254, literalCount);
                            temp.write(0);
                            temp.write(literalRun); // Literal OP-code
                            writeInts24LE(temp, data, xy - literalCount, literalRun);
                            ///if (literalRun % 2 == 1) {
                            ///    temp.write(0); // pad byte
                            ///}
                            literalCount -= literalRun;
                        }
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 0) {
                            temp.write(0);
                            temp.write(0x0002); // Skip OP-code
                            temp.write(min(255, skipCount));
                            temp.write(0);
                            xy += min(255, skipCount);
                            skipCount -= min(255, skipCount);
                        }
                        xy -= 1;
                    } else {
                        temp.write(repeatCount); // Repeat OP-code
                        writeInt24LE(temp, v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            while (literalCount > 0) {
                if (literalCount < 3) {
                    temp.write(1); // Repeat OP-code
                    writeInt24LE(temp, data[xy - literalCount]);
                    literalCount--;
                } else {
                    int literalRun = min(254, literalCount);
                    temp.write(0);
                    temp.write(literalRun); // Literal OP-code
                    writeInts24LE(temp, data, xy - literalCount, literalRun);
                    ///if (literalRun % 2 == 1) {
                    ///   temp.write(0); // pad byte
                    ///}
                    literalCount -= literalRun;
                }
            }

            temp.write(0); // Escape code
            temp.write(0x00); // End of line OP-code
        }

        temp.write(0); // Escape code
        temp.write(0x01);// End of bitmap

        if (temp.length() == 2) {
            temp.toOutputStream(out);
        } else {
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            temp.toOutputStream(defl);
            defl.finish();
        }
    }

    public static void main(String[] args) {
        byte[] data = {//
                8, 2, 3, 4, 4, 3, 7, 7, 7, 8,//
                8, 1, 1, 1, 1, 2, 7, 7, 7, 8,//
                8, 0, 2, 0, 0, 0, 7, 7, 7, 8,//
                8, 2, 2, 3, 4, 4, 7, 7, 7, 8,//
                8, 1, 4, 4, 4, 5, 7, 7, 7, 8};


        byte[] prev = {//
                8, 3, 3, 3, 3, 3, 7, 7, 7, 8,//
                8, 1, 1, 1, 1, 1, 7, 7, 7, 8, //
                8, 5, 5, 5, 5, 0, 7, 7, 7, 8,//
                8, 2, 2, 0, 0, 0, 7, 7, 7, 8,//
                8, 2, 0, 0, 0, 5, 7, 7, 7, 8};
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataChunkOutputStream out = new DataChunkOutputStream(buf);
        TechSmithCodec enc = new TechSmithCodec();

        try {
            enc.writeDelta8(out, data, prev, 1, 8, 10, 5);
            //enc.writeKey8(out, data, 1, 8, 10,5);
            out.close();

            byte[] result = buf.toByteArray();
            System.out.println("size:" + result.length);
            System.out.println(Arrays.toString(result));
            System.out.print("0x [");

            for (int i = 0; i < result.length; i++) {
                if (i != 0) {
                    System.out.print(',');
                }
                String hex = "00" + Integer.toHexString(result[i]);
                System.out.print(hex.substring(hex.length() - 2));
            }
            System.out.println(']');

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
