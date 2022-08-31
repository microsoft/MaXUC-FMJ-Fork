package com.sun.media.format;

import javax.media.*;
import javax.media.format.*;

import net.sf.fmj.utility.*;

/**
 * @author Ken Larson
 */
public class WavAudioFormat extends AudioFormat
{
    private static final long serialVersionUID = 0L;

    protected byte[] codecSpecificHeader;
    private int averageBytesPerSecond = NOT_SPECIFIED;

    public WavAudioFormat(String encoding)
    {
        super(encoding);
    }

    public WavAudioFormat(String encoding, double sampleRate,
            int sampleSizeInBits, int channels, int frameSizeInBits,
            int averageBytesPerSecond, byte[] codecSpecificHeader)
    {
        // averageBytesPerSecond seems to be substituted for frameRate.
        super(encoding, sampleRate, sampleSizeInBits, channels, NOT_SPECIFIED,
                NOT_SPECIFIED, frameSizeInBits,
                averageBytesPerSecond /* frameRate */, byteArray);
        this.averageBytesPerSecond = averageBytesPerSecond;
        this.codecSpecificHeader = codecSpecificHeader;
    }

    public WavAudioFormat(String encoding, double sampleRate,
            int sampleSizeInBits, int channels, int frameSizeInBits,
            int averageBytesPerSecond, int endian, int signed, float frameRate,
            Class<?> dataType, byte[] codecSpecificHeader)
    {
        // averageBytesPerSecond seems to be substituted for frameRate.
        super(encoding, sampleRate, sampleSizeInBits, channels, endian, signed,
                frameSizeInBits, averageBytesPerSecond /* frameRate */,
                dataType);
        this.averageBytesPerSecond = averageBytesPerSecond;
        this.codecSpecificHeader = codecSpecificHeader;
    }

    @Override
    public Object clone()
    {
        return new WavAudioFormat(encoding, sampleRate, sampleSizeInBits,
                channels, frameSizeInBits, averageBytesPerSecond, endian,
                signed, (float) frameRate, dataType, codecSpecificHeader);
    }

    @Override
    protected void copy(Format f)
    {
        super.copy(f);
        final WavAudioFormat oCast = (WavAudioFormat) f; // it has to be a
                                                         // WavAudioFormat, or
                                                         // ClassCastException
                                                         // will be thrown.
        this.averageBytesPerSecond = oCast.averageBytesPerSecond;
        this.codecSpecificHeader = oCast.codecSpecificHeader;
    }

    @Override
    public boolean equals(Object format)
    {
        if (!super.equals(format))
            return false;

        if (!(format instanceof WavAudioFormat))
        {
            return false;
        }

        final WavAudioFormat oCast = (WavAudioFormat) format;
        return this.averageBytesPerSecond == oCast.averageBytesPerSecond
                && this.codecSpecificHeader == oCast.codecSpecificHeader; // TODO:
                                                                          // equals
                                                                          // or
                                                                          // ==
    }

    public int getAverageBytesPerSecond()
    {
        return averageBytesPerSecond;
    }

    public byte[] getCodecSpecificHeader()
    {
        return codecSpecificHeader;
    }

    @Override
    public Format intersects(Format other)
    {
        final Format result = super.intersects(other);

        if (other instanceof WavAudioFormat)
        {
            final WavAudioFormat resultCast = (WavAudioFormat) result;

            final WavAudioFormat oCast = (WavAudioFormat) other;
            if (getClass().isAssignableFrom(other.getClass()))
            {
                // "other" was cloned.

                if (FormatUtils.specified(this.averageBytesPerSecond))
                    resultCast.averageBytesPerSecond = this.averageBytesPerSecond;
                if (FormatUtils.specified(this.codecSpecificHeader))
                    resultCast.codecSpecificHeader = this.codecSpecificHeader;

            } else if (other.getClass().isAssignableFrom(getClass()))
            { // this was cloned

                if (!FormatUtils.specified(resultCast.averageBytesPerSecond))
                    resultCast.averageBytesPerSecond = oCast.averageBytesPerSecond;
                if (!FormatUtils.specified(resultCast.codecSpecificHeader))
                    resultCast.codecSpecificHeader = oCast.codecSpecificHeader;
            }
        }
        return result;
    }

    @Override
    public boolean matches(Format format)
    {
        if (!super.matches(format))
            return false;

        if (!(format instanceof WavAudioFormat))
            return true;

        final WavAudioFormat oCast = (WavAudioFormat) format;

        return FormatUtils.matches(this.averageBytesPerSecond,
                oCast.averageBytesPerSecond)
                && FormatUtils.matches(this.codecSpecificHeader,
                        oCast.codecSpecificHeader);
    }
}
