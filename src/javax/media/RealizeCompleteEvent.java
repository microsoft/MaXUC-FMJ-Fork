package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/RealizeCompleteEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class RealizeCompleteEvent extends TransitionEvent
{
    private static final long serialVersionUID = 0L;

    public RealizeCompleteEvent(Controller from, int previous, int current,
            int target)
    {
        super(from, previous, current, target);
    }
}
