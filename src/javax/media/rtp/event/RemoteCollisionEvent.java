package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/event/RemoteCollisionEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class RemoteCollisionEvent extends RemoteEvent
{
    private static final long serialVersionUID = 0L;

    private long collidingSSRC;

    public RemoteCollisionEvent(@SuppressWarnings("deprecation") SessionManager from, long ssrc)
    {
        super(from);
        this.collidingSSRC = ssrc;
    }

    public long getSSRC()
    {
        return collidingSSRC;
    }
}
