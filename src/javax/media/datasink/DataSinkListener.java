package javax.media.datasink;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/datasink/DataSinkListener.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public interface DataSinkListener
{
    public void dataSinkUpdate(DataSinkEvent event);
}
