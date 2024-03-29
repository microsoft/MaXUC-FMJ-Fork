package javax.media.control;

import javax.media.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/control/StreamWriterControl.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public interface StreamWriterControl extends Control
{
    public long getStreamSize();

    public boolean setStreamSizeLimit(long numOfBytes);
}
