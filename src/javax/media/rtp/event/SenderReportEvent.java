package javax.media.rtp.event;

import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/event/SenderReportEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class SenderReportEvent extends RemoteEvent
{
    private static final long serialVersionUID = 0L;

    private SenderReport report;

    public SenderReportEvent(@SuppressWarnings("deprecation") SessionManager from, SenderReport report)
    {
        super(from);
        this.report = report;
    }

    public SenderReport getReport()
    {
        return report;
    }
}
