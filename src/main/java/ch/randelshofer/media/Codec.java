/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.randelshofer.media;

/**
 * A {@code Codec} processes a {@code Buffer} and stores the result in another
 * {@code Buffer}.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public interface Codec {
    /** Sets the input format.
     * Returns the format that was actually set. This is the closest format
     * that the Codec supports. Returns null if the specified format is not
     * supported and no reasonable match could be found.
     */
    public Format setInputFormat(Format f);
    /** Sets the output format.
     * Returns the format that was actually set. This is the closest format
     * that the Codec supports. Returns null if the specified format is not
     * supported and no reasonable match could be found.
     */
    public Format setOutputFormat(Format f);

    /** Performs the media processing defined by this codec. 
     * @throw UnsupportedOperationException if the codec can not process
     * the input buffer.
     */
    public void process(Buffer in, Buffer out);

    /** Sets the processing quality. */
    public void setQuality(float newValue);

    /** Returns the processing quality. */
    public float getQuality();
}
