package net.sf.fmj.media;

/**
 * Audio format encodings in FMJ but not JMF.
 *
 * @author Ken Larson
 *
 */
public class BonusAudioFormatEncodings
{
    public static final String ALAW_RTP = "ALAW/rtp";
    public static final String SPEEX_RTP = "speex/rtp";

    public static final String[] ALL = new String[] { ALAW_RTP, SPEEX_RTP };
}
