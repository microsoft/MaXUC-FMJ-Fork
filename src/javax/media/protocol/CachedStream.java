package javax.media.protocol;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/protocol/CachedStream.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public interface CachedStream
{
    public void abortRead();

    public boolean getEnabledBuffering();

    public void setEnabledBuffering(boolean b);

    public boolean willReadBytesBlock(int numBytes);

    public boolean willReadBytesBlock(long offset, int numBytes);
}
