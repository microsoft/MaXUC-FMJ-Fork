// Portions (c) Microsoft Corporation. All rights reserved.
package javax.media.rtp.rtcp;

import java.net.*;

import net.sf.fmj.media.*;

/**
 * Standard JMF class -- see <a href=
 * "https://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/rtp/rtcp/SourceDescription.html"
 * target="_blank">this class in the JMF Javadoc</a>. Coding complete.
 *
 * @author Ken Larson
 *
 */
public class SourceDescription implements java.io.Serializable
{
    private static final long serialVersionUID = 0L;

    public static final int SOURCE_DESC_CNAME = 1;

    public static final int SOURCE_DESC_NAME = 2;

    public static final int SOURCE_DESC_EMAIL = 3;

    public static final int SOURCE_DESC_PHONE = 4;

    public static final int SOURCE_DESC_LOC = 5;

    public static final int SOURCE_DESC_TOOL = 6;

    public static final int SOURCE_DESC_NOTE = 7;

    public static final int SOURCE_DESC_PRIV = 8;

    public static String generateCNAME()
    {
        // generate something like user@host
        String osName = System.getProperty("os.name");
        String hostname;
        if (osName.startsWith("Mac"))
        {
            // We are in a critical path for call setup, and getLocalHost can take up
            // to 10s on Mac - so just use a hardcoded hostname.  The CNAME we generate
            // is not very unique, but neither is the Mac hostname - typically
            // MacBook-Pro.local.
            //
            // Consider implementing RFC 7022 (https://tools.ietf.org/html/rfc7022)
            // if we need to choose a better CNAME.
            hostname = "hostname";
        }
        else
        {
            try
            {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            catch (Exception e)
            {
                Log.warning("Hit exception getting local host - set to hostname");
                hostname = "hostname";
            }
        }

        return System.getProperty("user.name") + '@' + hostname;
    }

    private int type;

    private String description;

    private int frequency;

    private boolean encrypted;

    public SourceDescription(int type, String description, int frequency,
            boolean encrypted)
    {
        this.type = type;
        this.description = description;
        this.frequency = frequency;
        this.encrypted = encrypted;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean getEncrypted()
    {
        return encrypted;
    }

    public int getFrequency()
    {
        return frequency;
    }

    public int getType()
    {
        return type;
    }

    public void setDescription(String desc)
    {
        this.description = desc;
    }
}
