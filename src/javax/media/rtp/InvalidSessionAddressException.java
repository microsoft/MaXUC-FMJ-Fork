package javax.media.rtp;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/InvalidSessionAddressException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class InvalidSessionAddressException extends SessionManagerException
{
    private static final long serialVersionUID = 0L;

    public InvalidSessionAddressException()
    {
        super();
    }

    public InvalidSessionAddressException(String reason)
    {
        super(reason);
    }
}
