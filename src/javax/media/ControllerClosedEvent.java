package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/ControllerClosedEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class ControllerClosedEvent extends ControllerEvent
{
    private static final long serialVersionUID = 0L;

    protected String message;

    public ControllerClosedEvent(Controller from)
    {
        super(from);
    }

    public ControllerClosedEvent(Controller from, String why)
    {
        super(from);
        this.message = why;
    }

    public String getMessage()
    {
        return message;
    }
}
