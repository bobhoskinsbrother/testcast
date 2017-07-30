/*
 * @(#)PNGCodec.java  1.0  2011-03-12
 *
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.png;

import ch.randelshofer.media.AbstractVideoCodec;
import ch.randelshofer.media.Buffer;
import ch.randelshofer.media.Format;
import ch.randelshofer.media.VideoFormat;
import ch.randelshofer.media.io.ByteArrayImageOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * {@code PNGCodec} encodes a BufferedImage as a byte[] array..
 * <p>
 * Supported input formats:
 * <ul>
 * {@code VideoFormat} with {@code BufferedImage.class}, any width, any height,
 * any depth.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code VideoFormat} with {@code byte[].class}, same width and height as input
 * format, depth=24.
 * </ul>
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class PNGCodec extends AbstractVideoCodec {

    @Override
    public Format setInputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
                return super.setInputFormat(new VideoFormat(VideoFormat.QT_PNG,vf.getDataClass(), vf.getWidth(), vf.getHeight(), vf.getDepth()));
            }
        }
        return super.setInputFormat(null);
    }

    @Override
    public Format setOutputFormat(Format f) {
        if (f instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) f;
            return super.setOutputFormat(new VideoFormat(VideoFormat.QT_PNG,byte[].class, vf.getWidth(), vf.getHeight(), 24));
        }
        return super.setOutputFormat(null);
    }

    @Override
    public void process(Buffer in, Buffer out) {
        if ((in.flags & Buffer.FLAG_DISCARD) != 0) {
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }
        BufferedImage image = getBufferedImage(in);
        if (image == null) {
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }

        ByteArrayImageOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new ByteArrayImageOutputStream((byte[]) out.data);
        } else {
            tmp = new ByteArrayImageOutputStream();
        }

        try {
            ImageWriter iw = (ImageWriter) ImageIO.getImageWritersByMIMEType("image/png").next();
            ImageWriteParam iwParam = iw.getDefaultWriteParam();
            iw.setOutput(tmp);
            IIOImage img = new IIOImage(image, null, null);
            iw.write(null, img, iwParam);
            iw.dispose();

            out.flags = Buffer.FLAG_KEY_FRAME;
            out.data = tmp.getBuffer();
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
        } catch (IOException ex) {
            ex.printStackTrace();
            out.flags = Buffer.FLAG_DISCARD;
            return;
        }
    }
}
