package javax.media.rtp;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/SSRCInUseException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class SSRCInUseException extends SessionManagerException
{
    private static final long serialVersionUID = 0L;

    public SSRCInUseException()
    {
        super();
    }

    public SSRCInUseException(String message)
    {
        super(message);
    }

    public SSRCInUseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SSRCInUseException(Throwable cause)
    {
        super(cause);
    }
}
