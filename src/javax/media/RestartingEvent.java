package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/RestartingEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class RestartingEvent extends StopEvent
{
    private static final long serialVersionUID = 0L;

    public RestartingEvent(Controller from, int previous, int current,
            int target, Time mediaTime)
    {
        super(from, previous, current, target, mediaTime);
    }
}
