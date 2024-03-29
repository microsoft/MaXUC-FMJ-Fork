/*
 * @(#)RTCPHeader.java
 * Created: 2005-04-21
 * Version: 2-0-alpha
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 * Andrew G D Rowley
 * Christian Vincenot <sipcom@cyberspace7.net>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp;

import java.io.*;
import java.net.*;

/**
 * Represents and parses an RTCP header
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha
 */
public class RTCPHeader
{
    /**
     * The current RTP version
     */
    public static final int VERSION = 2;

    /**
     * The number of bytes to skip for a SDES header
     */
    public static final int SDES_SKIP = 8;

    /**
     * An SDES CNAME header
     */
    public static final int SDES_CNAME = 1;

    /**
     * An SDES NAME header
     */
    public static final int SDES_NAME = 2;

    /**
     * An SDES EMAIL header
     */
    public static final int SDES_EMAIL = 3;

    /**
     * An SDES PHONE header
     */
    public static final int SDES_PHONE = 4;

    /**
     * An SDES LOC header
     */
    public static final int SDES_LOC = 5;

    /**
     * An SDES TOOL header
     */
    public static final int SDES_TOOL = 6;

    /**
     * An SDES NOTE header
     */
    public static final int SDES_NOTE = 7;

    /**
     * The size of the header in bytes
     */
    public static final int SIZE = 8;

    // The masks and shifts of header items
    private static final int VERSION_MASK = 0xc000;

    private static final int VERSION_SHIFT = 14;

    private static final int PADDING_MASK = 0x2000;

    private static final int PADDING_SHIFT = 13;

    private static final int RCOUNT_MASK = 0x1f00;

    private static final int RCOUNT_SHIFT = 8;

    private static final int TYPE_MASK = 0x00ff;

    private static final int TYPE_SHIFT = 0;

    // The first 16 bits
    private int flags;

    // The second 16 bits
    private int length;

    // The third and fourth 16 bits
    private long ssrc;

    /**
     * Creates a new RTCPHeader
     *
     * @param data
     *            The data to read the header from
     * @param offset
     *            The offset in the data to start
     * @param length
     *            The length of the data to read
     * @throws IOException
     *             I/O Exception
     */
    public RTCPHeader(byte data[], int offset, int length) throws IOException
    {
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                data, offset, length));
        // Read the header values
        this.flags = stream.readUnsignedShort();
        this.length = stream.readUnsignedShort();
        this.ssrc = stream.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;

        if (getVersion() != VERSION)
        {
            throw new IOException("Invalid RTCP Version - received was " +
                                getVersion() + ". Must be version " + VERSION);
        }
    }

    /**
     * Creates a new RTCPHeader
     *
     * @param packet
     *            The packet from which to parse the header
     * @throws IOException
     *             I/O Exception
     */
    public RTCPHeader(DatagramPacket packet) throws IOException
    {
        this(packet.getData(), packet.getOffset(), packet.getLength());
    }

    /**
     * Returns the RTCP header flags. This is a 16 bits short integer composed
     * by: . the version number: 2 bits . the padding bit . the reception report
     * count (RC): 5 bits . the packet type (PT): 8 bits
     *
     * @return the header flags (version|P|RC|PT)
     */
    public int getFlags()
    {
        return flags;
    }

    /**
     * Returns the length of the RTCP packet
     *
     * @return The length of the RTCP packet
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Returns the type of RTCP packet (SR || RR)
     *
     * @return The type of the RTCP packet (SR or RR)
     */
    public short getPacketType()
    {
        return (short) ((getFlags() & TYPE_MASK) >> TYPE_SHIFT);
    }

    /**
     * Returns the value of the padding bit, indicating if this individual RTCP
     * packet contains some additional padding octets at the end which are not
     * part of the control information but are included in the length field
     *
     * @return the padding value
     */
    public short getPadding()
    {
        return (short) ((getFlags() & PADDING_MASK) >> PADDING_SHIFT);
    }

    /**
     * Returns the reception report count (RC). This represents the number of
     * reception report blocks contained in this packet. A value of zero is
     * valid.
     *
     * @return The number of reception blocks in the packet (0 is valid)
     */
    public short getReceptionCount()
    {
        return (short) ((getFlags() & RCOUNT_MASK) >> RCOUNT_SHIFT);
    }

    /**
     * Returns the SSRC being described by this packet.
     *
     * @return The ssrc being described
     */
    public long getSsrc()
    {
        return ssrc;
    }

    /**
     * Returns the version of the RTCP packet
     *
     * @return The RTP version implemented
     */
    public short getVersion()
    {
        return (short) ((getFlags() & VERSION_MASK) >> VERSION_SHIFT);
    }

    /**
     * Displays the header
     *
     */
    public void print()
    {
        System.err.println(getVersion() + "|" + getPadding() + "|"
                + getReceptionCount() + "|" + getPacketType() + "|"
                + getLength() + "|" + getSsrc());
    }
}