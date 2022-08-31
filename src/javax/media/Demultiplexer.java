package javax.media;

import java.io.*;

import javax.media.protocol.*;

/**
 *
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/Demultiplexer.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public interface Demultiplexer extends PlugIn, MediaHandler, Duration
{
    public Time getMediaTime();

    public ContentDescriptor[] getSupportedInputContentDescriptors();

    public Track[] getTracks() throws IOException, BadHeaderException;

    public boolean isPositionable();

    public boolean isRandomAccess();

    public Time setPosition(Time where, int rounding);

    public void start() throws IOException;

    public void stop();
}
