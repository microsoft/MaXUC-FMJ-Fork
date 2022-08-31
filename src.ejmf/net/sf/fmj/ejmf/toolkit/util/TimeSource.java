package net.sf.fmj.ejmf.toolkit.util;

/**
 * Implemented by classes that will provide a time value.
 *
 * From the book: Essential JMF, Gordon, Talley (ISBN 0130801046). Used with
 * permission.
 *
 * see ejmf.toolkit.SourcedTime
 *
 * @version 1.0
 * @author Rob Gordon & Steve Talley
 */

public interface TimeSource
{
    /**
     * Useful values for return by an implementation of getConversionDivisor.
     *
     */
    public static final long NANOS_PER_SEC = 1000000000;
    public static final long MICROS_PER_SEC = 1000000;
    public static final long MILLIS_PER_SEC = 1000;

    /**
     * Return a number used to divide source units to convert to seconds.
     */
    public long getConversionDivisor();

    /**
     * Return time in source units
     */
    public long getTime();
}
