package net.sf.fmj.ejmf.toolkit.media;

import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.swing.*;

/**
 * This class provides a CachingControl for the AbstractController class. It
 * provides a progress bar to monitor the media download. Its control Component
 * is simply a button that stops the media download. Whenever there is a change
 * in this BasicCachingControl, a CachingControlEvent is posted automatically.
 *
 * From the book: Essential JMF, Gordon, Talley (ISBN 0130801046). Used with
 * permission.
 *
 * @see net.sf.fmj.ejmf.toolkit.media.AbstractController
 *
 * @author Steve Talley & Rob Gordon
 */
public class BasicCachingControl implements CachingControl
{
    private static final String PAUSEMESSAGE = "Pause";
    private static final String RESUMEMESSAGE = "Resume";

    private boolean isDownloading;
    private boolean isPaused;
    private long length;
    private long progress;
    private JProgressBar progressBar;
    private JButton pauseButton;
    private AbstractController controller;

    /**
     * Constructs a BasicCachingControl for the given AbstractController. Uses
     * the length arg to initialize the progress bar.
     *
     * @param c
     *            The AbstractController from which CachingControlEvents are
     *            posted whenever the status of this BasicCachingControl
     *            changes.
     *
     * @param length
     *            The length of the media to be downloaded.
     */
    public BasicCachingControl(AbstractController c, long length)
    {
        controller = c;
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);

        pauseButton = new JButton(PAUSEMESSAGE);

        pauseButton.addActionListener(new ActionListener()
        {
            /**
             * Stops and deallocates the AbstractController when the stop button
             * is pushed.
             */
            public void actionPerformed(ActionEvent e)
            {
                if (isPaused())
                {
                    pauseButton.setText(PAUSEMESSAGE);
                    setPaused(false);
                } else
                {
                    pauseButton.setText(RESUMEMESSAGE);
                    setPaused(true);
                }

                // Resize button to fit new label
                pauseButton.getParent().validate();
            }
        });

        reset(length);
        controller.addControl(this);
    }

    /**
     * Adds an amount of bytes to the media already downloaded.
     *
     * @param toAdd
     *            Number of bytes that have been downloaded since the last time
     *            this method or setContentProgress have been called.
     */
    public void addToProgress(long toAdd)
    {
        setContentProgress(progress + toAdd);
    }

    /**
     * Blocks the current thread until the download is not paused.
     */
    public synchronized void blockWhilePaused()
    {
        while (isPaused)
        {
            try
            {
                wait();
            } catch (InterruptedException e)
            {
            }
        }
    }

    /**
     * Get the total number of bytes in the media being downloaded. Returns
     * LENGTH_UNKNOWN if this information is not available.
     *
     * @return The media length in bytes, or LENGTH_UNKNOWN.
     */
    public long getContentLength()
    {
        return length;
    }

    /**
     * Get the total number of bytes of media data that have been downloaded so
     * far.
     *
     * @return The number of bytes downloaded.
     */
    public long getContentProgress()
    {
        return progress;
    }

    /**
     * Get a Component that provides additional download control. Returns null
     * if only a progress bar is provided.
     *
     * @return Download control GUI Component.
     */
    public Component getControlComponent()
    {
        return pauseButton;
    }

    /**
     * Get a Component for displaying the download progress.
     *
     * @return Progress bar GUI Component.
     */
    public Component getProgressBarComponent()
    {
        return progressBar;
    }

    /**
     * Check whether or not media is being downloaded.
     */
    public boolean isDownloading()
    {
        return isDownloading;
    }

    /**
     * Tells whether the media download is paused or not.
     */
    public boolean isPaused()
    {
        return isPaused;
    }

    /**
     * Resets this BasicCachingControl. Sets the media length, reinitializes the
     * progress bar, and posts a CachingControlEvent.
     *
     * @param length
     *            The length of the media.
     */
    public synchronized void reset(long length)
    {
        this.length = length;

        progress = 0;
        progressBar.setValue(0);

        setContentLength(length);
        setDownLoading(false);
        setPaused(false);

        // Post CachingControlEvent
        controller
                .postEvent(new CachingControlEvent(controller, this, progress));
    }

    // //////////////////////////////////////////////////////
    //
    // javax.media.CachingControl methods
    //
    // //////////////////////////////////////////////////////

    /**
     * Set the length of the media without reinitializing the progress bar.
     *
     * @param length
     *            The length of the media.
     */
    public synchronized void setContentLength(long length)
    {
        this.length = length;
        if (length == LENGTH_UNKNOWN)
        {
            progressBar.setMaximum(0);
        } else
        {
            progressBar.setMaximum((int) length);
        }
    }

    /**
     * Set the current progress of the media download.
     *
     * @param progress
     *            Number of bytes that have been downloaded.
     */
    public synchronized void setContentProgress(long progress)
    {
        blockWhilePaused();
        this.progress = progress;
        setDownLoading(progress < length);

        progressBar.setValue((int) progress);

        // Post CachingControlEvent
        controller
                .postEvent(new CachingControlEvent(controller, this, progress));

        // Uncomment following line to slow down download enough
        // for this CachingControl to be useful
        // try { Thread.sleep(500); } catch( InterruptedException e) {}
    }

    /**
     * Indicates that the media is fully downloaded.
     */
    public void setDone()
    {
        setContentProgress(length);
    }

    /**
     * Sets whether the AbstractController is downloading or not. Other methods
     * in the BasicCachingControl call this method automatically.
     *
     * @param isDownloading
     *            boolean indicating whether the media is downloading.
     */
    public void setDownLoading(boolean isDownloading)
    {
        this.isDownloading = isDownloading;
        if (!isDownloading)
        {
            setPaused(false);
        }
    }

    /**
     * Sets whether the AbstractController is paused or not.
     *
     * @param isPaused
     *            boolean indicating whether the media is paused.
     */
    protected synchronized void setPaused(boolean isPaused)
    {
        this.isPaused = isPaused;
        notifyAll();
    }
}
