package net.sf.fmj.media.protocol;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.sf.fmj.media.util.*;

public class CloneableSourceStreamAdapter
{
    class PullBufferStreamAdapter extends SourceStreamAdapter implements
            PullBufferStream
    {
        public javax.media.Format getFormat()
        {
            return ((PullBufferStream) primary).getFormat();
        }

        public void read(Buffer buffer) throws IOException
        {
            copyAndRead(buffer);
        }

        public boolean willReadBlock()
        {
            return ((PullBufferStream) primary).willReadBlock();
        }
    }

    class PullSourceStreamAdapter extends SourceStreamAdapter implements
            PullSourceStream
    {
        public int read(byte[] buffer, int offset, int length)
                throws IOException
        {
            return copyAndRead(buffer, offset, length);
        }

        public boolean willReadBlock()
        {
            return ((PullSourceStream) primary).willReadBlock();
        }
    }

    class PushBufferStreamAdapter extends SourceStreamAdapter implements
            PushBufferStream, BufferTransferHandler
    {
        BufferTransferHandler handler;

        public javax.media.Format getFormat()
        {
            return ((PushBufferStream) primary).getFormat();
        }

        public void read(Buffer buffer) throws IOException
        {
            copyAndRead(buffer);
        }

        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            handler = transferHandler;
            ((PushBufferStream) primary).setTransferHandler(this);
        }

        public void transferData(PushBufferStream stream)
        {
            if (handler != null)
                handler.transferData(this);
        }
    }

    class PushBufferStreamRepeater extends PushStreamRepeater implements
            PushBufferStream, Runnable
    {
        BufferTransferHandler handler;
        private Buffer b;

        public javax.media.Format getFormat()
        {
            if (primary instanceof PullBufferStream)
                return ((PullBufferStream) primary).getFormat();
            if (primary instanceof PushBufferStream)
                return ((PushBufferStream) primary).getFormat();
            return null;
        }

        BufferTransferHandler getTransferHandler()
        {
            return handler;
        }

        public synchronized void read(Buffer buffer) throws IOException
        {
            // block till we have a buffer to read from
            while (b == null && connected)
            {
                try
                {
                    wait(50);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace(System.err);
                }
            }

            if (!connected)
                throw new IOException("DataSource is not connected");

            buffer.copy(b);
            b = null;
        }

        /**
         * Implementation of Runnable interface.
         */
        public void run()
        {
            while (!endOfStream() && connected)
            {
                try
                {
                    synchronized (this)
                    {
                        wait(); // till we will be notified that a read occurred
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace(System.err);
                }
                if (connected && handler != null)
                    handler.transferData(this);
            }
        }

        /**
         * Set the buffer this stream can provide for the next read
         */
        synchronized void setBuffer(Buffer b)
        {
            this.b = b;
            notifyAll();
        }

        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            handler = transferHandler;
        }
    }

    class PushSourceStreamAdapter extends SourceStreamAdapter implements
            PushSourceStream, SourceTransferHandler
    {
        SourceTransferHandler handler;

        public int getMinimumTransferSize()
        {
            return ((PushSourceStream) primary).getMinimumTransferSize();
        }

        public int read(byte[] buffer, int offset, int length)
                throws IOException
        {
            return copyAndRead(buffer, offset, length);
        }

        public void setTransferHandler(SourceTransferHandler transferHandler)
        {
            handler = transferHandler;
            ((PushSourceStream) primary).setTransferHandler(this);
        }

        public void transferData(PushSourceStream stream)
        {
            if (handler != null)
                handler.transferData(this);
        }
    }

    class PushSourceStreamRepeater extends PushStreamRepeater implements
            PushSourceStream, Runnable
    {
        SourceTransferHandler handler;
        private byte[] buffer;

        public int getMinimumTransferSize()
        {
            return
                (primary instanceof PushSourceStream)
                    ? ((PushSourceStream) primary).getMinimumTransferSize()
                    : 0;
        }

        SourceTransferHandler getTransferHandler()
        {
            return handler;
        }

        public synchronized int read(byte[] buffer, int offset, int length)
                throws IOException
        {
            if (length + offset > buffer.length)
                throw new IOException("buffer is too small");

            // block till we have a buffer to read from
            while (this.buffer == null && connected)
            {
                try
                {
                    wait(50);
                }
                catch (InterruptedException e)
                {
                    System.out.println("Exception: " + e);
                }
            }

            if (!connected)
                throw new IOException("DataSource is not connected");

            int copyLength = (length > this.buffer.length ? this.buffer.length
                    : length);
            System.arraycopy(this.buffer, 0, buffer, offset, copyLength);
            this.buffer = null;

            return copyLength;
        }

        /**
         * Implementation of Runnable interface.
         */
        public void run()
        {
            while (!endOfStream() && connected)
            {
                try
                {
                    synchronized (this)
                    {
                        wait(); // till we will be notified that a read occurred
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace(System.err);
                }

                if (connected && handler != null)
                    handler.transferData(this);
            }
        }

        /**
         * Set the buffer this stream can provide for the next read
         */
        synchronized void setBuffer(byte[] buffer)
        {
            this.buffer = buffer;
            notifyAll();
        }

        public void setTransferHandler(SourceTransferHandler transferHandler)
        {
            handler = transferHandler;
        }
    }

    abstract class PushStreamRepeater extends SourceStreamAdapter implements
            SourceStreamRepeater, Runnable
    {
        MediaThread notifyingThread;
        boolean connected = false;

        public synchronized void connect()
        {
            if (connected)
                return;

            connected = true;

            notifyingThread = new MediaThread(this);
            if (notifyingThread != null)
            {
                if (this instanceof PushBufferStream)
                {
                    if (((PushBufferStream) this).getFormat()
                            instanceof VideoFormat)
                        notifyingThread.useVideoPriority();
                    else
                        notifyingThread.useAudioPriority();
                }
                notifyingThread.start(); // You don't need permission for start
            }
        }

        public synchronized void disconnect()
        {
            connected = false;
            notifyAll();
        }
    }

    class SourceStreamAdapter implements SourceStream
    {
        public boolean endOfStream()
        {
            return primary.endOfStream();
        }

        public ContentDescriptor getContentDescriptor()
        {
            return primary.getContentDescriptor();
        }

        public long getContentLength()
        {
            return primary.getContentLength();
        }

        public Object getControl(String controlType)
        {
            return primary.getControl(controlType);
        }

        public Object[] getControls()
        {
            return primary.getControls();
        }
    }

    SourceStream primary;

    SourceStream adapter = null;

    // //////////////////////////
    //
    // INNER CLASSES
    // //////////////////////////

    Vector<SourceStream> repeaters = new Vector<>();

    protected int numTracks = 0;

    protected Format[] trackFormats;

    /**
     * Constructor
     */
    CloneableSourceStreamAdapter(SourceStream primary)
    {
        this.primary = primary;

        // create the matching adapter according to the stream's type
        if (primary instanceof PullSourceStream)
            adapter = new PullSourceStreamAdapter();
        if (primary instanceof PullBufferStream)
            adapter = new PullBufferStreamAdapter();
        if (primary instanceof PushSourceStream)
            adapter = new PushSourceStreamAdapter();
        if (primary instanceof PushBufferStream)
            adapter = new PushBufferStreamAdapter();
    }

    void copyAndRead(Buffer b) throws IOException
    {
        if (primary instanceof PullBufferStream)
            ((PullBufferStream) primary).read(b);
        else if (primary instanceof PushBufferStream)
            ((PushBufferStream) primary).read(b);

        for (Enumeration<SourceStream> e = repeaters.elements(); e.hasMoreElements();)
        {
            Object stream = e.nextElement();
            ((PushBufferStreamRepeater) stream).setBuffer((Buffer) b.clone());
            Thread.yield();
        }
    }

    int copyAndRead(byte[] buffer, int offset, int length) throws IOException
    {
        int totalRead = 0;

        if (primary instanceof PullSourceStream)
            totalRead
                = ((PullSourceStream) primary).read(buffer, offset, length);
        else if (primary instanceof PushSourceStream)
            totalRead
                = ((PushSourceStream) primary).read(buffer, offset, length);

        for (Enumeration<SourceStream> e = repeaters.elements(); e.hasMoreElements();)
        {
            Object stream = e.nextElement();
            byte[] copyBuffer = new byte[totalRead];
            System.arraycopy(buffer, offset, copyBuffer, 0, totalRead);
            ((PushSourceStreamRepeater) stream).setBuffer(copyBuffer);
        }

        return totalRead;
    }

    /**
     * This method should be could only by the <tt>CloneableDataSource</tt>.
     *
     * @return a repeater <tt>SourceStream</tt> which will either a
     *         <tt>PushSourceStream</tt> or a <tt>PushBufferStream.
     */
    SourceStream createRepeater()
    {
        SourceStream repeater = null;

        if ((primary instanceof PullSourceStream)
                || (primary instanceof PushSourceStream))
            repeater = new PushSourceStreamRepeater();
        else if ((primary instanceof PullBufferStream)
                || (primary instanceof PushBufferStream))
            repeater = new PushBufferStreamRepeater();
        repeaters.addElement(repeater);

        return repeater;
    }

    /**
     * Return the stream adapter to be used by the Handler. There is only one
     * adapter per stream since there is only one primary stream.
     */
    SourceStream getAdapter()
    {
        return adapter;
    }
}
