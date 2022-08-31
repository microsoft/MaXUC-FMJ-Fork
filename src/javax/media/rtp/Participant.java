package javax.media.rtp;

import java.util.*;

import javax.media.rtp.rtcp.*;

import net.sf.fmj.media.rtp.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/Participant.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 *
 * @author Ken Larson
 *
 */
public interface Participant
{
    public String getCNAME();

    public Vector<Report> getReports();

    public Vector<SourceDescription> getSourceDescription();

    public Vector<SSRCInfo> getStreams();
}
