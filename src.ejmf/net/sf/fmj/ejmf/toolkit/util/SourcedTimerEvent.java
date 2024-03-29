package net.sf.fmj.ejmf.toolkit.util;

import java.util.*;

/**
 * This event is sent by a SourcedTimer to its listeners in response from a
 * 'tick' by the SourcedTimer's base timer.
 *
 * From the book: Essential JMF, Gordon, Talley (ISBN 0130801046). Used with
 * permission.
 *
 * see ejmf.toolkit.SourcedTimer see ejmf.toolkit.TimeSource
 *
 * @version 1.0
 * @author Rob Gordon & Steve Talley
 */
public class SourcedTimerEvent extends EventObject
{
    private static final long serialVersionUID = 0L;

    private long time;

    /**
     * Event constructor.
     *
     * @param src
     *            Source of event.
     *
     * @param t
     *            Time in units of source.
     */
    public SourcedTimerEvent(Object src, long t)
    {
        super(src);
        time = t;
    }

    /**
     * Retrieve the time from the event Object.
     *
     * @return Time in units of time source.
     */
    public long getTime()
    {
        return time;
    }

    /**
     * Set the time of event object. This operation for a client unless it
     * happens to re-generated event for its own purposes.
     *
     * @param t
     *            Time in units detemined by caller.
     */
    public void setTime(long t)
    {
        time = t;
    }
}
