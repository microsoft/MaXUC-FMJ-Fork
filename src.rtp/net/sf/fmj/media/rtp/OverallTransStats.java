package net.sf.fmj.media.rtp;

import javax.media.rtp.*;

public class OverallTransStats implements GlobalTransmissionStats
{
    protected int rtp_sent;
    protected int bytes_sent;
    protected int rtcp_sent;
    protected int local_coll;
    protected int remote_coll;
    protected int transmit_failed;

    public OverallTransStats()
    {
        rtp_sent = 0;
        bytes_sent = 0;
        rtcp_sent = 0;
        local_coll = 0;
        remote_coll = 0;
        transmit_failed = 0;
    }

    @Override
    public int getBytesSent()
    {
        return bytes_sent;
    }

    @Override
    public int getLocalColls()
    {
        return local_coll;
    }

    @Override
    public int getRemoteColls()
    {
        return remote_coll;
    }

    @Override
    public int getRTCPSent()
    {
        return rtcp_sent;
    }

    @Override
    public int getRTPSent()
    {
        return rtp_sent;
    }

    @Override
    public int getTransmitFailed()
    {
        return transmit_failed;
    }
}
