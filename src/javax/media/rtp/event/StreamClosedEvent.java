package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/event/StreamClosedEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class StreamClosedEvent extends SendStreamEvent
{
    private static final long serialVersionUID = 0L;
    public StreamClosedEvent(@SuppressWarnings("deprecation") SessionManager from, SendStream sendStream)
    {
        super(from, sendStream, null);
    }
}
