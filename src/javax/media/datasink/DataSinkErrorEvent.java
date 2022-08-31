package javax.media.datasink;

import javax.media.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/datasink/DataSinkErrorEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class DataSinkErrorEvent extends DataSinkEvent
{
    private static final long serialVersionUID = 0L;

    public DataSinkErrorEvent(DataSink from)
    {
        super(from);
    }

    public DataSinkErrorEvent(DataSink from, String reason)
    {
        super(from, reason);
    }
}
