// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.protocol;

/**
 * This is a special tagging interface to specify whether a DataSource is
 * intended comes from the RTPSessionMgr which gives some additional info.
 */
public interface RTPSource
{
    /**
     * Flush the data buffers.
     */
    public void flush();

    /**
     * Get the cname.
     */
    public String getCNAME();

    /**
     * Get the ssrc.
     */
    public int getSSRC();
}
