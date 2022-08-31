package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/EndOfMediaEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class EndOfMediaEvent extends StopEvent
{
    private static final long serialVersionUID = 0L;

    public EndOfMediaEvent(Controller from, int previous, int current,
            int target, Time mediaTime)
    {
        super(from, previous, current, target, mediaTime);
    }
}
