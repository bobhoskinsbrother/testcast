/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.randelshofer.media;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;

/**
 * {@code AbstractVideoCodec}.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public abstract class AbstractVideoCodec extends AbstractCodec {

    private BufferedImage imgConverter;

    /**
     * Gets 8-bit indexed pixels from a buffer. Returns null if conversion failed.
     */
    protected byte[] getIndexed8(Buffer buf) {
        if (buf.data instanceof byte[]) {
            return (byte[]) buf.data;
        }
        if (buf.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) buf.data;
            if (image.getRaster().getDataBuffer() instanceof DataBufferByte) {
                return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            }
        }
        return null;
    }

    /**
     * Gets 16-bit RGB pixels from a buffer. Returns null if conversion failed.
     */
    protected short[] getRGB15(Buffer buf) {
        if (buf.data instanceof int[]) {
            return (short[]) buf.data;
        }
        if (buf.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) buf.data;
            if (image.getColorModel() instanceof DirectColorModel) {
                DirectColorModel dcm = (DirectColorModel) image.getColorModel();
                if (image.getRaster().getDataBuffer() instanceof DataBufferShort) {
                    // FIXME - Implement additional checks
                    return ((DataBufferShort) image.getRaster().getDataBuffer()).getData();
                } else if (image.getRaster().getDataBuffer() instanceof DataBufferUShort) {
                    // FIXME - Implement additional checks
                    return ((DataBufferUShort) image.getRaster().getDataBuffer()).getData();
                }
            }
            if (imgConverter == null) {
                int width = ((VideoFormat) outputFormat).getWidth();
                int height = ((VideoFormat) outputFormat).getHeight();
                imgConverter = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
            }
            Graphics2D g = imgConverter.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return ((DataBufferShort) imgConverter.getRaster().getDataBuffer()).getData();
        }
        return null;
    }

    /**
     * Gets 24-bit RGB pixels from a buffer. Returns null if conversion failed.
     */
    protected int[] getRGB24(Buffer buf) {
        if (buf.data instanceof int[]) {
            return (int[]) buf.data;
        }
        if (buf.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) buf.data;
            if (image.getColorModel() instanceof DirectColorModel) {
                DirectColorModel dcm = (DirectColorModel) image.getColorModel();
                if (dcm.getBlueMask() == 0xff && dcm.getGreenMask() == 0xff00 && dcm.getRedMask() == 0xff0000) {
                    if (image.getRaster().getDataBuffer() instanceof DataBufferInt) {
                        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
                    }
                }
            }
            VideoFormat vf = (VideoFormat) outputFormat;
            return image.getRGB(0, 0, vf.getWidth(), vf.getHeight(), null, 0, vf.getWidth());
        }
        return null;
    }

    /**
     * Gets 32-bit ARGB pixels from a buffer. Returns null if conversion failed.
     */
    protected int[] getARGB32(Buffer buf) {
        if (buf.data instanceof int[]) {
            return (int[]) buf.data;
        }
        if (buf.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) buf.data;
            if (image.getColorModel() instanceof DirectColorModel) {
                DirectColorModel dcm = (DirectColorModel) image.getColorModel();
                if (dcm.getBlueMask() == 0xff && dcm.getGreenMask() == 0xff00 && dcm.getRedMask() == 0xff0000) {
                    if (image.getRaster().getDataBuffer() instanceof DataBufferInt) {
                        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
                    }
                }
            }
            VideoFormat vf = (VideoFormat) outputFormat;
            return image.getRGB(0, 0, vf.getWidth(), vf.getHeight(), null, 0, vf.getWidth());
        }
        return null;
    }

    /**
     * Gets a buffered image from a buffer. Returns null if conversion failed.
     */
    protected BufferedImage getBufferedImage(Buffer buf) {
        if (buf.data instanceof BufferedImage) {
            return (BufferedImage) buf.data;
        }
        return null;
    }

    private byte[] byteBuf = new byte[4];

    protected void writeInt24(ImageOutputStream out, int v) throws IOException {
        byteBuf[0] = (byte) (v >>> 16);
        byteBuf[1] = (byte) (v >>> 8);
        byteBuf[2] = (byte) (v >>> 0);
        out.write(byteBuf, 0, 3);
    }

    protected void writeInt24LE(ImageOutputStream out, int v) throws IOException {
        byteBuf[2] = (byte) (v >>> 16);
        byteBuf[1] = (byte) (v >>> 8);
        byteBuf[0] = (byte) (v >>> 0);
        out.write(byteBuf, 0, 3);
    }

    protected void writeInts24(ImageOutputStream out, int[] i, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > i.length || off + len < 0) {
            throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > i.length!");
        }

        byte[] b = new byte[len * 3];
        int boff = 0;
        for (int j = 0; j < len; j++) {
            int v = i[off + j];
            //b[boff++] = (byte)(v >>> 24);
            b[boff++] = (byte) (v >>> 16);
            b[boff++] = (byte) (v >>> 8);
            b[boff++] = (byte) (v >>> 0);
        }

        out.write(b, 0, len * 3);
    }

    protected void writeInts24LE(ImageOutputStream out, int[] i, int off, int len) throws IOException {
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > i.length || off + len < 0) {
            throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > i.length!");
        }

        byte[] b = new byte[len * 3];
        int boff = 0;
        for (int j = 0; j < len; j++) {
            int v = i[off + j];
            b[boff++] = (byte) (v >>> 0);
            b[boff++] = (byte) (v >>> 8);
            b[boff++] = (byte) (v >>> 16);
            //b[boff++] = (byte)(v >>> 24);
        }

        out.write(b, 0, len * 3);
    }
}
