/*
 * This class does the work of writing a file to disk.
 *
 * The mechanism is that the "run" method writes the bytes to disk, and waits
 * for transferData to put bytes into the buffer variables.  The majority of
 * the complexity is the transfer of data between the caller and the writing
 * thread.  Also, there is pleanty of complexity organising calls to
 * "open/close/seek" and the writing thread.
 *
 */

// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.datasink.file;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.Seekable;
import javax.media.protocol.SourceStream;
import javax.media.protocol.SourceTransferHandler;

import net.sf.fmj.media.Log;
import net.sf.fmj.media.Syncable;
import net.sf.fmj.media.datasink.BasicDataSink;
import net.sf.fmj.media.datasink.RandomAccess;

public class Handler extends BasicDataSink implements SourceTransferHandler,
        Seekable, Runnable, RandomAccess, Syncable
{
    private static final boolean DEBUG = false;

    protected static final int NOT_INITIALIZED = 0;
    protected static final int OPENED = 1;
    protected static final int STARTED = 2;
    // protected static final int STOPPED = 3;
    protected static final int CLOSED = 3;

    protected int state = NOT_INITIALIZED;

    protected DataSource source;
    protected SourceStream[] streams;
    protected SourceStream stream;
    protected boolean push;

    protected boolean errorEncountered = false;
    protected String errorReason = null;
    private   Throwable closeCallerStack = null;

    protected Control[] controls;

    protected File file;
    protected File tempFile = null;
    protected RandomAccessFile raFile = null;
    protected RandomAccessFile qtStrRaFile = null;
    protected boolean fileClosed = false;
    protected FileDescriptor fileDescriptor = null;
    protected MediaLocator locator = null;
    protected String contentType = null;
    protected int fileSize = 0; // Good for over 2 Gigabytes
    protected int filePointer = 0;
    protected int bytesWritten = 0;
    protected static final int BUFFER_LEN = 128 * 1024;
    protected boolean syncEnabled = false;

    protected byte[] buffer1 = new byte[BUFFER_LEN];
    protected byte[] buffer2 = new byte[BUFFER_LEN];
    protected boolean buffer1Pending = false;
    protected long buffer1PendingLocation = -1;
    protected int buffer1Length;
    protected boolean buffer2Pending = false;
    protected long buffer2PendingLocation = -1;
    protected int buffer2Length;
    protected long nextLocation = 0;
    protected Thread writeThread = null;
    private Integer bufferLock = new Integer(0);
    private boolean receivedEOS = false;

    public int WRITE_CHUNK_SIZE = 16384;

    private boolean streamingEnabled = false;
    private boolean errorCreatingStreamingFile = false;

    long lastSyncTime = -1;

    public void close()
    {
        close(null); // No Error;
    }

    protected final void close(String reason)
    {
        Log.comment("Closing: " + reason);

        // Store off the caller's stack.  We think someone may be calling
        // close prematurely, so we hope this may help (eventually) tracking
        // down who that is.
        closeCallerStack = new Throwable();

        synchronized (this)
        {
            if (state == CLOSED)
                return;
            setState(CLOSED);
        }

        if (push)
        {
            for (int i = 0; i < streams.length; i++)
            {
                ((PushSourceStream) streams[i]).setTransferHandler(null);
            }
        }

        if (reason != null)
        {
            errorEncountered = true;
            sendDataSinkErrorEvent(reason);
            // Wake up the write thread
            synchronized (bufferLock)
            {
                bufferLock.notifyAll();
            }
        }

        // Wait for all buffers to be written
        /*
         * synchronized (bufferLock) { while (reason == null && (buffer2Pending
         * || buffer1Pending)) { try { bufferLock.wait(250); } catch
         * (InterruptedException ie) { } } }
         */

        try
        {
            source.stop();
        } catch (IOException e)
        {
            System.err.println("IOException when stopping source " + e);
        }

        try
        {
            if (raFile != null)
            {
                raFile.close();
            }

            if (streamingEnabled)
            {
                if (qtStrRaFile != null)
                {
                    qtStrRaFile.close();
                }
            }

            // Disconnect the data source
            if (source != null)
                source.disconnect();
            // //////////////////////////////////////////////////////////

            if (streamingEnabled && (tempFile != null))
            {
                // Delete the temp file if no errors creating streamable file.
                // If errors creating streamable file, delete streamable file.
                if (!errorCreatingStreamingFile)
                {
                    boolean status = deleteFile(tempFile);
                } else
                {
                    boolean status = deleteFile(file);
                }
            }
            // fileClosed = true;
            // sendEndofStreamEvent();
        } catch (IOException e)
        {
            System.out.println("close: " + e);
        }

        raFile = null; // Should be done after setting the state to CLOSED
        qtStrRaFile = null;

        removeAllListeners();
    }

    private boolean deleteFile(File file)
    {
        Log.comment("Deleting file");
        boolean fileDeleted = false;
        try
        {
            fileDeleted = file.delete();
        } catch (Throwable e)
        {
        }
        return fileDeleted;
    }

    public long doSeek(long where)
    {
        Log.comment("Seeking to " + where);
        if (raFile != null)
        {
            try
            {
                raFile.seek(where);
                filePointer = (int) where;
                return where;
            } catch (IOException ioe)
            {
                close("Error in seek: " + ioe);
            }
        }
        return -1;
    }

    public long doTell()
    {
        if (raFile != null)
        {
            try
            {
                return raFile.getFilePointer();
            } catch (IOException ioe)
            {
                close("Error in tell: " + ioe);
            }
        }
        return -1;
    }

    public String getContentType()
    {
        return contentType;
    }

    public Object getControl(String controlName)
    {
        return null;
    }

    public Object[] getControls()
    {
        if (controls == null)
        {
            controls = new Control[0];
        }
        return controls;
    }

    public MediaLocator getOutputLocator()
    {
        return locator;
    }

    public boolean isRandomAccess()
    {
        return true;
    }

    public void open() throws IOException, SecurityException
    {
        Log.comment("Opening");
        closeCallerStack = null;

        try
        {
            if (state == NOT_INITIALIZED)
            {
                if (locator != null)
                {
                    String pathName = locator.getRemainder(); // getFileName(locator);
                    // Strip off excess /'s
                    while (pathName.charAt(0) == '/'
                            && (pathName.charAt(1) == '/' || pathName.charAt(2) == ':'))
                    {
                        pathName = pathName.substring(1);
                    }
                    String fileSeparator = System.getProperty("file.separator");
                    if (fileSeparator.equals("\\"))
                    {
                        pathName = pathName.replace('/', '\\');
                    }
                    // Path sanitised from PII - used for logs
                    String sanitisedPath = sanitiseFilePath(pathName);

                    // In jdk1.2, RandomAccessFile has a useful method called
                    // setLength() which can be used to truncate an existing
                    // file. But this won't work on jdk1.1. So we have to
                    // delete a file if it exists

                    // On Windows, you cannot delete a file if some process
                    // is using it.
                    //
                    Log.comment("Path=" + sanitisedPath);

                    file = new File(pathName);
                    if (file.exists())
                    {
                        Log.comment("File exists, deleting");
                        if (!deleteFile(file))
                        {
                            System.err.println("datasink open: Existing file "
                                    + sanitisedPath
                                    + " cannot be deleted. Check if "
                                    + "some other process is using "
                                    + " this file");
                            if (push)
                                ((PushSourceStream) stream)
                                        .setTransferHandler(null);
                            throw new IOException("Existing file " + sanitisedPath
                                    + " cannot be deleted");
                        }
                    }

                    String parent = file.getParent();
                    if (parent != null)
                    {
                        new File(parent).mkdirs();
                    }
                    try
                    {
                        if (!streamingEnabled)
                        {
                            raFile = new RandomAccessFile(file, "rw");
                            fileDescriptor = raFile.getFD();
                        } else
                        {
                            String fileqt;
                            int index;
                            if ((index = pathName.lastIndexOf(".")) > 0)
                            {
                                // TODO: maybe $$$ add random string
                                fileqt = pathName.substring(0, index)
                                        + ".nonstreamable"
                                        + pathName.substring(index,
                                                pathName.length());
                            } else
                            {
                                // TODO Maybe add random string?
                                // TODO Don't assume .mov if the file doesn't
                                // have an extension. Try to use content type if
                                // possible to guess the extension. However,
                                // extensions will be there as FMJ provides it
                                // even if user doesn't.
                                fileqt = file + ".nonstreamable.mov";
                            }
                            tempFile = new File(fileqt);

                            raFile = new RandomAccessFile(tempFile, "rw");
                            fileDescriptor = raFile.getFD();
                            qtStrRaFile = new RandomAccessFile(file, "rw");
                        }
                    } catch (IOException e)
                    {
                        // Catch the exception for debugging purpose and
                        // throw it again
                        Log.comment("IO Exception " + e);
                        System.err
                                .println("datasink open: IOException when creating RandomAccessFile "
                                        + sanitisedPath + " : " + e);
                        if (push)
                            ((PushSourceStream) stream)
                                    .setTransferHandler(null);
                        throw e;
                    }

                    setState(OPENED);
                }
            }
        } finally
        {
            if ((state == NOT_INITIALIZED) && (stream != null))
            {
                ((PushSourceStream) stream).setTransferHandler(null);
            }
        }
    }

    /**
     * Removes PII from any filepath(s) in a string by removing the username.
     *
     * Eg "C:\Users\USERNAME\folder;C:\Users\USERNAME" will be
     * replaced with "C:\Users\<redacted>\folder;C:\Users\<redacted>"
     *
     * If passed something where the username is the last part of the filepath, any following
     * information will be eaten up to the next delimiter
     * example: "C:\Users\USERNAME is a file path" will become "C:\Users\<redacted>"
     *
     * @param stringToSanitise The string to be treated.
     * @return A string with the relevant PII removed
     */
    private String sanitiseFilePath(String stringToSanitise)
    {
        if (stringToSanitise == null) {
            return null;
        }
        // replace anything following the string "users/" or "users\" (case
        // insensitive) up to the next /\;:
        // \\\\ makes one \ as you have to escape \ once in regex, and each \
        // once in java
        return stringToSanitise.replaceAll("(?i)(?<=users[/\\\\])[^/\\\\;:]+", "<redacted>");
    }

    // Asynchronous write thread
    public void run()
    {
        Log.comment("Running");
        while (!(state == CLOSED || errorEncountered))
        {
            synchronized (bufferLock)
            {
                // Wait for some data or error
                while (!buffer1Pending && !buffer2Pending && !errorEncountered
                        && state != CLOSED && !receivedEOS)
                {
                    if (DEBUG)
                        System.err.println("Waiting for filled buffer");
                    try
                    {
                        bufferLock.wait(500);
                    } catch (InterruptedException ie)
                    {
                    }
                    if (DEBUG)
                        System.err.println("Consumer notified");
                }
            }
            // Something's pending
            if (buffer2Pending)
            {
                if (DEBUG)
                    System.err.println("Writing Buffer2");
                // write that first
                write(buffer2, buffer2PendingLocation, buffer2Length);
                if (DEBUG)
                    System.err.println("Done writing Buffer2");
                buffer2Pending = false;
            }

            synchronized (bufferLock)
            {
                if (buffer1Pending)
                {
                    byte[] tempBuffer = buffer2;
                    buffer2 = buffer1;
                    buffer2Pending = true;
                    buffer2PendingLocation = buffer1PendingLocation;
                    buffer2Length = buffer1Length;
                    buffer1Pending = false;
                    buffer1 = tempBuffer;
                    if (DEBUG)
                        System.err.println("Notifying producer");
                    bufferLock.notifyAll();
                } else
                {
                    if (receivedEOS)
                        break;
                }
            }
        }

        Log.comment("Exitted loop state=" + state +
                     ", error=" + errorEncountered +
                     ", receivedEOS=" + receivedEOS);

        if (receivedEOS)
        {
            if (DEBUG)
                System.err.println("Sending EOS: streamingEnabled is "
                        + streamingEnabled);
            // Close the file and flag it
            if (raFile != null)
            {
                if (!streamingEnabled)
                {
                    try
                    {
                        raFile.close();
                    } catch (IOException ioe)
                    {
                    }
                    raFile = null;
                }
                fileClosed = true;
            }
            if (!streamingEnabled)
            {
                sendEndofStreamEvent();
            }
        }
        if (errorEncountered && state != CLOSED)
        {
            close(errorReason);
        }
    }

    public synchronized long seek(long where)
    {
        nextLocation = where;
        return where;
    }

    public void setEnabled(boolean b)
    {
        streamingEnabled = b;
    }

    // TODO : Handle pull data source

    /**
     * Set the output <tt>MediaLocator</tt>. This method should only be called
     * once; an error is thrown if the locator has already been set.
     *
     * @param output
     *            <tt>MediaLocator</tt> that describes where the output goes.
     */
    public void setOutputLocator(MediaLocator output)
    {
        locator = output;
    }

    public void setSource(DataSource ds) throws IncompatibleSourceException
    {
        if (!(ds instanceof PushDataSource) && !(ds instanceof PullDataSource))
        {
            throw new IncompatibleSourceException("Incompatible datasource");
        }
        source = ds;

        if (source instanceof PushDataSource)
        {
            push = true;
            try
            {
                ((PushDataSource) source).connect();
            } catch (IOException ioe)
            {
            }
            streams = ((PushDataSource) source).getStreams();
        } else
        {
            push = false;
            try
            {
                ((PullDataSource) source).connect();
            } catch (IOException ioe)
            {
            }
            streams = ((PullDataSource) source).getStreams();
        }

        if (streams == null || streams.length != 1)
            throw new IncompatibleSourceException(
                    "DataSource should have 1 stream");
        stream = streams[0];

        contentType = source.getContentType();
        if (push)
            ((PushSourceStream) stream).setTransferHandler(this);
    }

    protected void setState(int state)
    {
        synchronized (this)
        {
            this.state = state;
        }
    }

    public void setSyncEnabled()
    {
        syncEnabled = true;
    }

    public void start() throws IOException
    {
        if (state == OPENED)
        {
            if (source != null)
                source.start();
            if (writeThread == null)
            {
                writeThread = new Thread(this);
                writeThread.start();
            }
            setState(STARTED);
        }
    }

    /**
     * Stop the data-transfer. If the source has not been connected and started,
     * <tt>stop</tt> does nothing.
     */
    public void stop() throws IOException
    {
        if (state == STARTED)
        {
            if (source != null)
                source.stop();
            setState(OPENED);
        }
    }

    public long tell()
    {
        return nextLocation;
    }

    public synchronized void transferData(PushSourceStream pss)
    {
        int totalRead = 0;
        int spaceAvailable = BUFFER_LEN;
        int bytesRead = 0;

        // The way this class works is that we, in this method, want to put
        // bytes in the buffers for run to send.  That is running on another
        // thread, so we will wait for it, if the buffers are currently full.
        //
        // However, there are windows in which run has terminated, in which
        // case waiting for it is not a terribly sensible thing to do.  Not
        // least because we may be on the EDT, which would hang the whole app.
        //
        // So we return if there has been an error (generated inside this
        // class), that's easy.
        //
        // But if someone has already called close, then it's a bit more
        // interesting.  We may, for example be trying to write the footer
        // bytes for the file.  If that is the case, then we'd very much like
        // to know who has closed the file before we are done.  So the plan is
        //   - to return if someone has already closed the Handler
        //   - but not before tracing out the stack of the caller to close to
        //     try to track down any potential premature closer.
        //
        if (errorEncountered)
        {
            Log.comment("Error previously encountered, returning");
            return;
        }

        if (state == CLOSED)
        {
            Log.comment("Someone previously called close!");

            if (closeCallerStack != null)
            {
                Log.dumpStack("Our stack, and then closer stack...");
                Log.dumpStack(closeCallerStack);
            }
            return;
        }

        if (buffer1Pending)
        {
            synchronized (bufferLock)
            {
                while (buffer1Pending)
                {
                    if (DEBUG)
                        System.err.println("Waiting for free buffer");
                    try
                    {
                        bufferLock.wait();
                    } catch (InterruptedException ie)
                    {
                    }
                }
            }
            if (DEBUG)
                System.err.println("Got free buffer");
        }

        // System.err.println("In transferData()");
        while (spaceAvailable > 0)
        {
            try
            {
                bytesRead = pss.read(buffer1, totalRead, spaceAvailable);
                // System.err.println("bytesRead = " + bytesRead);
                if (bytesRead > 16 * 1024 && WRITE_CHUNK_SIZE < 32 * 1024)
                {
                    if (bytesRead > 64 * 1024 && WRITE_CHUNK_SIZE < 128 * 1024)
                        WRITE_CHUNK_SIZE = 128 * 1024;
                    else if (bytesRead > 32 * 1024
                            && WRITE_CHUNK_SIZE < 64 * 1024)
                        WRITE_CHUNK_SIZE = 64 * 1024;
                    else if (WRITE_CHUNK_SIZE < 32 * 1024)
                        WRITE_CHUNK_SIZE = 32 * 1024;
                    // System.err.println("Increasing buffer to " +
                    // WRITE_CHUNK_SIZE);
                }
            } catch (IOException ioe)
            {
                // What to do here?
            }
            if (bytesRead <= 0)
            {
                break;
            }
            totalRead += bytesRead;
            spaceAvailable -= bytesRead;
        }

        if (totalRead > 0)
        {
            synchronized (bufferLock)
            {
                buffer1Pending = true;
                buffer1PendingLocation = nextLocation;
                buffer1Length = totalRead;
                nextLocation = -1; // assume next write is contiguous unless
                                   // seeked
                // Notify availability to write thread
                if (DEBUG)
                    System.err.println("Notifying consumer");
                bufferLock.notifyAll();
            }
        }
        // Send EOS if necessary
        if (bytesRead == -1)
        {
            if (DEBUG)
                System.err.println("Got EOS");
            receivedEOS = true;
            // Wait until file is closed. This makes the Processor's close
            // call to force the data sink to close the file, just in case
            // the user doesn't remember to close the datasink before exiting.
            while (!fileClosed && !errorEncountered && !(state == CLOSED))
            {
                try
                {
                    Thread.sleep(50);
                } catch (InterruptedException ie)
                {
                }
            }
        }
    }

    private void write(byte[] buffer, long location, int length)
    {
        int offset, toWrite;
        try
        {
            if (location != -1)
                doSeek(location);
            offset = 0;
            while (length > 0)
            {
                toWrite = WRITE_CHUNK_SIZE;
                if (length < toWrite)
                    toWrite = length;
                raFile.write(buffer, offset, toWrite);
                bytesWritten += toWrite;

                // Sync/Flush after a few write so that the
                // file writing is smooth. Improves capture smoothness

                /*
                 * if (fileDescriptor != null) { // Sync'ing the file system at
                 * every 1 sec interval. if (lastSyncTime < 0) lastSyncTime =
                 * System.currentTimeMillis(); else { long ts =
                 * System.currentTimeMillis(); if (ts - lastSyncTime > 1000L) {
                 * fileDescriptor.sync();
                 * //System.err.println("sync: byte written: " + bytesWritten);
                 * bytesWritten = 0; lastSyncTime = ts; } } }
                 */

                if (fileDescriptor != null && syncEnabled
                        && bytesWritten >= WRITE_CHUNK_SIZE)
                {
                    bytesWritten -= WRITE_CHUNK_SIZE;
                    fileDescriptor.sync();
                }

                filePointer += toWrite;
                length -= toWrite;
                offset += toWrite;
                if (filePointer > fileSize)
                    fileSize = filePointer;
                Thread.yield();
            }
        }
        catch (IOException ioe)
        {
            Log.comment("Hit IO exception writing: " + ioe);
            errorEncountered = true;
            errorReason = ioe.toString();
        }
    }

    // write chunk
    // Call with -1, -1 when done writing chunks to streamable file
    // Call with -1, (num > 0) to seek to num-1 and write 1 byte
    public boolean write(long inOffset, int numBytes)
    {
        try
        {
            if ((inOffset >= 0) && (numBytes > 0))
            {
                int remaining = numBytes;
                int bytesToRead;
                raFile.seek(inOffset);

                while (remaining > 0)
                {
                    bytesToRead = (remaining > BUFFER_LEN) ? BUFFER_LEN
                            : remaining;
                    raFile.read(buffer1, 0, bytesToRead); // $$ CAST
                    qtStrRaFile.write(buffer1, 0, bytesToRead); // $$ CAST
                    remaining -= bytesToRead;
                }
            } else if ((inOffset < 0) && (numBytes > 0))
            {
                qtStrRaFile.seek(0);
                qtStrRaFile.seek(numBytes - 1);
                qtStrRaFile.writeByte(0);
                qtStrRaFile.seek(0);
            } else
            {
                sendEndofStreamEvent();
            }
        }
        catch (Exception e)
        {
            Log.comment("Hit exception writing: " + e);
            errorCreatingStreamingFile = true;
            System.err
                    .println("Exception when creating streamable version of media file: "
                            + e.getMessage());
            return false;
        }
        return true;
    }
}
