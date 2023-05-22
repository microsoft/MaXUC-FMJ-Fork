// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp;

import java.io.*;
import java.net.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

import net.sf.fmj.media.*;
import net.sf.fmj.media.rtp.util.*;
import net.sf.fmj.utility.LoggerSingleton;

import java.util.logging.*;

/**
 * @author Damian Minkov
 * @author Boris Grozev
 * @author Lyubomir Marinov
 */
public class RTPReceiver extends PacketFilter
{
    public class PartiallyProcessedPacketException extends Exception
    {
        private static final long serialVersionUID = 1L;

        public PartiallyProcessedPacketException(String message)
        {
            super(message);
        }
    }

    private class FailedToProcessPacketException extends Exception
    {
        private static final long serialVersionUID = 1L;

        public FailedToProcessPacketException(String message)
        {
            super(message);
        }
    }

    private final SSRCCache cache;
    private final RTPDemultiplexer rtpdemultiplexer;
    private boolean rtcpstarted;
    //private int numPackets = 0;
    private static final int MAX_DROPOUT = 3000;
    private static final int MAX_MISORDER = 100;

    static final int SEQ_MOD = 0x10000;
    static final int MIN_SEQUENTIAL = 2;

    //BufferControl initialized
    private boolean initBC = false;
    private final String controlName;

    /**
     * This class logger.
     */
    private static final Logger logger = LoggerSingleton.logger;

    public RTPReceiver(SSRCCache ssrccache, RTPDemultiplexer rtpdemultiplexer)
    {
        rtcpstarted = false;
        controlName = "javax.media.rtp.RTPControl";
        cache = ssrccache;
        this.rtpdemultiplexer = rtpdemultiplexer;
        setConsumer(null);
        Log.objectCreated(this, "RTPReceiver");
        Log.createLink(this, rtpdemultiplexer, "RTPReceiver uses RTPDemux");
    }

    @Override
    public String filtername()
    {
        return "RTP Packet Receiver";
    }

    @Override
    public Packet handlePacket(Packet packet)
    {
        return handlePacket((RTPPacket) packet);
    }

    @Override
    public Packet handlePacket(Packet packet, int i)
    {
        return null;
    }

    @Override
    public Packet handlePacket(Packet packet, SessionAddress sessionaddress)
    {
        return null;
    }

    /**
     * Handle an RTP packet.
     *
     * @param rtpPacket The packet to process.
     * @return The processed packet. Can be null
     */
    public Packet handlePacket(RTPPacket rtpPacket)
    {
        try
        {
            Log.logReceived(this);

            // Call into this method if you want to do some testing of messing
            // with the types of packets we're receiving for test purposes.
            //hackPacketsForTesting(rtpPacket);

            /*
             * Now the main processing of the RTPReceiver.  Run through various
             * checks to confirm this RTP packet should be processed and, if
             * so, demux it to the appropriate RTP Source Stream (aka Jitter
             * Buffer).
             *
             * If one of the checks fails, that method will throw an exception
             * indicating the packet should be discarded.  These exceptions are
             * handled and logged below.
             */
            //Log.comment("Received RTP packet (seq " + rtpPacket.seqnum + ")");
            handleUnsupportedPayloadType(rtpPacket);
            checkNetworkAddress(rtpPacket);
            SSRCInfo ssrcinfo = getSsrcInfo(rtpPacket);
            processCsrcs(rtpPacket);
            initSsrcInfoIfRequired(rtpPacket, ssrcinfo);
            updateStats(rtpPacket, ssrcinfo);
            handleRTCP(rtpPacket);
            ssrcinfo.maxseq = rtpPacket.seqnum;
            checkPayloadTypeCache(rtpPacket);
            performMisMatchedPayloadCheck(rtpPacket, ssrcinfo);
            setCurrentFormatIfRequired(rtpPacket, ssrcinfo);
            initBufferControlIfRequired(ssrcinfo);
            connectStreamIfRequired(rtpPacket, ssrcinfo);
            fireNewReceiveStreamEventIfRequired(ssrcinfo);
            updateSsrcInfoStats(rtpPacket, ssrcinfo);
            updateQuietStatusIfRequired(ssrcinfo);
            demuxPacket(rtpPacket, ssrcinfo);
        }
        catch (FailedToProcessPacketException e)
        {
            Log.warning("FailedToProcessPacket: " + e);
            rtpPacket = null;
        }
        catch (PartiallyProcessedPacketException e)
        {
            Log.warning("PartiallyProcessedPacket: " + e);
            String message = e.getMessage();
            if (message != null)
            {
                Log.info(message);
            }
        }

        return rtpPacket;
    }

    private void handleUnsupportedPayloadType(RTPPacket rtpPacket) throws PartiallyProcessedPacketException
    {
        if (rtpPacket.payloadType == 13 ||
            rtpPacket.payloadType == 18)
        {
            // Drop CN and G.729 packets as not supported
            throw new PartiallyProcessedPacketException(
                "Dropping unsupported packet type: " + rtpPacket.payloadType);
        }
    }

    private void demuxPacket(RTPPacket rtpPacket,
                             SSRCInfo ssrcinfo)
    {
        if (ssrcinfo.dsource != null)
        {
            // Demux the actual packet
            SourceRTPPacket sourcertppacket = new SourceRTPPacket(rtpPacket, ssrcinfo);
            rtpdemultiplexer.demuxpayload(sourcertppacket);
        }
    }

    private void updateQuietStatusIfRequired(SSRCInfo ssrcinfo)
    {
        if (ssrcinfo.quiet)
        {
            ssrcinfo.quiet = false;
            ActiveReceiveStreamEvent activereceivestreamevent = null;
            if (ssrcinfo instanceof ReceiveStream)
                activereceivestreamevent = new ActiveReceiveStreamEvent(
                        cache.sm, ssrcinfo.sourceInfo, (ReceiveStream) ssrcinfo);
            else
                activereceivestreamevent = new ActiveReceiveStreamEvent(
                        cache.sm, ssrcinfo.sourceInfo, null);
            cache.eventhandler.postEvent(activereceivestreamevent);
        }
    }

    private void updateSsrcInfoStats(RTPPacket rtpPacket, SSRCInfo ssrcinfo)
    {
        if (ssrcinfo.lastRTPReceiptTime != 0L
                && ssrcinfo.lastPayloadType == rtpPacket.payloadType)
        {
            long l = ((Packet) (rtpPacket)).receiptTime
                    - ssrcinfo.lastRTPReceiptTime;
            l = (l * cache.clockrate[ssrcinfo.payloadType]) / 1000L;
            long l1 = rtpPacket.timestamp - ssrcinfo.lasttimestamp;
            double d = l - l1;
            if (d < 0.0D)
                d = -d;
            ssrcinfo.jitter += 0.0625D * (d - ssrcinfo.jitter);
        }
        ssrcinfo.lastRTPReceiptTime = ((Packet) (rtpPacket)).receiptTime;
        ssrcinfo.lasttimestamp = rtpPacket.timestamp;
        ssrcinfo.payloadType = rtpPacket.payloadType;
        ssrcinfo.lastPayloadType = rtpPacket.payloadType;
        ssrcinfo.bytesreceived += rtpPacket.payloadlength;
        ssrcinfo.lastHeardFrom = ((Packet) (rtpPacket)).receiptTime;

        int diff = rtpPacket.seqnum - ssrcinfo.maxseq;
        if (diff > 0)
        {
            if (ssrcinfo.maxseq + 1 != rtpPacket.seqnum)
                ssrcinfo.stats.update(RTPStats.PDULOST, diff - 1);
        }
        else if (diff < 0)
        {
            // Packets arriving out of order have already been counted as lost
            // (by the clause above), so decrease the lost count.
            if (diff > -MAX_MISORDER)
                ssrcinfo.stats.update(RTPStats.PDULOST, -1);
        }
        if (ssrcinfo.wrapped)
            ssrcinfo.wrapped = false;

        net.sf.fmj.media.protocol.rtp.DataSource datasource = (net.sf.fmj.media.protocol.rtp.DataSource) cache.sm.dslist.get(ssrcinfo.ssrc);

        if (datasource == null)
        {
            net.sf.fmj.media.protocol.rtp.DataSource dataSource = cache.sm.getDataSource(null);
            if (dataSource == null)
            {
                datasource = cache.sm.createNewDS(null);
                cache.sm.setDefaultDSassigned(ssrcinfo.ssrc);
            }
            else if (!cache.sm.isDefaultDSassigned())
            {
                datasource = dataSource;
                cache.sm.setDefaultDSassigned(ssrcinfo.ssrc);
            }
            else
            {
                datasource = cache.sm.createNewDS(ssrcinfo.ssrc);
            }
        }

        javax.media.protocol.PushBufferStream apushbufferstream[] =
                                                       datasource.getStreams();

        ssrcinfo.dsource = datasource;
        ssrcinfo.dstream = (RTPSourceStream) apushbufferstream[0];
        ssrcinfo.dstream.setFormat(ssrcinfo.currentformat);

        RTPControlImpl rtpControlImpl =
                     (RTPControlImpl) ssrcinfo.dsource.getControl(controlName);

        if (rtpControlImpl != null)
        {
            Format format = cache.sm.formatinfo.get(rtpPacket.payloadType);
            rtpControlImpl.currentformat = format;
             rtpControlImpl.stream = ssrcinfo;
        }

        ssrcinfo.streamconnect = true;

        if (ssrcinfo.dsource != null)
        {
            ssrcinfo.active = true;
        }
    }

    private void fireNewReceiveStreamEventIfRequired(SSRCInfo ssrcinfo)
    {
        if (ssrcinfo.newrecvstream)
        {
            NewReceiveStreamEvent newreceivestreamevent = new NewReceiveStreamEvent(
                    cache.sm, (ReceiveStream) ssrcinfo);
            ssrcinfo.newrecvstream = false;
            Log.info("Posting NewReceivedStreamEvent for stream " + ssrcinfo.hashCode());
            cache.eventhandler.postEvent(newreceivestreamevent);
        }
    }

    private void connectStreamIfRequired(RTPPacket rtpPacket, SSRCInfo ssrcinfo)
    {
        if (!ssrcinfo.streamconnect)
        {
            net.sf.fmj.media.protocol.rtp.DataSource defaultDataSource = (net.sf.fmj.media.protocol.rtp.DataSource) cache.sm.dslist.get(ssrcinfo.ssrc);

            if (defaultDataSource == null)
            {
                net.sf.fmj.media.protocol.rtp.DataSource dataSource = cache.sm.getDataSource(null);
                int initialCacheSize = cache.sm.dslist.size();

                if (dataSource == null)
                {
                    defaultDataSource = cache.sm.createNewDS(null);
                    cache.sm.setDefaultDSassigned(ssrcinfo.ssrc);
                }
                else if (!cache.sm.isDefaultDSassigned())
                {
                    defaultDataSource = dataSource;
                    cache.sm.setDefaultDSassigned(ssrcinfo.ssrc);
                }
                else
                {
                    defaultDataSource = cache.sm.createNewDS(ssrcinfo.ssrc);
                }
                logger.fine("Datasource for ssrc: " + ssrcinfo.ssrc + " not found in cache list " + cache.sm.dslist.hashCode()
                            + " of RTP session manager: " + cache.sm.hashCode()
                            + ". Adding data source to cache list with size:  " + initialCacheSize
                            + ". After adding datasource cache size is: " + cache.sm.dslist.size());
            }

            javax.media.protocol.PushBufferStream apushbufferstream[] = defaultDataSource.getStreams();

            ssrcinfo.dsource = defaultDataSource;
            ssrcinfo.dstream = (RTPSourceStream) apushbufferstream[0];
            ssrcinfo.dstream.setFormat(ssrcinfo.currentformat);

            RTPControlImpl rtpControlImpl = (RTPControlImpl) ssrcinfo.dsource.getControl(controlName);

            if (rtpControlImpl != null)
            {
                Format format = cache.sm.formatinfo.get(rtpPacket.payloadType);
                rtpControlImpl.currentformat = format;
                rtpControlImpl.stream = ssrcinfo;
            }

            ssrcinfo.streamconnect = true;
        }

        if (ssrcinfo.dsource != null)
        {
            ssrcinfo.active = true;
        }
    }

    private void initBufferControlIfRequired(SSRCInfo ssrcinfo)
    {
        if (!initBC)
        {
            ((BufferControlImpl) cache.sm.buffercontrol)
                    .initBufferControl(ssrcinfo.currentformat);
            initBC = true;
        }
    }

    /**
     * Check whether we have a format for the current packet's payload type.
     * If not, we're not going to be able to process it so throw it away.
     *
     * @param rtpPacket The RTP packet to check
     * @throws PartiallyProcessedPacketException
     */
    private void checkPayloadTypeCache(RTPPacket rtpPacket)
        throws PartiallyProcessedPacketException
    {
        if (cache.sm.formatinfo.get(rtpPacket.payloadType) == null)
        {
            throw new PartiallyProcessedPacketException(
                "No format has been registered for RTP Payload type " +
                rtpPacket.payloadType);
        }
    }

    /**
     * Check whether the payload type of the current packet is different from
     * the previous type on this SSRC.  If it's changed, stop the data source
     * and fire a <tt>RemotePayloadChangeEvent</tt> so that the
     * <tt>MediaStream</tt> will reprogram the codec chain for the new codec.
     *
     * @param rtpPacket The current RTP packet to check
     * @param ssrcinfo The existing SSRC info
     */
    private void performMisMatchedPayloadCheck(RTPPacket rtpPacket,
                                               SSRCInfo ssrcinfo)
    {
        if (ssrcinfo.lastPayloadType != -1 &&
            ssrcinfo.lastPayloadType != rtpPacket.payloadType)
        {
            ssrcinfo.currentformat = null;

            if (ssrcinfo.dsource != null)
            {
                RTPControlImpl rtpcontrolimpl = (RTPControlImpl) ssrcinfo.dsource
                        .getControl(controlName);
                if (rtpcontrolimpl != null)
                {
                    rtpcontrolimpl.currentformat = null;
                    rtpcontrolimpl.payload = -1;
                }

                try
                {
                    StringBuffer buf = new StringBuffer("[");
                    for (PushBufferStream aStream : ssrcinfo.dsource.getStreams())
                    {
                        buf.append(aStream.hashCode());
                        buf.append(" ");
                    }

                    buf.append("]");
                    Log.warning("Stopping datasource " + ssrcinfo.dsource.hashCode() +
                        " (used by stream(s) " + buf.toString() +
                        ")because of payload type mismatch: expecting pt=" +
                        ssrcinfo.lastPayloadType + ", got pt=" +
                        rtpPacket.payloadType);
                    ssrcinfo.dsource.stop();
                }
                catch (IOException ioexception)
                {
                    Log.warning("Problem stopping DataSource after PT change " +
                        ioexception.getMessage());
                }
            }

            ssrcinfo.lastPayloadType = rtpPacket.payloadType;

            RemotePayloadChangeEvent remotepayloadchangeevent = new RemotePayloadChangeEvent(
                    cache.sm, (ReceiveStream) ssrcinfo,
                    ssrcinfo.lastPayloadType, rtpPacket.payloadType);
            cache.eventhandler.postEvent(remotepayloadchangeevent);
        }
    }

    /**
     * Set the format on this SSRC, the data source and the RTP control if we
     * haven't already.
     *
     * @param rtpPacket The current RTP packet to take the format from
     * @param ssrcinfo The current SSRC info
     * @throws PartiallyProcessedPacketException
     */
    private void setCurrentFormatIfRequired(RTPPacket rtpPacket,
                                                   SSRCInfo ssrcinfo)
        throws PartiallyProcessedPacketException
    {
        if (ssrcinfo.currentformat == null)
        {
            // Update the current format from our cache of valid payload types
            // for this stream.  We've already checked the payload type is in
            // the cache in checkPayloadTypeCache().
            //
            // Note: Payload types never get removed from the cache but it's
            // possible they may be changed.  Even if they do, we should just
            // proceed with whatever is currently in the cache and log what
            // we're doing here.
            ssrcinfo.currentformat =
                cache.sm.formatinfo.get(rtpPacket.payloadType);
            if (ssrcinfo.dstream != null)
            {
                Log.info("Setting format on RTPSourceStream to: " +
                    ssrcinfo.currentformat);
                ssrcinfo.dstream.setFormat(ssrcinfo.currentformat);

                // And update the format on the RTP control for this data
                // source.
                if (ssrcinfo.dsource != null)
                {
                    RTPControlImpl rtpControlImpl =
                        (RTPControlImpl)ssrcinfo.dsource.getControl(controlName);
                    if (rtpControlImpl != null)
                    {
                        Log.info("Setting format on RTPControl to: " +
                            ssrcinfo.currentformat);
                        rtpControlImpl.currentformat = ssrcinfo.currentformat;
                    }
                }
            }
        }
    }

    private void handleRTCP(RTPPacket rtpPacket)
    {
        if (cache.sm.isUnicast())
        {
            if (!rtcpstarted)
            {
                cache.sm.startRTCPReports(((UDPPacket) rtpPacket.base).remoteAddress);
                rtcpstarted = true;

                // XXX Not clear why this code is doing what it is.  It appears
                // to checking whether the control IP address ends in 255 and
                // if so, using it.  Otherwise, use the local IP address.
                byte controlAddress[] = cache.sm.controladdress.getAddress();
                int k = controlAddress[3] & 0xff;
                if ((k & 0xff) == 255)
                {
                    cache.sm.addUnicastAddr(cache.sm.controladdress);
                }
                else
                {
                    InetAddress inetaddress1 = null;
                    boolean haveLocalHost = true;
                    try
                    {
                        inetaddress1 = InetAddress.getLocalHost();
                    } catch (UnknownHostException unknownhostexception)
                    {
                        haveLocalHost = false;
                    }
                    if (haveLocalHost)
                        cache.sm.addUnicastAddr(inetaddress1);
                }
            }
            else if (!cache.sm.isSenderDefaultAddr(
                                    ((UDPPacket) rtpPacket.base).remoteAddress))
            {
                cache.sm.addUnicastAddr(
                    ((UDPPacket) rtpPacket.base).remoteAddress);
            }
        }
    }

    private void updateStats(RTPPacket rtpPacket, SSRCInfo ssrcinfo)
    {
        int diff = rtpPacket.seqnum - ssrcinfo.maxseq;

        if (ssrcinfo.maxseq + 1 != rtpPacket.seqnum && diff > 0)
        {
            ssrcinfo.stats.update(RTPStats.PDULOST, diff - 1);
        }

        //Packets arriving out of order have already been counted as lost (by
        //the clause above), so decrease the lost count.
        if (diff > -MAX_MISORDER && diff < 0)
        {
            ssrcinfo.stats.update(RTPStats.PDULOST, -1);
        }

        if (ssrcinfo.wrapped)
        {
            ssrcinfo.wrapped = false;
        }

        if (diff < MAX_DROPOUT)
        {
            if (rtpPacket.seqnum < ssrcinfo.baseseq)
            {
                /*
                 * Vincent Lucas: Without any lost, the seqnum cycles when
                 * passing from 65535 to 0. Thus, diff is equal to -65535. But
                 * if there have been losses, diff may be -65534, -65533, etc.
                 * On the other hand, if diff is too close to 0 (i.e. -1, -2,
                 * etc.), it may correspond to a packet out of sequence. This is
                 * why it is a sound choice to differentiate between a cycle and
                 * an out-of-sequence on the basis of a value in between the two
                 * cases i.e. -65535 / 2.
                 */
                if (diff < -65535 / 2)
                {
                    ssrcinfo.cycles += 0x10000;
                    ssrcinfo.wrapped = true;
                }
            }
            ssrcinfo.maxseq = rtpPacket.seqnum;
        }
        else if (diff <= (65536 - MAX_MISORDER))
        {
            ssrcinfo.stats.update(RTPStats.PDUINVALID);
            if (rtpPacket.seqnum == ssrcinfo.lastbadseq)
                ssrcinfo.initsource(rtpPacket.seqnum);
            else
                ssrcinfo.lastbadseq = rtpPacket.seqnum + 1 & 0xffff;
        }
        else
        {
            /*
             * TODO Boris Grozev: The case of diff==0 is caught in
             * diff<MAX_DROPOUT and does NOT end up here. Is this the way it is
             * supposed to work?
             */
            ssrcinfo.stats.update(RTPStats.PDUDUP);
        }

        ssrcinfo.received++;
        ssrcinfo.stats.update(RTPStats.PDUPROCSD);
    }

    private void initSsrcInfoIfRequired(RTPPacket rtpPacket, SSRCInfo ssrcinfo)
    {
        if (!ssrcinfo.sender)
        {
            ssrcinfo.initsource(rtpPacket.seqnum);
            ssrcinfo.payloadType = rtpPacket.payloadType;
        }
    }

    private void processCsrcs(RTPPacket rtpPacket)
    {
        //update lastHeardFrom fields in the cache for csrc's
        for (int i = 0; i < rtpPacket.csrc.length; i++)
        {
            SSRCInfo csrcinfo = null;
            if (rtpPacket.base instanceof UDPPacket)
                csrcinfo = cache.get(rtpPacket.csrc[i],
                        ((UDPPacket) rtpPacket.base).remoteAddress,
                        ((UDPPacket) rtpPacket.base).remotePort, 1);
            else
                csrcinfo = cache.get(rtpPacket.csrc[i], null, 0, 1);
            if (csrcinfo != null)
                csrcinfo.lastHeardFrom = ((Packet) (rtpPacket)).receiptTime;
        }
    }

    private SSRCInfo getSsrcInfo(RTPPacket rtpPacket)
        throws FailedToProcessPacketException
    {
        SSRCInfo ssrcInfo = null;

        if (rtpPacket.base instanceof UDPPacket)
        {
            ssrcInfo = cache.get(rtpPacket.ssrc,
                                 ((UDPPacket) rtpPacket.base).remoteAddress,
                                 ((UDPPacket) rtpPacket.base).remotePort, 1);
        }
        else
        {
            ssrcInfo = cache.get(rtpPacket.ssrc, null, 0, 1);
        }

        if (ssrcInfo == null)
        {
            throw new FailedToProcessPacketException(String.format(
                "Dropping RTP packet because ssrcinfo couldn't be obtained " +
                "from the cache network address. seqnum=%s, ssrc=%s",
                rtpPacket.seqnum, rtpPacket.ssrc));
        }

        return ssrcInfo;
    }

    /**
     * Check whether this packet is from the expected remote IP address and
     * discard it if not.
     *
     * @param rtppacket The current packet to check
     * @throws FailedToProcessPacketException
     */
    private void checkNetworkAddress(RTPPacket rtppacket)
        throws FailedToProcessPacketException
    {
        if (rtppacket.base instanceof UDPPacket)
        {
            InetAddress inetaddress = ((UDPPacket) rtppacket.base).remoteAddress;
            if (cache.sm.bindtome
                    && !cache.sm.isBroadcast(cache.sm.dataaddress)
                    && !inetaddress.equals(cache.sm.dataaddress))
            {
                throw new FailedToProcessPacketException(String.format(
                    "Dropping RTP packet because of a problem with the " +
                    "network address. seqnum=%s", rtppacket.seqnum));
            }
        }
    }

    /**
     * The following code is for testing only - it allows us to
     * simulate unexpected RTP packets by changing either:
     * - the payload type
     * - the SSRC
     *
     * @param rtpPacket the current packet to possibly hack
     */
//    private void hackPacketsForTesting(RTPPacket rtpPacket)
//    {
//        // Change these variables to actually do any packet hacking!
//        int packetsToHack = 0;
//        int hackedPayloadType = -1; // e.g. 0 for G.711 u-law
//        int hackedSSRC = -1; // e.g. 0x12341234
//        if (numPackets < packetsToHack)
//        {
//            if (hackedPayloadType != -1)
//            {
//                Log.info("Hacking packet " + rtpPacket.seqnum + " to be PT " + hackedPayloadType);
//                rtpPacket.payloadType = hackedPayloadType;
//            }
//
//            if (hackedSSRC != -1)
//            {
//                Log.info("Hacking packet " + rtpPacket.seqnum + " to be SSRC " + hackedSSRC);
//                rtpPacket.ssrc = hackedSSRC;
//            }
//        }
//        else if ((numPackets == packetsToHack) &&
//                 (numPackets != 0))
//        {
//            Log.info("Stopped hacking packets at seq " + rtpPacket.seqnum);
//        }
//
//        numPackets++;
//    }
}
