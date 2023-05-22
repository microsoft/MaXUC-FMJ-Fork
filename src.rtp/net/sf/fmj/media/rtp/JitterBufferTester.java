// Portions (c) Microsoft Corporation. All rights reserved.
/**
 * This file is historically created by splitting an open source file into
 * multiple files
 */
package net.sf.fmj.media.rtp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class JitterBufferTester
{
    public static final AtomicInteger audioPacketsToDiscard = new AtomicInteger(0);
    public static final AtomicBoolean pleaseReset = new AtomicBoolean(false);
    public static final AtomicBoolean audioDiscardAllowFec = new AtomicBoolean(true);

    public JitterBufferTester(JitterBuffer q, JitterBufferStats stats)
    {
    }

    public static boolean shouldReset()
    {
        return pleaseReset.getAndSet(false);
    }

    public static boolean dropPacketIfRequired(JitterBufferBehaviour behaviour, boolean skipFec)
    {
        if (audioPacketsToDiscard.get() > 0)
        {
            audioPacketsToDiscard.decrementAndGet();
            behaviour.dropPkt();
            skipFec = ! audioDiscardAllowFec.get();
        }
        return skipFec;
    }
}
