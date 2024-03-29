// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp;

import java.net.*;
import java.util.*;

import javax.media.format.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

import net.sf.fmj.media.*;
import net.sf.fmj.media.rtp.util.*;

public class SSRCCache
{
    public final SSRCTable<SSRCInfo> cache = new SSRCTable<SSRCInfo>();
    RTPSourceInfoCache sourceInfoCache;
    private OverallStats stats;
    private OverallTransStats transstats;
    RTPEventHandler eventhandler;
    int clockrate[] = new int[128];
    static final int DATA = 1;
    static final int CONTROL = 2;
    static final int SRCDATA = 3;
    static final int RTCP_MIN_TIME = 5000;
    static final int BYE_THRESHOLD = 50;
    int sendercount;
    double rtcp_bw_fraction = 0D;
    double rtcp_sender_bw_fraction = 0D;
    private int rtcp_min_time = 5000;
    int sessionbandwidth = 0;
    boolean initial = true;
    boolean byestate = false;
    boolean rtcpsent = false;
    private int avgrtcpsize = 128;
    SSRCInfo ourssrc;
    public RTPSessionMgr sm;

    SSRCCache(RTPSessionMgr sm)
    {
        stats = sm.defaultstats;
        transstats = sm.transstats;
        sourceInfoCache = new RTPSourceInfoCache();
        sourceInfoCache.setMainCache(sourceInfoCache);
        sourceInfoCache.setSSRCCache(this);
        this.sm = sm;
        eventhandler = new RTPEventHandler(sm);
        setclockrates();
    }

    SSRCCache(RTPSessionMgr sm, RTPSourceInfoCache sic)
    {
        stats = sm.defaultstats;
        transstats = sm.transstats;
        sourceInfoCache = sic;
        sic.setSSRCCache(this);
        this.sm = sm;
        eventhandler = new RTPEventHandler(sm);
    }

    int aliveCount()
    {
        int tot = 0;
        synchronized (cache)
        {
        for (Enumeration<SSRCInfo> e = cache.elements(); e.hasMoreElements();)
        {
            SSRCInfo s = e.nextElement();
            if (s.alive)
                tot++;
        }
        }
        return tot;
    }

    double calcReportInterval(boolean sender, boolean recvfromothers)
    {
        rtcp_min_time = 5000;
        double rtcp_bw = rtcp_bw_fraction;
        if (initial)
            rtcp_min_time = rtcp_min_time / 2;
        int n = aliveCount();
        if (sendercount > 0 && sendercount < n * rtcp_sender_bw_fraction)
            if (sender)
            {
                rtcp_bw *= rtcp_sender_bw_fraction;
                n = sendercount;
            } else
            {
                rtcp_bw *= 1.0D - rtcp_sender_bw_fraction;
                n -= sendercount;
            }
        if (recvfromothers && rtcp_bw == 0.0D)
        {
            rtcp_bw = 0.050000000000000003D;
            if (sendercount > 0 && sendercount < n * 0.25D)
                if (sender)
                {
                    rtcp_bw *= 0.25D;
                    n = sendercount;
                } else
                {
                    rtcp_bw *= 0.75D;
                    n -= sendercount;
                }
        }
        double time = 0.0D;
        if (rtcp_bw != 0.0D)
        {
            time = (avgrtcpsize * n) / rtcp_bw;
            if (time < rtcp_min_time)
                time = rtcp_min_time;
        }
        if (recvfromothers)
            return time;
        else
            return time * (Math.random() + 0.5D);
    }

    private void changessrc(SSRCInfo info)
    {
        info.setOurs(true);
        if (ourssrc != null)
        {
            info.sourceInfo = sourceInfoCache.get(
                    ourssrc.sourceInfo.getCNAME(), info.ours);
            info.sourceInfo.addSSRC(info);
        }
        info.reporter.releasessrc("Local Collision Detected");
        ourssrc = info;
        info.reporter.restart = true;
    }

    synchronized void destroy()
    {
        cache.removeAll();
        if (eventhandler != null)
            eventhandler.close();
    }

    SSRCInfo get(int ssrc, InetAddress address, int port)
    {
        synchronized (this)
        {
            return lookup(ssrc);
        }
    }

    SSRCInfo get(int ssrc, InetAddress address, int port, int mode)
    {
        SSRCInfo info = null;
        boolean localcollision = false;
        synchronized (this)
        {
            if (ourssrc != null && ourssrc.ssrc == ssrc
                    && ourssrc.address != null
                    && !ourssrc.address.equals(address))
            {
                localcollision = true;
                localCollision(ssrc);
            }
            info = lookup(ssrc);
            if (info != null)
            {
                synchronized (info)
                {
                    if (info.address == null || !info.alive)
                    {
                        info.address = address;
                        info.port = port;
                    } else if (!info.address.equals(address))
                        {
                            stats.update(OverallStats.REMOTECOLL, 1);
                            transstats.remote_coll++;
                            RemoteCollisionEvent evt = new RemoteCollisionEvent(
                                    sm, info.ssrc);
                            eventhandler.postEvent(evt);
                            SSRCInfo ssrcinfo5 = null;
                            return ssrcinfo5;
                        }
                }
            }

            if (info != null && mode == 1 && !(info instanceof RecvSSRCInfo))
            {
                if (info.ours)
                {
                    SSRCInfo ssrcinfo1 = null;
                    return ssrcinfo1;
                }

                SSRCInfo newinfo = new RecvSSRCInfo(info);

                Log.info("changing to Recv and setting newrecvstream to true " +
                         "\nExisting one " + info + " " + info.ssrc +
                         "\nNew one " + newinfo + " " + newinfo.ssrc);

                // Mark this as a new receive stream to ensure we fire the
                // correct events for starting the playback engine
                newinfo.newrecvstream = true;
                info = newinfo;
                cache.put(ssrc, info);
            }
            if (info != null && mode == 2 && !(info instanceof PassiveSSRCInfo))
            {
                if (info.ours)
                {
                    return null;
                }

                // This code executes when we already have a RecvSSRCInfo
                // (with its associated resources for an incoming stream)
                // and we receive an RTCP Receiver Report. Receiving an RR
                // does not necessarily mean that the other side has stopped
                // sending RTP, thus changing to PassiveSSRCInfo (which
                // stops and removes the RecvSSRCInfo/ReceiveStream) is
                // not desired.
                // The task of freeing the ReceiveStream resources is left
                // to SSRCCacheCleaner.

                // SSRCInfo newinfo = new PassiveSSRCInfo(info);
                //
                // Log.info("changing to Passive " +
                //          "\nExisting one " + info + " " + info.ssrc +
                //          "\nNew one " + newinfo + " " + newinfo.ssrc);
                //
                // info = newinfo;
                //cache.put(ssrc, info);
            }
            if (info == null)
            {
                if (mode == 3)
                {
                    if (ourssrc != null && ourssrc.ssrc == ssrc)
                    {
                        return ourssrc;
                    }
                    info = new SendSSRCInfo(this, ssrc);
                    info.initsource((int) TrueRandom.rand());
                }
                if (mode == 1)
                    info = new RecvSSRCInfo(this, ssrc);
                if (mode == 2)
                    info = new PassiveSSRCInfo(this, ssrc);
                if (info == null)
                {
                    return null;
                }
                info.address = address;
                info.port = port;
                cache.put(ssrc, info);
            }
            if (info.address == null && info.port == 0)
            {
                info.address = address;
                info.port = port;
            }
            if (localcollision)
            {
                LocalCollisionEvent levt = null;
                if (info instanceof RecvSSRCInfo)
                    levt = new LocalCollisionEvent(sm, (ReceiveStream) info,
                            ourssrc.ssrc);
                else
                    levt = new LocalCollisionEvent(sm, null, ourssrc.ssrc);
                eventhandler.postEvent(levt);
            }
        }
        return info;
    }

    SSRCTable<SSRCInfo> getMainCache()
    {
        return cache;
    }

    RTPSourceInfoCache getRTPSICache()
    {
        return sourceInfoCache;
    }

    int getSessionBandwidth()
    {
        if (sessionbandwidth == 0)
            throw new IllegalArgumentException("Session Bandwidth not set");
        else
            return sessionbandwidth;
    }

    private void localCollision(int ssrc)
    {
        int newssrc = 0;
        do
            newssrc = (int) TrueRandom.rand();
        while (lookup(newssrc) != null);
        SSRCInfo newinfo = new PassiveSSRCInfo(ourssrc);
        newinfo.ssrc = newssrc;
        cache.put(newssrc, newinfo);
        changessrc(newinfo);
        ourssrc = newinfo;
        stats.update(OverallStats.LOCALCOLL, 1);
        transstats.local_coll++;
    }

    SSRCInfo lookup(int ssrc)
    {
        return cache.get(ssrc);
    }

    void remove(int ssrc)
    {
        SSRCInfo info = cache.remove(ssrc);
        if (info != null)
            info.delete();
    }

    public void reset(int size)
    {
        initial = true;
        sendercount = 0;
        avgrtcpsize = size;
    }

    void setclockrates()
    {
        for (int i = 0; i < 16; i++)
            clockrate[i] = 8000;

        clockrate[6] = 16000;
        clockrate[10] = 44100;
        clockrate[11] = 44100;
        clockrate[14] = 0x15f90;
        clockrate[16] = 11025;
        clockrate[17] = 22050;
        clockrate[18] = 44100;
        for (int i = 24; i < 34; i++)
            clockrate[i] = 0x15f90;

        for (int i = 96; i < 128; i++)
        {
            javax.media.Format fmt = sm.formatinfo.get(i);
            if (fmt instanceof AudioFormat)
                clockrate[i] = (int) ((AudioFormat) fmt).getSampleRate();
            else
                clockrate[i] = 0x15f90;
        }
    }

    synchronized void updateavgrtcpsize(int size)
    {
        avgrtcpsize = (int) (0.0625D * size + 0.9375D * avgrtcpsize);
    }
}
