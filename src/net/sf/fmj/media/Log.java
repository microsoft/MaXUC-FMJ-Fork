// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media;

import java.util.*;
import java.util.logging.*;

/**
 * A public static class to generate and write to fmj.log.
 */
public class Log
{
    public static boolean isEnabled = false; /* The default JMF value is false. */
    private static int indent = 0;

    /*
     * Set of objects that have been seen by the logger.  This must be
     * synchronized as multiple threads will write logs.
     */
    static Set<Integer> seenObjects =
        Collections.synchronizedSet(new HashSet<>());

    /**
     * The Logger instance to be used.
     */
    private static Logger logger = Logger.getLogger(Log.class.getName());

    static
    {
        // Check the registry file to see if logging is turned on.
        Object llog = com.sun.media.util.Registry.get("allowLogging");

        if (llog instanceof Boolean)
            isEnabled = (Boolean) llog;

        if (isEnabled)
            writeHeader();
    }

    public static synchronized void comment(Object str)
    {
        if (isEnabled && logger.isLoggable(Level.FINE))
            logger.fine((str!=null ? str.toString() : "null"));
    }

    public static synchronized void info(Object str)
    {
        if (isEnabled && logger.isLoggable(Level.INFO))
            logger.info((str!=null ? str.toString() : "null"));
    }

    public static synchronized void decrIndent()
    {
        indent--;
    }

    /**
     * Dump the stack on hitting an exception.
     * @param e The exception hit.
     */
    public static synchronized void dumpStack(Throwable e)
    {
        if (isEnabled && logger.isLoggable(Level.INFO))
        {
            StringBuffer buf = new StringBuffer(e.toString() + "\n");
            for(StackTraceElement s : e.getStackTrace())
            {
                buf.append(s.toString());
                buf.append("\n");
            }

            logger.info(buf.toString());
        }
    }

    /**
     * Dump the stack from a random point in the code, to provide more info
     * about how we've got to a particular point in the code.
     *
     * @param message Message to include in the log.
     */
    public static synchronized void dumpStack(String message)
    {
        if (isEnabled && logger.isLoggable(Level.FINEST))
        {
            Exception e = new Exception(message);
            StringBuffer buf = new StringBuffer(e.getMessage() + "\n");
            for(StackTraceElement s : e.getStackTrace())
            {
                buf.append(s.toString());
                buf.append("\n");
            }

            logger.finest(buf.toString());
        }
    }

    /**
     * Data on how many packets have been received by various objects in the media
     * stream.
     */
    private static final Map<Integer, int[]> receivedPacketTracker =
            new HashMap<>();

    /**
     * Data on how many packets have been removed by various objects in the media
     * stream
     */
    private static final Map<Integer, int[]> removedPacketTracker =
            new HashMap<>();

    private static int LOG_READ_MAX_INTERVAL = 1024;

    /**
     * Log that this object has received a packet (or other chunk of data).  Logs
     * are made increasingly rarely as the call progresses.
     * @param obj The object making the call (so call as <tt>logRead(this)</tt>)
     */
    public static void logReceived(Object obj)
    {
        logBytes(obj, 0, false, true);
    }

    /**
     * Log that this object has received a packet (or other chunk of data).  Logs
     * are made increasingly rarely as the call progresses.
     * @param obj The object making the call (so call as
     * <tt>logRead(this, nBytes)</tt>)
     * @param nBytes The number of bytes that were read
     */
    public static void logReceivedBytes(Object obj, int nBytes)
    {
        logBytes(obj, nBytes, true, true);
    }

    /**
     * Log that this object has had a packet (or other chunk of data) removed.
     * Logs are made increasingly rarely as the call progresses.
     *
     * @param obj The object making the call (so call as <tt>logRead(this)</tt>)
     */
    public static void logRemoved(Object obj)
    {
        logBytes(obj, 0, false, false);
    }

    /**
     * Log that this object has had a packet (or other chunk of data) removed.
     * Logs are made increasingly rarely as the call progresses.
     *
     * @param obj The object making the call (so call as
     * <tt>logRead(this, nBytes)</tt>)
     * @param nBytes The number of bytes that were read
     */
    public static void logRemovedBytes(Object obj, int nBytes)
    {
        logBytes(obj, nBytes, true, false);
    }

    // This method contains a small per-call memory leak - we never remove
    // the per-object data from the hashMap.  Since the amount of leaked data is
    // very small (~10b per object) that's OK, but if we ever beef this up we
    // should add some removal code as well.
    private static void logBytes(Object obj,
                                 int nBytes,
                                 boolean logBytes,
                                 boolean isReceived)
    {
        Map<Integer, int[]> packetTracker = isReceived ?
                                   receivedPacketTracker : removedPacketTracker;

        if (isEnabled && logger.isLoggable(Level.FINEST))
        {
            synchronized (packetTracker)
            {
                int[] thisData = packetTracker.get(obj.hashCode());
                if (thisData == null)
                {
                    thisData = new int[3];
                    thisData[0] = 0; // Number of times we've been called for this object
                    thisData[1] = 0; // Total number of bytes read
                    thisData[2] = 1; // First packet to log
                }

                int nCalled = thisData[0] + 1;
                int totalBytes = thisData[1] + nBytes;
                int nextCallToLog = thisData[2];

                if (nCalled >= nextCallToLog)
                {
                    logger.finest((isReceived ? "logReceivedBytes called " :
                                                "logRemovedBytes called ") +
                                  nCalled + " times " +
                                  (logBytes ? "(" + totalBytes + " bytes total) " :
                                              "") +
                                  "from " + obj);
                    thisData[2] = (nextCallToLog < LOG_READ_MAX_INTERVAL) ?
                      (2 * nextCallToLog) : (LOG_READ_MAX_INTERVAL + nextCallToLog);
                }

                thisData[0] = nCalled;
                thisData[1] = totalBytes;
                packetTracker.put(obj.hashCode(), thisData);
            }
        }
    }

    /**
     * Common log to indicated when a media object is started (or opened, or
     * whatever else makes sense for that object).  Using a common log makes it
     * easier to automatically parse lifetimes for media stack objects when
     * looking through the logs.
     * @param obj - The object being started
     */
    public static void logMediaStackObjectStarted(Object obj)
    {
        logger.fine("Start media stack object: " + obj);
    }

    /**
     * Common log to indicated when a media object is stopped (or closed, or
     * whatever else makes sense for that object).  Using a common log makes it
     * easier to automatically parse lifetimes for media stack objects when
     * looking through the logs.
     * @param obj - The object being started
     */
    public static void logMediaStackObjectStopped(Object obj)
    {
        logger.fine("Stop media stack object: " + obj);
    }

    public static synchronized void error(Object str)
    {
        if (isEnabled && logger.isLoggable(Level.SEVERE))
        {
            logger.severe((str!=null ? str.toString() : "null"));
        } else
        {
            System.err.println(str);
        }
    }

    public static int getIndent()
    {
        return indent;
    }

    public static synchronized void incrIndent()
    {
        indent++;
    }

    public static synchronized void profile(Object str)
    {
        if (isEnabled && logger.isLoggable(Level.FINER))
            logger.finer((str!=null ? str.toString() : "null"));
    }

    public static synchronized void setIndent(int i)
    {
        indent = i;
    }

    public static synchronized void warning(Object str)
    {
        if (isEnabled && logger.isLoggable(Level.WARNING))
        {
                logger.warning((str!=null ? str.toString() : "null"));
        }
    }

    public static synchronized void write(Object str)
    {
        if (isEnabled && logger.isLoggable(Level.FINE))
        {
            StringBuilder sb = new StringBuilder();
            for (int i = indent; i > 0; i--)
                sb.append("    ");
            sb.append(str!=null ? str.toString() : "null");
            logger.fine(sb.toString());
        }
    }

    private static synchronized void writeHeader()
    {
        write("#\n# FMJ\n#\n");

        String os = null, osver = null, osarch = null;
        String java = null, jver = null;
        try
        {
            os = System.getProperty("os.name");
            osarch = System.getProperty("os.arch");
            osver = System.getProperty("os.version");
            java = System.getProperty("java.vendor");
            jver = System.getProperty("java.version");
        } catch (Throwable e)
        {
            // Can't get the info. No big deal.
            return;
        }

        if (os != null)
            comment("Platform: " + os + ", " + osarch + ", " + osver);
        if (java != null)
            comment("Java VM: " + java + ", " + jver);
        write("");
    }

    public static void objectCreated(Object object,
            String description)
    {
        seenObjects.add(object.hashCode());

        if (isEnabled && logger.isLoggable(Level.FINE))
            logger.fine("CREATE " + object.hashCode() + " " + description);
    }

    public static void createLink(Object source, Object destination, String description)
    {
        if (source != null && ! seenObjects.contains(source.hashCode()))
        {
            logger.fine("LINK missing CREATE for source " + source.toString());
        }

        if (destination != null && ! seenObjects.contains(destination.hashCode()))
        {
            logger.fine("LINK missing CREATE for destination " + destination.toString());
        }

        if (isEnabled && logger.isLoggable(Level.FINE))
            logger.fine("LINK " + ((source == null) ? "null" :source.hashCode()) + " " + ((destination == null) ? "null" : destination.hashCode()) + " " + description);
    }

    public static void annotate(Object source, String description)
    {
        if (isEnabled && logger.isLoggable(Level.FINE))
            logger.fine("ANNOTATE " + source.hashCode() + " " + description);
    }
}
