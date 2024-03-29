// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

import net.sf.fmj.media.protocol.rtp.DataSource;
import net.sf.fmj.media.rtp.util.*;

/**
 * @author Emil Ivov - modified binding to local addresses.
 */
@SuppressWarnings("deprecation")
public class RTPSessionMgr extends RTPManager implements SessionManager
{
    private static final Logger logger = Logger.getLogger(RTPSessionMgr.class
            .getName());

    private static final String SOURCE_DESC_TOOL = "FMJ RTP Player";

    public static boolean formatSupported(Format format)
    {
        if (supportedList == null)
        {
            supportedList = new FormatInfo();
        }
        if (supportedList.getPayload(format) != -1)
        {
            return true;
        }
        for (int i = 0; i < addedList.size(); i++)
        {
            Format format1 = (Format) addedList.elementAt(i);
            if (format1.matches(format))
            {
                return true;
            }
        }

        return false;
    }

    boolean bindtome = false;
    private SSRCCache cache = null;
    int ttl = 0;
    int sendercount = 0;
    InetAddress localDataAddress = null;
    int localDataPort = 0;
    InetAddress localControlAddress = null;
    int localControlPort = 0;
    InetAddress dataaddress = null;
    InetAddress controladdress = null;
    int dataport = 0;
    int controlport = 0;
    RTPPushDataSource rtpsource = null;
    RTPPushDataSource rtcpsource = null;
    long defaultSSRC = 0;
    SessionAddress localSenderAddress = null;
    private SessionAddress localReceiverAddress = null;
    UDPPacketSender udpsender = null;
    RTPPacketSender rtpsender = null;
    RTCPRawSender sender = null;
    SSRCCacheCleaner cleaner = null;
    private boolean unicast = false;
    private boolean startedparticipating = false;
    private boolean nonparticipating = false;
    private boolean nosockets = false;
    private boolean started = false;
    private boolean initialized = false;
    protected Vector<SessionListener> sessionlistener = null;
    protected Vector<RemoteListener> remotelistener = null;
    protected Vector<ReceiveStreamListener> streamlistener = null;
    protected Vector<SendStreamListener> sendstreamlistener = null;
    boolean encryption = false;
    SSRCTable<DataSource> dslist = null;
    StreamSynch streamSynch = null;
    FormatInfo formatinfo = null;
    DataSource defaultsource = null;
    PushBufferStream defaultstream = null;
    Format defaultformat = null;
    BufferControl buffercontrol = null;
    OverallStats defaultstats = null;
    OverallTransStats transstats = null;
    int defaultsourceid = 0;
    Vector<SendStream> sendstreamlist = null;
    RTPTransmitter rtpTransmitter = null;
    private RTCPReportBuilder rtcpReportBuilder;
    boolean bds = false;
    Vector<SessionAddress> peerlist = null;
    boolean multi_unicast = false;
    Hashtable<SessionAddress, PacketForwarder> peerrtplist = null;
    Hashtable<SessionAddress, PacketForwarder> peerrtcplist = null;
    static FormatInfo supportedList = null;
    static Vector<Format> addedList = new Vector<Format>();
    private boolean newRtpInterface = false;
    private SessionAddress remoteAddress = null;
    private SessionAddress localAddress = null;
    private RTCPRawReceiver rtcpRawReceiver = null;
    private RTPRawReceiver rtpRawReceiver = null;
    private PacketForwarder rtpForwarder = null;
    private PacketForwarder rtcpForwarder = null;
    private RTPDemultiplexer rtpDemultiplexer = null;
    private boolean participating = false;
    private UDPPacketSender udpPacketSender = null;
    private Vector<SessionAddress> remoteAddresses = null;
    private RTCPTransmitter rtcpTransmitter = null;
    private RTPConnector rtpConnector = null;
    private DatagramSocket dataSocket = null;
    private DatagramSocket controlSocket = null;

    public RTPSessionMgr()
    {
        logger.fine("Creating RTPSessionMgr: " + this);
        bindtome = false;
        cache = null;
        sendercount = 0;
        localDataAddress = null;
        localDataPort = 0;
        localControlAddress = null;
        localControlPort = 0;
        dataaddress = null;
        controladdress = null;
        dataport = 0;
        controlport = 0;
        rtpsource = null;
        rtcpsource = null;
        defaultSSRC = 0L;
        udpsender = null;
        rtpsender = null;
        sender = null;
        cleaner = null;
        unicast = false;
        startedparticipating = false;
        nonparticipating = false;
        nosockets = false;
        started = false;
        initialized = false;
        sessionlistener = new Vector<SessionListener>();
        remotelistener = new Vector<RemoteListener>();
        streamlistener = new Vector<ReceiveStreamListener>();
        sendstreamlistener = new Vector<SendStreamListener>();
        encryption = false;
        dslist = new SSRCTable<DataSource>();
        formatinfo = null;
        defaultsource = null;
        defaultstream = null;
        defaultformat = null;
        buffercontrol = null;
        defaultstats = null;
        transstats = null;
        defaultsourceid = 0;
        sendstreamlist = new Vector<SendStream>(1);
        rtpTransmitter = null;
        bds = false;
        peerlist = new Vector<SessionAddress>();
        multi_unicast = false;
        peerrtplist = new Hashtable<SessionAddress, PacketForwarder>(5);
        peerrtcplist = new Hashtable<SessionAddress, PacketForwarder>(5);
        newRtpInterface = false;
        formatinfo = new FormatInfo();
        buffercontrol = new BufferControlImpl();
        defaultstats = new OverallStats();
        transstats = new OverallTransStats();
        streamSynch = new StreamSynch();
    }

    public RTPSessionMgr(DataSource datasource) throws IOException
    {
        bindtome = false;
        cache = null;
        sendercount = 0;
        localDataAddress = null;
        localDataPort = 0;
        localControlAddress = null;
        localControlPort = 0;
        dataaddress = null;
        controladdress = null;
        dataport = 0;
        controlport = 0;
        rtpsource = null;
        rtcpsource = null;
        defaultSSRC = 0L;
        udpsender = null;
        rtpsender = null;
        sender = null;
        cleaner = null;
        unicast = false;
        startedparticipating = false;
        nonparticipating = false;
        nosockets = false;
        started = false;
        initialized = false;
        sessionlistener = new Vector<SessionListener>();
        remotelistener = new Vector<RemoteListener>();
        streamlistener = new Vector<ReceiveStreamListener>();
        sendstreamlistener = new Vector<SendStreamListener>();
        encryption = false;
        dslist = new SSRCTable<DataSource>();
        formatinfo = null;
        defaultsource = null;
        defaultstream = null;
        defaultformat = null;
        buffercontrol = null;
        defaultstats = null;
        transstats = null;
        defaultsourceid = 0;
        sendstreamlist = new Vector<SendStream>(1);
        rtpTransmitter = null;
        bds = false;
        peerlist = new Vector<SessionAddress>();
        multi_unicast = false;
        peerrtplist = new Hashtable<SessionAddress, PacketForwarder>(5);
        peerrtcplist = new Hashtable<SessionAddress, PacketForwarder>(5);
        newRtpInterface = false;
        formatinfo = new FormatInfo();
        buffercontrol = new BufferControlImpl();
        defaultstats = new OverallStats();
        transstats = new OverallTransStats();
        UpdateEncodings(datasource);
        RTPMediaLocator rtpmedialocator = null;
        try
        {
            rtpmedialocator = new RTPMediaLocator(datasource.getLocator()
                    .toString());
        } catch (MalformedURLException malformedurlexception)
        {
            throw new IOException("RTP URL is Malformed "
                    + malformedurlexception.getMessage());
        }
        DataSource datasource1 = createNewDS(rtpmedialocator);
        RTPControl rtpcontrol = (RTPControl) datasource
                .getControl("javax.media.rtp.RTPControl");
        datasource1.setControl(rtpcontrol);
        String s = rtpmedialocator.getSessionAddress();
        dataport = rtpmedialocator.getSessionPort();
        controlport = dataport + 1;
        ttl = rtpmedialocator.getTTL();
        try
        {
            dataaddress = InetAddress.getByName(s);
        } catch (Throwable throwable1)
        {
            logger.log(Level.WARNING, "error retrieving address " + s
                    + " by name" + throwable1.getMessage(), throwable1);
        }
        controladdress = dataaddress;
        SessionAddress sessionaddress = new SessionAddress();
        try
        {
            initSession(sessionaddress, setSDES(), 0.050000000000000003D, 0.25D);
        } catch (SessionManagerException sessionmanagerexception)
        {
            throw new IOException("SessionManager exception "
                    + sessionmanagerexception.getMessage());
        }
    }

    public RTPSessionMgr(RTPPushDataSource rtppushdatasource)
    {
        bindtome = false;
        cache = null;
        sendercount = 0;
        localDataAddress = null;
        localDataPort = 0;
        localControlAddress = null;
        localControlPort = 0;
        dataaddress = null;
        controladdress = null;
        dataport = 0;
        controlport = 0;
        rtpsource = null;
        rtcpsource = null;
        defaultSSRC = 0L;
        udpsender = null;
        rtpsender = null;
        sender = null;
        cleaner = null;
        unicast = false;
        startedparticipating = false;
        nonparticipating = false;
        nosockets = false;
        started = false;
        initialized = false;
        sessionlistener = new Vector<SessionListener>();
        remotelistener = new Vector<RemoteListener>();
        streamlistener = new Vector<ReceiveStreamListener>();
        sendstreamlistener = new Vector<SendStreamListener>();
        encryption = false;
        dslist = new SSRCTable<DataSource>();
        formatinfo = null;
        defaultsource = null;
        defaultstream = null;
        defaultformat = null;
        buffercontrol = null;
        defaultstats = null;
        transstats = null;
        defaultsourceid = 0;
        sendstreamlist = new Vector<SendStream>(1);
        rtpTransmitter = null;
        bds = false;
        peerlist = new Vector<SessionAddress>();
        multi_unicast = false;
        peerrtplist = new Hashtable<SessionAddress, PacketForwarder>(5);
        peerrtcplist = new Hashtable<SessionAddress, PacketForwarder>(5);
        newRtpInterface = false;
        nosockets = true;
        rtpsource = rtppushdatasource;
        if (rtpsource instanceof RTPSocket)
        {
            rtcpsource = ((RTPSocket) rtpsource).getControlChannel();
        }
        formatinfo = new FormatInfo();
        buffercontrol = new BufferControlImpl();
        defaultstats = new OverallStats();
        transstats = new OverallTransStats();
        DataSource datasource = createNewDS(((RTPMediaLocator) (null)));
        UpdateEncodings(rtppushdatasource);
        RTPControl rtpcontrol = (RTPControl) rtppushdatasource
                .getControl(RTPControl.class.getName());
        datasource.setControl(rtpcontrol);
        initSession(setSDES(), 0.050000000000000003D, 0.25D);
        startSession(rtpsource, rtcpsource, null);
    }

    @Override
    public void addFormat(Format format, int i)
    {
        if (formatinfo != null)
        {
            formatinfo.add(i, format);
        }
        if (format != null)
        {
            addedList.addElement(format);
        }
    }

    public void addMRL(RTPMediaLocator rtpmedialocator)
    {
        int i = (int) rtpmedialocator.getSSRC();
        if (i == 0)
        {
            return;
        }
        DataSource datasource = (DataSource) dslist.get(i);
        if (datasource != null)
        {
            return;
        } else
        {
            DataSource datasource1 = createNewDS(rtpmedialocator);
            return;
        }
    }

    @Override
    public void addPeer(SessionAddress sessionaddress) throws IOException,
            InvalidSessionAddressException
    {
        RTCPRawReceiver rtcprawreceiver;
        RTPRawReceiver rtprawreceiver;
        InetAddress inetaddress;
        InetAddress inetaddress1;
        int j;
        int k;
        InetAddress inetaddress2;
        for (int i = 0; i < peerlist.size(); i++)
        {
            SessionAddress sessionaddress1 = (SessionAddress) peerlist
                    .elementAt(i);
            if (sessionaddress1.equals(sessionaddress))
            {
                return;
            }
        }

        peerlist.addElement(sessionaddress);
        CheckRTPPorts(sessionaddress.getDataPort(),
                sessionaddress.getControlPort());
        rtcprawreceiver = null;
        rtprawreceiver = null;
        inetaddress = sessionaddress.getDataAddress();
        inetaddress1 = sessionaddress.getControlAddress();
        j = sessionaddress.getDataPort();
        k = sessionaddress.getControlPort();
        CheckRTPAddress(inetaddress, inetaddress1);
        inetaddress2 = null;
        try
        {
            inetaddress2 = InetAddress.getLocalHost();
        } catch (Throwable throwable1)
        {
            logger.log(Level.WARNING, "InitSession : UnknownHostExcpetion "
                    + throwable1.getMessage(), throwable1);
        }
        if (!inetaddress.isMulticastAddress()
                && !inetaddress.equals(inetaddress2))
        {
            if (isBroadcast(inetaddress) && !Win32())
            {
                bindtome = false;
            } else
            {
                bindtome = true;
            }
        }
        if (bindtome)
        {
            return;
        }
        try
        {
            rtcprawreceiver = new RTCPRawReceiver(k,
                    inetaddress1.getHostAddress(), defaultstats, streamSynch);
            if (inetaddress != null)
            {
                rtprawreceiver = new RTPRawReceiver(j,
                        inetaddress.getHostAddress(), defaultstats);
            }
        } catch (SocketException socketexception)
        {
            throw new IOException(socketexception.getMessage());
        } finally
        {
            if (inetaddress != null && rtprawreceiver == null
                    && rtcprawreceiver != null)
            {
                logger.warning("could not create RTCP/RTP raw receivers");
                rtcprawreceiver.closeSource();
            }
        }

        try
        {
            rtcprawreceiver = new RTCPRawReceiver(k,
                    inetaddress2.getHostAddress(), defaultstats, streamSynch);
            if (inetaddress != null)
            {
                rtprawreceiver = new RTPRawReceiver(j,
                        inetaddress2.getHostAddress(), defaultstats);
            }
        } catch (SocketException socketexception1)
        {
            throw new IOException(socketexception1.getMessage());
        } finally
        {
            if (inetaddress != null && rtprawreceiver == null
                    && rtcprawreceiver != null)
            {
                logger.warning("could not create RTCP/RTP raw receivers");
                rtcprawreceiver.closeSource();
            }
        }
        PacketForwarder packetforwarder = new PacketForwarder(rtcprawreceiver,
                new RTCPReceiver(cache));
        PacketForwarder packetforwarder1 = null;
        if (rtprawreceiver != null)
        {
            packetforwarder1 = new PacketForwarder(rtprawreceiver,
                    new RTPReceiver(cache, rtpDemultiplexer));
        }
        packetforwarder.startPF("RTCP Forwarder for address"
                + inetaddress1.toString() + "port " + k);
        if (packetforwarder1 != null)
        {
            packetforwarder1.startPF("RTP Forwarder for address "
                    + inetaddress.toString() + "port " + j);
        }
        peerrtplist.put(sessionaddress, packetforwarder1);
        peerrtcplist.put(sessionaddress, packetforwarder);
        if (cache.ourssrc != null)
        {
            if (cache.ourssrc.reporter == null)
            {
                controladdress = inetaddress1;
                controlport = k;
                cache.ourssrc.reporter = startParticipating(k,
                        inetaddress.getHostAddress(), cache.ourssrc);
            }
            if (((PacketFilter) (cache.ourssrc.reporter.transmit.sender)).peerlist == null)
            {
                cache.ourssrc.reporter.transmit.sender.peerlist = new Vector<SessionAddress>();
            }
        }
        ((PacketFilter) (cache.ourssrc.reporter.transmit.sender)).peerlist
                .addElement(sessionaddress);
        if (cache != null)
        {
            for (Enumeration<?> elements = cache.cache.elements();
                    elements.hasMoreElements();)
            {
                SSRCInfo ssrcinfo = (SSRCInfo) elements.nextElement();
                if (ssrcinfo instanceof SendSSRCInfo)
                {
                    ssrcinfo.reporter.transmit.sender.control = true;
                    if (((PacketFilter) (ssrcinfo.reporter.transmit.sender)).peerlist == null)
                    {
                        ssrcinfo.reporter.transmit.sender.peerlist = new Vector<SessionAddress>();
                        ((PacketFilter) (ssrcinfo.reporter.transmit.sender)).peerlist
                                .addElement(sessionaddress);
                    }
                }
            }
        }
        for (int l = 0; l < sendstreamlist.size(); l++)
        {
            SendSSRCInfo sendssrcinfo = (SendSSRCInfo) sendstreamlist
                    .elementAt(l);
            if (((PacketFilter) (((SSRCInfo) (sendssrcinfo)).sinkstream.transmitter.sender)).peerlist == null)
            {
                ((SSRCInfo) (sendssrcinfo)).sinkstream.transmitter.sender.peerlist = new Vector<SessionAddress>();
                ((PacketFilter) (((SSRCInfo) (sendssrcinfo)).sinkstream.transmitter.sender)).peerlist
                        .addElement(sessionaddress);
            }
        }
    }

    @Override
    public void addReceiveStreamListener(
            ReceiveStreamListener receivestreamlistener)
    {
        if (!streamlistener.contains(receivestreamlistener))
        {
            streamlistener.addElement(receivestreamlistener);
        }
    }

    @Override
    public void addRemoteListener(RemoteListener remotelistener1)
    {
        if (!remotelistener.contains(remotelistener1))
        {
            remotelistener.addElement(remotelistener1);
        }
    }

    void addSendStream(SendStream sendstream)
    {
        sendstreamlist.addElement(sendstream);
    }

    @Override
    public void addSendStreamListener(SendStreamListener sendstreamlistener1)
    {
        if (!sendstreamlistener.contains(sendstreamlistener1))
        {
            sendstreamlistener.addElement(sendstreamlistener1);
        }
    }

    @Override
    public void addSessionListener(SessionListener sessionlistener1)
    {
        if (!sessionlistener.contains(sessionlistener1))
        {
            sessionlistener.addElement(sessionlistener1);
        }
    }

    @Override
    public void addTarget(SessionAddress sessionaddress) throws IOException
    {
        remoteAddresses.addElement(sessionaddress);
        if (remoteAddresses.size() > 1)
        {
            setRemoteAddresses();
            return;
        }
        remoteAddress = sessionaddress;
        logger.finest("Added target: " + sessionaddress);
        try
        {
            rtcpRawReceiver = new RTCPRawReceiver(localAddress, sessionaddress,
                    defaultstats, streamSynch, controlSocket);
            rtpRawReceiver = new RTPRawReceiver(localAddress, sessionaddress,
                    defaultstats, dataSocket);
        } catch (SocketException socketexception)
        {
            throw new IOException(socketexception.getMessage());
        } catch (UnknownHostException unknownhostexception)
        {
            throw new IOException(unknownhostexception.getMessage());
        }
        rtpDemultiplexer = new RTPDemultiplexer(cache, rtpRawReceiver,
                streamSynch);
        rtcpForwarder = new PacketForwarder(rtcpRawReceiver, new RTCPReceiver(
                cache));
        if (rtpRawReceiver != null)
        {
            rtpForwarder = new PacketForwarder(rtpRawReceiver, new RTPReceiver(
                    cache, rtpDemultiplexer));
        }
        rtcpForwarder.startPF("RTCP Forwarder for address"
                + sessionaddress.getControlHostAddress() + " port "
                + sessionaddress.getControlPort());
        if (rtpForwarder != null)
        {
            rtpForwarder.startPF("RTP Forwarder for address "
                    + sessionaddress.getDataHostAddress() + " port "
                    + sessionaddress.getDataPort());
        }
        cleaner = new SSRCCacheCleaner(cache, streamSynch);
        if (cache.ourssrc != null && participating)
        {
            cache.ourssrc.reporter = startParticipating(rtcpRawReceiver.socket);
        }
    }

    public void addUnicastAddr(InetAddress inetaddress)
    {
        if (sender != null)
        {
            sender.addDestAddr(inetaddress);
        }
    }

    private void CheckRTPAddress(InetAddress inetaddress,
            InetAddress inetaddress1) throws InvalidSessionAddressException
    {
        if (inetaddress == null && inetaddress1 == null)
        {
            throw new InvalidSessionAddressException(
                    "Data and control addresses are null");
        }
        else if (inetaddress1 == null)
        {
            inetaddress1 = inetaddress;
        }
        else if (inetaddress == null)
        {
            inetaddress = inetaddress1;
        }
    }

    private void CheckRTPPorts(int i, int j)
            throws InvalidSessionAddressException
    {
        if (i == 0 || i == -1)
        {
            i = j - 1;
        }
        if (j == 0 || j == -1)
        {
            j = i + 1;
        }
        if (i % 2 != 0)
        {
            throw new InvalidSessionAddressException(
                    "Data Port must be valid and even");
        }
        if (j != 0 && j % 2 != 1)
        {
            throw new InvalidSessionAddressException(
                    "Control Port must be valid and odd");
        }
        if (j != i + 1)
        {
            throw new InvalidSessionAddressException(
                    "Control Port must be one higher than the Data Port");
        }
    }

    public void closeSession()
    {
        if (dslist.isEmpty() || nosockets)
        {
            closeSession("DataSource disconnected");
        }
    }

    @Override
    public void closeSession(String s)
    {
        stopParticipating(s, cache.ourssrc);
        if (defaultsource != null)
            defaultsource.disconnect();
        if (cache != null)
        {
            SSRCInfo ssrcinfo;

            for (Enumeration<?> elements = cache.cache.elements();
                    elements.hasMoreElements();
                    stopParticipating(s, ssrcinfo))
            {
                ssrcinfo = (SSRCInfo) elements.nextElement();
                if (ssrcinfo.dstream != null)
                    ssrcinfo.dstream.close();
                if (ssrcinfo instanceof SendSSRCInfo)
                    ((SendSSRCInfo) ssrcinfo).close();
            }
        }
        for (int i = 0; i < sendstreamlist.size(); i++)
            removeSendStream((SendStream) sendstreamlist.elementAt(i));

        if (rtpTransmitter != null)
            rtpTransmitter.close();
        if (rtcpForwarder != null)
        {
            RTCPRawReceiver rtcprawreceiver
                = (RTCPRawReceiver) rtcpForwarder.getSource();
            rtcpForwarder.close();
            if (rtcprawreceiver != null)
                rtcprawreceiver.close();
        }
        if (cleaner != null)
            cleaner.stop();
        if (cache != null)
            cache.destroy();
        if (rtpForwarder != null)
        {
            RTPRawReceiver rtprawreceiver
                = (RTPRawReceiver) rtpForwarder.getSource();
            rtpForwarder.close();
            if (rtprawreceiver != null)
                rtprawreceiver.close();
        }
        if (multi_unicast)
            removeAllPeers();
    }

    public DataSource createNewDS(int i)
    {
        DataSource datasource = new DataSource();
        datasource.setContentType("raw");
        try
        {
            datasource.connect();
        } catch (IOException ioexception)
        {
            logger.log(Level.WARNING, "Error connecting data source "
                    + ioexception.getMessage(), ioexception);
        }
        RTPSourceStream rtpsourcestream = new RTPSourceStream(datasource);
        ((BufferControlImpl) buffercontrol).addSourceStream(rtpsourcestream);
        dslist.put(i, datasource);
        datasource.setSSRC(i);
        datasource.setMgr(this);
        return datasource;
    }

    public DataSource createNewDS(RTPMediaLocator rtpmedialocator)
    {
        DataSource datasource = new DataSource();
        datasource.setContentType("raw");
        try
        {
            datasource.connect();
        } catch (IOException ioexception)
        {
            logger.log(Level.WARNING, "IOException in createNewDS() "
                    + ioexception.getMessage(), ioexception);
        }
        RTPSourceStream rtpsourcestream = new RTPSourceStream(datasource);
        ((BufferControlImpl) buffercontrol).addSourceStream(rtpsourcestream);
        if (rtpmedialocator != null && (int) rtpmedialocator.getSSRC() != 0)
        {
            dslist.put((int) rtpmedialocator.getSSRC(), datasource);
            datasource.setSSRC((int) rtpmedialocator.getSSRC());
            datasource.setMgr(this);
        } else
        {
            defaultsource = datasource;
            defaultstream = rtpsourcestream;
        }
        return datasource;
    }

    @Override
    public SendStream createSendStream(int i,
            javax.media.protocol.DataSource datasource, int j)
            throws UnsupportedFormatException, IOException, SSRCInUseException
    {
        SSRCInfo ssrcinfo = cache.lookup(i);
        if (ssrcinfo != null)
        {
            throw new SSRCInUseException("SSRC supplied is already in use");
        }
        int k = i;
        if (cache.rtcp_bw_fraction == 0.0D)
        {
            throw new IOException(
                    "Initialized with zero RTP/RTCP outgoing bandwidth. Cannot create a sending stream ");
        }
        PushBufferStream apushbufferstream[] = ((PushBufferDataSource) datasource)
                .getStreams();
        PushBufferStream pushbufferstream = apushbufferstream[j];
        Format format = pushbufferstream.getFormat();
        int l = formatinfo.getPayload(format);
        if (l == -1)
        {
            throw new UnsupportedFormatException(
                    "Format of Stream not supported in RTP Session Manager",
                    format);
        }
        SendSSRCInfo obj = null;
        if (sendercount == 0)
        {
            obj = new SendSSRCInfo(cache.ourssrc);
            obj.ours = true;
            cache.ourssrc = obj;
            cache.getMainCache().put(obj.ssrc, obj);
        } else
        {
            obj = (SendSSRCInfo) cache.get(k, dataaddress, dataport, 3);
            obj.ours = true;
            if (!nosockets)
            {
                obj.reporter = startParticipating(controlport,
                        controladdress.getHostAddress(), obj);
            } else
            {
                obj.reporter = startParticipating(rtcpsource, obj);
            }
        }
        obj.payloadType = l;
        ((SSRCInfo) (obj)).sinkstream.setSSRCInfo(obj);
        obj.setFormat(format);
        if (format instanceof VideoFormat)
        {
            obj.clockrate = 0x15f90;
        } else if (format instanceof AudioFormat)
        {
            obj.clockrate = (int) ((AudioFormat) format).getSampleRate();
        } else
        {
            throw new UnsupportedFormatException("Format not supported", format);
        }
        obj.pds = datasource;
        pushbufferstream.setTransferHandler(((SSRCInfo) (obj)).sinkstream);
        if (multi_unicast)
        {
            if (peerlist.size() > 0)
            {
                SessionAddress sessionaddress = (SessionAddress) peerlist
                        .firstElement();
                dataport = sessionaddress.getDataPort();
                dataaddress = sessionaddress.getDataAddress();
            } else
            {
                throw new IOException("At least one peer must be added");
            }
        }
        if (rtpTransmitter == null)
        {
            logger.fine("rtpTransmitter is null, so must create one");
            logger.fine("rtpConnector: " + rtpConnector +
                        ", nosockets: " + nosockets +
                        ", newRtpInterface: " + newRtpInterface);
            if (rtpConnector != null)
            {
                rtpTransmitter = startDataTransmission(rtpConnector);
            } else if (nosockets)
            {
                rtpTransmitter = startDataTransmission(rtpsource);
            } else
            {
                if (newRtpInterface)
                {
                    if (remoteAddress == null)
                    {
                        throw new IllegalStateException("remoteAddress is null, so cannot start data transmission");
                    }

                    dataport = remoteAddress.getDataPort();
                    dataaddress = remoteAddress.getDataAddress();
                }
                if (dataaddress == null)
                {
                    throw new IllegalStateException("dataaddress is null, so cannot start data transmission");
                }
                rtpTransmitter = startDataTransmission(dataport,
                                                       dataaddress.getHostAddress());
            }
            if (rtpTransmitter == null)
            {
                throw new IOException("Cannot create a transmitter");
            }
        }
        ((SSRCInfo) (obj)).sinkstream.setTransmitter(rtpTransmitter);
        addSendStream(obj);
        if (multi_unicast)
        {
            for (int i1 = 0; i1 < peerlist.size(); i1++)
            {
                SessionAddress sessionaddress1 = (SessionAddress) peerlist
                        .elementAt(i1);
                if (((PacketFilter) (((SSRCInfo) (obj)).sinkstream.transmitter.sender)).peerlist == null)
                {
                    ((SSRCInfo) (obj)).sinkstream.transmitter.sender.peerlist = new Vector<SessionAddress>();
                }
                ((PacketFilter) (((SSRCInfo) (obj)).sinkstream.transmitter.sender)).peerlist
                        .addElement(sessionaddress1);
                if (cache != null)
                {
                    for (Enumeration<?> elements = cache.cache.elements();
                            elements.hasMoreElements();)
                    {
                        SSRCInfo ssrcinfo1 = (SSRCInfo) elements.nextElement();
                        if (ssrcinfo1 instanceof SendSSRCInfo)
                        {
                            ssrcinfo1.reporter.transmit.sender.control = true;
                            if (((PacketFilter) (ssrcinfo1.reporter.transmit.sender)).peerlist == null)
                                ssrcinfo1.reporter.transmit.sender.peerlist = new Vector<SessionAddress>();
                            ((PacketFilter) (ssrcinfo1.reporter.transmit.sender)).peerlist
                                    .addElement(sessionaddress1);
                        }
                    }
                }
            }
        }

        NewSendStreamEvent newsendstreamevent = new NewSendStreamEvent(this,
                obj);
        cache.eventhandler.postEvent(newsendstreamevent);
        return obj;
    }

    @Override
    public SendStream createSendStream(
            javax.media.protocol.DataSource datasource, int i)
            throws IOException, UnsupportedFormatException
    {
        int j = 0;
        do
        {
            j = (int) generateSSRC();
        } while (cache.lookup(j) != null);
        SendStream sendstream = null;
        try
        {
            sendstream = createSendStream(j, datasource, i);
            if (newRtpInterface)
            {
                setRemoteAddresses();
            }
        } catch (SSRCInUseException ssrcinuseexception)
        {
        }
        return sendstream;
    }

    @Override
    public void dispose()
    {
        logger.info("Enter RTPSessionManager.dispose()");
        if (rtpConnector != null)
        {
            rtpConnector.close();
            /*
             * lyub0m1r: Make sure rtpConnector will not be accessed by
             * assigning null to it because it may resurrect any of the control
             * and/or data input and/or output streams (and, thus, their
             * underlying sockets).
             */
            rtpConnector = null;
        }
        if (defaultsource != null)
            defaultsource.disconnect();
        if (cache != null)
        {
            SSRCInfo ssrcinfo;
            for (Enumeration<?> elements = cache.cache.elements();
                    elements.hasMoreElements();
                    stopParticipating("dispose", ssrcinfo))
            {
                ssrcinfo = (SSRCInfo) elements.nextElement();
                if (ssrcinfo.dstream != null)
                    ssrcinfo.dstream.close();
                if (ssrcinfo instanceof SendSSRCInfo)
                    ((SendSSRCInfo) ssrcinfo).close();
            }
        }
        for (int i = 0; i < sendstreamlist.size(); i++)
            removeSendStream((SendStream) sendstreamlist.elementAt(i));
        if (rtpTransmitter != null)
            rtpTransmitter.close();
        if (rtcpTransmitter != null)
            rtcpTransmitter.close();
        if (rtcpForwarder != null)
        {
            RTCPRawReceiver rtcprawreceiver
                = (RTCPRawReceiver) rtcpForwarder.getSource();
            rtcpForwarder.close();
            if (rtcprawreceiver != null)
                rtcprawreceiver.close();
        }
        if (cleaner != null)
        {
            logger.fine("Call cleaner.stop()...");
            cleaner.stop(); // This has been seen to take 900ms
            logger.fine("... cleaner.stop() complete.");
        }
        if (cache != null)
            cache.destroy();
        if (rtpForwarder != null)
        {
            RTPRawReceiver rtprawreceiver
                = (RTPRawReceiver) rtpForwarder.getSource();
            rtpForwarder.close();
            if (rtprawreceiver != null)
                rtprawreceiver.close();
        }

        /*
         * damencho: If no targets are added, dataSocket and controlSocket are
         * not closed. So close them explicitly.
         */
        if (dataSocket != null)
            dataSocket.close();
        if (controlSocket != null)
            controlSocket.close();

        logger.info("Exit RTPSessionManager.dispose()");
    }

    private int findLocalPorts()
    {
        boolean flag = false;
        int i = -1;
        while (!flag)
        {
            do
            {
                double d = Math.random();
                i = (int) (d * 65535D);
                if (i % 2 != 0)
                {
                    i++;
                }
            } while (i < 1024 || i > 65534);
            try
            {
                DatagramSocket datagramsocket = new DatagramSocket(i);
                datagramsocket.close();
                datagramsocket = new DatagramSocket(i + 1);
                datagramsocket.close();
                flag = true;
            } catch (SocketException socketexception)
            {
                flag = false;
            }
        }
        return i;
    }

    @Override
    public String generateCNAME()
    {
        return SourceDescription.generateCNAME();
    }

    @Override
    public long generateSSRC()
    {
        long l = TrueRandom.rand();
        return l;
    }

    @Override
    public Vector<Participant> getActiveParticipants()
    {
        Vector<Participant> vector1 = new Vector<Participant>();
        RTPSourceInfoCache rtpsourceinfocache = cache.getRTPSICache();
        Hashtable<String, RTPSourceInfo> hashtable = rtpsourceinfocache.getCacheTable();
        for (Enumeration<RTPSourceInfo> enumeration = hashtable.elements(); enumeration
                .hasMoreElements();)
        {
            Participant participant = (Participant) enumeration.nextElement();
            if (!(participant instanceof LocalParticipant) || !nonparticipating)
            {
                Vector<SSRCInfo> vector = participant.getStreams();
                if (vector.size() > 0)
                {
                    vector1.addElement(participant);
                }
            }
        }

        return vector1;
    }

    @Override
    public Vector<Participant> getAllParticipants()
    {
        Vector<Participant> vector = new Vector<Participant>();
        RTPSourceInfoCache rtpsourceinfocache = cache.getRTPSICache();
        Hashtable<?, ?> hashtable = rtpsourceinfocache.getCacheTable();
        for (Enumeration<?> enumeration = hashtable.elements(); enumeration
                .hasMoreElements();)
        {
            Participant participant = (Participant) enumeration.nextElement();
            if (participant != null
                    && (!(participant instanceof LocalParticipant) || !nonparticipating))
            {
                vector.addElement(participant);
            }
        }

        return vector;
    }

    @Override
    public Object getControl(String s)
    {
        if (s.equals("javax.media.control.BufferControl"))
        {
            return buffercontrol;
        } else
        {
            return null;
        }
    }

    @Override
    public Object[] getControls()
    {
        Object aobj[] = new Object[1];
        aobj[0] = buffercontrol;
        return aobj;
    }

    public DataSource getDataSource(RTPMediaLocator rtpmedialocator)
    {
        if (rtpmedialocator == null)
        {
            return defaultsource;
        }
        int i = (int) rtpmedialocator.getSSRC();
        if (i == 0)
        {
            return defaultsource;
        } else
        {
            return (DataSource) dslist.get(i);
        }
    }

    @Override
    public long getDefaultSSRC()
    {
        return defaultSSRC;
    }

    public Format getFormat(int i)
    {
        return formatinfo.get(i);
    }

    @Override
    public GlobalReceptionStats getGlobalReceptionStats()
    {
        return defaultstats;
    }

    @Override
    public GlobalTransmissionStats getGlobalTransmissionStats()
    {
        return transstats;
    }

    @Override
    public LocalParticipant getLocalParticipant()
    {
        RTPSourceInfoCache rtpsourceinfocache = cache.getRTPSICache();
        Hashtable<?, ?> hashtable = rtpsourceinfocache.getCacheTable();
        for (Enumeration<?> enumeration = hashtable.elements(); enumeration
                .hasMoreElements();)
        {
            Participant participant = (Participant) enumeration.nextElement();
            if ((participant instanceof LocalParticipant) && !nonparticipating)
            {
                return (LocalParticipant) participant;
            }
        }

        return null;
    }

    public SessionAddress getLocalReceiverAddress()
    {
        return localReceiverAddress;
    }

    @Override
    public SessionAddress getLocalSessionAddress()
    {
        if (newRtpInterface)
        {
            return localAddress;
        } else
        {
            SessionAddress sessionaddress = new SessionAddress(
                    localDataAddress, localDataPort, localControlAddress,
                    localControlPort);
            return sessionaddress;
        }
    }

    /**
     * Emcho: Returns the SSRC ID that we are currently using or -1 if we don't
     * know it just yet.
     *
     * @return the SSRC ID that we are currently using or -1 if we don't know it
     *         just yet.
     */
    public long getLocalSSRC()
    {
        if (cache != null && cache.ourssrc != null)
            return cache.ourssrc.ssrc;
        return -1;
    }

    @Override
    public int getMulticastScope()
    {
        return ttl;
    }

    @Override
    public Vector<Participant> getPassiveParticipants()
    {
        Vector<Participant> vector1 = new Vector<Participant>();
        RTPSourceInfoCache rtpsourceinfocache = cache.getRTPSICache();
        Hashtable<?, ?> hashtable = rtpsourceinfocache.getCacheTable();
        for (Enumeration<?> enumeration = hashtable.elements(); enumeration
                .hasMoreElements();)
        {
            Participant participant = (Participant) enumeration.nextElement();
            if (!(participant instanceof LocalParticipant) || !nonparticipating)
            {
                Vector<?> vector = participant.getStreams();
                if (vector.size() == 0)
                {
                    vector1.addElement(participant);
                }
            }
        }

        return vector1;
    }

    @Override
    public Vector<SessionAddress> getPeers()
    {
        return peerlist;
    }

    private String getProperty(String s)
    {
        String s1 = null;
        try
        {
            s1 = System.getProperty(s);
        } catch (Throwable t)
        {
        }
        return s1;
    }

    @Override
    public Vector<RTPStream> getReceiveStreams()
    {
        Vector<RTPStream> vector = new Vector<RTPStream>();
        Vector<Participant> vector1 = getAllParticipants();
        for (int i = 0; i < vector1.size(); i++)
        {
            Participant participant = (Participant) vector1.elementAt(i);
            Vector<?> vector2 = participant.getStreams();
            for (int j = 0; j < vector2.size(); j++)
            {
                RTPStream rtpstream = (RTPStream) vector2.elementAt(j);
                if (rtpstream instanceof ReceiveStream)
                {
                    vector.addElement(rtpstream);
                }
            }
        }

        vector.trimToSize();
        return vector;
    }

    @Override
    public Vector<Participant> getRemoteParticipants()
    {
        Vector<Participant> vector = new Vector<Participant>();
        RTPSourceInfoCache rtpsourceinfocache = cache.getRTPSICache();
        Hashtable<?, ?> hashtable = rtpsourceinfocache.getCacheTable();
        for (Enumeration<?> enumeration = hashtable.elements(); enumeration
                .hasMoreElements();)
        {
            Participant participant = (Participant) enumeration.nextElement();
            if (participant instanceof RemoteParticipant)
            {
                vector.addElement(participant);
            }
        }

        return vector;
    }

    public SessionAddress getRemoteSessionAddress()
    {
        return remoteAddress;
    }

    @Override
    public Vector<? extends SendStream> getSendStreams()
    {
        return new Vector<SendStream>(sendstreamlist);
    }

    @Override
    public SessionAddress getSessionAddress()
    {
        SessionAddress sessionaddress = new SessionAddress(dataaddress,
                dataport, controladdress, controlport);
        return sessionaddress;
    }

    public int getSSRC()
    {
        return 0;
    }

    SSRCCache getSSRCCache()
    {
        return cache;
    }

    public SSRCInfo getSSRCInfo(int i)
    {
        SSRCInfo ssrcinfo = cache.lookup(i);
        return ssrcinfo;
    }

    @Override
    public RTPStream getStream(long l)
    {
        Vector<Participant> vector = null;
        vector = getAllParticipants();
        if (vector == null)
        {
            return null;
        }
        for (int i = 0; i < vector.size(); i++)
        {
            RTPSourceInfo rtpsourceinfo = (RTPSourceInfo) vector.elementAt(i);
            RTPStream rtpstream = rtpsourceinfo.getSSRCStream(l);
            if (rtpstream != null)
            {
                return rtpstream;
            }
        }

        return null;
    }

    @Override
    public void initialize(RTPConnector rtpconnector)
    {
        logger.fine("Initializing RTPSessionMgr " + this + " with RTPConnector " + rtpconnector);
        rtpConnector = rtpconnector;
        newRtpInterface = true;
        String s = SourceDescription.generateCNAME();
        SourceDescription asourcedescription[] = {
                new SourceDescription(1, s, 1, false),
                new SourceDescription(6, SOURCE_DESC_TOOL, 1, false) };
        int i = (int) generateSSRC();
        ttl = 1;
        participating = (rtpConnector.getRTCPBandwidthFraction() != 0.0D);
        cache = new SSRCCache(this);
        cache.sessionbandwidth = 0x5dc00;
        formatinfo.setCache(cache);
        if (rtpConnector.getRTCPBandwidthFraction() > 0.0D)
        {
            cache.rtcp_bw_fraction = rtpConnector.getRTCPBandwidthFraction();
        } else
        {
            cache.rtcp_bw_fraction = 0.050000000000000003D;
        }
        if (rtpConnector.getRTCPSenderBandwidthFraction() > 0.0D)
        {
            cache.rtcp_sender_bw_fraction = rtpConnector
                    .getRTCPSenderBandwidthFraction();
        } else
        {
            cache.rtcp_sender_bw_fraction = 0.25D;
        }
        cache.ourssrc = cache.get(i, null, 0, 2);
        cache.ourssrc.setAlive(true);
        if (!isCNAME(asourcedescription))
        {
            SourceDescription asourcedescription1[] = setCNAME(asourcedescription);
            cache.ourssrc.setSourceDescription(asourcedescription1);
        } else
        {
            cache.ourssrc.setSourceDescription(asourcedescription);
        }
        cache.ourssrc.ssrc = i;
        cache.ourssrc.setOurs(true);
        initialized = true;
        rtpRawReceiver = new RTPRawReceiver(rtpConnector, defaultstats);
        rtcpRawReceiver = new RTCPRawReceiver(rtpConnector, defaultstats,
                streamSynch);
        rtpDemultiplexer = new RTPDemultiplexer(cache, rtpRawReceiver,
                streamSynch);
        rtpForwarder = new PacketForwarder(rtpRawReceiver, new RTPReceiver(
                cache, rtpDemultiplexer));
        if (rtpForwarder != null)
            rtpForwarder.startPF("RTP Forwarder: " + rtpConnector);
        rtcpForwarder = new PacketForwarder(rtcpRawReceiver, new RTCPReceiver(
                cache));
        if (rtcpForwarder != null)
            rtcpForwarder.startPF("RTCP Forwarder: " + rtpConnector);
        cleaner = new SSRCCacheCleaner(cache, streamSynch);
        if (participating && cache.ourssrc != null)
        {
            cache.ourssrc.reporter = startParticipating(rtpConnector,
                    cache.ourssrc);
        }
    }

    @Override
    public void initialize(SessionAddress sessionaddress)
            throws InvalidSessionAddressException
    {
        String s = SourceDescription.generateCNAME();
        SourceDescription asourcedescription[] = {
                new SourceDescription(1, s, 1, false),
                new SourceDescription(6, SOURCE_DESC_TOOL, 1, false) };
        double d = 0.050000000000000003D;
        double d1 = 0.25D;
        SessionAddress asessionaddress[] = new SessionAddress[1];
        asessionaddress[0] = sessionaddress;
        initialize(asessionaddress, asourcedescription, d, d1, null);
    }

    @Override
    public void initialize(SessionAddress asessionaddress[],
            SourceDescription asourcedescription[], double d, double d1,
            EncryptionInfo encryptioninfo)
            throws InvalidSessionAddressException
    {
        if (initialized)
        {
            return;
        }
        newRtpInterface = true;
        remoteAddresses = new Vector<SessionAddress>();
        int i = (int) generateSSRC();
        ttl = 1;
        if (d == 0.0D)
        {
            participating = false;
        } else
        {
            participating = true;
        }
        if (asessionaddress.length == 0)
        {
            throw new InvalidSessionAddressException(
                    "At least one local address is required!");
        }
        localAddress = asessionaddress[0];
        if (localAddress == null)
        {
            throw new InvalidSessionAddressException(
                    "Invalid local address: null");
        }
        InetAddress ainetaddress[] = null;
        InetAddress inetaddress;
        try
        {
            inetaddress = localAddress.getDataAddress();
            // damencho fix slow when establishing a call
            String s1 = null;
            if (inetaddress.getHostAddress().equals("0.0.0.0"))
                s1 = "0.0.0.0";
            else
                s1 = inetaddress.getHostName();

            ainetaddress = InetAddress.getAllByName(s1);
        } catch (Throwable throwable1)
        {
            logger.log(Level.WARNING, "Error during initialization: "
                    + throwable1.getMessage(), throwable1);

            return;
        }
        if (localAddress.getDataAddress() == null)
        {
            localAddress.setDataHostAddress(inetaddress);
        }
        if (localAddress.getControlAddress() == null)
        {
            localAddress.setControlHostAddress(inetaddress);
        }
        if (localAddress.getDataAddress().isMulticastAddress())
        {
            if (localAddress.getControlAddress().isMulticastAddress())
            {
                ttl = localAddress.getTimeToLive();
            } else
            {
                throw new InvalidSessionAddressException(
                        "Invalid multicast address");
            }
        } else
        {
            // boolean flag = false;
            // boolean flag1 = false;
            // this is ridiculous i don't see what is the point of
            // verifying this.
            boolean flag = true;
            boolean flag1 = true;
            try
            {
                logger.fine("Looking for local data address: "
                        + localAddress.getDataAddress()
                        + " and control address"
                        + localAddress.getControlAddress());
                if (localAddress.getDataHostAddress().equals("0.0.0.0")
                        || localAddress.getDataHostAddress().equals("::0"))
                    flag = true;
                if (localAddress.getControlHostAddress().equals("0.0.0.0")
                        || localAddress.getControlHostAddress().equals("::0"))
                    flag1 = true;

                for (Enumeration<?> intfs = NetworkInterface
                        .getNetworkInterfaces(); intfs.hasMoreElements();)
                {
                    if (flag && flag1)
                        break;

                    NetworkInterface intf = (NetworkInterface) intfs
                            .nextElement();
                    for (Enumeration<?> addrs = intf.getInetAddresses(); addrs
                            .hasMoreElements();)
                    {
                        try
                        {
                            InetAddress addr = (InetAddress) addrs
                                    .nextElement();
                            logger.fine("Testing iface address "
                                    + localAddress.getDataAddress());
                            if (addr.equals(localAddress.getDataAddress()))
                            {
                                flag = true;
                            }
                            if (addr.equals(localAddress.getControlAddress()))
                            {
                                flag1 = true;
                            }
                        } catch (Exception exception)
                        {
                        }
                    }
                }

            } catch (Exception exc)
            {
                logger.log(Level.WARNING,
                        "Error while enumerating local interfaces.", exc);
            }
            if (!flag)
            {
                String s2 = "Does not belong to any of this hosts local interfaces";
                throw new InvalidSessionAddressException("Local Data Address "
                        + s2);
            }
            if (!flag1)
            {
                String s3 = "Does not belong to any of this hosts local interfaces";
                throw new InvalidSessionAddressException(
                        "Local Control Address " + s3);
            }
            if (localAddress.getDataPort() == -1)
            {
                int k = findLocalPorts();
                localAddress.setDataPort(k);
                localAddress.setControlPort(k + 1);
            }
            if (!localAddress.getDataAddress().isMulticastAddress())
            {
                try
                {
                    dataSocket = new DatagramSocket(localAddress.getDataPort(),
                            localAddress.getDataAddress());
                } catch (SocketException socketexception)
                {
                    throw new InvalidSessionAddressException(
                            "Can't open local data port: "
                                    + localAddress.getDataPort());
                }
            }
            if (!localAddress.getControlAddress().isMulticastAddress())
            {
                try
                {
                    controlSocket = new DatagramSocket(
                            localAddress.getControlPort(),
                            localAddress.getControlAddress());
                } catch (SocketException socketexception1)
                {
                    if (dataSocket != null)
                    {
                        dataSocket.close();
                    }
                    throw new InvalidSessionAddressException(
                            "Can't open local control port: "
                                    + localAddress.getControlPort());
                }
            }
        }
        cache = new SSRCCache(this);
        if (ttl <= 16)
            cache.sessionbandwidth = 0x5dc00;
        else if (ttl <= 64)
            cache.sessionbandwidth = 0x1f400;
        else if (ttl <= 128)
            cache.sessionbandwidth = 16000;
        else if (ttl <= 192)
            cache.sessionbandwidth = 6625;
        else
            cache.sessionbandwidth = 4000;
        formatinfo.setCache(cache);
        cache.rtcp_bw_fraction = d;
        cache.rtcp_sender_bw_fraction = d1;
        cache.ourssrc = cache.get(i, inetaddress, 0, 2);
        cache.ourssrc.setAlive(true);
        if (!isCNAME(asourcedescription))
        {
            SourceDescription asourcedescription1[] = setCNAME(asourcedescription);
            cache.ourssrc.setSourceDescription(asourcedescription1);
        } else
        {
            cache.ourssrc.setSourceDescription(asourcedescription);
        }
        cache.ourssrc.ssrc = i;
        cache.ourssrc.setOurs(true);
        initialized = true;
    }

    @Override
    public int initSession(SessionAddress sessionaddress, long l,
            SourceDescription asourcedescription[], double d, double d1)
            throws InvalidSessionAddressException
    {
        if (initialized)
        {
            return -1;
        }
        if (d == 0.0D)
        {
            nonparticipating = true;
        }
        defaultSSRC = l;
        localDataAddress = sessionaddress.getDataAddress();
        localControlAddress = sessionaddress.getControlAddress();
        localDataPort = sessionaddress.getDataPort();
        localControlPort = sessionaddress.getControlPort();
        InetAddress ainetaddress[] = null;
        InetAddress inetaddress;
        try
        {
            inetaddress = localAddress.getDataAddress();
            String s1 = inetaddress.getHostName();
            ainetaddress = InetAddress.getAllByName(s1);
        } catch (Throwable throwable1)
        {
            logger.log(Level.WARNING, "InitSession  RTPSessionMgr :"
                    + throwable1.getMessage(), throwable1);
            return -1;
        }
        if (localDataAddress == null)
        {
            localDataAddress = inetaddress;
        }
        if (localControlAddress == null)
        {
            localControlAddress = inetaddress;
        }
        boolean flag = false;
        boolean flag1 = false;
        try
        {
            for (Enumeration<?> intfs = NetworkInterface.getNetworkInterfaces(); intfs
                    .hasMoreElements();)
            {
                NetworkInterface intf = (NetworkInterface) intfs.nextElement();
                for (Enumeration<?> addrs = intf.getInetAddresses(); addrs
                        .hasMoreElements();)
                {
                    try
                    {
                        InetAddress addr = (InetAddress) addrs.nextElement();
                        if (addr.equals(localAddress.getDataAddress()))
                        {
                            flag = true;
                        }
                        if (addr.equals(localAddress.getControlAddress()))
                        {
                            flag1 = true;
                        }
                    } catch (Exception exception)
                    {
                    }
                }
            }

        } catch (Exception exc)
        {
            logger.log(Level.SEVERE, "Error while enumerating interfaces", exc);
        }
        String s2 = "Does not belong to any of this hosts local interfaces";
        if (!flag)
        {
            throw new InvalidSessionAddressException("Local Data Address " + s2);
        }
        if (!flag1)
        {
            throw new InvalidSessionAddressException("Local Control Address"
                    + s2);
        }
        cache = new SSRCCache(this);
        formatinfo.setCache(cache);
        cache.rtcp_bw_fraction = d;
        cache.rtcp_sender_bw_fraction = d1;
        cache.ourssrc = cache.get((int) l, inetaddress, 0, 2);
        cache.ourssrc.setAlive(true);
        if (!isCNAME(asourcedescription))
        {
            SourceDescription asourcedescription1[] = setCNAME(asourcedescription);
            cache.ourssrc.setSourceDescription(asourcedescription1);
        } else
        {
            cache.ourssrc.setSourceDescription(asourcedescription);
        }
        cache.ourssrc.ssrc = (int) l;
        cache.ourssrc.setOurs(true);
        initialized = true;
        return 0;
    }

    @Override
    public int initSession(SessionAddress sessionaddress,
            SourceDescription asourcedescription[], double d, double d1)
            throws InvalidSessionAddressException
    {
        long l = generateSSRC();
        return initSession(sessionaddress, l, asourcedescription, d, d1);
    }

    private int initSession(SourceDescription asourcedescription[], double d,
            double d1)
    {
        if (initialized)
        {
            return -1;
        }
        InetAddress inetaddress = null;
        if (d == 0.0D)
        {
            nonparticipating = true;
        }
        defaultSSRC = generateSSRC();
        cache = new SSRCCache(this);
        formatinfo.setCache(cache);
        cache.rtcp_bw_fraction = d;
        cache.rtcp_sender_bw_fraction = d1;
        try
        {
            inetaddress = InetAddress.getLocalHost();
        } catch (Throwable throwable1)
        {
            logger.log(Level.WARNING, "InitSession UnknownHostExcpetion "
                    + throwable1.getMessage(), throwable1);

            return -1;
        }
        cache.ourssrc = cache.get((int) defaultSSRC, null, 0, 2);
        cache.ourssrc.setAlive(true);
        if (!isCNAME(asourcedescription))
        {
            SourceDescription asourcedescription1[] = setCNAME(asourcedescription);
            cache.ourssrc.setSourceDescription(asourcedescription1);
        } else
        {
            cache.ourssrc.setSourceDescription(asourcedescription);
        }
        cache.ourssrc.ssrc = (int) defaultSSRC;
        cache.ourssrc.setOurs(true);
        initialized = true;
        return 0;
    }

    boolean isBroadcast(InetAddress inetaddress)
    {
        try
        {
            InetAddress inetaddress1 = InetAddress.getLocalHost();
            byte abyte0[] = inetaddress1.getAddress();
            int i = abyte0[3] & 0xff;
            i |= abyte0[2] << 8 & 0xff00;
            i |= abyte0[1] << 16 & 0xff0000;
            i |= abyte0[0] << 24 & 0xff000000;
            byte abyte1[] = inetaddress.getAddress();
            int j = abyte1[3] & 0xff;
            j |= abyte1[2] << 8 & 0xff00;
            j |= abyte1[1] << 16 & 0xff0000;
            j |= abyte1[0] << 24 & 0xff000000;
            if ((i | 0xff) == j)
            {
                return true;
            }
        } catch (UnknownHostException unknownhostexception)
        {
            logger.warning(unknownhostexception.getMessage());
        }
        return false;
    }

    private boolean isCNAME(SourceDescription asourcedescription[])
    {
        boolean flag = false;
        if (asourcedescription == null)
        {
            return flag;
        }
        for (int j = 0; j < asourcedescription.length; j++)
        {
            try
            {
                int i = asourcedescription[j].getType();
                String s = asourcedescription[j].getDescription();
                if (i == 1 && s != null)
                {
                    flag = true;
                }
            } catch (Exception exception)
            {
            }
        }

        return flag;
    }

    public boolean isDefaultDSassigned()
    {
        return bds;
    }

    public boolean IsNonParticipating()
    {
        return nonparticipating;
    }

    public boolean isSenderDefaultAddr(InetAddress inetaddress)
    {
        if (sender == null)
        {
            return false;
        } else
        {
            InetAddress inetaddress1 = sender.getRemoteAddr();
            return inetaddress1.equals(inetaddress);
        }
    }

    boolean isUnicast()
    {
        return unicast;
    }

    @Override
    public void removeAllPeers()
    {
        for (int i = 0; i < peerlist.size(); i++)
        {
            removePeer((SessionAddress) peerlist.elementAt(i));
        }
    }

    public void removeDataSource(DataSource datasource)
    {
        if (datasource == defaultsource)
        {
            defaultsource.disconnect();
            defaultsource = null;
            defaultstream = null;
            defaultsourceid = 0;
            bds = false;
        }
        dslist.removeObj(datasource);
    }

    @Override
    public void removePeer(SessionAddress sessionaddress)
    {
        PacketForwarder packetforwarder = (PacketForwarder) peerrtplist
                .get(sessionaddress);
        PacketForwarder packetforwarder1 = (PacketForwarder) peerrtplist
                .get(sessionaddress);
        if (packetforwarder != null)
        {
            packetforwarder.close();
        }
        if (packetforwarder1 != null)
        {
            packetforwarder1.close();
        }
        for (int i = 0; i < peerlist.size(); i++)
        {
            SessionAddress sessionaddress1 = (SessionAddress) peerlist
                    .elementAt(i);
            if (sessionaddress1.equals(sessionaddress))
            {
                peerlist.removeElementAt(i);
            }
        }
    }

    @Override
    public void removeReceiveStreamListener(
            ReceiveStreamListener receivestreamlistener)
    {
        streamlistener.removeElement(receivestreamlistener);
    }

    @Override
    public void removeRemoteListener(RemoteListener remotelistener1)
    {
        remotelistener.removeElement(remotelistener1);
    }

    void removeSendStream(SendStream sendstream)
    {
        sendstreamlist.removeElement(sendstream);

        SendSSRCInfo sendstreamAsSendSSRCInfo = (SendSSRCInfo) sendstream;

        if (sendstreamAsSendSSRCInfo.sinkstream != null)
        {
            sendstreamAsSendSSRCInfo.sinkstream.close();
            StreamClosedEvent streamclosedevent = new StreamClosedEvent(this,
                    sendstream);
            cache.eventhandler.postEvent(streamclosedevent);
            stopParticipating("Closed Stream", sendstreamAsSendSSRCInfo);
        }
        if (sendstreamlist.size() == 0 && cache.ourssrc != null)
        {
            SSRCInfo passivessrcinfo;

            /*
             * If we've just removed (i.e. said RTCP BYE) the SendStream with
             * the SSRC of this RTPSessionMgr, then it's time to change the SSRC
             * of this RTPSessionMgr.
             */
            if ((cache.ourssrc.ssrc == sendstreamAsSendSSRCInfo.ssrc)
                    && (sendstreamAsSendSSRCInfo.reporter == null))
            {
                /*
                 * The code bellow tries to mimic #initialize(RTPConnector) in
                 * order to correctly generate a new SSRC for this
                 * RTPSessionMgr.
                 */

                int newSSRC;

                do
                {
                    newSSRC = (int) generateSSRC();
                } while (cache.lookup(newSSRC) != null);

                passivessrcinfo = cache.get(newSSRC, null, 0, 2);
                passivessrcinfo.setAlive(true);

                SourceDescription asourcedescription[] = {
                        new SourceDescription(1, generateCNAME(), 1, false),
                        new SourceDescription(6, SOURCE_DESC_TOOL, 1,
                                false) };
                if (!isCNAME(asourcedescription))
                {
                    passivessrcinfo
                            .setSourceDescription(setCNAME(asourcedescription));
                } else
                {
                    passivessrcinfo.setSourceDescription(asourcedescription);
                }
                passivessrcinfo.ssrc = newSSRC;
                passivessrcinfo.setOurs(true);
            } else
            {
                passivessrcinfo = new PassiveSSRCInfo(getSSRCCache().ourssrc);
            }

            cache.ourssrc = passivessrcinfo;
            cache.getMainCache().put(passivessrcinfo.ssrc, passivessrcinfo);

            /*
             * damencho: Create reporter in order to process further RTCP
             * requests when adding and removing streams.
             */
            if (rtpConnector != null)
            {
                cache.ourssrc.reporter
                    = startParticipating(rtpConnector, cache.ourssrc);
            }
        }
    }

    @Override
    public void removeSendStreamListener(SendStreamListener sendstreamlistener2)
    {
    }

    @Override
    public void removeSessionListener(SessionListener sessionlistener1)
    {
        sessionlistener.removeElement(sessionlistener1);
    }

    @Override
    public void removeTarget(SessionAddress sessionaddress, String s)
    {
        remoteAddresses.removeElement(sessionaddress);
        setRemoteAddresses();
        if (remoteAddresses.size() == 0 && cache != null)
        {
            stopParticipating(s, cache.ourssrc);
        }
    }

    @Override
    public void removeTargets(String s)
    {
        if (cache != null)
        {
            stopParticipating(s, cache.ourssrc);
        }
        if (remoteAddresses != null)
        {
            remoteAddresses.removeAllElements();
        }
        setRemoteAddresses();
    }

    private SourceDescription[] setCNAME(SourceDescription asourcedescription[])
    {
        boolean flag = false;
        if (asourcedescription == null)
        {
            asourcedescription = new SourceDescription[1];
            String s = SourceDescription.generateCNAME();
            asourcedescription[0] = new SourceDescription(1, s, 1, false);
            return asourcedescription;
        }
        for (int j = 0; j < asourcedescription.length; j++)
        {
            int i = asourcedescription[j].getType();
            String s1 = asourcedescription[j].getDescription();
            if (i != 1 || s1 != null)
            {
                continue;
            }
            s1 = SourceDescription.generateCNAME();
            flag = true;
            break;
        }

        if (flag)
        {
            return asourcedescription;
        }
        SourceDescription asourcedescription1[] = new SourceDescription[asourcedescription.length + 1];
        asourcedescription1[0] = new SourceDescription(1,
                SourceDescription.generateCNAME(), 1, false);
        int k = 1;
        for (int l = 0; l < asourcedescription.length; l++)
        {
            asourcedescription1[k] = new SourceDescription(
                    asourcedescription[l].getType(),
                    asourcedescription[l].getDescription(), 1, false);
            k++;
        }

        return asourcedescription1;
    }

    public void setDefaultDSassigned(int i)
    {
        bds = true;
        defaultsourceid = i;
        dslist.put(i, defaultsource);
        defaultsource.setSSRC(i);
        defaultsource.setMgr(this);
    }

    @Override
    public void setMulticastScope(int i)
    {
        if (i < 1)
        {
            i = 1;
        }
        ttl = i;
        if (ttl <= 16)
        {
            cache.sessionbandwidth = 0x5dc00;
        } else if (ttl <= 64)
        {
            cache.sessionbandwidth = 0x1f400;
        } else if (ttl <= 128)
        {
            cache.sessionbandwidth = 16000;
        } else if (ttl <= 192)
        {
            cache.sessionbandwidth = 6625;
        } else
        {
            cache.sessionbandwidth = 4000;
        }
        if (udpsender != null)
        {
            try
            {
                udpsender.setttl(ttl);
            } catch (IOException ioexception)
            {
                logger.log(Level.WARNING, "setMulticastScope Exception ",
                        ioexception);
            }
        }
    }

    private void setRemoteAddresses()
    {
        if (rtpTransmitter != null)
        {
            RTPRawSender rtprawsender = rtpTransmitter.getSender();
            rtprawsender.setDestAddresses(remoteAddresses);
        }
        if (rtcpTransmitter != null)
        {
            RTCPRawSender rtcprawsender = rtcpTransmitter.getSender();
            rtcprawsender.setDestAddresses(remoteAddresses);
        }
    }

    private SourceDescription[] setSDES()
    {
        SourceDescription asourcedescription[] = new SourceDescription[3];
        {
            asourcedescription[0] = new SourceDescription(2,
                    getProperty("user.name"), 1, false);
            asourcedescription[1] = new SourceDescription(1,
                    SourceDescription.generateCNAME(), 1, false);
            asourcedescription[2] = new SourceDescription(6,
                    SOURCE_DESC_TOOL, 1, false);
            return asourcedescription;
        }
    }

    public void setRTCPReportBuilder(RTCPReportBuilder rtcpReportBuilder)
    {
        this.rtcpReportBuilder = rtcpReportBuilder;
    }

    void setSessionBandwidth(int i)
    {
        cache.sessionbandwidth = i;
    }

    private RTPTransmitter startDataTransmission(int i, String s)
            throws IOException
    {
        logger.fine("Starting data transmission with port: " + i +
                    " and address: " + s);
        RTPTransmitter rtptransmitter = null;
        RTPRawSender rtprawsender = null;
        if (localDataPort == -1)
        {
            udpsender = new UDPPacketSender(dataaddress, dataport);
        } else if (newRtpInterface)
        {
            udpsender = new UDPPacketSender(rtpRawReceiver.socket);
        } else
        {
            int j = localSenderAddress.getDataPort();
            InetAddress inetaddress = localSenderAddress.getDataAddress();
            udpsender = new UDPPacketSender(j, inetaddress, dataaddress,
                    dataport);
        }
        if (ttl != 1)
        {
            udpsender.setttl(ttl);
        }
        rtprawsender = new RTPRawSender(dataport, s, udpsender);
        rtptransmitter = new RTPTransmitter(cache, rtprawsender);
        return rtptransmitter;
    }

    private RTPTransmitter startDataTransmission(RTPConnector rtpconnector)
    {
        logger.fine("Starting data transmission using RTPConnector " + rtpconnector);
        try
        {
            RTPRawSender rtprawsender = null;
            RTPTransmitter rtptransmitter = null;
            rtpsender = new RTPPacketSender(rtpconnector);
            rtprawsender = new RTPRawSender(rtpsender);
            rtptransmitter = new RTPTransmitter(cache, rtprawsender);
            return rtptransmitter;
        } catch (IOException ioexception)
        {
            return null;
        }
    }

    private RTPTransmitter startDataTransmission(
            RTPPushDataSource rtppushdatasource)
    {
        logger.fine("Starting data transmission using RTPPushDataSource " + rtppushdatasource);
        RTPRawSender rtprawsender = null;
        RTPTransmitter rtptransmitter = null;
        rtpsender = new RTPPacketSender(rtppushdatasource);
        rtprawsender = new RTPRawSender(rtpsender);
        rtptransmitter = new RTPTransmitter(cache, rtprawsender);
        return rtptransmitter;
    }

    private synchronized RTCPReporter startParticipating(
            DatagramSocket datagramsocket) throws IOException
    {
        UDPPacketSender udppacketsender = new UDPPacketSender(datagramsocket);
        udpPacketSender = udppacketsender;
        if (ttl != 1)
        {
            udppacketsender.setttl(ttl);
        }
        // damencho
        // RTCPRawSender rtcprawsender = new RTCPRawSender(remoteAddress.
        // getControlPort(), remoteAddress.getControlAddress().getHostName(),
        // udppacketsender);
        RTCPRawSender rtcprawsender = new RTCPRawSender(
                remoteAddress.getControlPort(), remoteAddress
                        .getControlAddress().getHostAddress(), udppacketsender);
        rtcpTransmitter = new RTCPTransmitter(cache, rtcprawsender);
        rtcpTransmitter.setSSRCInfo(cache.ourssrc);
        rtcpTransmitter.setReportBuilder(rtcpReportBuilder);
        RTCPReporter rtcpreporter = new RTCPReporter(cache, rtcpTransmitter);
        startedparticipating = true;
        return rtcpreporter;
    }

    private synchronized RTCPReporter startParticipating(int i, String s,
            SSRCInfo ssrcinfo) throws IOException
    {
        startedparticipating = true;
        UDPPacketSender udppacketsender = null;
        if (localControlPort == -1)
        {
            udppacketsender = new UDPPacketSender(controladdress, controlport);
            localControlPort = udppacketsender.getLocalPort();
            localControlAddress = udppacketsender.getLocalAddress();
        } else
        {
            udppacketsender = new UDPPacketSender(localControlPort,
                    localControlAddress, controladdress, controlport);
        }
        if (ttl != 1)
        {
            udppacketsender.setttl(ttl);
        }
        RTCPRawSender rtcprawsender = new RTCPRawSender(i, s, udppacketsender);
        RTCPTransmitter rtcptransmitter = new RTCPTransmitter(cache,
                rtcprawsender);
        rtcptransmitter.setSSRCInfo(ssrcinfo);
        rtcptransmitter.setReportBuilder(rtcpReportBuilder);
        return new RTCPReporter(cache, rtcptransmitter);
    }

    private synchronized RTCPReporter startParticipating(
            RTPConnector rtpconnector, SSRCInfo ssrcinfo)
    {
        startedparticipating = true;
        try
        {
            rtpsender = new RTPPacketSender(
                    rtpconnector.getControlOutputStream());
        } catch (IOException ioexception)
        {
            logger.log(Level.WARNING, "error initializing rtp sender  "
                    + ioexception.getMessage(), ioexception);
        }
        RTCPRawSender rtcprawsender = new RTCPRawSender(rtpsender);
        RTCPTransmitter rtcptransmitter = new RTCPTransmitter(cache,
                rtcprawsender);
        rtcptransmitter.setSSRCInfo(ssrcinfo);
        rtcptransmitter.setReportBuilder(rtcpReportBuilder);
        return new RTCPReporter(cache, rtcptransmitter);
    }

    private synchronized RTCPReporter startParticipating(
            RTPPushDataSource rtppushdatasource, SSRCInfo ssrcinfo)
    {
        startedparticipating = true;
        rtpsender = new RTPPacketSender(rtppushdatasource);
        RTCPRawSender rtcprawsender = new RTCPRawSender(rtpsender);
        RTCPTransmitter rtcptransmitter = new RTCPTransmitter(cache,
                rtcprawsender);
        rtcptransmitter.setSSRCInfo(ssrcinfo);
        rtcptransmitter.setReportBuilder(rtcpReportBuilder);
        return new RTCPReporter(cache, rtcptransmitter);
    }

    private synchronized RTCPReporter startParticipating(
            SessionAddress sessionaddress, SessionAddress sessionaddress1,
            SSRCInfo ssrcinfo, DatagramSocket datagramsocket)
            throws IOException
    {
        localReceiverAddress = sessionaddress;
        startedparticipating = true;
        UDPPacketSender udppacketsender = null;
        int i = sessionaddress1.getControlPort();
        InetAddress inetaddress = sessionaddress1.getControlAddress();
        int j = sessionaddress.getControlPort();
        InetAddress inetaddress1 = sessionaddress.getControlAddress();
        if (i == -1)
        {
            udppacketsender = new UDPPacketSender(inetaddress, i);
        } else if (i == j)
        {
            udppacketsender = new UDPPacketSender(datagramsocket);
        } else
        {
            udppacketsender = new UDPPacketSender(i, inetaddress,
                    controladdress, controlport);
        }
        if (ttl != 1)
        {
            udppacketsender.setttl(ttl);
        }
        // damencho
        // RTCPRawSender rtcprawsender = new RTCPRawSender(controlport,
        // controladdress.getHostName(), udppacketsender);
        RTCPRawSender rtcprawsender = new RTCPRawSender(controlport,
                controladdress.getHostAddress(), udppacketsender);
        RTCPTransmitter rtcptransmitter = new RTCPTransmitter(cache,
                rtcprawsender);
        rtcptransmitter.setSSRCInfo(ssrcinfo);
        rtcptransmitter.setReportBuilder(rtcpReportBuilder);
        return new RTCPReporter(cache, rtcptransmitter);
    }

    void startRTCPReports(InetAddress inetaddress)
    {
        if (!nonparticipating && !startedparticipating)
        {
            try
            {
                if (cache.ourssrc != null)
                {
                    cache.ourssrc.reporter = startParticipating(controlport,
                            inetaddress.getHostAddress(), cache.ourssrc);
                }
            } catch (IOException ioexception)
            {
                logger.log(Level.WARNING,
                        "start rtcp reports  " + ioexception.getMessage(),
                        ioexception);
            }
        }
    }

    public void startSession() throws IOException
    {
        SessionAddress sessionaddress = new SessionAddress(dataaddress,
                dataport, controladdress, controlport);
        try
        {
            startSession(sessionaddress, ttl, null);
        } catch (SessionManagerException sessionmanagerexception)
        {
            throw new IOException("SessionManager exception "
                    + sessionmanagerexception.getMessage());
        }
    }

    @Override
    public int startSession(int i, EncryptionInfo encryptioninfo)
            throws IOException
    {
        multi_unicast = true;
        if (i < 1)
        {
            i = 1;
        }
        ttl = i;
        if (ttl <= 16)
        {
            cache.sessionbandwidth = 0x5dc00;
        } else if (ttl <= 64)
        {
            cache.sessionbandwidth = 0x1f400;
        } else if (ttl <= 128)
        {
            cache.sessionbandwidth = 16000;
        } else if (ttl <= 192)
        {
            cache.sessionbandwidth = 6625;
        } else
        {
            cache.sessionbandwidth = 4000;
        }
        cleaner = new SSRCCacheCleaner(cache, streamSynch);
        return 0;
    }

    private int startSession(RTPPushDataSource rtppushdatasource,
            RTPPushDataSource rtppushdatasource1, EncryptionInfo encryptioninfo)
    {
        if (!initialized)
        {
            return -1;
        }
        if (started)
        {
            return -1;
        }
        cache.sessionbandwidth = 0x5dc00;
        RTPRawReceiver rtprawreceiver = new RTPRawReceiver(rtppushdatasource,
                defaultstats);
        RTCPRawReceiver rtcprawreceiver = new RTCPRawReceiver(
                rtppushdatasource1, defaultstats, streamSynch);
        rtpDemultiplexer = new RTPDemultiplexer(cache, rtprawreceiver,
                streamSynch);
        rtpForwarder = new PacketForwarder(rtprawreceiver, new RTPReceiver(
                cache, rtpDemultiplexer));
        if (rtpForwarder != null)
        {
            rtpForwarder.startPF("RTP Forwarder " + rtppushdatasource);
        }
        rtcpForwarder = new PacketForwarder(rtcprawreceiver, new RTCPReceiver(
                cache));
        if (rtcpForwarder != null)
        {
            rtcpForwarder.startPF("RTCP Forwarder " + rtppushdatasource);
        }
        cleaner = new SSRCCacheCleaner(cache, streamSynch);
        if (!nonparticipating && cache.ourssrc != null)
        {
            cache.ourssrc.reporter = startParticipating(rtppushdatasource1,
                    cache.ourssrc);
        }
        started = true;
        return 0;
    }

    @Override
    public int startSession(SessionAddress sessionaddress, int i,
            EncryptionInfo encryptioninfo) throws IOException,
            InvalidSessionAddressException
    {
        RTCPRawReceiver rtcprawreceiver;
        RTPRawReceiver rtprawreceiver;
        InetAddress inetaddress;
        if (started)
        {
            return -1;
        }
        if (i < 1)
        {
            i = 1;
        }
        ttl = i;
        if (ttl <= 16)
        {
            cache.sessionbandwidth = 0x5dc00;
        } else if (ttl <= 64)
        {
            cache.sessionbandwidth = 0x1f400;
        } else if (ttl <= 128)
        {
            cache.sessionbandwidth = 16000;
        } else if (ttl <= 192)
        {
            cache.sessionbandwidth = 6625;
        } else
        {
            cache.sessionbandwidth = 4000;
        }
        controlport = sessionaddress.getControlPort();
        dataport = sessionaddress.getDataPort();
        CheckRTPPorts(dataport, controlport);
        dataaddress = sessionaddress.getDataAddress();
        controladdress = sessionaddress.getControlAddress();
        CheckRTPAddress(dataaddress, controladdress);
        rtcprawreceiver = null;
        rtprawreceiver = null;
        inetaddress = null;
        try
        {
            inetaddress = InetAddress.getLocalHost();
        } catch (Throwable throwable1)
        {
            logger.log(Level.WARNING, "InitSession  RTPSessionMgr : "
                    + throwable1.getMessage(), throwable1);
            return -1;
        }
        if (dataaddress.equals(inetaddress))
        {
            unicast = true;
        }
        if (!dataaddress.isMulticastAddress()
                && !dataaddress.equals(inetaddress))
        {
            if (isBroadcast(dataaddress) && !Win32())
            {
                bindtome = false;
            } else
            {
                bindtome = true;
            }
        }
        if (bindtome)
        {
            return -1;
        }
        try
        {
            rtcprawreceiver = new RTCPRawReceiver(controlport,
                    controladdress.getHostAddress(), defaultstats, streamSynch);
            if (dataaddress != null)
            {
                rtprawreceiver = new RTPRawReceiver(dataport,
                        dataaddress.getHostAddress(), defaultstats);
            }
        } catch (SocketException socketexception)
        {
            throw new IOException(socketexception.getMessage());
        } finally
        {
            if (dataaddress != null && rtprawreceiver == null
                    && rtcprawreceiver != null)
            {
                logger.warning("could not create RTCP/RTP raw receivers");
                rtcprawreceiver.closeSource();
            }
        }

        try
        {
            rtcprawreceiver = new RTCPRawReceiver(controlport,
                    inetaddress.getHostAddress(), defaultstats, streamSynch);
            if (dataaddress != null)
            {
                rtprawreceiver = new RTPRawReceiver(dataport,
                        inetaddress.getHostAddress(), defaultstats);
            }
        } catch (SocketException socketexception1)
        {
            throw new IOException(socketexception1.getMessage());
        } finally
        {
            if (dataaddress != null && rtprawreceiver == null
                    && rtcprawreceiver != null)
            {
                logger.warning("could not create RTCP/RTP raw receivers");
                rtcprawreceiver.closeSource();
            }
        }
        rtpDemultiplexer = new RTPDemultiplexer(cache, rtprawreceiver,
                streamSynch);
        rtcpForwarder = new PacketForwarder(rtcprawreceiver, new RTCPReceiver(
                cache));
        if (rtprawreceiver != null)
        {
            rtpForwarder = new PacketForwarder(rtprawreceiver, new RTPReceiver(
                    cache, rtpDemultiplexer));
        }
        rtcpForwarder.startPF("RTCP Forwarder for address"
                + controladdress.toString() + "port " + controlport);
        if (rtpForwarder != null)
        {
            rtpForwarder.startPF("RTP Forwarder for address "
                    + dataaddress.toString() + "port " + dataport);
        }
        cleaner = new SSRCCacheCleaner(cache, streamSynch);
        if (!nonparticipating && !unicast && cache.ourssrc != null)
        {
            cache.ourssrc.reporter = startParticipating(controlport,
                    dataaddress.getHostAddress(), cache.ourssrc);
        }
        started = true;
        return 0;
    }

    @Override
    public int startSession(SessionAddress sessionaddress,
            SessionAddress sessionaddress1, SessionAddress sessionaddress2,
            EncryptionInfo encryptioninfo) throws IOException,
            InvalidSessionAddressException
    {
        RTCPRawReceiver rtcprawreceiver;
        RTPRawReceiver rtprawreceiver;
        if (started)
        {
            return -1;
        }
        localSenderAddress = sessionaddress1;
        cache.sessionbandwidth = 0x5dc00;
        controlport = sessionaddress.getControlPort();
        dataport = sessionaddress.getDataPort();
        CheckRTPPorts(dataport, controlport);
        dataaddress = sessionaddress.getDataAddress();
        controladdress = sessionaddress.getControlAddress();
        if (dataaddress.isMulticastAddress()
                || controladdress.isMulticastAddress()
                || isBroadcast(dataaddress) || isBroadcast(controladdress))
        {
            throw new InvalidSessionAddressException(
                    "Local Address must be UNICAST IP addresses");
        }
        CheckRTPAddress(dataaddress, controladdress);
        rtcprawreceiver = null;
        rtprawreceiver = null;
        InetAddress inetaddress = null;
        try
        {
            inetaddress = InetAddress.getLocalHost();
        } catch (Throwable throwable)
        {
            logger.log(Level.SEVERE, "InitSession : UnknownHostExcpetion "
                    + throwable.getMessage(), throwable);
            return -1;
        }
        try
        {
            rtcprawreceiver = new RTCPRawReceiver(controlport,
                    controladdress.getHostAddress(), defaultstats, streamSynch);
            if (dataaddress != null)
            {
                rtprawreceiver = new RTPRawReceiver(dataport,
                        dataaddress.getHostAddress(), defaultstats);
            }
        } catch (SocketException socketexception)
        {
            throw new IOException(socketexception.getMessage());
        } finally
        {
            if (dataaddress != null && rtprawreceiver == null
                    && rtcprawreceiver != null)
            {
                logger.warning("could not create RTCP/RTP raw receivers");
                rtcprawreceiver.closeSource();
            }
        }
        rtpDemultiplexer = new RTPDemultiplexer(cache, rtprawreceiver,
                streamSynch);
        rtcpForwarder = new PacketForwarder(rtcprawreceiver, new RTCPReceiver(
                cache));
        if (rtprawreceiver != null)
        {
            rtpForwarder = new PacketForwarder(rtprawreceiver, new RTPReceiver(
                    cache, rtpDemultiplexer));
        }
        rtcpForwarder.startPF("RTCP Forwarder for address"
                + controladdress.toString() + "port " + controlport);
        if (rtpForwarder != null)
        {
            rtpForwarder.startPF("RTP Forwarder for address "
                    + dataaddress.toString() + "port " + dataport);
        }
        controlport = sessionaddress2.getControlPort();
        dataport = sessionaddress2.getDataPort();
        CheckRTPPorts(dataport, controlport);
        dataaddress = sessionaddress2.getDataAddress();
        controladdress = sessionaddress2.getControlAddress();
        if (dataaddress.isMulticastAddress()
                || controladdress.isMulticastAddress()
                || isBroadcast(dataaddress) || isBroadcast(controladdress))
        {
            throw new InvalidSessionAddressException(
                    "Remote Address must be UNICAST IP addresses");
        }
        CheckRTPAddress(dataaddress, controladdress);
        cleaner = new SSRCCacheCleaner(cache, streamSynch);
        if (!nonparticipating && !unicast && cache.ourssrc != null)
        {
            cache.ourssrc.reporter = startParticipating(sessionaddress,
                    sessionaddress1, cache.ourssrc, rtcprawreceiver.socket);
        }
        started = true;
        return 0;
    }

    private synchronized void stopParticipating(String s, SSRCInfo ssrcinfo)
    {
        if (ssrcinfo.reporter != null)
        {
            ssrcinfo.reporter.close(s);
            ssrcinfo.reporter = null;
        }
    }

    @Override
    public String toString()
    {
        String s;
        if (newRtpInterface)
        {
            int i = 0;
            int j = 0;
            String s1 = "";
            if (localAddress != null)
            {
                i = localAddress.getControlPort();
                j = localAddress.getDataPort();
                s1 = localAddress.getDataHostAddress();
            }
            s = "RTPManager \n\tSSRCCache  " + cache + "\n\tDataport  " + j
                    + "\n\tControlport  " + i + "\n\tAddress  " + s1
                    + "\n\tRTPForwarder  " + rtpForwarder + "\n\tRTPDemux  "
                    + rtpDemultiplexer;
        } else
        {
            s = "RTPSession Manager  \n\tSSRCCache  " + cache
                    + "\n\tDataport  " + dataport + "\n\tControlport  "
                    + controlport + "\n\tAddress  " + dataaddress
                    + "\n\tRTPForwarder  " + rtpForwarder + "\n\tRTPDEmux  "
                    + rtpDemultiplexer;
        }
        return s;
    }

    public void UpdateEncodings(javax.media.protocol.DataSource datasource)
    {
        RTPControlImpl rtpcontrolimpl = (RTPControlImpl) datasource
                .getControl(RTPControl.class.getName());
        if (rtpcontrolimpl != null && rtpcontrolimpl.codeclist != null)
        {
            Integer integer;
            for (Enumeration<?> enumeration = rtpcontrolimpl.codeclist.keys(); enumeration
                    .hasMoreElements(); formatinfo.add(integer.intValue(),
                    (Format) rtpcontrolimpl.codeclist.get(integer)))
            {
                integer = (Integer) enumeration.nextElement();
            }
        }
    }

    private boolean Win32()
    {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
