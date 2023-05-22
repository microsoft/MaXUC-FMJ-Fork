// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp.util;

import java.io.*;
import java.util.logging.*;

import javax.media.rtp.*;

public class RTPPacketSender implements PacketConsumer
{
    private static final Logger logger = Logger.getLogger(RTPPacketSender.class.getName());

    @SuppressWarnings("deprecation")
    RTPPushDataSource dest;
    RTPConnector connector;
    OutputDataStream outstream;

    public RTPPacketSender(OutputDataStream os)
    {
        dest = null;
        connector = null;
        outstream = null;
        outstream = os;
        logger.fine("Created RTP Packet sender for os " + os);
    }

    public RTPPacketSender(RTPConnector connector) throws IOException
    {
        dest = null;
        this.connector = null;
        outstream = null;
        this.connector = connector;
        outstream = connector.getDataOutputStream();
        logger.fine("Created RTP Packet sender for connector " + connector);
    }

    @SuppressWarnings("deprecation")
    public RTPPacketSender(RTPPushDataSource dest)
    {
        this.dest = null;
        connector = null;
        outstream = null;
        this.dest = dest;
        outstream = dest.getInputStream();
        logger.fine("Created RTP Packet sender for dest " + dest);
    }

    @Override
    public void closeConsumer()
    {
    }

    @Override
    public String consumerString()
    {
        String s = "RTPPacketSender for " + dest;
        return s;
    }

    public RTPConnector getConnector()
    {
        return connector;
    }

    @Override
    public void sendTo(Packet p) throws IOException
    {
        if (outstream == null)
        {
            throw new IOException();
        } else
        {
            outstream.write(p.data, 0, p.length);
            return;
        }
    }
}
