package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/ResourceUnavailableEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class ResourceUnavailableEvent extends ControllerErrorEvent
{
    private static final long serialVersionUID = 0L;

    public ResourceUnavailableEvent(Controller from)
    {
        super(from);
    }

    public ResourceUnavailableEvent(Controller from, String why)
    {
        super(from, why);
    }
}
