// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.util;

/**
 * A thread class on which all FMJ created threads should based.
 */
public class MediaThread extends Thread
{
    private static ThreadGroup threadGroup;
    static boolean securityPrivilege = true;
    private static final boolean debug = false;

    private static int controlPriority = Thread.MAX_PRIORITY - 1;
    private static int audioPriority = Thread.MAX_PRIORITY - 2;
    /* To be less than the Appletpriority */
    private static int videoPriority = Thread.NORM_PRIORITY - 2;
    private static int networkPriority = audioPriority + 1;
    private static int videoNetworkPriority = networkPriority - 1;

    // If you don't have threadgroup and thread permissions.
    private static int defaultMaxPriority = 4;

    static
    {
        try
        {
            defaultMaxPriority = Thread.currentThread().getPriority();
            defaultMaxPriority = Thread.currentThread().getThreadGroup()
                    .getMaxPriority();
        } catch (Throwable e)
        {
            // System.err.println("Permission to manipulate threads and/or thread groups not granted "
            // + e + " : " + e.getMessage());
            securityPrivilege = false;
            // System.out.println("defaultMaxPriority is " +
            // defaultMaxPriority);

            // TODO: tweak these based on testing
            controlPriority = defaultMaxPriority;
            audioPriority = defaultMaxPriority;
            videoPriority = defaultMaxPriority - 1;
            networkPriority = defaultMaxPriority;
            videoNetworkPriority = defaultMaxPriority;

            // TODO: Do the right thing if permissions cannot be obtained.
            // User should be notified via an event
        }

        if (securityPrivilege)
        {
            threadGroup = getRootThreadGroup();
            // System.out.println("threadGroup is " + threadGroup);
        } else
        {
            threadGroup = null;
            // System.out.println("threadGroup is null");
        }
    }

    public static int getAudioPriority()
    {
        return audioPriority;
    }

    public static int getControlPriority()
    {
        return controlPriority;
    }

    public static int getNetworkPriority()
    {
        return networkPriority;
    }

    private static ThreadGroup getRootThreadGroup()
    {
        ThreadGroup current = null;
        try
        {
            current = Thread.currentThread().getThreadGroup();
            ThreadGroup g = current;
            for (; g.getParent() != null; g = g.getParent())
                ;
            // System.out.println("Root threadgroup is " + g);
            return g;
        } catch (Exception e)
        {
            return null; // current
        } catch (Error e)
        {
            return null; // current;
        }
    }

    public static int getVideoNetworkPriority()
    {
        return videoNetworkPriority;
    }

    public static int getVideoPriority()
    {
        return videoPriority;
    }

    public MediaThread()
    {
        this("FMJ Thread");
    }

    public MediaThread(Runnable r)
    {
        this(r, "FMJ Thread");
    }

    public MediaThread(Runnable r, String name)
    {
        super(threadGroup, r, name);
    }

    public MediaThread(String name)
    {
        super(threadGroup, name);
    }

    private void checkPriority(String name, int ask, boolean priv, int got)
    {
        if (ask != got)
        {
            System.out.println("MediaThread: " + name + " privilege? " + priv
                    + "  ask pri: " + ask + " got pri:  " + got);
        }
    }

    /**
     * This should be used for threads handling the audio medium.
     */
    public void useAudioPriority()
    {
        usePriority(audioPriority);
    }

    /**
     * This should be used for Manager, events threads etc. -- the mechanism to
     * maintain the players.
     */
    public void useControlPriority()
    {
        usePriority(controlPriority);
    }

    /**
     * This should be used for threads handling network packets. e.g. RTP
     */
    public void useNetworkPriority()
    {
        usePriority(networkPriority);
    }

    private void usePriority(int priority)
    {
        try
        {
            setPriority(priority);
        } catch (Throwable t)
        {
        }
        if (debug)
        {
            checkPriority("priority", priority, securityPrivilege,
                    getPriority());
        }
    }

    public void useVideoNetworkPriority()
    {
        usePriority(videoNetworkPriority);
    }

    /**
     * This should be used for threads handling the video medium.
     */
    public void useVideoPriority()
    {
        usePriority(videoPriority);
    }
}
