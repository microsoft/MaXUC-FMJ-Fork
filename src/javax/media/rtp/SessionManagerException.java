package javax.media.rtp;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/SessionManagerException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class SessionManagerException extends Exception
{
    private static final long serialVersionUID = 0L;

    public SessionManagerException()
    {
        super();
    }

    public SessionManagerException(String message)
    {
        super(message);
    }

    public SessionManagerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SessionManagerException(Throwable cause)
    {
        super(cause);
    }
}
