package net.sf.fmj.media.rtp.util;

/**
 * Created by gp on 6/30/14.
 */
public class BadVersionException extends BadFormatException
{
    private static final long serialVersionUID = 0L;

    public BadVersionException(String s)
    {
        super(s);
    }
}
