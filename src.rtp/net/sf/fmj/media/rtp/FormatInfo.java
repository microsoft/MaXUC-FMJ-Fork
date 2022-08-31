package net.sf.fmj.media.rtp;

import javax.media.*;
import javax.media.format.*;

public final class FormatInfo
{
    private SSRCCache mCache;

    private Format mFormatList[];

    private static final AudioFormat mpegAudio = new AudioFormat("mpegaudio/rtp");

    public FormatInfo()
    {
        mCache = null;
        mFormatList = new Format[111];
        initFormats();
    }

    public void add(int i, Format format)
    {
        if (i >= mFormatList.length)
        {
            expandTable(i);
        }
        Format format1;
        if ((format1 = mFormatList[i]) != null)
            return;
        mFormatList[i] = format;
        if (mCache != null && (format instanceof VideoFormat))
        {
            mCache.clockrate[i] = 0x15f90;
        }
        if (mCache != null && (format instanceof AudioFormat))
        {
            if (mpegAudio.matches(format))
            {
                mCache.clockrate[i] = 0x15f90;
            }
            else
            {
                mCache.clockrate[i] = (int) ((AudioFormat) format)
                        .getSampleRate();
            }
        }
    }

    private void expandTable(int i)
    {
        Format aformat[] = new Format[i + 1];
        for (int j = 0; j < mFormatList.length; j++)
            aformat[j] = mFormatList[j];

        mFormatList = aformat;
    }

    public Format get(int i)
    {
        return i >= mFormatList.length ? null : mFormatList[i];
    }

    public int getPayload(Format format)
    {
        if (format.getEncoding() != null
                && format.getEncoding().equals("g729a/rtp"))
        {
            format = new AudioFormat("g729/rtp");
        }

        for (int i = 0; i < mFormatList.length; i++)
        {
            if (format.matches(mFormatList[i]))
            {
                return i;
            }
        }

        return -1;
    }

    private void initFormats()
    {
        mFormatList[0] = new AudioFormat("ULAW/rtp", 8000D, 8, 1);
        mFormatList[3] = new AudioFormat("gsm/rtp", 8000D, -1, 1);
        mFormatList[4] = new AudioFormat("g723/rtp", 8000D, -1, 1);
        mFormatList[5] = new AudioFormat("dvi/rtp", 8000D, 4, 1);
        mFormatList[8] = new AudioFormat("ALAW/rtp", 8000D, 8, 1);
        mFormatList[14] = new AudioFormat("mpegaudio/rtp", -1D, -1, -1);
        mFormatList[15] = new AudioFormat("g728/rtp", 8000D, -1, 1);
        mFormatList[16] = new AudioFormat("dvi/rtp", 11025D, 4, 1);
        mFormatList[17] = new AudioFormat("dvi/rtp", 22050D, 4, 1);
        mFormatList[18] = new AudioFormat("g729/rtp", 8000D, -1, 1);
        mFormatList[31] = new VideoFormat("h261/rtp");
        mFormatList[32] = new VideoFormat("mpeg/rtp");
        mFormatList[34] = new VideoFormat("h263/rtp");
    }

    public void setCache(SSRCCache cache)
    {
        mCache = cache;
    }
}
