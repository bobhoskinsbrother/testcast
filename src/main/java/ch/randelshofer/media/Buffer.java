/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.randelshofer.media;

/**
 * A {@code Buffer} carries media data from one media processing unit to another.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class Buffer {

    /** Indicates that the data in this buffer should be ignored. */
    public final static int FLAG_DISCARD = 1 << 1;
    /** Indicates that this Buffer starts with a key frame. */
    public final static int FLAG_KEY_FRAME = 1 << 4;
    /** A flag mask that describes the boolean attributes for this buffer. */
    public int flags;
    /** The media data. */
    public Object data;
    /** The data offset. This field is only used if {@code data} is an array. */
    public int offset;
    /** The data length. This field is only used if {@code data} is an array. */
    public int length;
    /** Duration of the buffer in {@code timeScale} units. */
    public long duration;
    /** The time scale.
     * A time value that indicates the time scale for this media—that is,
     * the number of time units that pass per second in its time coordinate
     * system.
     */
    public long timeScale;
    /** The number of samples in the data field. */
    public int sampleCount = 1;
}
