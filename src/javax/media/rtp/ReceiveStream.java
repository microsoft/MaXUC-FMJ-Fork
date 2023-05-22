// Portions (c) Microsoft Corporation. All rights reserved.
package javax.media.rtp;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/ReceiveStream.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public interface ReceiveStream extends RTPStream
{
    public ReceptionStats getSourceReceptionStats();

    public void stopReceiving();
}
