package javax.media;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/StopTimeChangeEvent.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public class StopTimeChangeEvent extends ControllerEvent
{
    private static final long serialVersionUID = 0L;

    Time stopTime;

    public StopTimeChangeEvent(Controller from, Time newStopTime)
    {
        super(from);
        this.stopTime = newStopTime;
    }

    public Time getStopTime()
    {
        return stopTime;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "[source=" + getSource() + ",stopTime="
                + stopTime + "]";
    }
}
