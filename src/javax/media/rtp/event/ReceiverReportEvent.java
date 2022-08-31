package javax.media.rtp.event;

import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/event/ReceiverReportEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class ReceiverReportEvent extends RemoteEvent
{
    private static final long serialVersionUID = 0L;

    private ReceiverReport report;

    public ReceiverReportEvent(@SuppressWarnings("deprecation") SessionManager from, ReceiverReport report)
    {
        super(from);
        this.report = report;
    }

    public ReceiverReport getReport()
    {
        return report;
    }
}
