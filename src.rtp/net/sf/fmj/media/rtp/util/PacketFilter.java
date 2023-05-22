// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp.util;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.media.rtp.*;

public abstract class PacketFilter implements PacketSource, PacketConsumer
{
    private static final Logger logger = Logger.getLogger(PacketFilter.class.getName());
    PacketSource source;
    PacketConsumer consumer;
    @SuppressWarnings("rawtypes")
    public Vector destAddressList;
    public Vector<SessionAddress> peerlist;
    public boolean control;

    public PacketFilter()
    {
        destAddressList = null;
        peerlist = null;
        control = false;
    }

    public void close()
    {
    }

    @Override
    public void closeConsumer()
    {
        close();
        if (consumer != null)
            consumer.closeConsumer();
    }

    @Override
    public void closeSource()
    {
        close();
        if (source != null)
            source.closeSource();
    }

    @Override
    public String consumerString()
    {
        if (consumer == null)
            return filtername();
        else
            return filtername() + " connected to " + consumer.consumerString();
    }

    public String filtername()
    {
        return getClass().getName();
    }

    public PacketConsumer getConsumer()
    {
        return consumer;
    }

    public Vector<SessionAddress> getDestList()
    {
        return null;
    }

    public PacketSource getSource()
    {
        return source;
    }

    public abstract Packet handlePacket(Packet packet);

    public abstract Packet handlePacket(Packet packet, int i);

    public abstract Packet handlePacket(Packet packet,
            SessionAddress sessionaddress);

    @Override
    public Packet receiveFrom() throws IOException
    {
        Packet p = null;
        Packet rawp = source.receiveFrom();
        if (rawp != null)
            p = handlePacket(rawp);
        return p;
    }

    @Override
    public void sendTo(Packet p) throws IOException
    {
        Packet origpacket = p;
        if (peerlist != null)
        {
            p = handlePacket(origpacket);
            for (int i = 0; i < peerlist.size(); i++)
            {
                SessionAddress a = (SessionAddress) peerlist.elementAt(i);
                if (!control)
                {
                    ((UDPPacket) p).remoteAddress = a.getDataAddress();
                    ((UDPPacket) p).remotePort = a.getDataPort();
                } else
                {
                    ((UDPPacket) p).remoteAddress = a.getControlAddress();
                    ((UDPPacket) p).remotePort = a.getControlPort();
                }
                if (p != null && consumer != null)
                    consumer.sendTo(p);
            }

        } else if (destAddressList != null)
        {
            for (int i = 0; i < destAddressList.size(); i++)
            {
                SessionAddress sa = (SessionAddress) destAddressList
                        .elementAt(i);
                p = handlePacket(origpacket, sa);
                if (p != null && consumer != null)
                    consumer.sendTo(p);
            }

        } else if (destAddressList == null)
        {
            p = handlePacket(p);
            if (p != null && consumer != null)
                consumer.sendTo(p);
        }
    }

    public void setConsumer(PacketConsumer c)
    {
        logger.info("Updating consumer to " + c);
        consumer = c;
    }

    public void setSource(PacketSource s)
    {
        source = s;
    }

    @Override
    public String sourceString()
    {
        if (source == null)
            return filtername();
        else
            return filtername() + " attached to " + source.sourceString();
    }
}
