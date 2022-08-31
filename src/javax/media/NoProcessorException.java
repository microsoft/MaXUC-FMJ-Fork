package javax.media;

/**
 *
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/NoProcessorException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class NoProcessorException extends NoPlayerException
{
    private static final long serialVersionUID = 0L;

    public NoProcessorException()
    {
        super();
    }

    public NoProcessorException(String message)
    {
        super(message);
    }
}
