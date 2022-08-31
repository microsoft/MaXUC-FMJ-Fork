package net.sf.fmj.media.format;

import javax.media.*;
import javax.media.format.*;

import net.sf.fmj.media.*;
import net.sf.fmj.utility.*;

/**
 * GIF video format. Used for MGIF, which is like MJPEG but with GIF.
 *
 * @author Ken Larson
 *
 */
public class GIFFormat extends VideoFormat
{
    private static final long serialVersionUID = 0L;

    public GIFFormat()
    {
        super(BonusVideoFormatEncodings.GIF);
        dataType = Format.byteArray;
    }

    public GIFFormat(java.awt.Dimension size, int maxDataLength,
            Class<?> dataType, float frameRate)
    {
        super(BonusVideoFormatEncodings.GIF, size, maxDataLength, dataType,
                frameRate);
    }

    @Override
    public Object clone()
    {
        return new GIFFormat(FormatUtils.clone(size), maxDataLength, dataType,
                frameRate);
    }

    @Override
    public boolean equals(Object format)
    {
        if (!super.equals(format))
            return false;

        if (!(format instanceof GIFFormat))
        {
            return false;
        }

        return true;
    }

    @Override
    public Format intersects(Format other)
    {
        final Format result = super.intersects(other);

        if (other instanceof GIFFormat)
        {
            if (getClass().isAssignableFrom(other.getClass()))
            {
                // "other" was cloned.
            } else if (other.getClass().isAssignableFrom(getClass()))
            { // this was cloned
            }
        }

        return result;
    }

    @Override
    public boolean matches(Format format)
    {
        if (!super.matches(format))
        {
            return false;
        }

        if (!(format instanceof GIFFormat))
        {
            final boolean result = true;
            return result;
        }

        final boolean result = true;

        return result;
    }

    @Override
    public String toString()
    {
        final StringBuffer b = new StringBuffer();
        b.append("GIF video format:");
        if (FormatUtils.specified(size))
            b.append(" size = " + size.width + "x" + size.height);
        if (FormatUtils.specified(frameRate))
            b.append(" FrameRate = " + frameRate);
        if (FormatUtils.specified(maxDataLength))
            b.append(" maxDataLength = " + maxDataLength);
        if (FormatUtils.specified(dataType))
            b.append(" dataType = " + dataType);
        return b.toString();
    }
}
