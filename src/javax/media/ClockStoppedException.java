package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/ClockStoppedException.html"
 * target="_blank">this class in the JMF Javadoc</a>.
 *
 * Complete.
 *
 * @author Ken Larson
 *
 */
public class ClockStoppedException extends MediaException
{
    private static final long serialVersionUID = 0L;

    public ClockStoppedException()
    {
        super();
    }

    public ClockStoppedException(String message)
    {
        super(message);
    }
}
