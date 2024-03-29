package javax.media.rtp.event;

import javax.media.*;
import javax.media.rtp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/event/RTPEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class RTPEvent extends MediaEvent
{
    private static final long serialVersionUID = 0L;

    @SuppressWarnings("deprecation")
    private SessionManager eventSrc;

    public RTPEvent(@SuppressWarnings("deprecation") SessionManager from)
    {
        super(from);
        this.eventSrc = from;
    }

    @SuppressWarnings("deprecation")
    public SessionManager getSessionManager()
    {
        return eventSrc;
    }

    @Override
    public Object getSource()
    {
        return eventSrc;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "[source = " + eventSrc + "]";
    }
}
