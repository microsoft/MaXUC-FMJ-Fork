package javax.media.format;

import javax.media.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/format/UnsupportedFormatException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class UnsupportedFormatException extends MediaException
{
    private static final long serialVersionUID = 0L;

    private final Format unsupportedFormat;

    public UnsupportedFormatException(Format unsupportedFormat)
    {
        super();
        this.unsupportedFormat = unsupportedFormat;
    }

    public UnsupportedFormatException(String message, Format unsupportedFormat)
    {
        super(message);
        this.unsupportedFormat = unsupportedFormat;
    }

    public Format getFailedFormat()
    {
        return unsupportedFormat;
    }
}
