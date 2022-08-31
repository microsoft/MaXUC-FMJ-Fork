package javax.media.protocol;

import java.io.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/protocol/InputSourceStream.html"
 * target="_blank">this class in the JMF Javadoc</a>. Coding complete.
 *
 * @author Ken Larson
 *
 */
public class InputSourceStream implements PullSourceStream
{
    protected java.io.InputStream stream;

    protected boolean eosReached;
    private ContentDescriptor contentDescriptor;

    public InputSourceStream(java.io.InputStream s, ContentDescriptor type)
    {
        stream = s;
        contentDescriptor = type;
    }

    public void close() throws java.io.IOException
    {
        stream.close();
    }

    @Override
    public boolean endOfStream()
    {
        return eosReached;
    }

    @Override
    public ContentDescriptor getContentDescriptor()
    {
        return contentDescriptor;
    }

    @Override
    public long getContentLength()
    {
        return LENGTH_UNKNOWN; // TODO
    }

    @Override
    public Object getControl(String controlName)
    {
        return null;
    }

    @Override
    public Object[] getControls()
    {
        return new Object[0];
    }

    @Override
    public int read(byte[] buffer, int offset, int length)
            throws java.io.IOException
    {
        int result = stream.read(buffer, offset, length);
        if (result == -1)
            eosReached = true;
        return result;
    }

    @Override
    public boolean willReadBlock()
    {
        try
        {
            return stream.available() <= 0;
        } catch (IOException e)
        {
            return true;
        }
    }
}
