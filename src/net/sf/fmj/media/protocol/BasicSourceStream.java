package net.sf.fmj.media.protocol;

import javax.media.protocol.*;

public class BasicSourceStream implements SourceStream
{
    protected ContentDescriptor contentDescriptor = null;
    protected long contentLength = LENGTH_UNKNOWN;
    protected Object[] controls = new Object[0];

    public static final int LENGTH_DISCARD = -2;

    public BasicSourceStream()
    {
    }

    public BasicSourceStream(ContentDescriptor cd, long contentLength)
    {
        this.contentDescriptor = cd;
        this.contentLength = contentLength;
    }

    /** Must override this class */
    @Override
    public boolean endOfStream()
    {
        return false;
    }

    @Override
    public ContentDescriptor getContentDescriptor()
    {
        return contentDescriptor;
    }

    @Override
    public long getContentLength()
    {
        return contentLength;
    }

    @Override
    public Object getControl(String controlType)
    {
        try
        {
            Class<?> cls = Class.forName(controlType);
            Object cs[] = getControls();
            for (int i = 0; i < cs.length; i++)
            {
                if (cls.isInstance(cs[i]))
                    return cs[i];
            }
            return null;

        } catch (Exception e)
        { // no such controlType or such control
            return null;
        }
    }

    @Override
    public Object[] getControls()
    {
        return controls;
    }
}
