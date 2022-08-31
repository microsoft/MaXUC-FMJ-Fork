package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/event/RemoteEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class RemoteEvent extends RTPEvent
{
    private static final long serialVersionUID = 0L;

    public RemoteEvent(@SuppressWarnings("deprecation") SessionManager from)
    {
        super(from);
    }
}
