// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp;

import javax.media.*;

import net.sf.fmj.media.*;
import net.sf.fmj.media.rtp.util.*;

/**
 *  Used by the RTPReceiver to convert an RTPPacket into a buffer.
 *
 *  This class uses a single buffer which is added to a DataSource from the
 * SSRCInfo passed in with the RTPPacket.
 */
public class RTPDemultiplexer
{
    private RTPRawReceiver rtpr;
    private Buffer buffer;
    private StreamSynch streamSynch;

    public RTPDemultiplexer(SSRCCache c, RTPRawReceiver r,
            StreamSynch streamSynch)
    {
        Log.objectCreated(this, "RTPDemultiplexer");
        rtpr = r;
        this.streamSynch = streamSynch;
        buffer = new Buffer();
    }

    public void demuxpayload(SourceRTPPacket sp)
    {
        SSRCInfo info = sp.ssrcinfo;
        RTPPacket rtpPacket = sp.p;
        info.payloadType = rtpPacket.payloadType;

        if (info.dstream != null)
        {
            buffer.setData(rtpPacket.base.data);
            buffer.setFlags(0);
            if (rtpPacket.marker == 1)
                buffer.setFlags(buffer.getFlags() | Buffer.FLAG_RTP_MARKER);
            buffer.setLength(rtpPacket.payloadlength);
            buffer.setOffset(rtpPacket.payloadoffset);

            long ts = streamSynch.calcTimestamp(info.ssrc, rtpPacket.payloadType,
                    rtpPacket.timestamp);
            buffer.setTimeStamp(ts);

            buffer.setFlags(buffer.getFlags() | Buffer.FLAG_RTP_TIME);
            buffer.setSequenceNumber(rtpPacket.seqnum);
            buffer.setFormat(info.dstream.getFormat());
            info.dstream.add(buffer, true, rtpr);
        }
    }
}
