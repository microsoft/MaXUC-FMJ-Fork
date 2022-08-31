package net.sf.fmj.media.protocol;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

import net.sf.fmj.media.*;

/**
 * This special DataSource is used to prebuild a streaming player before the
 * actual streaming DataSource is not available e.g. RTP.
 */
public class DelegateDataSource extends PushBufferDataSource implements
        Streamable
{
    class DelegateStream implements PushBufferStream, BufferTransferHandler
    {
        Format format;
        PushBufferStream primary;
        BufferTransferHandler th;

        public DelegateStream(Format format)
        {
            this.format = format;
        }

        public boolean endOfStream()
        {
            if (primary != null)
                return primary.endOfStream();
            return false;
        }

        public ContentDescriptor getContentDescriptor()
        {
            if (primary != null)
                return primary.getContentDescriptor();
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        public long getContentLength()
        {
            if (primary != null)
                return primary.getContentLength();
            return LENGTH_UNKNOWN;
        }

        public Object getControl(String controlType)
        {
            if (primary != null)
                return primary.getControl(controlType);
            return null;
        }

        public Object[] getControls()
        {
            if (primary != null)
                return primary.getControls();
            return new Object[0];
        }

        public Format getFormat()
        {
            if (primary != null)
                return primary.getFormat();
            return format;
        }

        public PushBufferStream getPrimary()
        {
            return primary;
        }

        public void read(Buffer buffer) throws IOException
        {
            if (primary != null)
                primary.read(buffer);
            throw new IOException("No data available");
        }

        public void setPrimary(PushBufferStream primary)
        {
            this.primary = primary;
            primary.setTransferHandler(this);
        }

        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            th = transferHandler;
        }

        public void transferData(PushBufferStream stream)
        {
            if (th != null)
                th.transferData(stream);
        }
    }

    protected String contentType = ContentDescriptor.RAW;
    protected PushBufferDataSource primary;

    protected DelegateStream streams[];
    protected boolean started = false;

    protected boolean connected = false;

    public DelegateDataSource(Format format[])
    {
        streams = new DelegateStream[format.length];
        for (int i = 0; i < format.length; i++)
        {
            streams[i] = new DelegateStream(format[i]);
        }
        try
        {
            connect();
        } catch (IOException e)
        {
        }
    }

    @Override
    public void connect() throws IOException
    {
        if (connected)
            return;
        if (primary != null)
            primary.connect();
        connected = true;
    }

    @Override
    public void disconnect()
    {
        try
        {
            if (started)
                stop();
        } catch (IOException e)
        {
        }
        if (primary != null)
            primary.disconnect();
        connected = false;
    }

    @Override
    public String getContentType()
    {
        if (!connected)
        {
            System.err.println("Error: DataSource not connected");
            return null;
        }
        return contentType;
    }

    @Override
    public Object getControl(String controlType)
    {
        if (primary != null)
            return primary.getControl(controlType);
        return null;
    }

    @Override
    public Object[] getControls()
    {
        if (primary != null)
            return primary.getControls();
        return new Object[0];
    }

    @Override
    public Time getDuration()
    {
        if (primary != null)
            return primary.getDuration();
        return Duration.DURATION_UNKNOWN;
    }

    @Override
    public MediaLocator getLocator()
    {
        if (primary != null)
            return primary.getLocator();
        return null;
    }

    public javax.media.protocol.DataSource getPrimary()
    {
        return primary;
    }

    @Override
    public PushBufferStream[] getStreams()
    {
        return streams;
    }

    public boolean isPrefetchable()
    {
        return false;
    }

    public void setPrimary(PushBufferDataSource ds) throws IOException
    {
        primary = ds;

        PushBufferStream mstrms[] = ds.getStreams();
        for (int i = 0; i < mstrms.length; i++)
        {
            for (int j = 0; j < streams.length; j++)
            {
                if (streams[j].getFormat().matches(mstrms[i].getFormat()))
                    streams[j].setPrimary(mstrms[i]);
            }
        }

        for (int i = 0; i < mstrms.length; i++)
        {
            if (streams[i].getPrimary() == null)
            {
                Log.error("DelegateDataSource: cannot not find a matching track from the primary with this format: "
                        + streams[i].getFormat());
            }
        }

        if (connected)
            primary.connect();
        if (started)
            primary.start();
    }

    @Override
    public void start() throws IOException
    {
        // we need to throw error if connect() has not been called
        if (!connected)
            throw new java.lang.Error(
                    "DataSource must be connected before it can be started");
        if (started)
            return;
        if (primary != null)
            primary.start();
        started = true;
    }

    // ///////////////////
    //
    // INNER CLASSES
    // ///////////////////
    @Override
    public void stop() throws IOException
    {
        if ((!connected) || (!started))
            return;
        if (primary != null)
            primary.stop();
        started = false;
    }
}
