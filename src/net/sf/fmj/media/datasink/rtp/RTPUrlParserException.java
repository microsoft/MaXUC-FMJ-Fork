package net.sf.fmj.media.datasink.rtp;

/**
 * Exception thrown by {@link RTPUrlParser}.
 *
 * @author Ken Larson
 *
 */
public class RTPUrlParserException extends Exception
{
    private static final long serialVersionUID = 0L;

    public RTPUrlParserException()
    {
        super();
    }

    public RTPUrlParserException(String message)
    {
        super(message);
    }

    public RTPUrlParserException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RTPUrlParserException(Throwable cause)
    {
        super(cause);
    }
}
